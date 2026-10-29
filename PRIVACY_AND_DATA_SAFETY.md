# Google Play Data Safety & SDK Privacy Declarations — Bonbon Bloom

This document details every integrated SDK, data collected, data sharing practices, and precise suggested answers for the Google Play Console Data Safety questionnaire.

---

## 1. Overview of SDKs & Data Collection

| SDK / Component | Purpose | Data Collected | Shared with 3rd Parties? | Ephemeral / Stored |
|---|---|---|---|---|
| **Google Mobile Ads (AdMob)** | In-game non-intrusive banner, interstitial, and rewarded ads | IP address, Advertising ID (AAID), coarse location derived from IP, ad interaction metrics | Yes (Google LLC for advertising and fraud prevention) | Processed by Google per Google Privacy Policy |
| **Google User Messaging Platform (UMP)** | GDPR / UK consent gathering | Consent string, region, device identifier | Yes (IAB TCF framework & Google) | Stored locally and transmitted for consent verification |
| **Google Play Billing Library 7.x** | In-app purchases (Coins, Gems, Boosters, VIP Pass) | Order ID, Purchase token, Product ID | Yes (Google Play Store) | Processed securely by Google Play |
| **Android Jetpack WorkManager** | Local offline background notifications (lives full, daily spin) | None (Local only) | No | Kept on device |
| **Android Room Database** | Local player game state, stars, levels, inventory | None (Local only) | No | Encrypted on device |
| **Firebase (Optional / Standalone)** | Optional cloud save & crash logs (only if configured) | User ID, crash logs, game level state | Yes (Google Cloud Firebase if enabled) | Encrypted in transit |

---

## 2. Google Play Console Data Safety Questionnaire Answers

### A. Data Collection and Security
1. **Does your app collect or share any of the required user data types?**
   - **Yes** (Due to AdMob Advertising ID and crash logs if enabled).
2. **Is all of the user data collected by your app encrypted in transit?**
   - **Yes** (All network communication uses HTTPS/TLS).
3. **Do you provide a way for users to request that their data be deleted?**
   - **Yes** (Bonbon Bloom provides a dedicated **"Delete My Data"** button directly inside in-game Settings, which wipes all local records and cloud associations).

---

### B. Data Types Breakdown

#### 1. Device or Other Identifiers
* **Data Type:** Device or other IDs (Advertising ID / AAID)
* **Collected:** Yes
* **Shared:** Yes (AdMob)
* **Processed ephemerally:** No
* **Required or Optional:** Required for ad-supported players
* **Purposes:**
  * Advertising or marketing
  * Analytics
  * Fraud prevention and security

#### 2. Financial Info (Purchases)
* **Data Type:** Purchase history
* **Collected:** Handled directly by Google Play Billing
* **Shared:** No (App receives purchase token only; no credit card or bank data is ever seen or collected by the app)
* **Purposes:** Account management, functionality

#### 3. App Info and Performance
* **Data Type:** Crash logs, Diagnostics
* **Collected:** Yes (for stability monitoring)
* **Shared:** Yes (Firebase Crashlytics if enabled)
* **Purposes:** Analytics, App functionality

---

## 3. Privacy Policy & Compliance Links
* **Privacy Policy URL:** `https://bonbonbloom.game/privacy`
* **Terms of Service URL:** `https://bonbonbloom.game/terms`
* **Target Audience:** General Audience (Age 13+). Not directed to children (`tagForChildDirectedTreatment = false`).
