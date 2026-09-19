package com.kadireren.ex30vhalbridge

import org.json.JSONArray
import org.json.JSONObject

data class Subscription(val deviceId: String, val keys: Set<String>)

object BridgeProtocol {
    const val VERSION = 1
    const val SUBSCRIPTION_PORT = 4211
    const val TELEMETRY_PORT = 4210

    fun parseSubscription(payload: String): Subscription? = runCatching {
        val json = JSONObject(payload)
        require(json.optString("type") == "subscribe")
        require(json.optInt("protocolVersion") == VERSION)
        val keysJson = json.optJSONArray("keys") ?: JSONArray()
        val keys = buildSet {
            for (index in 0 until keysJson.length()) add(keysJson.getString(index))
        }.intersect(VhalCatalog.keys)
        Subscription(json.optString("deviceId", "crowpanel"), keys)
    }.getOrNull()

    fun telemetry(sessionId: String, sequence: Long, timestampMs: Long, values: Map<String, Double>): ByteArray =
        JSONObject()
            .put("type", "telemetry")
            .put("protocolVersion", VERSION)
            .put("sessionId", sessionId)
            .put("sequence", sequence)
            .put("timestampMs", timestampMs)
            .put("values", JSONObject(values))
            .toString()
            .toByteArray(Charsets.UTF_8)
}
