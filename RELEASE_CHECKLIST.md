# Bonbon Bloom — Google Play Console Release Checklist

Follow this step-by-step checklist to ensure a seamless Google Play Store submission and review approval.

---

### Phase 1: Google Play Console Account & Identity
- [ ] Create or log in to your verified **Google Play Developer Account**.
- [ ] Ensure D-U-N-S verification (for organizations) or identity verification (for individuals) is complete.
- [ ] Set up Merchant Account in Play Console for in-app purchases and subscriptions.

---

### Phase 2: Store Presence & Metadata Setup
- [ ] **App Title:** `Bonbon Bloom` (12 / 30 characters).
- [ ] **Short Description:** `Match delicious bonbons, blast juicy bloom combos & explore 1,000 sweet worlds!` (78 / 80 characters).
- [ ] **Full Description:** Copy from `STORE_LISTING.md`.
- [ ] **Hi-res Icon:** 512 x 512 px PNG (32-bit color with alpha, up to 1024KB).
- [ ] **Feature Graphic:** 1024 x 500 px JPEG or PNG.
- [ ] **Screenshots:** Minimum 4 screenshots (portrait phone 1080x1920 or 1080x2400) showcasing gameplay, world map, combos, and store.

---

### Phase 3: Content Rating & Policy Compliance
- [ ] Complete the **IARC Content Rating Questionnaire** using answers in `CONTENT_RATING_ANSWERS.md` (Target: Everyone / PEGI 3).
- [ ] **Target Audience:** Select **13 and older** (General Audience). Do NOT declare "Children" to avoid COPPA / Families Policy friction.
- [ ] **Privacy Policy URL:** Host `PRIVACY_POLICY.md` on your domain or GitHub Pages and provide the live link in Play Console (`https://yourdomain.com/privacy`).
- [ ] Complete the **Data Safety Form** using the exact guidelines in `PRIVACY_AND_DATA_SAFETY.md`.
- [ ] Complete **Financial Features & Ads Declaration:**
  - Select "Contains Ads" = **Yes**.
  - No real-money gambling or real prizes.

---

### Phase 4: In-App Products & Subscriptions Setup
- [ ] Create the following Managed In-App Products in Play Console:
  - `remove_ads` (Non-consumable, e.g. $2.99)
  - `coins_500` (Consumable, $0.99)
  - `coins_1500` (Consumable, $2.49)
  - `coins_5000` (Consumable, $6.99)
  - `gems_50` (Consumable, $1.99)
  - `gems_200` (Consumable, $4.99)
  - `gems_600` (Consumable, $9.99)
  - `booster_starter_pack` (Consumable, $1.99)
- [ ] Create the Subscription Product:
  - Product ID: `vip_monthly`
  - Base Plan: Auto-renewing monthly ($7.99/mo) or weekly ($2.99/wk)
  - Cancellation Policy: Handled automatically by Google Play Subscriptions

---

### Phase 5: AdMob & UMP Setup
- [ ] Register `com.bonbonbloom.game` in your Google AdMob Console.
- [ ] Create App ID and Ad Units:
  - Adaptive Banner Unit
  - Interstitial Ad Unit
  - Rewarded Video Unit
- [ ] Configure European Regulations (GDPR) message in AdMob Privacy & Messaging (UMP).
- [ ] Link your AdMob app to your Google Play Console app once live.

---

### Phase 6: Build & Signing
- [ ] Generate your production release Keystore and store backup securely.
- [ ] Build release Android App Bundle:
  `gradle :app:bundleRelease`
- [ ] Test release build locally on a physical device.
- [ ] Upload `.aab` to Closed Testing Track first.
