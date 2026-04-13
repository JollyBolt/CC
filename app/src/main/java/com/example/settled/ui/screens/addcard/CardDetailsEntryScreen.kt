package com.example.settled.ui.screens.addcard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailsEntryScreen(
    viewModel: AddCardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel.uiEvent, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    AddCardUiEvent.NavigateToSuccess -> onNavigateSuccess()
                    is AddCardUiEvent.ShowError -> { /* Handled natively via state binding */ }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details Entry") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp)) {
            Text("Saving details for ${uiState.bankName} ${uiState.cardName}", style = MaterialTheme.typography.titleMedium)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = uiState.lastFour,
                onValueChange = { viewModel.onEvent(AddCardEvent.LastFourChanged(it)) },
                label = { Text("Last 4 digits") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = uiState.statementDate,
                onValueChange = { viewModel.onEvent(AddCardEvent.StatementDateChanged(it)) },
                label = { Text("Statement Date (1-31)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            val isEnabled = uiState.lastFour.length == 4 && uiState.statementDate.isNotBlank() && !uiState.isSaving
            
            Button(
                onClick = { viewModel.onEvent(AddCardEvent.SubmitCard) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = isEnabled
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Save Card securely")
                }
            }
        }
    }
}
