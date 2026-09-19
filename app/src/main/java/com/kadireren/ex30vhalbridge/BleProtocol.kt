package com.kadireren.ex30vhalbridge

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Calendar

object BleProtocol {
    const val VERSION: Byte = 1
    const val TYPE_SUBSCRIPTION: Byte = 1
    const val TYPE_TELEMETRY: Byte = 2
    const val TYPE_TIME: Byte = 3
    val SERVICE_UUID = java.util.UUID.fromString("7d2f0001-8d3b-4a6c-9f21-6a9b4e303001")
    val TELEMETRY_UUID = java.util.UUID.fromString("7d2f0002-8d3b-4a6c-9f21-6a9b4e303001")
    val SUBSCRIPTION_UUID = java.util.UUID.fromString("7d2f0003-8d3b-4a6c-9f21-6a9b4e303001")

    private val keyToId = linkedMapOf(
        "speed" to 1, "perf_speed" to 2, "power" to 3, "soc" to 4,
        "range" to 5, "gear" to 6, "current_gear" to 7, "ignition" to 8,
        "parking_brake" to 9, "outside_temp" to 10, "night_mode" to 11,
        "charge_port" to 12, "battery_energy" to 13, "battery_capacity" to 14,
    )
    private val idToKey = keyToId.entries.associate { (key, id) -> id to key }

    fun parseSubscription(payload: ByteArray): Subscription? = runCatching {
        require(payload.size >= 5 && payload[0] == 0xE3.toByte() && payload[1] == 0x30.toByte())
        require(payload[2] == VERSION && payload[3] == TYPE_SUBSCRIPTION)
        val count = payload[4].toInt() and 0xff
        require(payload.size == 5 + count)
        Subscription("crowpanel-ble", payload.drop(5).mapNotNull { idToKey[it.toInt() and 0xff] }.toSet())
    }.getOrNull()

    fun telemetry(sequence: Long, timestampMs: Long, values: Map<String, Double>): ByteArray {
        val entries = values.mapNotNull { (key, value) -> keyToId[key]?.let { Triple(it, key, value) } }
            .sortedBy { it.first }
        return ByteBuffer.allocate(13 + entries.size * 5).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(0xE3.toByte()).put(0x30.toByte()).put(VERSION).put(TYPE_TELEMETRY)
            putInt(sequence.toInt()).putInt(timestampMs.toInt()).put(entries.size.toByte())
            entries.forEach { (id, _, value) -> put(id.toByte()).putFloat(value.toFloat()) }
        }.array()
    }

    fun timeSync(unixSeconds: Long, offsetMinutes: Int): ByteArray =
        ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(0xE3.toByte()).put(0x30.toByte()).put(VERSION).put(TYPE_TIME)
            putInt(unixSeconds.toInt()).putShort(offsetMinutes.toShort())
        }.array()

    fun timeSyncNow(): ByteArray {
        val calendar = Calendar.getInstance()
        val offsetMinutes = (calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) / 60_000
        return timeSync(System.currentTimeMillis() / 1000L, offsetMinutes)
    }
}
