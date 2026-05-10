package com.example.settled.ui.theme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.settled.data.prefs.ThemePreferences
import com.example.settled.domain.billing.EntitlementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class ThemeEvent {
    object ToggleDarkMode : ThemeEvent()
}

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
    private val entitlementRepository: EntitlementRepository
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = combine(
        themePreferences.isDarkMode,
        entitlementRepository.isPro
    ) { darkMode, isPro ->
        darkMode && isPro
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun onEvent(event: ThemeEvent) {
        when (event) {
            ThemeEvent.ToggleDarkMode -> {
                if (entitlementRepository.isPro.value) {
                    themePreferences.setDarkMode(!themePreferences.isDarkMode.value)
                }
            }
        }
    }
}
