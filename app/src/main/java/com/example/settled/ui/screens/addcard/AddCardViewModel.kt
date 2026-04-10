package com.example.settled.ui.screens.addcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.settled.core.Result
import com.example.settled.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddCardViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCardUiState())
    val uiState: StateFlow<AddCardUiState> = _uiState.asStateFlow()

    fun onEvent(event: AddCardEvent) {
        when (event) {
            is AddCardEvent.BankNameChanged -> {
                _uiState.update { it.copy(bankName = event.value, error = null) }
                validateDetails()
            }
            is AddCardEvent.CardNameChanged -> {
                _uiState.update { it.copy(cardName = event.value, error = null) }
                validateDetails()
            }
            is AddCardEvent.LastFourChanged -> {
                val filtered = event.value.filter { it.isDigit() }.take(4)
                _uiState.update { it.copy(lastFour = filtered, error = null) }
                validateDetails()
            }
            is AddCardEvent.StatementDateChanged -> {
                val filtered = event.value.filter { it.isDigit() }.take(2)
                _uiState.update { it.copy(statementDate = filtered, error = null) }
                validateStatement()
            }
            AddCardEvent.DetailsSubmitted -> {
                if (_uiState.value.isDetailsValid) {
                    _uiState.update { it.copy(currentStep = AddCardStep.STATEMENT, error = null) }
                }
            }
            AddCardEvent.StatementSubmitted -> {
                if (_uiState.value.isStatementValid) {
                    saveCard()
                }
            }
            AddCardEvent.BackClicked -> {
                when (_uiState.value.currentStep) {
                    AddCardStep.DETAILS -> { /* Handle returning to home natively in compose */ }
                    AddCardStep.STATEMENT -> _uiState.update { it.copy(currentStep = AddCardStep.DETAILS, error = null) }
                    AddCardStep.SUCCESS -> { /* Handled natively */ }
                }
            }
        }
    }

    private fun validateDetails() {
        val state = _uiState.value
        val isValid = state.bankName.isNotBlank() && 
                      state.cardName.isNotBlank() && 
                      state.lastFour.length == 4
        _uiState.update { it.copy(isDetailsValid = isValid) }
    }

    private fun validateStatement() {
        val state = _uiState.value
        val date = state.statementDate.toIntOrNull()
        val isValid = date != null && date in 1..31
        _uiState.update { it.copy(isStatementValid = isValid) }
    }

    private fun saveCard() {
        val state = _uiState.value
        val date = state.statementDate.toIntOrNull() ?: return
        
        _uiState.update { it.copy(isSaving = true, error = null) }
        
        viewModelScope.launch {
            val result = cardRepository.addCard(
                bankName = state.bankName,
                cardName = state.cardName,
                lastFourDigits = state.lastFour,
                statementDate = date
            )
            
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, currentStep = AddCardStep.SUCCESS) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = result.message) }
                }
                else -> Unit
            }
        }
    }
}
