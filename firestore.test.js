const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const pastTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-candycus-ab354175-b4a1-417f-a1ac-88c83a67355a";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validInitialProfile() {
  return {
    username: "Alice",
    email: "alice@example.com",
    authProvider: "GOOGLE",
    highestUnlockedLevel: 1,
    coins: 500,
    diamonds: 25,
    lives: 5,
    hammerCount: 3,
    shuffleCount: 2,
    colorBlastCount: 2,
    extraMovesCount: 2,
    magicWandCount: 1,
    cosmicBlastCount: 1,
    isVipSubscribed: false,
    vipExpiryTimestamp: 0,
    hasRemovedAds: false,
    isAdmin: false,
    soundEnabled: true,
    musicEnabled: true,
    hapticsEnabled: true,
    languageCode: "en",
    lastCloudSyncTime: Date.now(),
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function getDb(context) {
  // Single named DB target matching firebase.json
  return context.firestore();
}

test("Unauthenticated caller cannot read or write any collection", async () => {
  const unauthDb = getDb(testEnv.unauthenticatedContext());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validInitialProfile()));
  await assertFails(
    unauthDb.collection("account_deletion_requests").doc("DEL-1").set({
      requestId: "DEL-1",
      email: "a@b.com",
      status: "PENDING",
      timestamp: Date.now(),
    })
  );
});

test("Authenticated owner can create non-premium initial profile", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validInitialProfile()));
});

test("Client CANNOT self-grant free VIP, removeAds, admin, extra coins, or ghost fields on create", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validInitialProfile(),
      isVipSubscribed: true,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validInitialProfile(),
      hasRemovedAds: true,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validInitialProfile(),
      isAdmin: true,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validInitialProfile(),
      coins: 99999,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validInitialProfile(),
      ghostField: "hacked",
    })
  );
});

test("Owner can update settings and monotonic level progress, but CANNOT tamper VIP/coins/diamonds/admin", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const aliceDoc = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(aliceDoc.set(validInitialProfile()));

  // Allowed settings & monotonic level progress update
  await assertSucceeds(
    aliceDoc.update({
      soundEnabled: false,
      highestUnlockedLevel: 5,
      updatedAt: pastTimestamp(),
    })
  );

  // Disallowed: downgrade level
  await assertFails(
    aliceDoc.update({
      highestUnlockedLevel: 2,
      updatedAt: pastTimestamp(),
    })
  );

  // Disallowed: self-grant VIP
  await assertFails(
    aliceDoc.update({
      isVipSubscribed: true,
      updatedAt: pastTimestamp(),
    })
  );

  // Disallowed: self-grant Remove Ads
  await assertFails(
    aliceDoc.update({
      hasRemovedAds: true,
      updatedAt: pastTimestamp(),
    })
  );

  // Disallowed: inject coins
  await assertFails(
    aliceDoc.update({
      coins: 500000,
      updatedAt: pastTimestamp(),
    })
  );

  // Disallowed: self-promote admin
  await assertFails(
    aliceDoc.update({
      isAdmin: true,
      updatedAt: pastTimestamp(),
    })
  );
});

test("Cross-user isolation: Bob cannot read, update, or delete Alice profile or progress", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validInitialProfile()));

  const bobDb = getDb(testEnv.authenticatedContext(BOB_UID));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      username: "Hijacked",
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).delete());
});

test("Level progress validates bounds and monotonic progression", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const lvlRef = aliceDb.collection("users").doc(ALICE_UID).collection("level_progress").doc("12");

  await assertSucceeds(
    lvlRef.set({
      levelId: 12,
      stars: 2,
      highScore: 4500,
      completed: true,
      updatedAt: pastTimestamp(),
    })
  );

  // Higher score & stars succeed
  await assertSucceeds(
    lvlRef.update({
      levelId: 12,
      stars: 3,
      highScore: 8900,
      completed: true,
      updatedAt: pastTimestamp(),
    })
  );

  // Out-of-bounds stars fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("level_progress").doc("13").set({
      levelId: 13,
      stars: 5,
      highScore: 1000,
      completed: true,
      updatedAt: pastTimestamp(),
    })
  );
});

test("Purchases, ledger, and reward_requests are strictly read-only for clients (anti-free-premium)", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);

  // Client attempting to forge a purchase document must fail
  await assertFails(
    aliceRef.collection("purchases").doc("fake_token_1").set({
      productId: "vip_monthly",
      purchaseToken: "fake",
      status: "VERIFIED",
    })
  );

  // Client attempting to forge a ledger entry must fail
  await assertFails(
    aliceRef.collection("ledger").doc("fake_order_1").set({
      productId: "vip_monthly",
      status: "VERIFIED_AND_CREDITED",
    })
  );

  // Client attempting to forge an SSV reward entry must fail
  await assertFails(
    aliceRef.collection("reward_requests").doc("fake_ad_1").set({
      transactionId: "fake_ad_1",
      amount: 1,
    })
  );

  // Owner CAN list/read server-written purchases and ledger records
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("users").doc(ALICE_UID).collection("purchases").doc("tok_hash_1").set({
      orderId: "GPA.123",
      productId: "vip_monthly",
      status: "VERIFIED",
    });
  });

  await assertSucceeds(aliceRef.collection("purchases").get());
  await assertSucceeds(aliceRef.collection("purchases").doc("tok_hash_1").get());
});
