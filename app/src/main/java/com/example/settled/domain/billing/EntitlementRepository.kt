package com.example.settled.domain.billing

import android.app.Activity
import com.example.settled.core.Result
import kotlinx.coroutines.flow.StateFlow

interface EntitlementRepository {
    val isPro: StateFlow<Boolean>
    suspend fun launchBillingFlow(activity: Activity, sku: String): Result<Unit>
}
