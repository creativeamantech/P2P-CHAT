package com.example.p2pchat.feature.peers

import android.net.Uri
import android.util.Base64
import com.example.p2pchat.core.model.UserIdentity
import com.example.p2pchat.core.network.PeerDescriptor
import java.net.URLEncoder

object ChatAddressHelper {
    private const val SCHEME = "p2pchat"
    private const val HOST = "peer"

    fun generateAddress(identity: UserIdentity): String {
        val ik = Base64.encodeToString(identity.ed25519PublicKey, Base64.NO_WRAP)
        val ek = Base64.encodeToString(identity.x25519PublicKey, Base64.NO_WRAP)

        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST)
            .appendPath(identity.userId)
            .appendQueryParameter("name", identity.displayName)
            .appendQueryParameter("ik", ik)
            .appendQueryParameter("ek", ek)
            .build()
            .toString()
    }

    fun parseAddress(uriString: String): PeerDescriptor? {
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme != SCHEME || uri.host != HOST) return null

            val peerId = uri.lastPathSegment ?: return null
            val name = uri.getQueryParameter("name") ?: "Unknown"
            // We can also extract keys here if PeerDescriptor supported them (it should).
            // For now, we return basic descriptor.

            // To support keys, we need to update PeerDescriptor or return a richer object.
            // Let's assume we update PeerDescriptor or Peer entity.

            return PeerDescriptor(
                peerId = peerId,
                name = name,
                address = null // No physical address in deep link usually, unless 'relay' param
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun extractKeys(uriString: String): Pair<ByteArray, ByteArray>? {
        try {
            val uri = Uri.parse(uriString)
            val ikString = uri.getQueryParameter("ik") ?: return null
            val ekString = uri.getQueryParameter("ek") ?: return null

            val ik = Base64.decode(ikString, Base64.NO_WRAP)
            val ek = Base64.decode(ekString, Base64.NO_WRAP)
            return Pair(ik, ek)
        } catch (e: Exception) {
            return null
        }
    }
}
