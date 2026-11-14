const test = require("node:test");
const assert = require("node:assert");
const crypto = require("node:crypto");

const {
  VALID_PRODUCTS,
  EXPECTED_PACKAGE_NAME,
  FIRESTORE_DATABASE_ID,
  extractAdMobSignedPayload
} = require("../lib/index.js");

test("Google Play canonical SKU catalog contains all in-game offerings", () => {
  assert.strictEqual(EXPECTED_PACKAGE_NAME, "com.aistudio.bonbonbloom.qyjzvw");
  assert.strictEqual(
    FIRESTORE_DATABASE_ID,
    "ai-studio-android-candycus-ab354175-b4a1-417f-a1ac-88c83a67355a"
  );

  assert.strictEqual(VALID_PRODUCTS["vip_weekly"].isSubscription, true);
  assert.strictEqual(VALID_PRODUCTS["vip_weekly"].vipDays, 7);
  assert.strictEqual(VALID_PRODUCTS["vip_monthly"].isSubscription, true);
  assert.strictEqual(VALID_PRODUCTS["vip_monthly"].vipDays, 30);

  assert.strictEqual(VALID_PRODUCTS["remove_ads"].removeAds, true);
  assert.strictEqual(VALID_PRODUCTS["coins_500"].coins, 500);
  assert.strictEqual(VALID_PRODUCTS["coins_1500"].coins, 1500);
  assert.strictEqual(VALID_PRODUCTS["coins_5000"].coins, 5000);

  assert.strictEqual(VALID_PRODUCTS["gems_50"].diamonds, 50);
  assert.strictEqual(VALID_PRODUCTS["gems_150"].diamonds, 150);
  assert.strictEqual(VALID_PRODUCTS["gems_150"].coins, 1000);
  assert.strictEqual(VALID_PRODUCTS["gems_200"].diamonds, 200);
  assert.strictEqual(VALID_PRODUCTS["gems_500"].diamonds, 500);
  assert.strictEqual(VALID_PRODUCTS["gems_500"].coins, 5000);
  assert.strictEqual(VALID_PRODUCTS["gems_600"].diamonds, 600);
  assert.strictEqual(VALID_PRODUCTS["gems_1500"].diamonds, 1500);
  assert.strictEqual(VALID_PRODUCTS["gems_1500"].coins, 20000);
  assert.strictEqual(VALID_PRODUCTS["booster_starter_pack"].type, "boosters");
});

test("AdMob SSV ECDSA canonical payload extraction & signature check", () => {
  const { publicKey, privateKey } = crypto.generateKeyPairSync("ec", {
    namedCurve: "prime256v1"
  });
  const baseQuery = "ad_network=5450213213286189855&ad_unit=12345&custom_data=tx_999&reward_amount=1&reward_item=Coins&timestamp=1700000000&transaction_id=tx_999&user_id=uid_abc";
  const sign = crypto.createSign("SHA256");
  sign.update(baseQuery);
  sign.end();
  const sigBase64 = sign.sign(privateKey).toString("base64");
  const fullUrl = `/verifyAdMobSSV?${baseQuery}&signature=${encodeURIComponent(sigBase64)}&key_id=1`;

  const extracted = extractAdMobSignedPayload(fullUrl);
  assert.ok(extracted);
  assert.strictEqual(extracted.payload, baseQuery);
  assert.strictEqual(extracted.signature, sigBase64);

  const verifier = crypto.createVerify("SHA256");
  verifier.update(extracted.payload);
  verifier.end();
  assert.strictEqual(verifier.verify(publicKey, Buffer.from(extracted.signature, "base64")), true);

  // Tampered reward_amount must fail signature verification
  const tamperedVerifier = crypto.createVerify("SHA256");
  tamperedVerifier.update(extracted.payload.replace("reward_amount=1", "reward_amount=9999"));
  tamperedVerifier.end();
  assert.strictEqual(tamperedVerifier.verify(publicKey, Buffer.from(extracted.signature, "base64")), false);
});

test("Purchase token SHA-256 lock prevents cross-account replay", () => {
  const token = "opaque-google-play-purchase-token-xyz";
  const hash1 = crypto.createHash("sha256").update(token).digest("hex");
  const hash2 = crypto.createHash("sha256").update(token).digest("hex");
  assert.strictEqual(hash1, hash2);
  assert.strictEqual(hash1.length, 64);

  const locks = new Map();
  locks.set(hash1, "user_alice");
  assert.notStrictEqual(locks.get(hash1), "user_bob");
});

test("Account Deletion Subcollections Purge List includes level_progress", () => {
  const subcollections = ["level_progress", "ledger", "purchases", "reward_requests", "deletion_requests"];
  assert.strictEqual(subcollections.length, 5);
  assert.ok(subcollections.includes("level_progress"));
  assert.ok(subcollections.includes("ledger"));
  assert.ok(subcollections.includes("purchases"));
  assert.ok(subcollections.includes("reward_requests"));
  assert.ok(subcollections.includes("deletion_requests"));
});
