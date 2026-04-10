package com.example.settled.ui.screens.home

sealed class HomeUiEvent {
    object NavigateToAdd : HomeUiEvent()
    data class NavigateToDetails(val cardId: String) : HomeUiEvent()
}
