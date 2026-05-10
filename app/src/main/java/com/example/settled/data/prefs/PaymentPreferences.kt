package com.example.settled.data.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("settled_payment_prefs", Context.MODE_PRIVATE)

    fun getCustomPlatforms(): List<String> =
        prefs.getStringSet(KEY_CUSTOM_PLATFORMS, emptySet())
            ?.sortedBy { it.lowercase() }
            ?: emptyList()

    fun saveCustomPlatform(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val current = prefs.getStringSet(KEY_CUSTOM_PLATFORMS, emptySet())?.toMutableSet()
            ?: mutableSetOf()
        current.add(trimmed)
        prefs.edit().putStringSet(KEY_CUSTOM_PLATFORMS, current).apply()
    }

    companion object {
        private const val KEY_CUSTOM_PLATFORMS = "custom_platforms"
    }
}
