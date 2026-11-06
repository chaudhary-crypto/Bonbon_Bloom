# Bonbon Bloom — Release Changelog

All notable changes to the Bonbon Bloom mobile application and backend services are documented in this file.

---

## [12.11.16] (Version Code: 121116) - 2026-11-03

### Critical AdMob Remediation & Google UMP Compliance
- **Consent Initialization Lifecycle**: Removed premature `MobileAds.initialize()` from `GameApplication.onCreate()`. Initialization now strictly occurs after Google UMP consent evaluation in `MainActivity.kt`.
- **UMP Evaluation Hardening**: Default consent state in `ConsentManager.kt` is strictly `canRequestAds = false` until consent status has been evaluated by the UMP SDK. Added `AtomicBoolean` guards preventing duplicate initialization across Android activity recreation cycles.
- **Privacy Options in Settings**: Added `ConsentManager.showPrivacyOptionsForm(activity)` trigger within `SettingsDialog.kt` to allow users in the EEA, UK, and Switzerland to review or revoke their ad consent preferences at any time.
- **Elimination of Interstitial Policy Bypasses**: Permanently removed all `force = true` parameters from `AdManager.kt`, `GameScreen.kt`, and `SettingsDialog.kt`. Interstitial ads now obey a strict, centralized display policy:
  - Tutorial levels (Levels 1–10) are permanently exempt from interstitial ads.
  - Interstitial cooldown enforced at a minimum of 120 seconds, persisted in `SharedPreferences` (`bonbon_ad_prefs`).
  - Minimum of 3 successfully completed levels required between interstitial presentations, persisted in `bonbon_ad_prefs`.
  - Immediate suppression during board cascades and candy match animations (`isBoardAnimating == true`).
  - Debounce window: interstitial ads will not trigger within 2.0 seconds of player UI touch events.
  - Concurrency lock (`isFullScreenAdShowing`) prevents overlapping full-screen ad requests.
  - Active `remove_ads` or `vip_monthly` entitlements immediately suppress all banner and interstitial ads.
- **Rewarded Ad Security & SSV**:
  - Implemented AdMob Server-Side Verification (SSV) options (`ServerSideVerificationOptions`) passing authenticated Firebase user UID and unique cryptographically secure transaction IDs (`UUID.randomUUID()`).
  - Added atomic one-time reward processing with `AtomicBoolean` in `AdManager.kt` and persistent transaction deduplication set (`KEY_PROCESSED_REWARDS`) preventing replay or double-claim attacks.
  - Overload resolution for `showRewarded` hardened to avoid ambiguity between single-argument and zero-argument callback signatures.
- **Configuration & Separation of Environments**:
  - Configured `app/build.gradle.kts` with strict build flavor separation:
    - `debug`: Uses official Google AdMob test IDs (`ca-app-pub-3940256099942544/...`).
    - `release`: Validates real AdMob format via regex (`ca-app-pub-\d{16}~\d{10}`) and fails the build if production IDs are missing or configured as sample test IDs (with opt-in `ALLOW_TEST_ADS_IN_RELEASE=true` for CI validation).
  - Cleaned `app-ads.txt` and `public/app-ads.txt` by removing placeholder publisher IDs and specifying the standardized direct seller authorization format: `google.com, pub-################, DIRECT, f08c47fec0942fa0`.

---

### Zero-Trust Cloud Architecture & Firestore Security
- **Immutable Player Balances on Client**: Updated `firestore.rules` to prevent client-side modifications to privileged fields (`coins`, `diamonds`, `isVipSubscribed`, `vipExpiryTimestamp`, `hasRemovedAds`, `isAdmin`).
- **Monotonic Progression Enforcement**: Level progress updates from clients can only advance monotonically (`highestUnlockedLevel <= 1000 && request.resource.data.highestUnlockedLevel >= resource.data.highestUnlockedLevel`).
- **Append-Only Auditing Subcollections**: Added `/purchases/{purchaseId}` and `/reward_requests/{requestId}` subcollections restricted to append-only mode for verified transaction history. Added `/ledger/{entry}` subcollection readable only by user and writable only by Firebase Admin/Cloud Functions backend.
- **Firebase Cloud Functions Backend (`functions/`)**:
  - Implemented `verifyGooglePlayPurchase`: Validates Google Play Billing purchase tokens, checks for duplicate orders in `/ledger`, and safely credits currency and entitlements.
  - Implemented `verifyAdMobSSV`: Validates AdMob SSV callbacks with public key cryptographic signature verification and idempotent reward delivery.
  - Implemented `processAccountDeletion` and `onWebDeletionRequestCreated`: Automates complete cascade deletion of user records, ledger entries, purchase records, and authentication identities.

---

### Google Play Billing Robustness & Replay Prevention
- **Persistent Token Deduplication**: Replaced in-memory purchase token caching in `PlayBillingManager.kt` with persistent `SharedPreferences` storage (`KEY_PROCESSED_TOKENS`), preventing replay attacks across app restarts.
- **Two-Phase Crash-Resilient Consumption**: Added pending purchase staging (`KEY_PENDING_PURCHASES`). In-app currency items are committed to local profile balances before calling `consumeAsync`, with pending recoveries triggered on app launch.
- **Subscription Entitlement Revocation**: Added explicit check in `queryAndRestorePurchases()`. When active subscription queries return without an active `vip_monthly` token, local VIP status is immediately revoked.

---

### Account Deletion & Privacy Compliance
- **Atomic Two-Phase Local/Cloud Deletion**: Updated `UserRepository.deleteAccount()` to execute remote Firestore profile deletion before wiping local Room database records. If cloud deletion fails, local state is preserved and the player is notified.
- **Compliant Public Web Deletion Portal**: Updated `public/delete-account.html` and `docs/delete-account.html` to dispatch verified deletion requests to Firestore (`/account_deletion_requests`) with tracking IDs (`DEL-...`), accompanied by verified email fallback.
- **Data Backup & Extraction Rules**: Updated `data_extraction_rules.xml` and `backup_rules.xml` to exclude transient ad state (`bonbon_ad_prefs.xml`) while protecting player database records (`crush_candy_db`).

---

### Multi-Store Publishing & Build Automation
- **Unified Release Packaging**:
  - Generated and validated signed production Google Play App Bundle: `app/build/outputs/bundle/release/BonbonBloom-release.aab` (20 MB).
  - Generated and validated signed universal APK: `BonbonBloom.apk` / `public/BonbonBloom.apk` (21 MB).
- **Security Untracking**: Keystore files (`*.jks`, `*.keystore`) permanently excluded from Git index and configured for CI/CD via GitHub Secrets (`KEYSTORE_BASE64`).
- **Official Domain Alignment**: Standardized official developer domain `https://www.earnvero.app`, verified live endpoints (`/privacy`, `/terms`, `/delete-account`, `/ads.txt`), and updated BuildConfig URLs.
- **Backend Purchase Trigger**: Added `onPurchaseCreated` background Firestore trigger in `functions/src/index.ts` to automatically validate purchase tokens and credit server-authoritative balances.
- **100% Game Preservation**: All 1,000 levels, matching physics, special candy combos, animations, audio, and player profiles verified 100% intact.
