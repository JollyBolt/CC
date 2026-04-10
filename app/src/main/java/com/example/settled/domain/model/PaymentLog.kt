package com.example.settled.domain.model

data class PaymentLog(
    val id: String,
    val type: String, // "FULL", "MINIMUM"
    val platform: String, // e.g., "CRED"
    val timestamp: Long,
    val cycleMonth: Int,
    val cycleYear: Int
)
