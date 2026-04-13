package com.example.settled.ui.screens.addcard

sealed class AddCardUiEvent {
    object NavigateToSuccess : AddCardUiEvent()
    data class ShowError(val message: String) : AddCardUiEvent()
}
