# Bonbon Bloom — Security & Server-Side Receipt Validation Guide

## 1. Overview
Bonbon Bloom implements defense-in-depth security to protect player game balance, in-app purchases, and fair leaderboard competition.

### Core Security Features
1. **HMAC-SHA256 Anti-Cheat Data Signing**:
   - Every local save transaction computes a keyed cryptographic checksum over critical fields (`coins`, `gems`, `unlockedLevel`, `starsWon`, `vipActiveUntil`).
   - On load, if the signature fails verification, the app **quarantines** only the tampered currency rather than wiping the player's legitimate level progress, restoring the last trusted save state.
   - Rooted devices and debuggers are logged without abrupt crashes to maintain stability for developers and power users.

2. **Google Play Billing 7.x Security**:
   - All purchases require server-side/Play Store cryptographic token validation.
   - Acknowledgment (`acknowledgePurchase`) prevents Google Play from auto-refunding unfulfilled items within 3 days.
   - Pending purchase resolution (`isPendingPurchase = false`) guarantees items are only disbursed once payment clears.

---

## 2. Play Integrity API Scaffolding
Google Play Integrity API replaces SafetyNet to detect compromised environments, sideloaded APKs, and emulator tampering.

### Recommended Client Integration
```kotlin
// Integrity token request scaffold
val integrityManager = IntegrityManagerFactory.create(context)
val nonce = UUID.randomUUID().toString()

val request = IntegrityTokenRequest.builder()
    .setCloudProjectNumber(GOOGLE_CLOUD_PROJECT_NUMBER)
    .setNonce(nonce)
    .build()

integrityManager.requestIntegrityToken(request)
    .addOnSuccessListener { response ->
        val integrityToken = response.token()
        // Send integrityToken along with the purchase token to backend server
    }
    .addOnFailureListener { e ->
        Log.w("Integrity", "Integrity check deferred: ${e.message}")
    }
```

---

## 3. Server-Side Purchase & Subscription Receipt Validation
Never trust client-only purchase fulfillment for high-value items in production.

### Verification Flow Architecture
```
[Android App] --(purchaseToken + productId)--> [Your Secure Cloud Backend]
                                                         |
                                                         v
                                           [Google Play Developer API]
                                                         |
                                            (purchases.products.get /
                                             purchases.subscriptionsv2.get)
                                                         |
                                                         v
[Android App] <--(authorized entitlement)--- [Your Secure Cloud Backend]
```

### Steps for Production Go-Live:
1. **Google Cloud Service Account**:
   - In Google Play Console > **API Access**, link a Google Cloud Project.
   - Create a Service Account with "View financial data, orders, and cancellation survey responses" permissions.
   - Generate a JSON private key credentials file.

2. **Google Play Developer API Endpoint**:
   - Consumable/One-time products:
     `GET https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{packageName}/purchases/products/{productId}/tokens/{token}`
   - Subscriptions (VIP Pass):
     `GET https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{packageName}/purchases/subscriptionsv2/tokens/{token}`

3. **Validation Checks to Execute**:
   - `purchaseState == 0` (PURCHASED).
   - `consumptionState == 0` (YET_TO_BE_CONSUMED) before granting coins/gems.
   - Check duplicate `orderId` in your server database to defeat replay attacks.
   - For VIP subscription: verify `subscriptionState` is `SUBSCRIPTION_STATE_ACTIVE`.

4. **Real-time Developer Notifications (RTDN)**:
   - Configure Google Cloud Pub/Sub topic in Play Console to receive instant push alerts for subscription renewals, grace periods, cancellations, and refunds.
