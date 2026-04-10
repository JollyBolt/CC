package com.example.settled.ui.screens.addcard

sealed class AddCardEvent {
    data class BankNameChanged(val value: String) : AddCardEvent()
    data class CardNameChanged(val value: String) : AddCardEvent()
    data class LastFourChanged(val value: String) : AddCardEvent()
    data class StatementDateChanged(val value: String) : AddCardEvent()
    
    object DetailsSubmitted : AddCardEvent()
    object StatementSubmitted : AddCardEvent()
    
    object BackClicked : AddCardEvent() // Handles navigating back between steps
}
