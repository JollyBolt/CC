package com.example.settled.ui.screens.addcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.settled.core.Result
import com.example.settled.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddCardViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCardUiState())
    val uiState: StateFlow<AddCardUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<AddCardUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onEvent(event: AddCardEvent) {
        when (event) {
            is AddCardEvent.BankSelected -> {
                _uiState.update { it.copy(bankName = event.bankName, error = null) }
            }
            is AddCardEvent.CardSelected -> {
                _uiState.update { it.copy(cardName = event.cardName, error = null) }
            }
            is AddCardEvent.LastFourChanged -> {
                val filtered = event.value.filter { it.isDigit() }.take(4)
                _uiState.update { it.copy(lastFour = filtered, error = null) }
            }
            is AddCardEvent.StatementDayChanged -> {
                val filtered = event.value.filter { it.isDigit() }.take(2)
                _uiState.update { it.copy(statementDay = filtered, error = null) }
            }
            is AddCardEvent.DueDayChanged -> {
                val filtered = event.value.filter { it.isDigit() }.take(2)
                _uiState.update { it.copy(dueDay = filtered, error = null) }
            }
            AddCardEvent.SubmitCard -> saveCard()
        }
    }

    private fun saveCard() {
        val state = _uiState.value
        val statementDay = state.statementDay.toIntOrNull()
        val dueDay = state.dueDay.toIntOrNull()
        
        if (state.lastFour.length != 4 || 
            statementDay == null || statementDay !in 1..31 || 
            dueDay == null || dueDay !in 1..31) {
            viewModelScope.launch { _uiEvent.send(AddCardUiEvent.ShowError("Invalid details: Ensure last 4 digits and valid days (1-31).")) }
            return
        }
        
        _uiState.update { it.copy(isSaving = true, error = null) }
        
        viewModelScope.launch {
            val result = cardRepository.addCard(
                bankName = state.bankName,
                cardName = state.cardName,
                lastFourDigits = state.lastFour,
                statementDay = statementDay,
                dueDay = dueDay
            )
            
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEvent.send(AddCardUiEvent.NavigateToSuccess)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = result.message) }
                    _uiEvent.send(AddCardUiEvent.ShowError(result.message ?: "Failed to save card"))
                }
                else -> Unit
            }
        }
    }
}
