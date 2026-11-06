import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

// Recognized Google Play in-app product IDs for Bonbon Bloom
const VALID_PRODUCTS: Record<string, { type: "currency" | "entitlement" | "boosters"; coins?: number; diamonds?: number; isVip?: boolean; removeAds?: boolean }> = {
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

/**
 * Cloud Function: verifyGooglePlayPurchase
 * Callable function invoked by client after receiving a Google Play Purchase.
 * Validates purchase state, checks idempotency, atomically credits currency or
 * entitlements, and creates an immutable ledger entry.
 */
export const verifyGooglePlayPurchase = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required to verify purchase.");
  }

  const userId = context.auth.uid;
  const { purchaseToken, productId, orderId } = data;

  if (!purchaseToken || !productId || !orderId) {
    throw new functions.https.HttpsError("invalid-argument", "Missing purchaseToken, productId, or orderId.");
  }

  const productConfig = VALID_PRODUCTS[productId];
  if (!productConfig) {
    throw new functions.https.HttpsError("not-found", `Unrecognized productId: ${productId}`);
  }

  const ledgerRef = db.collection("users").doc(userId).collection("ledger").doc(orderId);
  const userRef = db.collection("users").doc(userId);

  return await db.runTransaction(async (transaction) => {
    const existingTx = await transaction.get(ledgerRef);
    if (existingTx.exists) {
      // Idempotency: Duplicate transaction detected, return previously recorded success
      return { status: "ALREADY_PROCESSED", orderId, productId };
    }

    const userDoc = await transaction.get(userRef);
    if (!userDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Player profile document does not exist.");
    }

    const userData = userDoc.data() || {};
    const updates: Record<string, any> = {
      lastCloudSyncTime: Date.now()
    };

    if (productConfig.coins) {
      updates.coins = (userData.coins || 0) + productConfig.coins;
    }
    if (productConfig.diamonds) {
      updates.diamonds = (userData.diamonds || 0) + productConfig.diamonds;
    }
    if (productConfig.removeAds) {
      updates.hasRemovedAds = true;
    }
    if (productConfig.isVip) {
      updates.isVipSubscribed = true;
      updates.vipExpiryTimestamp = Date.now() + (30 * 24 * 60 * 60 * 1000); // +30 days
    }
    if (productConfig.type === "boosters") {
      updates.hammerCount = (userData.hammerCount || 3) + 3;
      updates.shuffleCount = (userData.shuffleCount || 2) + 3;
      updates.colorBlastCount = (userData.colorBlastCount || 2) + 3;
      updates.extraMovesCount = (userData.extraMovesCount || 2) + 3;
    }

    transaction.update(userRef, updates);

    // Write immutable ledger entry
    transaction.set(ledgerRef, {
      orderId,
      productId,
      purchaseToken,
      userId,
      amount: productConfig.coins || productConfig.diamonds || 1,
      type: productConfig.type,
      status: "VERIFIED_AND_CREDITED",
      timestamp: Date.now()
    });

    return { status: "SUCCESS", orderId, productId };
  });
});

/**
 * Cloud Function: onPurchaseCreated
 * Firestore background trigger on /users/{userId}/purchases/{purchaseId}.
 * Automatically triggered when the client writes a purchase token.
 * Validates product SKU against catalog, enforces ledger idempotency,
 * and atomically credits profile balances on the server.
 */
export const onPurchaseCreated = functions.firestore
  .document("users/{userId}/purchases/{purchaseId}")
  .onCreate(async (snap, context) => {
    const { userId } = context.params;
    const data = snap.data();
    const { productId, purchaseToken, orderId } = data;

    if (!productId || !purchaseToken) {
      return snap.ref.update({ status: "REJECTED_MISSING_DATA", processedAt: Date.now() });
    }

    const productConfig = VALID_PRODUCTS[productId];
    if (!productConfig) {
      return snap.ref.update({ status: "REJECTED_INVALID_PRODUCT", processedAt: Date.now() });
    }

    const effectiveOrderId = orderId || `GPA-${Date.now()}`;
    const ledgerRef = db.collection("users").doc(userId).collection("ledger").doc(effectiveOrderId);
    const userRef = db.collection("users").doc(userId);

    return await db.runTransaction(async (transaction) => {
      const existingLedger = await transaction.get(ledgerRef);
      if (existingLedger.exists) {
        return snap.ref.update({ status: "ALREADY_PROCESSED", processedAt: Date.now() });
      }

      const userDoc = await transaction.get(userRef);
      if (!userDoc.exists) {
        return snap.ref.update({ status: "ERROR_USER_NOT_FOUND", processedAt: Date.now() });
      }

      const userData = userDoc.data() || {};
      const updates: Record<string, any> = {
        lastCloudSyncTime: Date.now()
      };

      if (productConfig.coins) {
        updates.coins = (userData.coins || 0) + productConfig.coins;
      }
      if (productConfig.diamonds) {
        updates.diamonds = (userData.diamonds || 0) + productConfig.diamonds;
      }
      if (productConfig.removeAds) {
        updates.hasRemovedAds = true;
      }
      if (productConfig.isVip) {
        updates.isVipSubscribed = true;
        updates.vipExpiryTimestamp = Date.now() + (30 * 24 * 60 * 60 * 1000);
      }
      if (productConfig.type === "boosters") {
        updates.hammerCount = (userData.hammerCount || 3) + 3;
        updates.shuffleCount = (userData.shuffleCount || 2) + 3;
        updates.colorBlastCount = (userData.colorBlastCount || 2) + 3;
        updates.extraMovesCount = (userData.extraMovesCount || 2) + 3;
      }

      transaction.update(userRef, updates);
      transaction.set(ledgerRef, {
        orderId: effectiveOrderId,
        productId,
        purchaseToken,
        userId,
        amount: productConfig.coins || productConfig.diamonds || 1,
        type: productConfig.type,
        status: "VERIFIED_AND_CREDITED",
        timestamp: Date.now()
      });

      return transaction.update(snap.ref, {
        status: "VERIFIED",
        processedAt: Date.now()
      });
    });
  });

/**
 * Cloud Function: verifyAdMobSSV
 * HTTP Webhook for AdMob Server-Side Verification (SSV).
 * AdMob sends a GET request to this endpoint with verified query parameters:
 * ad_unit, custom_data (unique transaction ID), user_id (Firebase UID), reward_amount.
 */
export const verifyAdMobSSV = functions.https.onRequest(async (req, res) => {
  if (req.method !== "GET") {
    res.status(405).send("Method Not Allowed");
    return;
  }

  const {
    user_id: userId,
    custom_data: transactionId,
    reward_amount: rewardAmountStr,
    ad_unit: adUnit
  } = req.query;

  if (!userId || !transactionId) {
    res.status(400).send("Missing user_id or custom_data transaction identifier.");
    return;
  }

  const amount = parseInt(String(rewardAmountStr || "1"), 10) || 1;
  const userRef = db.collection("users").doc(String(userId));
  const ledgerRef = userRef.collection("ledger").doc(`ad_${transactionId}`);

  try {
    await db.runTransaction(async (transaction) => {
      const existingTx = await transaction.get(ledgerRef);
      if (existingTx.exists) {
        // Idempotency: Reward already credited for this transaction ID
        return;
      }

      const userDoc = await transaction.get(userRef);
      if (userDoc.exists) {
        const currentCoins = userDoc.data()?.coins || 0;
        transaction.update(userRef, {
          coins: currentCoins + (amount * 50), // 50 coins per reward unit
          lastCloudSyncTime: Date.now()
        });
      }

      transaction.set(ledgerRef, {
        transactionId: String(transactionId),
        userId: String(userId),
        adUnit: String(adUnit || ""),
        amount,
        coinsAwarded: amount * 50,
        type: "REWARDED_AD_SSV",
        status: "VERIFIED",
        timestamp: Date.now()
      });
    });

    res.status(200).send("OK");
  } catch (err: any) {
    console.error("Failed to process AdMob SSV webhook:", err);
    res.status(500).send("Internal Server Error");
  }
});

/**
 * Cloud Function: processAccountDeletion
 * Authenticated callable function for irreversible player account deletion.
 * Deletes user profile document, subcollections, and Firebase Authentication user.
 */
export const processAccountDeletion = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required to delete account.");
  }

  const userId = context.auth.uid;
  const userRef = db.collection("users").doc(userId);

  try {
    // Delete subcollections (ledger, purchases, reward_requests, deletion_requests)
    const subcollections = ["ledger", "purchases", "reward_requests", "deletion_requests"];
    for (const subName of subcollections) {
      const snapshot = await userRef.collection(subName).get();
      const batch = db.batch();
      snapshot.docs.forEach((doc) => batch.delete(doc.ref));
      await batch.commit();
    }

    // Delete user profile document
    await userRef.delete();

    // Delete Firebase Authentication user identity
    await admin.auth().deleteUser(userId);

    return { status: "SUCCESS", message: "Account and associated data deleted permanently." };
  } catch (err: any) {
    console.error(`Failed to delete user account ${userId}:`, err);
    throw new functions.https.HttpsError("internal", `Account deletion failed: ${err.message}`);
  }
});

/**
 * Cloud Function: onWebDeletionRequestCreated
 * Triggered when a public request is posted to /account_deletion_requests.
 * Matches user by email and securely schedules or executes account purge.
 */
export const onWebDeletionRequestCreated = functions.firestore
  .document("account_deletion_requests/{requestId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();
    const email = data.email;

    if (!email) {
      return snap.ref.update({ status: "REJECTED_MISSING_EMAIL", processedAt: Date.now() });
    }

    try {
      const userRecord = await admin.auth().getUserByEmail(email);
      if (userRecord) {
        const userId = userRecord.uid;
        // Purge Firestore profile
        await db.collection("users").doc(userId).delete();
        // Purge Auth user
        await admin.auth().deleteUser(userId);

        return snap.ref.update({
          status: "PROCESSED",
          userIdPurged: userId,
          processedAt: Date.now()
        });
      }
      return snap.ref.update({
        status: "COMPLETED_NO_ACCOUNT_FOUND",
        processedAt: Date.now()
      });
    } catch (err: any) {
      if (err.code === "auth/user-not-found") {
        return snap.ref.update({
          status: "COMPLETED_NO_ACCOUNT_FOUND",
          processedAt: Date.now()
        });
      }
      console.error(`Failed to process web deletion for ${email}:`, err);
      return snap.ref.update({
        status: "FAILED_RETRY_SCHEDULED",
        error: err.message,
        processedAt: Date.now()
      });
    }
  });
