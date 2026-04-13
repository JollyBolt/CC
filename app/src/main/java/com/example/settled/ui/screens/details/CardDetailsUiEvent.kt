package com.example.settled.ui.screens.details

sealed class CardDetailsUiEvent {
    object NavigateBack : CardDetailsUiEvent()
    data class ShowSnackbar(val message: String) : CardDetailsUiEvent()
}
