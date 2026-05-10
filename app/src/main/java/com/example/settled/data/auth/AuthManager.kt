package com.example.settled.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_UID = "uid"

@Singleton
class AuthManager @Inject constructor(
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "settled_auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun currentUid(): String? = auth.currentUser?.uid ?: prefs.getString(PREF_UID, null)

    suspend fun ensureSignedIn(): String {
        val existing = auth.currentUser
        if (existing != null) {
            prefs.edit().putString(PREF_UID, existing.uid).apply()
            return existing.uid
        }
        val result = auth.signInAnonymously().await()
        val uid = requireNotNull(result.user?.uid) { "Anonymous sign-in succeeded but uid is null" }
        prefs.edit().putString(PREF_UID, uid).apply()
        return uid
    }
}
