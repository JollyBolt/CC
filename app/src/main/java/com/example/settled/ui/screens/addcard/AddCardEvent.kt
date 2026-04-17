package com.example.settled.ui.screens.addcard

sealed class AddCardEvent {
    data class BankSelected(val bankName: String) : AddCardEvent()
    data class CardSelected(val cardName: String) : AddCardEvent()
    data class LastFourChanged(val value: String) : AddCardEvent()
    data class StatementDayChanged(val value: String) : AddCardEvent()
    data class DueDayChanged(val value: String) : AddCardEvent()
    
    object SubmitCard : AddCardEvent()
}
