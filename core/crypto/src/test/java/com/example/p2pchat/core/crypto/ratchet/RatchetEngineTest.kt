package com.example.p2pchat.core.crypto.ratchet

import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.SecureRandom

class RatchetEngineTest {

    private val secureRandom = SecureRandom()

    @Test
    fun testRatchetExchange() {
        val engine = RatchetEngine()
        val sharedSecret = ByteArray(32) { 0 }

        // Setup Bob's keys (pre-shared)
        val bobGen = X25519KeyPairGenerator()
        bobGen.init(X25519KeyGenerationParameters(secureRandom))
        val bobKeyPair = bobGen.generateKeyPair()
        val bobPub = bobKeyPair.public as X25519PublicKeyParameters

        // Initialize Alice
        var aliceState = engine.initializeAlice(sharedSecret, bobPub)

        // Initialize Bob (he doesn't have Alice's ephemeral key yet, it comes in header)
        // Wait, current initializeBob implementation assumes it's passive or has initial state.
        // Actually initializeBob takes alicePublicKey which implies X3DH happened.
        // But in RatchetEngine.initializeBob, I passed alicePublicKey but didn't use it in logic fully for this test setup.
        // Let's assume sharedSecret is fully established Root Key.
        // Bob needs to match Alice's initial state.
        // Since Alice generated a NEW key pair in initializeAlice, Bob needs that public key to process the first message.
        // But Bob doesn't have it at init time in this simplified flow.

        // Let's test just the engine logic:
        // Alice encrypts.
        // Bob receives and decrypts.

        // Alice Encrypts M1
        val plaintext1 = "Hello Bob".toByteArray()
        val (newAliceState1, payload1) = engine.encrypt(aliceState, plaintext1)
        aliceState = newAliceState1

        // Bob receives M1
        // Bob needs to know Alice's initial ratchet key to handle the DH ratchet if it triggers.
        // In my impl, Alice sends her public key in the header.
        // So Bob should be able to pick it up.

        // Initialize Bob
        val bobKeyPairWrapper = RatchetSessionState.KeyPair(
            bobKeyPair.private as X25519PrivateKeyParameters,
            bobKeyPair.public as X25519PublicKeyParameters
        )
        var bobState = engine.initializeBob(sharedSecret, bobPub, bobKeyPairWrapper)

        // Bob Decrypts M1
        val (newBobState1, decrypted1) = engine.decrypt(bobState, payload1)
        bobState = newBobState1

        assertEquals("Hello Bob", String(decrypted1))

        // Bob Replies M2
        val plaintext2 = "Hello Alice".toByteArray()
        val (newBobState2, payload2) = engine.encrypt(bobState, plaintext2)
        bobState = newBobState2

        // Alice Decrypts M2
        val (newAliceState2, decrypted2) = engine.decrypt(aliceState, payload2)
        aliceState = newAliceState2

        assertEquals("Hello Alice", String(decrypted2))
    }
}
