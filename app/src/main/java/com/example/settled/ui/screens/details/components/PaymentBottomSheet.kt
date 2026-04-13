package com.example.settled.ui.screens.details.components

import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settled.ui.screens.details.CardDetailsEvent
import com.example.settled.ui.screens.details.CardDetailsUiState
import com.example.settled.ui.screens.details.CardDetailsViewModel
import com.example.settled.ui.theme.PrimaryBrand
import java.text.SimpleDateFormat
import java.util.*

// Payment type accent colors
private val FullPaymentColor  = Color(0xFF388E3C) // green
private val MinimumDueColor   = Color(0xFFF9A825) // amber-yellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentBottomSheet(
    viewModel: CardDetailsViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedType     by remember { mutableStateOf("FULL") }
    var selectedPlatform by remember { mutableStateOf("BANK_APP") }
    var selectedDate     by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker   by remember { mutableStateOf(false) }

    val isSaving  = (uiState as? CardDetailsUiState.Success)?.isSavingPayment == true
    val cardName  = (uiState as? CardDetailsUiState.Success)?.card?.cardName ?: ""

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Screen-relative height so the sheet always fills ~80 % of the screen
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val sheetContentHeight = screenHeightDp * 0.80f

    val dateLabel = remember(selectedDate) {
        val todayMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val chosenMidnight = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        if (chosenMidnight == todayMidnight)
            "Today, " + SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate))
        else
            SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate))
    }

    // ── Date picker dialog ──────────────────────────────────────────
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = pickerState) }
    }

    // ── Bottom sheet ────────────────────────────────────────────────
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetContentHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Title ───────────────────────────────────────────────
            Text(
                text = "Record Payment",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Card name pill chip
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PrimaryBrand.copy(alpha = 0.12f)
            ) {
                Text(
                    text = cardName,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryBrand,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Payment Type ─────────────────────────────────────────
            SectionLabel("PAYMENT TYPE")
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PaymentTypeChip(
                    label = "Full Payment",
                    selected = selectedType == "FULL",
                    selectedColor = FullPaymentColor,
                    onClick = { selectedType = "FULL" },
                    modifier = Modifier.weight(1f)
                )
                PaymentTypeChip(
                    label = "Minimum Due",
                    selected = selectedType == "MINIMUM",
                    selectedColor = MinimumDueColor,
                    onClick = { selectedType = "MINIMUM" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Payment Platform ─────────────────────────────────────
            SectionLabel("PAYMENT PLATFORM")
            Spacer(modifier = Modifier.height(12.dp))
            // Two rows of 3 if more than 4, single row for 4 or fewer
            val platforms = PaymentPlatformRegistry.all
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                platforms.take(4).forEach { platform ->
                    PlatformIconButton(
                        platform = platform,
                        selected = selectedPlatform == platform.id,
                        onClick = { selectedPlatform = platform.id },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (platforms.size > 4) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    platforms.drop(4).forEach { platform ->
                        PlatformIconButton(
                            platform = platform,
                            selected = selectedPlatform == platform.id,
                            onClick = { selectedPlatform = platform.id },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining slots in the row with invisible spacers
                    repeat((4 - platforms.drop(4).size).coerceAtLeast(0)) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Date of Payment ──────────────────────────────────────
            SectionLabel("DATE OF PAYMENT")
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { showDatePicker = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "CHANGE",
                            color = PrimaryBrand,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // push button to bottom

            // ── Confirm Button ───────────────────────────────────────
            Button(
                onClick = {
                    val label = PaymentPlatformRegistry.findById(selectedPlatform)?.displayLabel
                        ?: selectedPlatform
                    viewModel.onEvent(
                        CardDetailsEvent.PaymentSubmitted(
                            amountType = selectedType,
                            platform = label,
                            date = selectedDate
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBrand)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Confirm Payment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cancel text link
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PaymentTypeChip(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) selectedColor else MaterialTheme.colorScheme.outline
    val bgColor     = if (selected) selectedColor.copy(alpha = 0.10f) else Color.Transparent
    val textColor   = if (selected) selectedColor else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlatformIconButton(
    platform: PaymentPlatform,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor    = if (selected) PrimaryBrand else MaterialTheme.colorScheme.surfaceVariant
    val iconTint   = if (selected) Color.White  else MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = if (selected) PrimaryBrand else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (platform.logoRes != null) {
                // Real logo — shown once the PNG is dropped into res/drawable/payment_platforms/
                Image(
                    painter = painterResource(id = platform.logoRes),
                    contentDescription = platform.displayLabel,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                // Fallback Material icon until real logo is wired in
                Icon(
                    imageVector = platform.fallbackIcon,
                    contentDescription = platform.displayLabel,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Text(
            text = platform.displayLabel,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = labelColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
