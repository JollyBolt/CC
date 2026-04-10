package com.example.settled.ui.screens.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.settled.ui.screens.details.CardDetailsEvent
import com.example.settled.ui.screens.details.CardDetailsUiState
import com.example.settled.ui.screens.details.CardDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentBottomSheet(
    viewModel: CardDetailsViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedType by remember { mutableStateOf("FULL") }
    var platform by remember { mutableStateOf("CRED") }

    val isSaving = (uiState as? CardDetailsUiState.Success)?.isSavingPayment == true
    val error = (uiState as? CardDetailsUiState.Success)?.paymentError

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("Mark As Paid", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Payment Type", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedType == "FULL",
                    onClick = { selectedType = "FULL" },
                    label = { Text("Full Amount") }
                )
                FilterChip(
                    selected = selectedType == "MINIMUM",
                    onClick = { selectedType = "MINIMUM" },
                    label = { Text("Minimum Due") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = platform,
                onValueChange = { platform = it },
                label = { Text("Platform (e.g. CRED, Amazon)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { viewModel.onEvent(CardDetailsEvent.PaymentSubmitted(selectedType, platform)) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = platform.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Confirm Logging", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
