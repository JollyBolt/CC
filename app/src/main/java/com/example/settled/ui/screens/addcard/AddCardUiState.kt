package com.example.settled.ui.screens.addcard

data class AddCardUiState(
    val bankName: String = "",
    val cardName: String = "",
    val lastFour: String = "",
    val statementDay: String = "",
    val dueDay: String = "",
    
    val isSaving: Boolean = false,
    val error: String? = null
)
