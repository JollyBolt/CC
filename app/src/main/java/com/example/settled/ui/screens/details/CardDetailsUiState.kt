package com.example.settled.ui.screens.details

import com.example.settled.domain.model.Card
import com.example.settled.domain.model.PaymentLog

sealed class CardDetailsUiState {
    object Loading : CardDetailsUiState()
    data class Success(
        val card: Card,
        val paymentLogs: List<PaymentLog> = emptyList(),
        val showPaymentSheet: Boolean = false,
        val isSavingPayment: Boolean = false,
        val paymentError: String? = null
    ) : CardDetailsUiState()
    data class Error(val message: String) : CardDetailsUiState()
}
