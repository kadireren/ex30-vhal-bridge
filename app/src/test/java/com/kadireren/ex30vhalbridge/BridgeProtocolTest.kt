package com.kadireren.ex30vhalbridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BridgeProtocolTest {
    @Test fun parsesKnownKeysAndDropsUnknownKeys() {
        val subscription = BridgeProtocol.parseSubscription(
            """{"type":"subscribe","protocolVersion":1,"deviceId":"panel","keys":["speed","power","unknown"]}"""
        )!!
        assertEquals("panel", subscription.deviceId)
        assertEquals(setOf("speed", "power"), subscription.keys)
    }

    @Test fun rejectsAnotherProtocolVersion() {
        assertNull(BridgeProtocol.parseSubscription("""{"type":"subscribe","protocolVersion":2,"keys":[]}"""))
    }
}

class BleProtocolTest {
    @Test fun parsesBinarySubscriptionAndDropsUnknownIds() {
        val subscription = BleProtocol.parseSubscription(byteArrayOf(0xE3.toByte(), 0x30, 1, 1, 3, 1, 3, 99))!!
        assertEquals(setOf("speed", "power"), subscription.keys)
    }

    @Test fun encodesCompactTelemetry() {
        val packet = BleProtocol.telemetry(7, 42, mapOf("power" to -12.5, "speed" to 31.25, "unknown" to 1.0))
        assertEquals(23, packet.size)
        assertEquals(2, packet[12].toInt())
        assertEquals(1, packet[13].toInt())
        assertEquals(3, packet[18].toInt())
    }

    @Test fun encodesTimeSyncEpochAndOffset() {
        val packet = BleProtocol.timeSync(1_700_000_000L, 180)
        assertEquals(10, packet.size)
        assertEquals(0xE3.toByte(), packet[0])
        assertEquals(0x30.toByte(), packet[1])
        assertEquals(1, packet[2].toInt())
        assertEquals(3, packet[3].toInt())
        val unix = (packet[4].toInt() and 0xff) or
            ((packet[5].toInt() and 0xff) shl 8) or
            ((packet[6].toInt() and 0xff) shl 16) or
            ((packet[7].toInt() and 0xff) shl 24)
        val offset = (packet[8].toInt() and 0xff) or ((packet[9].toInt() and 0xff) shl 8)
        assertEquals(1_700_000_000, unix)
        assertEquals(180, offset)
    }
}
