package com.example.p2pchat.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class TransportMessageTest {

    @Test
    fun testSignalingMessageSerialization() {
        val msg = TransportMessage.Signaling(
            type = "OFFER",
            payload = "v=0\r\no=- 12345 2 IN IP4 127.0.0.1"
        )

        val bytes = msg.toBytes()
        val parsed = TransportMessage.fromBytes(bytes)

        assert(parsed is TransportMessage.Signaling)
        val sig = parsed as TransportMessage.Signaling
        assertEquals(msg.type, sig.type)
        assertEquals(msg.payload, sig.payload)
    }
}
