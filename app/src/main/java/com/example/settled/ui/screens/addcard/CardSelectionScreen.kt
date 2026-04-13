package com.example.settled.ui.screens.addcard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.settled.domain.model.SupportedCardsRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardSelectionScreen(
    viewModel: AddCardViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val bankName = uiState.bankName
    val availableCards = SupportedCardsRegistry.getCardsForBank(bankName)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Card") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Cards by $bankName", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableCards) { card ->
                    ListItem(
                        headlineContent = { Text(card, fontWeight = FontWeight.Medium) },
                        leadingContent = {
                            Icon(Icons.Default.Add, contentDescription = "Card", tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.clickable {
                            viewModel.onEvent(AddCardEvent.CardSelected(card))
                            onNavigateNext()
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
