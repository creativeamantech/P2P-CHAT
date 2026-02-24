package com.example.p2pchat.feature.peers

import com.example.p2pchat.core.model.UserIdentity
import com.example.p2pchat.core.network.PeerDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ChatAddressHelperTest {

    @Test
    fun testAddressGenerationAndParsing() {
        val identity = UserIdentity(
            userId = UUID.randomUUID().toString(),
            displayName = "Alice",
            ed25519PublicKey = ByteArray(32) { 1 },
            x25519PublicKey = ByteArray(32) { 2 },
            selfSignedCert = "cert"
        )

        val signer: (ByteArray) -> ByteArray = { data -> ByteArray(64) { 3 } } // Mock signer

        val address = ChatAddressHelper.generateAddress(identity, "test.onion", signer)
        println("Generated Address: $address")

        val descriptor = ChatAddressHelper.parseAddress(address)
        assertNotNull(descriptor)
        assertEquals(identity.userId, descriptor!!.peerId)
        assertEquals("Alice", descriptor.name)
        assertEquals("test.onion", descriptor.relay)

        // Check verification
        val verifier: (ByteArray, ByteArray, ByteArray) -> Boolean = { data, sig, key ->
            // Mock verify
            true
        }

        assertTrue(ChatAddressHelper.verifySignature(address, verifier))
    }
}
