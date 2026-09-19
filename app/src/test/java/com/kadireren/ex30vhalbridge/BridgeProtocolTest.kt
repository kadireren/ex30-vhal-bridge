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
