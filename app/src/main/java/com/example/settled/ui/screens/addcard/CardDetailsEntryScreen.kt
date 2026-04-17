package com.example.settled.ui.screens.addcard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                value = uiState.statementDay,
                onValueChange = { viewModel.onEvent(AddCardEvent.StatementDayChanged(it)) },
                label = { Text("Statement Date (1-31)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = uiState.dueDay,
                onValueChange = { viewModel.onEvent(AddCardEvent.DueDayChanged(it)) },
                label = { Text("Due Date (1-31)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tip: If Due Date is before Statement Date, it will fall in the 다음 month.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            val isEnabled = uiState.lastFour.length == 4 && 
                            uiState.statementDay.isNotBlank() && 
                            uiState.dueDay.isNotBlank() && 
                            !uiState.isSaving
            
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
