package com.example.settled.ui.screens.addcard

data class AddCardUiState(
    val bankName: String = "",
    val cardName: String = "",
    val lastFour: String = "",
    val statementDate: String = "",
    
    val isSaving: Boolean = false,
    val error: String? = null
)
