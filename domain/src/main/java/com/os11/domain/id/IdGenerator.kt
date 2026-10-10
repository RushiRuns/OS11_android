package com.os11.domain.id

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

/**
 * ID generator interface for decoupling ID creation in pure domain and tests.
 */
interface IdGenerator {
    fun randomId(): String
    fun deterministicId(namespace: UUID, name: String): String
}

/**
 * Standard UUID generator implementation.
 */
class DefaultIdGenerator : IdGenerator {
    override fun randomId(): String = UUID.randomUUID().toString()

    override fun deterministicId(namespace: UUID, name: String): String =
        DeterministicIds.uuidv5(namespace, name).toString()
}

/**
 * Fixed deterministic ID constants and UUIDv5 RFC 4122 implementation.
 */
object DeterministicIds {
    /**
     * Fixed OS11 namespace UUID matching desktop.
     * RFC 4122 DNS namespace is 6ba7b810-9dad-11d1-80b4-00c04fd430c8.
     */
    val OS11_NAMESPACE: UUID = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")

    const val WORKSPACE_DEFAULT_NAME = "workspace:default"
    const val AREA_DEFAULT_NAME = "area:default"

    fun defaultWorkspaceId(): String = uuidv5(OS11_NAMESPACE, WORKSPACE_DEFAULT_NAME).toString()

    fun defaultAreaId(): String = uuidv5(OS11_NAMESPACE, AREA_DEFAULT_NAME).toString()

    fun recurringOccurrenceId(sourceTaskId: String, occurrenceDate: String): String {
        val name = "occurrence:$sourceTaskId:$occurrenceDate"
        return uuidv5(OS11_NAMESPACE, name).toString()
    }

    private const val BUFFER_SIZE = 16
    private const val VERSION_BYTE_INDEX = 6
    private const val VARIANT_BYTE_INDEX = 8
    private const val VERSION_5_BITS = 0x50
    private const val VERSION_MASK = 0x0F
    private const val VARIANT_2_BITS = 0x80
    private const val VARIANT_MASK = 0x3F

    /**
     * Generate an RFC 4122 UUID version 5 (SHA-1 name-based).
     */
    fun uuidv5(namespace: UUID, name: String): UUID {
        val md = MessageDigest.getInstance("SHA-1")

        val nsBuffer = ByteBuffer.wrap(ByteArray(BUFFER_SIZE))
        nsBuffer.putLong(namespace.mostSignificantBits)
        nsBuffer.putLong(namespace.leastSignificantBits)

        md.update(nsBuffer.array())
        md.update(name.toByteArray(StandardCharsets.UTF_8))
        val raw = md.digest()

        // Set version to 5 (bits 4-7 of time_hi_and_version)
        raw[VERSION_BYTE_INDEX] = ((raw[VERSION_BYTE_INDEX].toInt() and VERSION_MASK) or VERSION_5_BITS).toByte()
        // Set variant to 2 (IETF RFC 4122: bits 6-7 of clock_seq_hi_and_reserved = 10)
        raw[VARIANT_BYTE_INDEX] = ((raw[VARIANT_BYTE_INDEX].toInt() and VARIANT_MASK) or VARIANT_2_BITS).toByte()

        val buf = ByteBuffer.wrap(raw)
        val msb = buf.long
        val lsb = buf.long
        return UUID(msb, lsb)
    }
}
