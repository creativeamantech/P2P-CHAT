package com.example.p2pchat.core.model

import kotlinx.datetime.Instant

sealed class DeliveryState {
    object Pending : DeliveryState()
    data class Sent(val at: Instant) : DeliveryState()
    data class Delivered(val at: Instant) : DeliveryState()
    data class Read(val at: Instant, val by: String) : DeliveryState()
    data class Failed(val reason: String, val retryCount: Int) : DeliveryState()
}
