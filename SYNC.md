# OS11 Android - Sync Design

> Sync is built into the data layer from day one. The transport is added later.
> No cloud. No accounts. Nothing that costs money to run.
> This file defines the contract both the Android app and the desktop app must implement.
> Where this contract differs from the desktop ARCHITECTURE.md Phase 2 section, the differences are listed in "Desktop-Side Changes Required" at the bottom. They need a desktop ADR before either side ships sync.

---

## 1. Goals and Non-Goals

**Goals**
- Phone and desktop converge to the same data after any sequence of offline edits.
- Works with no internet. Works on the same Wi-Fi. Free.
- Every device is fully usable offline, forever. Sync is an add-on, not a dependency.
- Re-delivering, reordering, or duplicating messages never corrupts anything.

**Non-goals (v1)**
- Real-time collaborative text editing (no CRDT text merge).
- Syncing over the internet or through any relay we operate.
- Multi-user permissions (Phase 2 collaboration reuses this layer later).
- Live syncing of a running Pomodoro timer.

---

## 2. How It Works in One Page

```
Every local write (TrackedWriter, one DB transaction):
  data row  +  sync_field_clock (HLC per changed field/group)  +  sync_queue row
  (deletes: tombstone instead of field clocks)

SyncEngine (when a transport is connected):
  1. handshake and auth
  2. send outbox rows for that peer, starting at its cursor
  3. receive the peer's rows
  4. ApplyRemoteWriter merges each incoming field by HLC (per-field last write wins)
  5. ack, advance cursors, re-queue applied changes for other peers

Merge is idempotent and order-independent, so retries and duplicates are harmless.
```

---

## 3. Components

| Component | Layer | Job |
|---|---|---|
| `Hlc`, `HlcClock` | `:domain/logic/sync` | Hybrid logical clock value and generator |
| `MergeGroups` | `:domain/logic/sync` | Which fields merge together |
| `Merge` | `:domain/logic/sync` | Pure function: local clocks + incoming change -> decision |
| `ChangeSet`, `FieldWrite` | `:domain/model` | Wire-neutral change representation |
| `TrackedWriter` | `data/sync` | The only local writer of synced tables |
| `ApplyRemoteWriter` | `data/sync` | The only remote writer of synced tables |
| `OutboxStore` | `data/sync` | Reads and acks `sync_queue` |
| `SyncEngine` | `service/sync` | Orchestrates a sync session over a transport |
| `SyncTransport` | interface in `service/sync` | Sends and receives frames. Knows nothing about data |
| `LanWebSocketTransport` | `data/sync` | Real transport (OkHttp WebSocket + TLS pinning) |
| `FakeTransport` | test source set | In-memory pair of connected transports |
| `PairingService` | `service/sync` | QR payload, pairing, secrets |
| `DiscoveryService` | `platform/` | NsdManager browse for the desktop service |

### Interfaces (contract, not full code)

```kotlin
// :domain
@JvmInline value class Hlc(val raw: String) : Comparable<Hlc> {
    override fun compareTo(other: Hlc) = raw.compareTo(other.raw)   // lexicographic == chronological
}

interface HlcClock {
    fun tick(): Hlc                     // local event
    fun receive(remote: Hlc): Hlc       // fold in a remote value
}

data class FieldWrite(val value: JsonElement, val hlc: Hlc)      // JsonNull = column set to NULL

data class Change(
    val id: String,                     // idempotency key
    val operation: Operation,           // UPSERT or DELETE
    val entity: String,                 // table name
    val entityId: String,               // row id or composite key
    val fields: Map<String, FieldWrite>,// empty for DELETE
    val deleteHlc: Hlc?,                // set for DELETE
)

// :app
interface SyncTransport {
    val state: StateFlow<TransportState>
    suspend fun connect(peer: PairedPeer)
    suspend fun send(frame: SyncFrame)
    val incoming: Flow<SyncFrame>
    suspend fun close()
}
```

The engine depends on `SyncTransport` only. Everything in section 6 onward about WebSockets lives inside `LanWebSocketTransport`.

---

## 4. Hybrid Logical Clock

A device's wall clock can be wrong. An HLC keeps ordering sane while staying close to real time.

**Format (string-comparable):** `<wallMs 13 digits zero-padded>-<counter 4 hex>-<device 8 hex>`

Example: `1760086800000-0003-9f3c21ab`

`device` is the first 8 hex characters of `local_identity.id` without dashes. Plain string comparison gives chronological order, with the device suffix as the last tiebreaker.

**Local event:**
```
now = clock.wallMs
if now > last.wall:  wall = now;        counter = 0
else:                wall = last.wall;  counter = last.counter + 1
```

**Receiving a remote HLC:**
```
wall = max(last.wall, remote.wall, now)
counter =
  if wall == last.wall && wall == remote.wall: max(last.counter, remote.counter) + 1
  elif wall == last.wall:                      last.counter + 1
  elif wall == remote.wall:                    remote.counter + 1
  else:                                        0
```

**Persistence:** the latest HLC lives in a one-row table `sync_local_state`. It is updated inside the same transaction as every write that ticks it, so a crash can never reuse a value.

**Clock-skew guard:** if an incoming HLC has `wall > now + 24h`, the whole batch is **quarantined, not applied**, and the user sees "The other device's clock looks wrong." One badly set clock must not be able to win every future conflict. Quarantined batches are retried after the user fixes the clock or taps Apply Anyway.

**Seed HLC:** rows created by first-launch seeding (default workspace, default area, default settings) use the constant `0000000000000-0000-00000000`. Any real edit on either device beats it, so identical seeds never conflict.

---

## 5. Change Capture

### Merge groups

Per-field last-write-wins breaks when fields depend on each other. If the phone moves a task to Someday (`bucket`, clearing `due_date`) while the desktop sets a due date (clearing `bucket`), field-by-field merging could keep both and violate the rule that they are mutually exclusive.

So some fields merge **as a group**. When any field in a group is written, the payload carries **every field in the group** with the **same HLC**. The group with the highest HLC wins whole.

| Entity | Group | Fields |
|---|---|---|
| tasks | `when` | `bucket`, `due_date`, `due_time`, `all_day`, `recurrence_rule`, `recurrence_basis` |
| tasks | `waiting` | `waiting_on`, `waiting_since`, `follow_up_date` |
| tasks | `completion` | `is_completed`, `completed_at` |
| tasks | `trash` | `is_trashed`, `trashed_at` |
| tasks | `placement` | `area_id`, `project_id`, `section_id`, `parent_task_id` |
| tasks | `myday` | `my_day_date`, `scheduled_start_min`, `scheduled_duration_min` |
| projects | `status` | `status`, `due_date` |
| goals | `progress` | `current_value`, `last_progress_at` |

Every other synced field is its own one-field group. `sort_order`, `title`, `notes`, `priority`, `is_starred`, `reviewed_at`, `estimated_minutes` and the rest are plain independent fields.

`MergeGroups` in `:domain` is the single source of truth and is covered by shared test vectors. The desktop must use the identical table.

After a merge, a `normalize` step re-checks invariants (bucket and due date exclusive, scheduled pair both-or-neither, placement consistency). If a violation somehow remains, the repair is deterministic and logged.

### TrackedWriter

All local writes to synced tables go through it. Inside one Room transaction:

1. Read current row (if update).
2. Compute changed fields. Expand to full groups.
3. `hlc = clock.tick()` once for the write. Stamp every field and group in the write with it.
4. Write the data row.
5. Upsert `sync_field_clock` rows.
6. Append one `sync_queue` row (`upsert`, payload = `{ field: { value, hlc } }`).
7. Update `sync_local_state.last_hlc`.

If nothing changed, nothing is written and nothing is queued.

Fields that are `local` or `derived` (see SCHEMA.md) are written in the same transaction but get no clock and never appear in a payload.

### Deletes

- **Trash** (`is_trashed`): an ordinary `trash` group write. Syncs like any edit. Restore is another write.
- **Permanent delete** (empty trash, 30-day purge, removing a tag or link): a **tombstone** in `sync_tombstone` with the delete HLC, plus a `sync_queue` row (`delete`), plus the physical row delete. All one transaction.
- **Cascades:** if deleting a row removes children through `ON DELETE CASCADE`, the repository enumerates the children and writes their tombstones in the same transaction. A cascade without tombstones is a bug (architecture test checks the repository delete paths).

**Resurrection rule:** an incoming upsert for an entity with a local tombstone is applied only if **its field HLCs beat the tombstone HLC**. Otherwise it is ignored. A newer edit after a delete on another device deliberately brings the row back.

**Tombstone retention:** 90 days **and** acknowledged by every trusted peer. A peer unseen for more than 90 days must do a full resync (section 8) on return, because it may still hold rows whose tombstones are gone.

---

## 6. Merge

`Merge` is a pure function in `:domain`.

```
for each field group G in the incoming change:
    local = clock row for (entity, id, G)       // may be absent
    if local is absent:                  apply
    elif incoming.hlc > local.hlc:       apply   (log a conflict if concurrent, see below)
    else:                                keep local (log a conflict if concurrent)

DELETE:
    if there is no local clock newer than deleteHlc for any group of that entity row:
        delete row, write tombstone
    else:
        ignore (a local edit is newer than the delete)
```

Properties the tests assert:
- **Idempotent:** applying the same change twice equals applying it once.
- **Commutative:** applying A then B equals B then A.
- **Convergent:** any two devices that have seen the same set of changes hold identical data.

### Conflict log

A conflict is recorded when **both sides changed the same group since the last successful sync with that peer** and the values differ. The losing value goes into `sync_conflict_log`. The task detail sheet shows "Changed on another device" with the losing value and a Restore action. Restore is an ordinary new write.

**Known limitation:** `title` and `notes` are single-value last-write-wins. If both devices edit the same note offline, one version loses whole. The conflict log keeps the loser so nothing is silently destroyed. No attempt is made to merge text.

---

## 7. Special Cases

### Natural-key collisions (tags)

`tags.name` is UNIQUE. Two devices that each create the tag "work" offline create two rows with different IDs.

When applying a remote tag upsert whose `name` already exists locally under a different ID:
1. The **lower ID** (string comparison) is the winner.
2. All `task_tags` rows pointing at the loser are repointed to the winner (tracked writes, so they sync).
3. The loser tag gets a tombstone.
4. Both devices apply the same rule independently and land on the same winner.

### Missing parent

A change can arrive before its parent (a task for a project that has not arrived yet). Such changes go into `sync_pending` and are retried whenever any change for the awaited entity is applied. Entries older than 14 days are dropped and logged.

### Subtask cycles

Two devices can each nest a task under the other. After applying a `placement` change, the cycle check runs. If it finds a cycle, the later (higher HLC) placement is rejected, the task keeps its previous parent, and the event goes to the conflict log. Same treatment for dependency links.

### Ordering collisions

Two devices inserting between the same neighbors can produce equal `sort_order`. Display order is `(sort_order, id)`, so every device orders identically with no write needed. A rebalance (when the gap between neighbors is too small) is a normal tracked service operation.

### Recurring tasks

If both devices complete a recurring task before syncing, both would create the next occurrence. The next occurrence's ID is `uuidv5(OS11_NAMESPACE, "occurrence:" + sourceTaskId + ":" + occurrenceDate)`. Both devices generate the **same ID**, so the two creates merge into one row.

### Device ID placeholder

Rows written with `created_by_device = 'local'`, `author_device_id = 'local'`, or `reactor_device_id = 'local'` are rewritten to the real `local_identity.id` before the first send.

### Settings

Only the allowlist in SCHEMA.md syncs. Each syncs as a `settings` entity keyed by `key`.

### Reminders

The reminder definition (`task_id`, `remind_at`) syncs. Fired and snoozed state does not. Each device fires its own alert. A reminder created on the desktop will notify on the phone too. That is intended.

### Attachment blobs

Metadata rows sync like anything else. File bytes move separately:

- Receiver sees an attachment row with no local file, sets `blob_state = 'pending'`, and asks for it.
- Sender streams 64 KB binary chunks, receiver verifies SHA-256, then sets `blob_state = 'present'`.
- Files over `sync_attachment_max_mb` (default 25) are not transferred. The row shows "Too large to sync."
- Blob transfer runs after metadata sync and on unmetered connections only. It is resumable by offset.

---

## 8. Sessions, First Pairing, Resync

### Normal session

```
client                                   server (desktop)
  | -- hello (ids, versions, cursor) --->  |
  | <-- challenge (nonce) ----------------- |
  | -- auth (HMAC) ----------------------> |
  | <-- hello_ack (versions, cursor) ----- |
  | <==== changes / ack in both directions ====> |
  | <-- ping / pong every 20 s ----------> |
  | -- bye -------------------------------> |
```

The version check compares `protocol_version` and `sync_schema_version`. `sync_schema_version` is a separate integer from the Room version: it bumps only when synced fields, entities, or merge groups change. A mismatch refuses the session with "Update OS11 on both devices", and neither side writes anything.

### First pairing and resync

Both devices already hold data. The exchange is a **full snapshot**: every synced row as an upsert with its stored field clocks, plus all tombstones. Because the merge is per-field and idempotent, two non-empty databases simply combine. Seed rows have the seed HLC and deterministic IDs, so they collapse into one.

A full resync also runs when a cursor is invalid (peer reinstalled, database restored from backup, tombstone window exceeded).

**Reinstalling the Android app** creates a new `local_identity.id`, so the desktop sees a new device. The old entry is removed by the user in Settings. The data comes back through a full resync.

### Batching

Changes go in batches of up to 200 items or 256 KB. Each batch is acked. The sender advances `sync_peer_state.last_sent_seq` only after the ack. A dropped connection resumes from the last acked seq.

### Relay

Changes applied from peer A are re-queued for every other trusted peer, with their original HLCs untouched. With one desktop and one phone nothing is re-queued. This is what lets a desktop hub carry changes between two phones later.

---

## 9. Transport (LAN WebSocket)

Everything in this section lives inside `LanWebSocketTransport`, `DiscoveryService`, and `PairingService`.

### Discovery

Android `NsdManager` browses for `_os11-sync._tcp.`. This matches the desktop's `bonjour-service` advertisement. If discovery fails, the app falls back to the last known host and port, then to manual host entry.

### Security

The desktop ARCHITECTURE.md describes `ws://`. This design requires **`wss://` with a pinned self-signed certificate**, because task data would otherwise cross the Wi-Fi in plain text, and Android blocks cleartext by default.

- Desktop generates a self-signed certificate on first run of the sync server and keeps it.
- The pairing QR carries the certificate's SHA-256 fingerprint.
- The Android transport uses a custom trust check that accepts exactly that fingerprint and nothing else. It does not use the system trust store for this connection and does not disable verification globally.
- No cleartext exception is added to the network security config.

### Pairing (QR)

QR payload:
```json
{ "v": 1, "host": "192.168.1.20", "port": 7411, "fp": "<sha256 hex>", "otp": "<one-time token>", "dev": "<desktop device id>", "name": "Krishna's PC" }
```
1. Android scans the QR (camera permission requested only now).
2. Connects to `wss://host:port`, verifying the certificate against `fp`.
3. Sends `pair` with `otp` and its own device info. The OTP is single-use and expires in 5 minutes.
4. Desktop replies with a long-lived `device_secret` (32 random bytes).
5. Both store the secret in `SecretStore` (Android: Keystore-wrapped). The secret never appears in the database or logs.
6. Later sessions authenticate by challenge-response: the server sends a nonce, the client answers `HMAC-SHA256(device_secret, nonce || device_id)`. The secret itself is never sent again.

Unpairing deletes the secret and the `devices` row on the device that initiated it, and the other side on its next connection attempt.

Desktop ARCHITECTURE.md says auth is "device UUID token only". This replaces a bearer token with challenge-response. See the desktop-side list.

### Frames

All text frames are JSON with a `t` field. Blob chunks are binary frames.

| `t` | Direction | Purpose |
|---|---|---|
| `pair` / `pair_ok` | client / server | First pairing |
| `hello` / `hello_ack` | both | Versions, device info, cursor |
| `challenge` / `auth` | server / client | Challenge-response |
| `changes` | both | A batch of `Change` items |
| `ack` | both | Acknowledge a batch by id |
| `resync_request` | either | Ask for a full snapshot |
| `blob_offer` / `blob_request` / `blob_chunk` / `blob_done` | both | Attachment bytes |
| `ping` / `pong` | both | Liveness |
| `error` | both | `{ code, message }` |
| `bye` | both | Clean close |

Reconnect uses exponential backoff from 2 s to 5 min, reset on success. Failures never show an error dialog. The sync status line shows the last successful time.

---

## 10. When Sync Runs (battery and Android limits)

Android will not let an app hold a socket open in the background. The design does not try.

- **App in foreground and a peer is reachable:** session stays open, changes flow both ways.
- **After a local write:** a debounced (about 2 s) push attempt if a session exists or a peer was seen recently.
- **App in background:** a WorkManager periodic job (minimum interval 15 minutes) with an unmetered-network constraint opens a short session, syncs, closes.
- **Network becomes available:** a `ConnectivityManager` callback schedules an expedited short session.
- **Manual:** "Sync now" in Settings and pull-to-refresh on Browse.
- **Never:** no foreground service for sync, no wake locks, no sync on metered networks unless the user turns it on.
- **App lock on:** sync still runs (data is not displayed), but nothing is shown on the lock screen.

The sync status shows one line in Settings and a small indicator in the Browse header: "Synced just now", "Synced 2 h ago", "Not paired", "Desktop not found". No blocking UI, ever.

---

## 11. Testing Sync

- **Domain vectors** (shared with desktop): HLC tick/receive, merge decisions per group, tombstone vs upsert, resurrection, tag collision, deterministic IDs. JSON in `domain/src/test/resources/vectors/sync/`.
- **Convergence test:** two in-memory devices, one `FakeTransport`. Generate random sequences of: create, edit, move, reorder, nest, complete (including recurring), trash, restore, permanent delete, tag create. Interleave offline periods and syncs. Assert both databases end identical, every time, across thousands of seeds.
- **Fault injection:** drop, duplicate, delay, reorder frames; kill the connection mid-batch; replay old batches.
- **Migration test with sync data:** field clocks and tombstones survive every migration.
- **Mock desktop server:** `tools/mock-desktop/` is a small Node script using the `ws` package that speaks this protocol (including the pinned certificate and QR payload). It lets the Android transport, pairing, and discovery be built and tested before the real desktop server exists. It is dev tooling only and never ships in the app.
- **Real device test** before go-live: phone and desktop on the same Wi-Fi, airplane-mode edits on both, reconnect, verify.

---

## 12. Desktop-Side Changes Required

None of these exist in the desktop docs I was given. Each needs a desktop ADR. Sync cannot go live until they are done.

1. **Reshape `sync_queue`:** `seq`, `id` (idempotency), `upsert`/`delete`, per-field `{value, hlc}` payload.
2. **Add tables:** `sync_field_clock`, `sync_tombstone`, `sync_peer_state`, `sync_pending`, `sync_conflict_log`, `sync_local_state`.
3. **HLC and merge groups** implemented identically (same format, same group table). Use the shared vector files as the conformance test.
4. **TLS:** `wss://` with a self-signed certificate and fingerprint in the QR, instead of `ws://`.
5. **Auth:** one-time pairing token, long-lived device secret, challenge-response. Not a bearer UUID.
6. **Exclude per-device columns from sync:** `reminders.is_triggered`, `reminders.snoozed_until`, `tasks.pomodoro_count`, `tasks.follow_up_notified_on`, `goals.streak_count`, `sections.is_collapsed`, `attachments.local_path`, `devices.pairing_token`.
7. **Deterministic IDs** for the default workspace, default area, and recurring occurrences (UUID v5, shared namespace constant).
8. **Tag-name collision merge** (lower ID wins, repoint, tombstone).
9. **Tombstones and cascade tombstones** on every permanent delete.
10. **`'local'` placeholder rewrite** to the real device ID before first send.
11. **Settings sync allowlist** (7 keys in SCHEMA.md).
12. **`collaboration_members.resource_type`** values updated for the new hierarchy.
13. **Attachment blob transfer** (frames in section 9) and id-derived file paths.
14. **Recurring completion** uses the deterministic occurrence ID.
15. **React Native plan removed.** The desktop phases doc Phase 20 plans a React Native Android app sharing TypeScript types. This design replaces that with native Kotlin. Types and the NLP/recurrence/scheduling logic are shared by **test vectors**, not by code.

---

## 13. Open Questions

These are real unknowns. Defaults are in place so work is not blocked.

| Question | Default |
|---|---|
| Does the desktop sync server run only while the app is open, or also from the tray? | Assumed tray (it stays alive on close per desktop Phase 4) |
| Should metered networks ever sync? | No. Setting to allow it later |
| Should notes use a text-merge (CRDT) later? | Not in v1. Conflict log is the safety net |
| Collaboration (multiple users) | Out of scope. This layer is built so it can reuse it |
