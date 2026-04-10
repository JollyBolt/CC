package com.example.settled.ui.screens.home

import com.example.settled.domain.model.Card

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val cards: List<Card>, val showPaywall: Boolean = false) : HomeUiState()
    object Error : HomeUiState()
}
