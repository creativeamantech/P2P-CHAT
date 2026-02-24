package com.example.p2pchat.core.crypto.ratchet

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters

object HKDF {
    private const val HASH_LEN = 32 // SHA-256 output length

    /**
     * Derives a new root key and chain key from an input key material.
     * Output: [RootKey, ChainKey]
     */
    fun deriveRoot(inputKeyMaterial: ByteArray, salt: ByteArray, info: ByteArray): Pair<ByteArray, ByteArray> {
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(inputKeyMaterial, salt, info))

        val output = ByteArray(64) // Need 2 * 32 bytes
        hkdf.generateBytes(output, 0, 64)

        val rootKey = output.copyOfRange(0, 32)
        val chainKey = output.copyOfRange(32, 64)

        return Pair(rootKey, chainKey)
    }

    /**
     * Derives a Message Key from a Chain Key.
     * Input: ChainKey
     * Output: [NewChainKey, MessageKey]
     * Uses HKDF with:
     *   Salt = ChainKey
     *   InputKey = Constant (e.g., 0x01 for MK, 0x02 for NextCK) or standard Signal KDF
     *   (Signal uses HMAC for chain step, not full HKDF, but HKDF is fine/better).
     *
     *   Signal spec:
     *   MessageKey = HMAC-SHA256(ChainKey, "0x01")
     *   NextChainKey = HMAC-SHA256(ChainKey, "0x02")
     */
    fun chainStep(chainKey: ByteArray): Pair<ByteArray, ByteArray> {
        // We'll use full HKDF for simplicity and strength, expanding to 64 bytes
        // Salt = 0 (or empty), IKM = chainKey, Info = "Ratchet"
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(chainKey, ByteArray(0), "RatchetStep".toByteArray()))

        val output = ByteArray(64)
        hkdf.generateBytes(output, 0, 64)

        val nextChainKey = output.copyOfRange(0, 32)
        val messageKey = output.copyOfRange(32, 64)

        return Pair(nextChainKey, messageKey)
    }
}
