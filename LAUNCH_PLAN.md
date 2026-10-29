# Bonbon Bloom — Launch & Staged Rollout Plan

This launch strategy ensures maximum initial rating momentum, early crash detection, and stable revenue scaling.

---

## Stage 1: Internal Testing (Days 1–3)
* **Audience:** Developers and internal stakeholders (up to 100 testers).
* **Focus Areas:**
  * Cold start latency (< 2.0s on low-end test device).
  * Smooth 60 FPS gameplay and zero memory leaks.
  * Google Play Billing license testing (Google test accounts).
  * AdMob test ad display and UMP consent popup on fresh install.
  * Background process death & phone call recovery test.

---

## Stage 2: Closed Testing (Days 4–18)
* **Audience:** 20+ opt-in testers active continuously for at least 14 days (Google Play Developer requirement for personal accounts).
* **Metrics to Track:**
  * Crash-free user rate $\ge 99.5\%$.
  * Day-1 retention $\ge 42\%$.
  * Average level win rate on levels 1–10 between 85%–95%.
  * Lucky wheel spin and booster inventory stability.
* **Exit Criteria:** 14 consecutive days of active testing with zero critical ANRs or crash spikes.

---

## Stage 3: Open Testing / Pre-Registration (Days 19–25)
* **Target Markets:** Tier-2/Tier-3 test regions (e.g. Philippines, Poland, Brazil) to validate server scalability, ad eCPMs, and organic funnel conversion.
* **A/B Testing:**
  * Test Icon Variant A (Glossy Cherry Bonbon) vs Variant B (Rainbow Sprinkle Bloom).
  * Test Screenshot orders in Play Console store experiments.

---

## Stage 4: Production Staged Rollout

| Phase | Rollout % | Duration | Health Metrics to Validate Before Increasing |
|---|---|---|---|
| **Step 1** | **5%** | 48 hours | Crash rate $< 0.5\%$, ANR rate $< 0.2\%$, In-app purchase acknowledgment success $= 100\%$ |
| **Step 2** | **20%** | 72 hours | Ad delivery rate $\ge 85\%$, Day-1 retention $\ge 38\%$, D7 return tracking |
| **Step 3** | **50%** | 48 hours | Customer support tickets $< 0.1\%$ of active users |
| **Step 4** | **100%** | Global | Full release scaling |

---

## Stage 5: Post-Launch LiveOps & Optimization
* Monitor Google Play Console **Android Vitals** daily.
* Adjust level difficulty multipliers remotely via Remote Config if any specific level displays unexpected choke points ($< 15\%$ win rate).
* Trigger Google In-App Review API after 3-win streaks to collect authentic 5-star reviews.
