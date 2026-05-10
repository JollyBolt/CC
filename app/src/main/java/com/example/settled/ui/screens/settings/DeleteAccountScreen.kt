package com.example.settled.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.settled.core.Result
import com.example.settled.domain.repository.CardRepository
import com.example.settled.ui.theme.LightBackground
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DeleteAccountUiEvent {
    object Cleared : DeleteAccountUiEvent()
}

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiEvent = Channel<DeleteAccountUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    var isDeleting by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun confirmDelete() {
        isDeleting = true
        errorMessage = null
        viewModelScope.launch {
            when (val result = cardRepository.clearAllData()) {
                is Result.Success -> _uiEvent.send(DeleteAccountUiEvent.Cleared)
                is Result.Error   -> {
                    isDeleting = false
                    errorMessage = result.message ?: "Failed to delete account data"
                }
                else -> isDeleting = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    viewModel: DeleteAccountViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                DeleteAccountUiEvent.Cleared -> onAccountDeleted()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        TopAppBar(
            title = { Text("Delete Account", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "This will permanently delete all your card data and payment history from this device and cloud storage. This action cannot be undone.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.DarkGray
            )

            viewModel.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !viewModel.isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (viewModel.isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Delete All Data", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Are you sure?") },
            text = { Text("All cards and payment history will be deleted permanently.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.confirmDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}
