package com.example.p2pchat.feature.peers

import android.net.Uri
import android.util.Base64
import com.example.p2pchat.core.model.UserIdentity
import com.example.p2pchat.core.network.PeerDescriptor
import java.net.URLEncoder

object ChatAddressHelper {
    private const val SCHEME = "p2pchat"
    private const val HOST = "peer"

    fun generateAddress(identity: UserIdentity, onionAddress: String? = null, signer: (ByteArray) -> ByteArray): String {
        val ik = Base64.encodeToString(identity.ed25519PublicKey, Base64.NO_WRAP)
        val ek = Base64.encodeToString(identity.x25519PublicKey, Base64.NO_WRAP)

        // Build base URI (data to be signed)
        val builder = Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST)
            .appendPath(identity.userId)
            .appendQueryParameter("name", identity.displayName)
            .appendQueryParameter("ik", ik)
            .appendQueryParameter("ek", ek)

        if (onionAddress != null) {
            builder.appendQueryParameter("relay", onionAddress)
        }

        val baseUri = builder.build().toString()

        val signature = signer(baseUri.toByteArray())
        val sigString = Base64.encodeToString(signature, Base64.NO_WRAP)

        return "$baseUri&sig=$sigString"
    }

    fun parseAddress(uriString: String): PeerDescriptor? {
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme != SCHEME || uri.host != HOST) return null

            val peerId = uri.lastPathSegment ?: return null
            val name = uri.getQueryParameter("name") ?: "Unknown"
            val relay = uri.getQueryParameter("relay")

            val ikString = uri.getQueryParameter("ik")
            val ekString = uri.getQueryParameter("ek")
            val sigString = uri.getQueryParameter("sig")

            val ik = if (ikString != null) Base64.decode(ikString, Base64.NO_WRAP) else null
            val ek = if (ekString != null) Base64.decode(ekString, Base64.NO_WRAP) else null
            val sig = if (sigString != null) Base64.decode(sigString, Base64.NO_WRAP) else null

            return PeerDescriptor(
                peerId = peerId,
                name = name,
                address = null, // No physical address
                identityKey = ik,
                exchangeKey = ek,
                relay = relay,
                signature = sig
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Verifies the signature of a chat address URI.
     * @param uriString The full URI string.
     * @param verifier A function that takes (data, signature, publicKey) and returns true if valid.
     */
    fun verifySignature(uriString: String, verifier: (ByteArray, ByteArray, ByteArray) -> Boolean): Boolean {
        try {
            val uri = Uri.parse(uriString)
            val sigString = uri.getQueryParameter("sig") ?: return false
            val ikString = uri.getQueryParameter("ik") ?: return false

            val signature = Base64.decode(sigString, Base64.NO_WRAP)
            val identityKey = Base64.decode(ikString, Base64.NO_WRAP)

            // Reconstruct the signed data (everything before &sig=)
            // Assumes &sig= is appended at the end.
            val signedDataString = uriString.substringBefore("&sig=")
            val signedData = signedDataString.toByteArray()

            return verifier(signedData, signature, identityKey)
        } catch (e: Exception) {
            return false
        }
    }
}
