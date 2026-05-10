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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.settled.R
import com.example.settled.ui.screens.details.CardDetailsEvent
import com.example.settled.ui.screens.details.CardDetailsUiState
import com.example.settled.ui.screens.details.CardDetailsViewModel
import com.example.settled.ui.theme.PrimaryBrand
import com.example.settled.ui.theme.SettledTheme
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
    val successState = uiState as? CardDetailsUiState.Success
    val existingLog = successState?.card?.lastPaymentInfo
    val lastUsedPlatform = successState?.card?.lastUsedPlatform
    val customPlatforms = successState?.customPlatforms ?: emptyList()
    val isModifying = successState?.card?.status == com.example.settled.domain.model.CardStatus.PAID

    var selectedType by remember {
        mutableStateOf(existingLog?.type ?: "FULL")
    }
    var selectedPlatform by remember {
        mutableStateOf(
            existingLog?.platform?.let { PaymentPlatformRegistry.findByLabel(it)?.id ?: it }
                ?: "BANK_APP"
        )
    }
    var selectedDate by remember {
        mutableStateOf(existingLog?.timestamp ?: System.currentTimeMillis())
    }
    var showDatePicker   by remember { mutableStateOf(false) }

    val isSaving  = successState?.isSavingPayment == true
    val cardName  = successState?.card?.cardName ?: ""

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Screen-relative height so the sheet always fills ~80 % of the screen
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val sheetContentHeight = screenHeightDp * 0.80f

    val todayStr = stringResource(R.string.date_today)
    val dateLabel = remember(selectedDate, todayStr) {
        val todayMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val chosenMidnight = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        if (chosenMidnight == todayMidnight) todayStr
        else SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(selectedDate))
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
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) { DatePicker(state = pickerState) }
    }

    val card = (uiState as? CardDetailsUiState.Success)?.card
    val bankSquareLogo = card?.let {
        when {
            it.bankName.contains("HDFC", ignoreCase = true) -> R.drawable.logo_bank_hdfc_sq
            it.bankName.contains("ICICI", ignoreCase = true) -> R.drawable.logo_bank_icici_sq
            it.bankName.contains("SBI", ignoreCase = true) -> R.drawable.logo_bank_sbi_sq
            it.bankName.contains("AXIS", ignoreCase = true) -> R.drawable.logo_bank_axis_sq
            it.bankName.contains("KOTAK", ignoreCase = true) -> R.drawable.logo_bank_kotak_sq
            it.bankName.contains("HSBC", ignoreCase = true) -> R.drawable.logo_bank_hsbc_sq
            it.bankName.contains("YES", ignoreCase = true) -> R.drawable.logo_bank_yes_sq
            it.bankName.contains("RBL", ignoreCase = true) -> R.drawable.logo_bank_rbl_sq
            it.bankName.contains("AMEX", ignoreCase = true) || it.bankName.contains("American Express", ignoreCase = true) -> R.drawable.logo_bank_amex_sq
            else -> null
        }
    }

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
        PaymentBottomSheetContent(
            isModifying = isModifying,
            cardName = cardName,
            bankSquareLogo = bankSquareLogo,
            selectedType = selectedType,
            selectedPlatform = selectedPlatform,
            lastUsedPlatform = lastUsedPlatform,
            customPlatforms = customPlatforms,
            dateLabel = dateLabel,
            isSaving = isSaving,
            sheetContentHeight = sheetContentHeight,
            onTypeSelected = { selectedType = it },
            onPlatformSelected = { selectedPlatform = it },
            onSaveCustomPlatform = { name ->
                viewModel.onEvent(CardDetailsEvent.SaveCustomPlatform(name))
            },
            onChangeDateClicked = { showDatePicker = true },
            onConfirm = {
                val label = PaymentPlatformRegistry.findById(selectedPlatform)?.displayLabel ?: selectedPlatform
                viewModel.onEvent(CardDetailsEvent.PaymentSubmitted(amountType = selectedType, platform = label, date = selectedDate))
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun PaymentBottomSheetContent(
    isModifying: Boolean,
    cardName: String,
    bankSquareLogo: Int?,
    selectedType: String,
    selectedPlatform: String,
    lastUsedPlatform: String?,
    customPlatforms: List<String>,
    dateLabel: String,
    isSaving: Boolean,
    sheetContentHeight: androidx.compose.ui.unit.Dp,
    onTypeSelected: (String) -> Unit,
    onPlatformSelected: (String) -> Unit,
    onSaveCustomPlatform: (String) -> Unit,
    onChangeDateClicked: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var showOtherInput by remember { mutableStateOf(false) }
    var otherInputText by remember { mutableStateOf("") }

    val standardPlatforms = PaymentPlatformRegistry.all.map { platform ->
        if (platform.id == "BANK_APP" && bankSquareLogo != null) platform.copy(logoRes = bankSquareLogo)
        else platform
    }
    val customPlatformEntries = customPlatforms.map { name ->
        PaymentPlatform(
            id = name,
            displayLabel = name.uppercase(),
            fallbackIcon = Icons.Default.Add,
            initials = computeInitials(name)
        )
    }
    val allPlatforms: List<PaymentPlatform?> = standardPlatforms + customPlatformEntries + listOf(null)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(sheetContentHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Title ────────────────────────────────────────────────────
        Text(
            text = if (isModifying) stringResource(R.string.card_details_modify_payment)
                   else stringResource(R.string.card_details_record_payment),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = PrimaryBrand.copy(alpha = 0.12f)) {
            Text(
                text = cardName,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryBrand,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(28.dp))

        // ── Payment Type ──────────────────────────────────────────────
        SectionLabel(stringResource(R.string.payment_section_type))
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PaymentTypeChip(
                label = stringResource(R.string.payment_type_full),
                selected = selectedType == "FULL",
                selectedColor = FullPaymentColor,
                onClick = { onTypeSelected("FULL") },
                modifier = Modifier.weight(1f)
            )
            PaymentTypeChip(
                label = stringResource(R.string.payment_type_minimum),
                selected = selectedType == "MINIMUM",
                selectedColor = MinimumDueColor,
                onClick = { onTypeSelected("MINIMUM") },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        // ── Payment Platform ──────────────────────────────────────────
        SectionLabel(stringResource(R.string.payment_section_platform))
        if (lastUsedPlatform != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Last used: $lastUsedPlatform",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        allPlatforms.chunked(4).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { platform ->
                    if (platform == null) {
                        OtherPlatformChip(
                            selected = showOtherInput,
                            onClick = {
                                showOtherInput = true
                                onPlatformSelected("__OTHER__")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        PlatformIconButton(
                            platform = platform,
                            selected = selectedPlatform == platform.id,
                            onClick = {
                                onPlatformSelected(platform.id)
                                showOtherInput = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                repeat((4 - row.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        if (showOtherInput) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = otherInputText,
                    onValueChange = { otherInputText = it.take(30) },
                    placeholder = { Text("e.g. Slice, Jupiter") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = {
                        val trimmed = otherInputText.trim()
                        if (trimmed.isNotEmpty()) {
                            onSaveCustomPlatform(trimmed)
                            onPlatformSelected(trimmed)
                            showOtherInput = false
                            otherInputText = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBrand)
                ) {
                    Text(stringResource(R.string.action_add))
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // ── Date of Payment ───────────────────────────────────────────
        SectionLabel(stringResource(R.string.payment_section_date))
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            onClick = onChangeDateClicked,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryBrand.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = PrimaryBrand,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = PrimaryBrand,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Action Buttons ─────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.action_cancel), fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBrand)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.payment_confirm), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

@Preview(name = "Bottom sheet — Record Payment", showBackground = true, heightDp = 700)
@Composable
private fun PaymentBottomSheetRecordPreview() {
    SettledTheme {
        PaymentBottomSheetContent(
            isModifying = false,
            cardName = "Regalia Gold",
            bankSquareLogo = R.drawable.logo_bank_hdfc_sq,
            selectedType = "FULL",
            selectedPlatform = "CRED",
            lastUsedPlatform = null,
            customPlatforms = emptyList(),
            dateLabel = "Today, May 10, 2026",
            isSaving = false,
            sheetContentHeight = 560.dp,
            onTypeSelected = {}, onPlatformSelected = {}, onSaveCustomPlatform = {},
            onChangeDateClicked = {}, onConfirm = {}, onDismiss = {}
        )
    }
}

@Preview(name = "Bottom sheet — Modify Payment (MINIMUM pre-selected)", showBackground = true, heightDp = 700)
@Composable
private fun PaymentBottomSheetModifyPreview() {
    SettledTheme {
        PaymentBottomSheetContent(
            isModifying = true,
            cardName = "Amazon Pay",
            bankSquareLogo = R.drawable.logo_bank_icici_sq,
            selectedType = "MINIMUM",
            selectedPlatform = "GPAY",
            lastUsedPlatform = "CRED",
            customPlatforms = listOf("Slice", "Jupiter"),
            dateLabel = "Thu, May 8, 2026",
            isSaving = false,
            sheetContentHeight = 560.dp,
            onTypeSelected = {}, onPlatformSelected = {}, onSaveCustomPlatform = {},
            onChangeDateClicked = {}, onConfirm = {}, onDismiss = {}
        )
    }
}

// ── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun OtherPlatformChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor    = if (selected) PrimaryBrand.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    val iconTint   = if (selected) PrimaryBrand else MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = if (selected) PrimaryBrand else MaterialTheme.colorScheme.onSurfaceVariant
    val border     = if (selected) BorderStroke(2.dp, PrimaryBrand) else null

    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .let { if (border != null) it.border(border, RoundedCornerShape(14.dp)) else it }
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Other",
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = "OTHER",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = labelColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
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
    val bgColor    = if (selected) PrimaryBrand.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    val iconTint   = if (selected) PrimaryBrand else MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = if (selected) PrimaryBrand else MaterialTheme.colorScheme.onSurfaceVariant
    val border     = if (selected) BorderStroke(2.dp, PrimaryBrand) else null

    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .let { if (border != null) it.border(border, RoundedCornerShape(14.dp)) else it }
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (platform.logoRes != null) {
                Image(
                    painter = painterResource(id = platform.logoRes),
                    contentDescription = platform.displayLabel,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else if (platform.initials != null) {
                Text(
                    text = platform.initials,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconTint
                )
            } else {
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
