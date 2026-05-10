package com.example.settled.data.billing

import android.app.Activity
import com.example.settled.core.Result
import com.example.settled.domain.billing.EntitlementRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntitlementRepositoryImpl @Inject constructor(
    private val billingManager: BillingManager
) : EntitlementRepository {

    override val isPro: StateFlow<Boolean> = billingManager.isPro

    override suspend fun launchBillingFlow(activity: Activity, sku: String): Result<Unit> {
        return billingManager.launchBillingFlow(activity, sku)
    }
}
