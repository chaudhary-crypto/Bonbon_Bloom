# Bonbon Bloom — Master Multi-Store Release Checklist

**Application Title:** Bonbon Bloom  
**Package Identifier:** `com.bonbonbloom.game`  
**Target Release Version:** `12.11.16` (Version Code: `121116`)  
**Target Stores:** Google Play Store, Samsung Galaxy Store, Amazon Appstore, Huawei AppGallery, Uptodown  

---

## Phase 1: Pre-Release Verification & Codebase Checks
- [x] **SDK Targets:** `compileSdk 36`, `targetSdk 36`, `minSdk 24`.
- [x] **Gradle Wrapper:** Restored and pinned to official Gradle 8.11.1 distribution.
- [x] **AdMob Security:** Rewarded ads strictly award on server callback (`ad.show { reward -> ... }`). No fallback awards on load failures.
- [x] **Frequency Capping:** Interstitial cooldown of $\ge 120$ seconds, no ads on levels 1–10, never during active cascades.
- [x] **In-App Billing:** Google Play Billing 7.x initialized with duplicate token rejection (`processedPurchaseTokens`).
- [x] **Zero-Trust Firestore:** Rules configured in `firestore.rules` preventing unauthorized writes, bounds overflow, and privilege escalation.
- [x] **Account Deletion:** Dual-channel deletion supported:
  - In-App: Settings ➡️ Delete My Account (purges Firestore, Firebase Auth, Room).
  - Web: Public deletion page available at `/delete-account.html`.
- [x] **Unit Testing:** Automated JVM unit test suite passed without regressions.
- [x] **CI/CD:** Automated GitHub Actions build pipeline configured at `.github/workflows/android-build-release.yml`.

---

## Phase 2: Google Play Store Release
- [ ] Log in to Google Play Console.
- [ ] Select **Release ➡️ Production** (or Closed Testing).
- [ ] Upload Android App Bundle: `app/build/outputs/bundle/release/app-release.aab`.
- [ ] **Data Safety Form:**
  - Data Collected: User ID (Firebase UID/email for cloud saves), Advertising ID (AdMob for ad delivery).
  - All data encrypted in transit (TLS 1.3).
  - Account deletion URL: `https://<your-domain>/delete-account.html`.
- [ ] **IARC Content Rating:** Questionnaire completed as Casual/Puzzle Match-3. Rated PEGI 3 / ESRB Everyone.
- [ ] **Target Audience:** General Audience (13+).
- [ ] **In-App Products Live:** Verify `coins_500`, `coins_1500`, `coins_5000`, `gems_50`, `gems_200`, `gems_600`, `remove_ads`, and `vip_monthly` are active.

---

## Phase 3: Google AdMob Approval & Linking
- [ ] Ensure `app-ads.txt` is accessible at the root of your developer domain (e.g. `https://yourdomain.com/app-ads.txt`).
- [ ] In Google AdMob Console, link app to the live Google Play listing.
- [ ] Enable Google User Messaging Platform (UMP) GDPR consent message for EEA/UK users.
- [ ] Replace test ad-unit placeholders with live production AdMob unit IDs in production build environment.

---

## Phase 4: Samsung Galaxy Store
- [ ] Log in to Samsung Developer Seller Portal.
- [ ] Upload universal release APK: `app-release.apk` (or `BonbonBloom.apk`).
- [ ] Set Category: Games ➡️ Puzzle.
- [ ] Attach promotional banners and 1080x1920 screenshots.

---

## Phase 5: Amazon Appstore
- [ ] Log in to Amazon Developer Console.
- [ ] Add new Android app with package `com.bonbonbloom.game`.
- [ ] Upload `app-release.apk`.
- [ ] Select Non-GMS compatibility flag (graceful fallback verified).

---

## Phase 6: Huawei AppGallery & Uptodown
- [ ] Upload `app-release.apk` to Huawei AppGallery Connect.
- [ ] Submit `BonbonBloom.apk` to Uptodown Developer Zone.
- [ ] Publish direct download landing page (`public/index.html`).
