# Google Play Data Safety & SDK Privacy Declarations — Bonbon Bloom

This document details every integrated SDK, data collected, data sharing practices, and Google Play Console Data Safety questionnaire answers for **Bonbon Bloom** (`com.aistudio.bonbonbloom.qyjzvw`, published by **Novaro Technologies**).

---

## 1. Overview of SDKs & Data Collection

| SDK / Component | Purpose | Data Collected | Shared with 3rd Parties? | Storage / Protection |
|---|---|---|---|---|
| **Google Mobile Ads (AdMob)** | Optional banner, interstitial, and rewarded video ads | Advertising ID (AAID), IP address, ad interaction telemetry | Yes (Google LLC for ad delivery & SSV fraud checks) | HTTPS/TLS, gated by UMP consent |
| **Google User Messaging Platform (UMP)** | EEA / UK GDPR & state privacy consent | Consent choice state & region | Yes (Google / IAB TCF) | Managed by UMP SDK |
| **Google Play Billing Library 8.0.0** | In-app purchases & VIP subscriptions | Order ID, Product ID, Purchase Token (SHA-256 hashed in backend lock) | Yes (Google Play Store) | Verified via Android Publisher API v3 |
| **Firebase Auth + Credential Manager** | Optional Google Sign-In cloud sync | Firebase UID, display name, email, avatar URL | Yes (Google Cloud Firebase) | Isolated per-UID zero-trust Firestore rules |
| **Cloud Firestore (Named DB)** | Cross-device save, level progress, purchase ledger | Player level progress, settings, verified purchase ledger | Yes (Google Cloud Firebase) | Encrypted in transit (TLS) & at rest |
| **Firebase App Check (reCAPTCHA Enterprise)** | Anti-abuse protection for backend endpoints | App attestation token | Yes (Google Cloud App Check) | Ephemeral token validation |
| **Android Room Database + WorkManager** | Offline-first cache & local notifications | Local game save, indexed purchase records | No (On-device only) | HMAC-SHA256 anti-tamper signature |

---

## 2. Google Play Console Data Safety Questionnaire Answers

### A. Data Collection and Security
1. **Does your app collect or share any of the required user data types?**
   - **Yes** (AdMob Advertising ID, diagnostic info, and optional Google Sign-In account/progress data).
2. **Is all of the user data collected by your app encrypted in transit?**
   - **Yes** (All Cloud Firestore, Cloud Functions, Play Billing, and AdMob traffic uses HTTPS/TLS).
3. **Do you provide a way for users to request that their data be deleted?**
   - **Yes** — Both **In-App** (*Settings > Delete Account & Data*) and **Web Deletion URL** (`https://chaudhary-crypto.github.io/Bonbon_Bloom/delete-account.html`).

---

### B. Data Types Breakdown

#### 1. Personal Info (Optional — Only for Google Sign-In players)
* **Data Type:** Name, Email address, User IDs
* **Collected:** Yes (when user taps Sign In with Google)
* **Shared:** No
* **Required or Optional:** Optional (Guest mode works offline without sign-in)
* **Purposes:** Account management, Cross-device cloud sync, Purchase restoration

#### 2. Financial Info (Purchases)
* **Data Type:** Purchase history (Order IDs, SKU IDs, purchase tokens)
* **Collected:** Yes (via Google Play Billing Library 8.0.0)
* **Shared:** No (Never shares credit/debit/UPI details with the developer)
* **Purposes:** App functionality, Server-side fraud prevention & entitlement verification

#### 3. Device or Other Identifiers
* **Data Type:** Device or other IDs (Advertising ID / AAID)
* **Collected:** Yes (for ad-supported non-VIP players)
* **Shared:** Yes (Google AdMob)
* **Purposes:** Advertising or marketing, Fraud prevention & security

---

## 3. Policy & Compliance Links
* **Business Category in Play Console:** Games (Puzzle)
* **Privacy Policy URL:** `https://chaudhary-crypto.github.io/Bonbon_Bloom/privacy.html`
* **Terms of Service URL:** `https://chaudhary-crypto.github.io/Bonbon_Bloom/terms.html`
* **Account Deletion URL:** `https://chaudhary-crypto.github.io/Bonbon_Bloom/delete-account.html`
* **Support Email:** `support@earnvero.app`
* **Target Audience:** General Audience (Age 13+). Not directed to children (`tagForChildDirectedTreatment = false`).
