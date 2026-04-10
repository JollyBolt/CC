package com.example.settled.ui.screens.addcard

enum class AddCardStep {
    DETAILS, // S3
    STATEMENT, // S4
    SUCCESS // S5
}

data class AddCardUiState(
    val currentStep: AddCardStep = AddCardStep.DETAILS,
    val bankName: String = "",
    val cardName: String = "",
    val lastFour: String = "",
    val statementDate: String = "",
    
    // Validation
    val isDetailsValid: Boolean = false,
    val isStatementValid: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)
