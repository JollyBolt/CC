package com.example.settled.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.settled.ui.screens.home.components.CardListItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel.uiEvent, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    HomeUiEvent.NavigateToAdd -> onNavigateToAdd()
                    is HomeUiEvent.NavigateToDetails -> onNavigateToDetails(event.cardId)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settled", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { viewModel.onEvent(HomeEvent.AddCardClicked) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Card")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is HomeUiState.Success -> {
                    if (state.cards.isEmpty()) {
                        EmptyState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.cards) { card ->
                                CardListItem(
                                    card = card,
                                    onClick = { viewModel.onEvent(HomeEvent.CardClicked(card.id)) }
                                )
                            }
                        }
                    }

                    if (state.showPaywall) {
                        AlertDialog(
                            onDismissRequest = { viewModel.onEvent(HomeEvent.DismissPaywall) },
                            title = { Text("Upgrade to Pro") },
                            text = { Text("You've reached the 3-card limit on the free plan. Upgrade to track unlimited cards!") },
                            confirmButton = {
                                Button(onClick = { viewModel.onEvent(HomeEvent.DismissPaywall) }) {
                                    Text("Subscribe Now")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.onEvent(HomeEvent.DismissPaywall) }) {
                                    Text("Not now")
                                }
                            }
                        )
                    }
                }
                is HomeUiState.Error -> {
                    Text("Error loading cards.", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "No cards added yet.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Add your first credit card to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
