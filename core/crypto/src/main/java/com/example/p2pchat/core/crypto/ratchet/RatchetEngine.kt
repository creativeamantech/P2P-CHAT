package com.example.p2pchat.core.crypto.ratchet

import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class RatchetEngine {

    private val secureRandom = SecureRandom()

    fun initializeAlice(
        sharedSecret: ByteArray,
        bobPublicKey: X25519PublicKeyParameters
    ): RatchetSessionState {
        // Alice initiates
        // Use sharedSecret as initial Root Key directly to match Bob's passive state
        val rootKey = sharedSecret
        val myKeyPair = generateKeyPair()

        val dhOutput = computeDH(myKeyPair.private, bobPublicKey)
        val (rk, cks) = HKDF.deriveRoot(rootKey, ByteArray(0), dhOutput)

        return RatchetSessionState(
            dhPair = myKeyPair,
            dhRemotePublic = bobPublicKey,
            rootKey = rk,
            chainKeySend = cks,
            chainKeyRecv = null,
            ns = 0,
            nr = 0,
            pn = 0
        )
    }

    fun initializeBob(
        sharedSecret: ByteArray,
        alicePublicKey: X25519PublicKeyParameters,
        myPreKeyPair: RatchetSessionState.KeyPair? = null // Allow injecting Bob's pre-key pair
    ): RatchetSessionState {
        // Bob State (passive initially until he receives Alice's first message)
        // He has the shared secret (SK).
        // He has his own key pair (Bob's signed prekey that Alice used).

        val myKeyPair = myPreKeyPair ?: generateKeyPair()

        return RatchetSessionState(
            dhPair = myKeyPair,
            dhRemotePublic = null, // Will learn from first message
            rootKey = sharedSecret,
            chainKeySend = null,
            chainKeyRecv = null,
            ns = 0,
            nr = 0,
            pn = 0
        )
    }

    fun encrypt(state: RatchetSessionState, plaintext: ByteArray): Pair<RatchetSessionState, ByteArray> {
        var currentState = state

        // Check if we need to ratchet?
        // In simple Double Ratchet, we only ratchet DH when we receive a new key.
        // So sending is just Symmetric Ratchet.

        val chainKey = currentState.chainKeySend ?: throw IllegalStateException("Cannot encrypt, no sending chain")

        // 1. KDF Step
        val (nextChainKey, messageKey) = HKDF.chainStep(chainKey)

        // 2. Encrypt
        val header = RatchetHeader(
            dhPublic = currentState.dhPair.public.encoded,
            pn = currentState.pn,
            n = currentState.ns
        )

        // Header needs to be authenticated (AD).
        // We serialize header to bytes for AD.
        // (Simplified serialization for MVP)
        val ad = headerToBytes(header)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12) // Deterministic IV from message key? Or random?
        // Signal uses KDF to derive IV too.
        // For MVP, we'll use random IV and append it (overhead, but safe).
        secureRandom.nextBytes(iv)

        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(messageKey, "AES"), GCMParameterSpec(128, iv))
        cipher.updateAAD(ad)
        val ciphertext = cipher.doFinal(plaintext)

        // Payload = Header + IV + Ciphertext
        val payload = ad + iv + ciphertext

        // 3. Update State
        val newState = currentState.copy(
            chainKeySend = nextChainKey,
            ns = currentState.ns + 1
        )

        return Pair(newState, payload)
    }

    fun decrypt(state: RatchetSessionState, payload: ByteArray): Pair<RatchetSessionState, ByteArray> {
        var currentState = state

        // 1. Parse Header
        // Assuming fixed length header for MVP (32 bytes key + 4 bytes PN + 4 bytes N) = 40 bytes
        if (payload.size < 40 + 12) throw IllegalArgumentException("Payload too short")

        val dhPublicBytes = payload.copyOfRange(0, 32)
        val pn = bytesToInt(payload.copyOfRange(32, 36))
        val n = bytesToInt(payload.copyOfRange(36, 40))
        val headerBytes = payload.copyOfRange(0, 40) // AD
        val iv = payload.copyOfRange(40, 52)
        val ciphertext = payload.copyOfRange(52, payload.size)

        val remotePublicKey = X25519PublicKeyParameters(dhPublicBytes, 0)

        // 2. Check for DH Ratchet (New remote key?)
        if (currentState.dhRemotePublic == null || !remotePublicKey.encoded.contentEquals(currentState.dhRemotePublic!!.encoded)) {
            // DHRatchet Step

            // A) Skip skipped messages (omitted for MVP - assumes ordered delivery)

            // B) DH Ratchet
            // 1. DHi = DH(myPriv, theirNewPub)
            val dhOutput1 = computeDH(currentState.dhPair.private, remotePublicKey)
            // 2. RK, CK_recv = KDF_RK(RK, DHi)
            val (rk1, ckRecv) = HKDF.deriveRoot(currentState.rootKey, ByteArray(0), dhOutput1)

            // 3. New Key Pair
            val newKeyPair = generateKeyPair()

            // 4. DHr = DH(newPriv, theirNewPub)
            val dhOutput2 = computeDH(newKeyPair.private, remotePublicKey)
            // 5. RK, CK_send = KDF_RK(rk1, DHr)
            val (rk2, ckSend) = HKDF.deriveRoot(rk1, ByteArray(0), dhOutput2)

            currentState = currentState.copy(
                rootKey = rk2,
                chainKeyRecv = ckRecv,
                chainKeySend = ckSend,
                dhPair = newKeyPair,
                dhRemotePublic = remotePublicKey,
                pn = currentState.ns, // Reset previous count
                ns = 0,
                nr = 0
            )
        }

        // 3. Symmetric Ratchet (Chain Step) to reach 'n'
        // (Omitted skipping logic, assumes n is next)
        val chainKey = currentState.chainKeyRecv ?: throw IllegalStateException("No recv chain")

        val (nextChainKey, messageKey) = HKDF.chainStep(chainKey)

        // 4. Decrypt
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(messageKey, "AES"), GCMParameterSpec(128, iv))
        cipher.updateAAD(headerBytes)
        val plaintext = cipher.doFinal(ciphertext)

        // 5. Update State
        val newState = currentState.copy(
            chainKeyRecv = nextChainKey,
            nr = n + 1
        )

        return Pair(newState, plaintext)
    }

    private fun generateKeyPair(): RatchetSessionState.KeyPair {
        val gen = X25519KeyPairGenerator()
        gen.init(X25519KeyGenerationParameters(secureRandom))
        val pair = gen.generateKeyPair()
        return RatchetSessionState.KeyPair(
            pair.private as X25519PrivateKeyParameters,
            pair.public as X25519PublicKeyParameters
        )
    }

    private fun computeDH(priv: X25519PrivateKeyParameters, pub: X25519PublicKeyParameters): ByteArray {
        val secret = ByteArray(32)
        priv.generateSecret(pub, secret, 0)
        return secret
    }

    private fun headerToBytes(header: RatchetHeader): ByteArray {
        val bytes = ByteArray(40)
        System.arraycopy(header.dhPublic, 0, bytes, 0, 32)
        intToBytes(header.pn, bytes, 32)
        intToBytes(header.n, bytes, 36)
        return bytes
    }

    private fun intToBytes(v: Int, buf: ByteArray, offset: Int) {
        buf[offset] = (v shr 24).toByte()
        buf[offset + 1] = (v shr 16).toByte()
        buf[offset + 2] = (v shr 8).toByte()
        buf[offset + 3] = v.toByte()
    }

    private fun bytesToInt(buf: ByteArray): Int {
        return ((buf[0].toInt() and 0xFF) shl 24) or
               ((buf[1].toInt() and 0xFF) shl 16) or
               ((buf[2].toInt() and 0xFF) shl 8) or
               (buf[3].toInt() and 0xFF)
    }
}
