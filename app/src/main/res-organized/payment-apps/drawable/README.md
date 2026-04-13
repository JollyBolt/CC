# Payment Platform Logos

Drop real logo files here using the naming convention below.
Once added, set the matching `logoRes` in `PaymentPlatformRegistry.kt`.

| Platform    | File name                         | Registry ID   |
|-------------|-----------------------------------|---------------|
| Bank App    | `logo_payment_bank_app.png`       | `BANK_APP`    |
| CRED        | `logo_payment_cred.png`           | `CRED`        |
| Google Pay  | `logo_payment_gpay.png`           | `GPAY`        |
| Amazon Pay  | `logo_payment_amazon_pay.png`     | `AMAZON_PAY`  |
| PhonePe     | `logo_payment_phonepe.png`        | `PHONEPE`     |
| Paytm       | `logo_payment_paytm.png`          | `PAYTM`       |

## Recommended specs
- Format: **PNG** with transparent background (or WebP lossless)
- Size: **192 × 192 px** minimum (will be displayed at 48 dp)
- Shape: Rounded-square or circle — the UI clips to a 14 dp rounded rectangle automatically

## Wiring up
After dropping a file:
1. Open `PaymentPlatformRegistry.kt`
2. Find the matching `PaymentPlatform` entry
3. Change `logoRes = null` to `logoRes = R.drawable.logo_payment_<name>`
4. Rebuild — done!
