package com.example.settled.ui.screens.details

sealed class CardDetailsEvent {
    object BackClicked : CardDetailsEvent()
    object MarkAsPaidClicked : CardDetailsEvent()
    object PaymentSheetDismissed : CardDetailsEvent()
    data class PaymentSubmitted(val amountType: String, val platform: String) : CardDetailsEvent()
}
