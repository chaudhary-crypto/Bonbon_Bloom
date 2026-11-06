# Bonbon Bloom — Google Play Console Publishing Guide

**Application Title:** Bonbon Bloom  
**Package Identifier:** `com.bonbonbloom.game`  
**Version:** `12.11.16` (Version Code: `121116`)  
**Artifact Format:** Android App Bundle (`.aab`)  
**AAB Path:** `app/build/outputs/bundle/release/BonbonBloom-release.aab` (20 MB)  
**Developer Website:** `https://www.earnvero.app`  
**Public Support Contact:** `support@earnvero.app`  

---

## 1. Release Track Upload

1. Log into [Google Play Console](https://play.google.com/console).
2. Select **Bonbon Bloom** (`com.bonbonbloom.game`).
3. Navigate to **Release > Production** (or **Testing > Closed testing**).
4. Click **Create new release**.
5. Upload the signed App Bundle:  
   `app/build/outputs/bundle/release/BonbonBloom-release.aab`.
6. Add Release Notes:
   ```
   Version 12.11.16:
   - Full 1,000 level puzzle adventure across 20 delicious worlds!
   - Enhanced candy matching engine with smooth 60 FPS cascades.
   - Google User Messaging Platform (UMP) privacy options in Settings.
   - Offline-friendly gameplay — play anytime, anywhere without WiFi.
   - Performance optimizations, bug fixes, and stability improvements.
   ```

---

## 2. Store Listing Metadata

* **App Title:** `Bonbon Bloom` *(12 / 30 characters)*
* **Short Description:** `Match delicious bonbons, blast juicy bloom combos & explore 1,000 sweet worlds!` *(78 / 80 characters)*
* **Category:** Games > Puzzle
* **Content Rating:** PEGI 3 / ESRB Everyone / IARC Everyone
* **Developer Contact Email:** `support@earnvero.app`
* **Developer Website:** `https://www.earnvero.app`
* **Privacy Policy URL:** `https://www.earnvero.app/privacy`

---

## 3. Data Safety Form Responses

* **Does your app collect or share user data?** Yes.
* **Data Types:**
  * *Email Address & User ID:* Optional (Firebase Authentication / Account Sync). Encrypted in transit. User can request deletion.
  * *Device or other identifiers (Advertising ID):* Handled by Google Mobile Ads SDK for advertising and analytics.
* **Account Deletion URL:** `https://www.earnvero.app/delete-account`

---

## 4. Ads & App Access Declarations

* **Does your app contain ads?** Yes (Google AdMob banner, interstitial, and rewarded ads).
* **App Access:** All features accessible without special login credentials (fully playable in guest mode).
