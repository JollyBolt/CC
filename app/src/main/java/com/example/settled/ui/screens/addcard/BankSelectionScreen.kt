package com.example.settled.ui.screens.addcard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.settled.domain.model.SupportedCardsRegistry
import com.example.settled.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankSelectionScreen(
    viewModel: AddCardViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredBanks = SupportedCardsRegistry.banks.filter {
        it.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Bank") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search specific banks") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredBanks) { bank ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clickable {
                                viewModel.onEvent(AddCardEvent.BankSelected(bank))
                                onNavigateNext()
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val logoRes = when {
                                bank.contains("HDFC", ignoreCase = true) -> R.drawable.logo_bank_hdfc_horiz
                                bank.contains("ICICI", ignoreCase = true) -> R.drawable.logo_bank_icici_horiz
                                bank.contains("SBI", ignoreCase = true) -> R.drawable.logo_bank_sbi_horiz
                                bank.contains("Axis", ignoreCase = true) -> R.drawable.logo_bank_axis_horiz
                                bank.contains("Kotak", ignoreCase = true) -> R.drawable.logo_bank_kotak_horiz
                                bank.contains("HSBC", ignoreCase = true) -> R.drawable.logo_bank_hsbc_horiz
                                bank.contains("YES", ignoreCase = true) -> R.drawable.logo_bank_yes_horiz
                                bank.contains("RBL", ignoreCase = true) -> R.drawable.logo_bank_rbl_horiz
                                bank.contains("AMEX", ignoreCase = true) || bank.contains("American Express", ignoreCase = true) -> R.drawable.logo_bank_amex_horiz
                                else -> null
                            }

                            if (logoRes != null) {
                                Image(
                                    painter = painterResource(id = logoRes),
                                    contentDescription = bank,
                                    modifier = Modifier.padding(16.dp).fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(
                                    text = bank,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
