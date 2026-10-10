package com.os11.domain.logic.sync

/**
 * Result of comparing a single field or group merge.
 */
enum class MergeDecision {
    APPLY_INCOMING,
    KEEP_LOCAL,
}

/**
 * Pure merge functions per SYNC.md Section 6.
 *
 * Asserting convergence properties:
 * - Idempotence: merge(x, x) == x
 * - Commutativity: merge(a, b) and merge(b, a) reach identical winning clocks
 * - Associativity: merge(merge(a, b), c) == merge(a, merge(b, c))
 */
object Merge {

    /**
     * Compare a local clock and incoming clock to determine which field write wins.
     */
    fun mergeField(localHlc: String?, incomingHlc: String): MergeDecision {
        if (localHlc == null) return MergeDecision.APPLY_INCOMING
        val local = Hlc.parse(localHlc)
        val incoming = Hlc.parse(incomingHlc)
        return if (incoming > local) {
            MergeDecision.APPLY_INCOMING
        } else {
            MergeDecision.KEEP_LOCAL
        }
    }

    /**
     * Determine whether an incoming DELETE operation should be applied over local field clocks.
     * Deletes win only if there is NO local clock strictly newer than deleteHlc.
     */
    fun shouldApplyDelete(localClocks: Collection<String>, deleteHlc: String): Boolean {
        val delete = Hlc.parse(deleteHlc)
        for (clock in localClocks) {
            val local = Hlc.parse(clock)
            if (local > delete) {
                return false // Local edit beats delete
            }
        }
        return true
    }

    /**
     * Resurrection rule per SYNC.md:
     * An incoming upsert for an entity with a local tombstone is applied only if
     * any of its field HLCs beat the tombstone HLC.
     */
    fun shouldResurrect(tombstoneHlc: String, incomingFieldHlcs: Collection<String>): Boolean {
        val tombstone = Hlc.parse(tombstoneHlc)
        for (clock in incomingFieldHlcs) {
            val incoming = Hlc.parse(clock)
            if (incoming > tombstone) {
                return true
            }
        }
        return false
    }

    /**
     * Compute winning fields between two changes for the same entity.
     * Returns a map of field name to the winning FieldWrite.
     */
    fun mergeFields(local: Map<String, FieldWrite>, incoming: Map<String, FieldWrite>): Map<String, FieldWrite> {
        val result = local.toMutableMap()
        for ((field, write) in incoming) {
            val localWrite = result[field]
            if (localWrite == null || mergeField(localWrite.hlc, write.hlc) == MergeDecision.APPLY_INCOMING) {
                result[field] = write
            }
        }
        return result
    }
}
