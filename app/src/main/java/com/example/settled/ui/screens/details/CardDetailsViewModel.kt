package com.example.settled.ui.screens.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.settled.core.Result
import com.example.settled.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardDetailsViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: String = checkNotNull(savedStateHandle["cardId"])

    private val _uiState = MutableStateFlow<CardDetailsUiState>(CardDetailsUiState.Loading)
    val uiState: StateFlow<CardDetailsUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<CardDetailsUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadData()
    }

    private fun loadData() {
        cardRepository.getCardDetails(cardId).combine(cardRepository.getPaymentLogs(cardId)) { cardResult, logsResult ->
            if (cardResult is Result.Success && logsResult is Result.Success) {
                val currentState = _uiState.value
                val showSheet = if (currentState is CardDetailsUiState.Success) currentState.showPaymentSheet else false
                
                CardDetailsUiState.Success(
                    card = cardResult.data,
                    paymentLogs = logsResult.data,
                    showPaymentSheet = showSheet,
                    showDeleteConfirmation = if (currentState is CardDetailsUiState.Success) currentState.showDeleteConfirmation else false,
                    isDeletingCard = if (currentState is CardDetailsUiState.Success) currentState.isDeletingCard else false
                )
            } else if (cardResult is Result.Error) {
                CardDetailsUiState.Error(cardResult.message)
            } else {
                CardDetailsUiState.Loading
            }
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: CardDetailsEvent) {
        when (event) {
            CardDetailsEvent.BackClicked -> { /* Handled natively in compose backstack */ }
            CardDetailsEvent.RecordPaymentClicked -> {
                val state = _uiState.value
                if (state is CardDetailsUiState.Success) {
                    _uiState.update { state.copy(showPaymentSheet = true) }
                }
            }
            CardDetailsEvent.PaymentSheetDismissed -> {
                val state = _uiState.value
                if (state is CardDetailsUiState.Success) {
                    _uiState.update { state.copy(showPaymentSheet = false, paymentError = null) }
                }
            }
            is CardDetailsEvent.PaymentSubmitted -> logPayment(event.amountType, event.platform, event.date)
            CardDetailsEvent.DeleteCardClicked -> {
                val state = _uiState.value
                if (state is CardDetailsUiState.Success) {
                    _uiState.update { state.copy(showDeleteConfirmation = true) }
                }
            }
            CardDetailsEvent.DismissDeleteConfirmation -> {
                val state = _uiState.value
                if (state is CardDetailsUiState.Success) {
                    _uiState.update { state.copy(showDeleteConfirmation = false) }
                }
            }
            CardDetailsEvent.ConfirmDeleteCard -> deleteCard()
        }
    }
    
    private fun deleteCard() {
        val state = _uiState.value
        if (state !is CardDetailsUiState.Success) return
        
        _uiState.update { state.copy(isDeletingCard = true) }
        viewModelScope.launch {
            val result = cardRepository.deleteCard(cardId)
            if (result is Result.Success) {
                _uiEvent.send(CardDetailsUiEvent.NavigateBack)
            } else {
                _uiState.update { state.copy(isDeletingCard = false, showDeleteConfirmation = false) }
                _uiEvent.send(CardDetailsUiEvent.ShowSnackbar("Failed to delete card"))
            }
        }
    }

    private fun logPayment(amountType: String, platform: String, date: Long) {
        val state = _uiState.value
        if (state !is CardDetailsUiState.Success) return

        _uiState.update { state.copy(isSavingPayment = true, paymentError = null) }
        
        viewModelScope.launch {
            val result = cardRepository.logPayment(cardId, amountType, platform, date)
            if (result is Result.Success) {
                val safeState = _uiState.value
                if (safeState is CardDetailsUiState.Success) {
                    _uiState.update { 
                        safeState.copy(
                            isSavingPayment = false, 
                            showPaymentSheet = false
                        ) 
                    }
                }
            } else if (result is Result.Error) {
                val safeState = _uiState.value
                if (safeState is CardDetailsUiState.Success) {
                    _uiState.update { 
                        safeState.copy(
                            isSavingPayment = false,
                            paymentError = result.message
                        ) 
                    }
                }
            }
        }
    }
}
