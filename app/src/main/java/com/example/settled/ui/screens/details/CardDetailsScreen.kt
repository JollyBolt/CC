package com.example.settled.ui.screens.details

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.settled.domain.model.Card
import com.example.settled.domain.model.CardStatus
import com.example.settled.domain.model.PaymentLog
import com.example.settled.ui.screens.details.components.CardVisual
import com.example.settled.ui.screens.details.components.CardStatusSection
import com.example.settled.ui.screens.details.components.CardDatesBox
import com.example.settled.ui.screens.details.components.PaymentHistorySection
import com.example.settled.ui.screens.details.components.PaymentBottomSheet
import com.example.settled.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailsScreen(
    viewModel: CardDetailsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel.uiEvent, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    CardDetailsUiEvent.NavigateBack -> onNavigateBack()
                    is CardDetailsUiEvent.ShowSnackbar -> {
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (uiState is CardDetailsUiState.Success) {
                        Text((uiState as CardDetailsUiState.Success).card.bankName, fontWeight = FontWeight.Bold) 
                    } else {
                        Text("Loading...")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is CardDetailsUiState.Success) {
                        IconButton(onClick = { viewModel.onEvent(CardDetailsEvent.DeleteCardClicked) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Card")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (uiState is CardDetailsUiState.Success) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.onEvent(CardDetailsEvent.RecordPaymentClicked) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text("Record Payment", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is CardDetailsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is CardDetailsUiState.Error -> {
                    Text(state.message ?: "Unknown error", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
                is CardDetailsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                    ) {
                        item {
                            CardVisual(
                                card = state.card,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                            )
                        }
                        
                        item {
                            CardStatusSection(
                                card = state.card,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            CardDatesBox(
                                card = state.card,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            PaymentHistorySection(
                                paymentLogs = state.paymentLogs
                            )
                        }
                    }

                    if (state.showPaymentSheet) {
                        PaymentBottomSheet(
                            viewModel = viewModel,
                            onDismiss = { viewModel.onEvent(CardDetailsEvent.PaymentSheetDismissed) }
                        )
                    }

                    if (state.showDeleteConfirmation) {
                        AlertDialog(
                            onDismissRequest = { viewModel.onEvent(CardDetailsEvent.DismissDeleteConfirmation) },
                            title = { Text("Delete Card?") },
                            text = { 
                                Text("Are you sure you want to remove ${state.card.cardName}? This action cannot be undone.") 
                            },
                            confirmButton = {
                                Button(
                                    onClick = { viewModel.onEvent(CardDetailsEvent.ConfirmDeleteCard) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    if (state.isDeletingCard) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onError)
                                    } else {
                                        Text("Delete", color = MaterialTheme.colorScheme.onError)
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.onEvent(CardDetailsEvent.DismissDeleteConfirmation) }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
