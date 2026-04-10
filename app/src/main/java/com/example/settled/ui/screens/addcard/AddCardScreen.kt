package com.example.settled.ui.screens.addcard

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.settled.ui.theme.StatusPaid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    viewModel: AddCardViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            if (uiState.currentStep != AddCardStep.SUCCESS) {
                TopAppBar(
                    title = { Text("Add Credit Card", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (uiState.currentStep == AddCardStep.STATEMENT) {
                                viewModel.onEvent(AddCardEvent.BackClicked)
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            Crossfade(targetState = uiState.currentStep, label = "AddCardSteps") { step ->
                when (step) {
                    AddCardStep.DETAILS -> DetailsStep(uiState, viewModel)
                    AddCardStep.STATEMENT -> StatementStep(uiState, viewModel)
                    AddCardStep.SUCCESS -> SuccessStep(onNavigateHome)
                }
            }
        }
    }
}

@Composable
fun DetailsStep(uiState: AddCardUiState, viewModel: AddCardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Card Information",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We never store your full card number.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = uiState.bankName,
            onValueChange = { viewModel.onEvent(AddCardEvent.BankNameChanged(it)) },
            label = { Text("Bank Name (e.g. HDFC, Amex)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = uiState.cardName,
            onValueChange = { viewModel.onEvent(AddCardEvent.CardNameChanged(it)) },
            label = { Text("Card Nickname (e.g. Regalia)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = uiState.lastFour,
            onValueChange = { viewModel.onEvent(AddCardEvent.LastFourChanged(it)) },
            label = { Text("Last 4 digits") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { viewModel.onEvent(AddCardEvent.DetailsSubmitted) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = uiState.isDetailsValid
        ) {
            Text("Next", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun StatementStep(uiState: AddCardUiState, viewModel: AddCardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Billing Cycle",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "When does your statement usually generate?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
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
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { viewModel.onEvent(AddCardEvent.StatementSubmitted) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = uiState.isStatementValid && !uiState.isSaving
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Save Card", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun SuccessStep(onNavigateHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(StatusPaid),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Card Secured!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Your card has been safely vaulted. Let's get back to your dashboard.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onNavigateHome,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Go to Home", style = MaterialTheme.typography.titleMedium)
        }
    }
}
