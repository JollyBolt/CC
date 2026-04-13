package com.example.settled.ui.screens.details

sealed class CardDetailsEvent {
    object BackClicked : CardDetailsEvent()
    object RecordPaymentClicked : CardDetailsEvent()
    object PaymentSheetDismissed : CardDetailsEvent()
    data class PaymentSubmitted(val amountType: String, val platform: String, val date: Long) : CardDetailsEvent()
    
    object DeleteCardClicked : CardDetailsEvent()
    object DismissDeleteConfirmation : CardDetailsEvent()
    object ConfirmDeleteCard : CardDetailsEvent()
}
