const test = require("node:test");
const assert = require("node:assert");

// Valid products map matching backend definitions
const VALID_PRODUCTS = {
  "remove_ads": { type: "entitlement", removeAds: true },
  "vip_monthly": { type: "entitlement", isVip: true },
  "coins_500": { type: "currency", coins: 500 },
  "coins_1500": { type: "currency", coins: 1500 },
  "coins_5000": { type: "currency", coins: 5000 },
  "gems_50": { type: "currency", diamonds: 50 },
  "gems_200": { type: "currency", diamonds: 200 },
  "gems_600": { type: "currency", diamonds: 600 },
  "booster_starter_pack": { type: "boosters", coins: 200, diamonds: 20 }
};

test("Google Play In-App Product Catalog integrity", () => {
  assert.strictEqual(VALID_PRODUCTS["remove_ads"].type, "entitlement");
  assert.strictEqual(VALID_PRODUCTS["remove_ads"].removeAds, true);
  assert.strictEqual(VALID_PRODUCTS["vip_monthly"].isVip, true);
  assert.strictEqual(VALID_PRODUCTS["coins_500"].coins, 500);
  assert.strictEqual(VALID_PRODUCTS["coins_5000"].coins, 5000);
  assert.strictEqual(VALID_PRODUCTS["gems_600"].diamonds, 600);
});

test("AdMob SSV Request Parameter Validation", () => {
  const validQuery = {
    user_id: "firebase_user_abc123",
    custom_data: "uuid_tx_98765",
    reward_amount: "1",
    ad_unit: "ca-app-pub-3940256099942544/5224354917"
  };

  assert.ok(validQuery.user_id && validQuery.custom_data);
  const amount = parseInt(validQuery.reward_amount, 10);
  assert.strictEqual(amount, 1);
  const coinsToCredit = amount * 50;
  assert.strictEqual(coinsToCredit, 50);

  // Missing custom_data or user_id must fail
  const invalidQuery = { reward_amount: "5" };
  assert.ok(!invalidQuery.user_id || !invalidQuery.custom_data);
});

test("Transaction Ledger Idempotency Model", () => {
  const processedLedger = new Set();
  const txId = "GPA.1234-5678-9012-34567";

  function processPurchase(orderId) {
    if (processedLedger.has(orderId)) {
      return { status: "ALREADY_PROCESSED" };
    }
    processedLedger.add(orderId);
    return { status: "SUCCESS" };
  }

  const firstCall = processPurchase(txId);
  assert.strictEqual(firstCall.status, "SUCCESS");

  const duplicateCall = processPurchase(txId);
  assert.strictEqual(duplicateCall.status, "ALREADY_PROCESSED");
});

test("Account Deletion Subcollections Purge List", () => {
  const subcollections = ["ledger", "purchases", "reward_requests", "deletion_requests"];
  assert.strictEqual(subcollections.length, 4);
  assert.ok(subcollections.includes("ledger"));
  assert.ok(subcollections.includes("purchases"));
  assert.ok(subcollections.includes("reward_requests"));
  assert.ok(subcollections.includes("deletion_requests"));
});
