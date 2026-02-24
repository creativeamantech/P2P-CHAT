package com.example.p2pchat.feature.peers

import com.example.p2pchat.core.model.UserIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ChatAddressHelperTest {

    @Test
    fun testGenerateAndParse() {
        val identity = UserIdentity(
            userId = "user123",
            displayName = "Alice Wonderland",
            ed25519PublicKey = ByteArray(32) { 1 },
            x25519PublicKey = ByteArray(32) { 2 },
            selfSignedCert = Any()
        )

        val address = ChatAddressHelper.generateAddress(identity)
        println("Generated Address: $address")

        assertNotNull(address)

        val descriptor = ChatAddressHelper.parseAddress(address)
        assertNotNull(descriptor)
        assertEquals("user123", descriptor?.peerId)
        assertEquals("Alice Wonderland", descriptor?.name)

        val keys = ChatAddressHelper.extractKeys(address)
        assertNotNull(keys)
        // Check byte arrays content if needed
    }
}
