package com.example.settled.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.settled.core.Result
import com.example.settled.domain.model.Card
import com.example.settled.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<HomeUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadCards()
    }

    private fun loadCards() {
        cardRepository.getAllCards().onEach { result ->
            when (result) {
                is Result.Success -> {
                    // Sort order: SOON -> DUE -> PAID
                    val sortedCards = result.data.sortedWith(
                        compareBy<Card> { it.status.ordinal }
                            .thenBy { it.daysUntilDue }
                    )
                    _uiState.update { 
                        if (it is HomeUiState.Success) it.copy(cards = sortedCards)
                        else HomeUiState.Success(cards = sortedCards)
                    }
                }
                is Result.Error -> _uiState.value = HomeUiState.Error
                is Result.Loading -> _uiState.value = HomeUiState.Loading
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.AddCardClicked -> checkCardLimitBeforeAdd()
            is HomeEvent.CardClicked -> handleCardClick(event.cardId)
            HomeEvent.DismissPaywall -> dismissPaywall()
            HomeEvent.SeedTestData -> seedTestData()
        }
    }

    private fun seedTestData() {
        viewModelScope.launch {
            cardRepository.insertDummyData()
        }
    }

    private fun checkCardLimitBeforeAdd() {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success) {
            // For MVP UI building, trigger limits automatically if >= 3
            if (currentState.cards.size >= 3) {
                _uiState.update { currentState.copy(showPaywall = true) }
            } else {
                viewModelScope.launch { _uiEvent.send(HomeUiEvent.NavigateToAdd) }
            }
        }
    }

    private fun handleCardClick(cardId: String) {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success) {
            val card = currentState.cards.find { it.id == cardId }
            if (card?.isLocked == true) {
                _uiState.update { currentState.copy(showPaywall = true) }
            } else {
                viewModelScope.launch { _uiEvent.send(HomeUiEvent.NavigateToDetails(cardId)) }
            }
        }
    }

    private fun dismissPaywall() {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success) {
            _uiState.update { currentState.copy(showPaywall = false) }
        }
    }
}
