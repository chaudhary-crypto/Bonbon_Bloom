# Bonbon Bloom — Multi-Store Publishing & Distribution Guide

**Application:** Bonbon Bloom  
**Package Identifier:** `com.bonbonbloom.game`  
**Version:** `12.11.16` (Code: `121116`)  
**Target Stores:** Google Play Store, Samsung Galaxy Store, Amazon Appstore, Huawei AppGallery, Uptodown  

---

## 1. Store Compatibility & Distribution Matrix

| Store | Recommended Format | Billing System | Ads Configuration | Account Deletion URL |
| :--- | :--- | :--- | :--- | :--- |
| **Google Play Store** | `.aab` (Android App Bundle) | Google Play Billing Library 7.x | Google Mobile Ads SDK (AdMob) | Required (`delete-account.html`) |
| **Samsung Galaxy Store** | `.apk` (Universal Release) | Play Billing or Direct Store | AdMob / Fallback | Recommended |
| **Amazon Appstore** | `.apk` (Universal Release) | Fallback / Amazon IAP Ready | Non-GMS Safe Fallback | Recommended |
| **Huawei AppGallery** | `.apk` (Universal Release) | HMS / Fallback | Non-GMS Safe Fallback | Required |
| **Uptodown** | `.apk` (Universal Release) | Direct / Non-GMS Safe | Non-GMS Safe Fallback | Supported |

---

## 2. Store Submission Procedures

### 2.1. Google Play Console

1. **Dashboard & Track Selection:**
   - Log into [Google Play Console](https://play.google.com/console).
   - Select `Production` or `Closed Testing`.
   - Upload `app/build/outputs/bundle/release/app-release.aab`.

2. **Data Safety Form Answers:**
   - **Does your app collect or share user data?** Yes.
   - **Data Types Collected:**
     - *User IDs / Account Email:* For authentication and cloud sync via Firebase. Optional; users can play as guest.
     - *Advertising ID:* Collected by Google Mobile Ads SDK for ad serving and fraud detection.
   - **Is data encrypted in transit?** Yes (HTTPS / TLS 1.3).
   - **Do users have a way to request that their data is deleted?** Yes.
     - Enter in-app deletion instructions.
     - Enter Public Deletion URL: `https://<your-domain>/delete-account.html`.

3. **Target Audience & Content Rating:**
   - Target Age: 13 and older (General Audience).
   - IARC Questionnaire: Select "Game" ➡️ "Puzzle / Match-3". No violent or sensitive content. Rated PEGI 3 / ESRB Everyone.

4. **App Access:** All features accessible without special credentials. (For testing, provide a test account or guest access).

---

### 2.2. Samsung Galaxy Store

1. Log into [Samsung Galaxy Developers Seller Portal](https://seller.samsungapps.com/).
2. Click **Add New Application** ➡️ Select **Android**.
3. Upload `app/build/outputs/apk/release/app-release.apk` (or copy from `./BonbonBloom.apk`).
4. Set Binary Type: Universal (supports 64-bit ARM, 32-bit ARM).
5. Add game screenshots, description, and privacy policy URL.

---

### 2.3. Amazon Appstore

1. Log into [Amazon Developer Console](https://developer.amazon.com/).
2. Select **Apps & Services** ➡️ **Add New App** ➡️ **Android**.
3. Upload `app-release.apk`.
4. Ensure **Fire OS / Non-Google Devices** support is enabled. (Bonbon Bloom handles missing GMS services gracefully without crashing).

---

### 2.4. Huawei AppGallery

1. Log into [Huawei Developer Console](https://developer.huawei.com/consumer/en/service/josp/agc/index.html).
2. Create new Project & App.
3. Upload `app-release.apk`.
4. Provide Data Privacy and User Deletion link (`delete-account.html`).

---

### 2.5. Uptodown Developer Console

1. Log into [Uptodown Developer Zone](https://developers.uptodown.com/).
2. Register the application package: `com.bonbonbloom.game`.
3. Provide the direct APK download link or upload `BonbonBloom.apk`.
4. Provide changelog: *"Version 12.11.16: Enhanced matching engine, zero-trust cloud sync, 1,000 levels, improved puzzle physics."*
