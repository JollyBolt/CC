package com.example.settled.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.settled.domain.model.Card
import com.example.settled.domain.model.CardStatus
import com.example.settled.ui.screens.home.components.CardListItem
import com.example.settled.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is HomeUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            MetricsDashboard(cards = state.cards, viewModel = viewModel)
                        }
                        
                        item {
                            Text(
                                text = "Your Active Cards (${state.cards.size})",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                color = Color(0xFF1A1C1E)
                            )
                        }
                        
                        if (state.cards.isNotEmpty()) {
                            items(state.cards) { card ->
                                CardListItem(
                                    card = card,
                                    onClick = { onNavigateToDetails(card.id) }
                                )
                            }
                        } else {
                            item {
                                EmptyState()
                            }
                        }
                    }
                    
                    PrivacyFooter()
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

@Composable
fun MetricsDashboard(cards: List<Card>, viewModel: HomeViewModel) {
    val paidCount = cards.count { it.status == CardStatus.PAID }
    val soonCount = cards.count { it.status == CardStatus.SOON }
    val dueCount = cards.count { it.status != CardStatus.PAID } // All unpaid are technically 'Due'

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACCOUNT OVERVIEW",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { viewModel.onEvent(HomeEvent.SeedTestData) }) {
                Text("Seed Data", style = MaterialTheme.typography.labelSmall)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(modifier = Modifier.weight(1f), label = "PAID", value = paidCount.toString(), color = StatusPaid)
            MetricCard(modifier = Modifier.weight(1f), label = "DUE", value = dueCount.toString(), color = StatusDue)
            MetricCard(modifier = Modifier.weight(1f), label = "DUE SOON", value = soonCount.toString(), color = StatusSoon)
        }
    }
}

@Composable
fun MetricCard(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
    Surface(
        modifier = modifier
            .height(90.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black
            ),
        shape = RoundedCornerShape(24.dp),
        color = color
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label.uppercase(), 
                style = MaterialTheme.typography.labelMedium, 
                fontWeight = FontWeight.ExtraBold, 
                color = Color.White,
                fontSize = 11.sp
            )
            Text(
                text = value, 
                style = MaterialTheme.typography.displayMedium, 
                fontWeight = FontWeight.Bold, 
                color = Color.White,
                fontSize = 28.sp
            )
        }
    }
}

@Composable
fun PrivacyFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Shield, 
            contentDescription = null, 
            tint = Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Privacy First",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Gray
        )
        Text(
            text = "Your messages and emails are never tracked. Your data stays with you.",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add your first card to start monitoring", color = Color.Gray, textAlign = TextAlign.Center)
    }
}
