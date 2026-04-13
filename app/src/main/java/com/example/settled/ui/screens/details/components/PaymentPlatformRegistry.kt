package com.example.settled.ui.screens.details.components

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.settled.R

/**
 * Central registry for payment platform entries shown in the "Record Payment" bottom sheet.
 *
 * ──────────────────────────────────────────────────────────────────
 *  HOW TO ADD A REAL LOGO
 * ──────────────────────────────────────────────────────────────────
 *  1. Drop the logo file into:
 *       app/src/main/res/drawable/payment_platforms/
 *     Use the naming convention:  logo_payment_<id_lowercase>.png
 *     e.g.  logo_payment_cred.png
 *
 *  2. Set `logoRes` for the matching entry below:
 *       logoRes = R.drawable.logo_payment_cred
 *
 *  3. Rebuild — the UI will automatically switch from the fallback
 *     Material icon to the real logo image.
 * ──────────────────────────────────────────────────────────────────
 *
 *  To add a brand new platform, append a new PaymentPlatform entry
 *  to the [all] list and drop the matching drawable.
 */
data class PaymentPlatform(
    /** Unique stable identifier used internally (e.g. stored in DB). */
    val id: String,
    /** Short label shown below the platform icon in the UI. */
    val displayLabel: String,
    /** Material icon shown while the real logo is not yet wired in. */
    val fallbackIcon: ImageVector,
    /**
     * Drawable resource for the real logo.
     * Set to null until the actual logo PNG is added.
     * Logo files live in: res/drawable/payment_platforms/
     */
    @DrawableRes val logoRes: Int? = null
)

object PaymentPlatformRegistry {

    val all: List<PaymentPlatform> = listOf(
        PaymentPlatform(
            id = "BANK_APP",
            displayLabel = "BANK APP",
            fallbackIcon = Icons.Default.AccountBalance,
            logoRes = null  // TODO: R.drawable.logo_payment_bank_app
        ),
        PaymentPlatform(
            id = "CRED",
            displayLabel = "CRED",
            fallbackIcon = Icons.Default.FlashOn,
            logoRes = null  // TODO: R.drawable.logo_payment_cred
        ),
        PaymentPlatform(
            id = "GPAY",
            displayLabel = "GPAY",
            fallbackIcon = Icons.Default.PhoneAndroid,
            logoRes = null  // TODO: R.drawable.logo_payment_gpay
        ),
        PaymentPlatform(
            id = "AMAZON_PAY",
            displayLabel = "AMAZON PAY",
            fallbackIcon = Icons.Default.ShoppingBag,
            logoRes = null  // TODO: R.drawable.logo_payment_amazon_pay
        ),
        PaymentPlatform(
            id = "PHONEPE",
            displayLabel = "PHONEPE",
            fallbackIcon = Icons.Default.Payment,
            logoRes = null  // TODO: R.drawable.logo_payment_phonepe
        ),
        PaymentPlatform(
            id = "PAYTM",
            displayLabel = "PAYTM",
            fallbackIcon = Icons.Default.AccountBalanceWallet,
            logoRes = null  // TODO: R.drawable.logo_payment_paytm
        ),
    )

    fun findById(id: String): PaymentPlatform? = all.find { it.id == id }
}
