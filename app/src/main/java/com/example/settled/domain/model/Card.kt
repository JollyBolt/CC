package com.example.settled.domain.model

import java.time.LocalDate

data class Card(
    val id: String,
    val bankName: String,
    val cardName: String,
    val lastFourDigits: String,
    val statementDay: Int,
    val dueDay: Int,
    val status: CardStatus,
    val minimumDueLastCycle: Boolean, // True if the last valid log was of type MINIMUM
    val daysUntilDue: Int,
    val activeStatementDate: LocalDate = LocalDate.now(),
    val activeDueDate: LocalDate = LocalDate.now(),
    val isLocked: Boolean = false, // True if they slipped under a plan boundary
    val lastPaymentInfo: PaymentLog? = null
)
