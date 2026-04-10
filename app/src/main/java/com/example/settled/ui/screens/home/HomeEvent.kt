package com.example.settled.ui.screens.home

sealed class HomeEvent {
    object AddCardClicked : HomeEvent()
    data class CardClicked(val cardId: String) : HomeEvent()
    object DismissPaywall : HomeEvent()
}
