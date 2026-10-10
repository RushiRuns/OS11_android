package com.os11.domain.logic.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
enum class Operation {
    @SerialName("upsert")
    UPSERT,

    @SerialName("delete")
    DELETE,
}

@Serializable
data class FieldWrite(val value: JsonElement, val hlc: String)

@Serializable
data class Change(
    val id: String,
    val operation: Operation,
    val entity: String,
    val entityId: String,
    val fields: Map<String, FieldWrite> = emptyMap(),
    val deleteHlc: String? = null,
)
