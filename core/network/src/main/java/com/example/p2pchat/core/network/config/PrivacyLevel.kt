package com.example.p2pchat.core.network.config

enum class PrivacyLevel {
    BASIC,      // Direct (WebRTC/WiFi), Peer IPs visible, Minimal latency
    STANDARD,   // Tor (IP hidden) + Padding (Size hidden), ~300ms latency
    HIGH,       // Standard + Mix Network (Timing hidden), ~1-3s latency
    MAXIMUM     // High + Cover Traffic (Traffic Pattern hidden), High battery usage
}
