package com.example.settled.ui.screens.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.settled.core.Result
import com.example.settled.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
                    showPaymentSheet = showSheet
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
            CardDetailsEvent.MarkAsPaidClicked -> {
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
            is CardDetailsEvent.PaymentSubmitted -> logPayment(event.amountType, event.platform)
        }
    }

    private fun logPayment(amountType: String, platform: String) {
        val state = _uiState.value
        if (state !is CardDetailsUiState.Success) return

        _uiState.update { state.copy(isSavingPayment = true, paymentError = null) }
        
        viewModelScope.launch {
            val result = cardRepository.logPayment(cardId, amountType, platform)
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
