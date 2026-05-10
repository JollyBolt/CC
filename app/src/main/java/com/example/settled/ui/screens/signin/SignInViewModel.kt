package com.example.settled.ui.screens.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.settled.data.auth.AuthManager
import com.example.settled.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SignInUiState {
    object Idle : SignInUiState()
    object Loading : SignInUiState()
    data class Error(val message: String) : SignInUiState()
}

sealed class SignInUiEvent {
    object SignedIn : SignInUiEvent()
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<SignInUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun signInWithGoogle(idToken: String) {
        _uiState.value = SignInUiState.Loading
        viewModelScope.launch {
            runCatching { authManager.signInWithGoogle(idToken) }
                .onSuccess {
                    runCatching<Unit> { cardRepository.initialSyncFromFirestore() }
                    _uiEvent.send(SignInUiEvent.SignedIn)
                    _uiState.value = SignInUiState.Idle
                }
                .onFailure { e ->
                    _uiState.value = SignInUiState.Error(e.message ?: "Sign-in failed")
                }
        }
    }
}
