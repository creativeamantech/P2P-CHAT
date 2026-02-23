package com.example.p2pchat.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserIdentityTest {
    @Test
    fun testUserIdentityEquality() {
        val identity1 = UserIdentity(
            userId = "id1",
            displayName = "User 1",
            ed25519PublicKey = ByteArray(32) { 1 },
            x25519PublicKey = ByteArray(32) { 2 },
            selfSignedCert = "cert"
        )
        val identity2 = UserIdentity(
            userId = "id1",
            displayName = "User 1",
            ed25519PublicKey = ByteArray(32) { 1 },
            x25519PublicKey = ByteArray(32) { 2 },
            selfSignedCert = "cert"
        )
        assertEquals(identity1, identity2)
    }
}
