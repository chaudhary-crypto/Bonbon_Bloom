import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import * as crypto from "crypto";
import { GoogleAuth } from "google-auth-library";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

export const EXPECTED_PACKAGE_NAME = "com.aistudio.bonbonbloom.qyjzvw";
export const FIRESTORE_DATABASE_ID = "ai-studio-android-candycus-ab354175-b4a1-417f-a1ac-88c83a67355a";
const db = getFirestore(admin.app(), FIRESTORE_DATABASE_ID);

export interface ProductCatalogEntry {
  type: "currency" | "entitlement" | "boosters";
  isSubscription?: boolean;
  coins?: number;
  diamonds?: number;
  isVip?: boolean;
  vipDays?: number;
  removeAds?: boolean;
}

// Canonical Google Play Console SKU catalog for Bonbon Bloom
export const VALID_PRODUCTS: Record<string, ProductCatalogEntry> = {
  "vip_weekly": {
    type: "entitlement",
    isSubscription: true,
    isVip: true,
    vipDays: 7,
    diamonds: 50
  },
  "vip_monthly": {
    type: "entitlement",
    isSubscription: true,
    isVip: true,
    vipDays: 30,
    diamonds: 50
  },
  "remove_ads": { type: "entitlement", removeAds: true },
  "coins_500": { type: "currency", coins: 500 },
  "coins_1500": { type: "currency", coins: 1500 },
  "coins_5000": { type: "currency", coins: 5000 },
  "gems_50": { type: "currency", diamonds: 50 },
  "gems_150": { type: "currency", diamonds: 150, coins: 1000 },
  "gems_200": { type: "currency", diamonds: 200 },
  "gems_500": { type: "currency", diamonds: 500, coins: 5000 },
  "gems_600": { type: "currency", diamonds: 600 },
  "gems_1500": { type: "currency", diamonds: 1500, coins: 20000 },
  "booster_starter_pack": { type: "boosters", coins: 200, diamonds: 20 }
};

const playAuth = new GoogleAuth({
  scopes: ["https://www.googleapis.com/auth/androidpublisher"]
});

interface PlayVerificationOutcome {
  verified: boolean;
  httpStatus: number;
  reason?: string;
  paymentState?: number;
  expiryTimeMillis?: number;
  orderId?: string;
}

/**
 * Verifies a Google Play Billing purchase or subscription token directly with the
 * official Google Play Developer API (androidpublisher v3).
 * Never trusts client claims alone; returns 503 if Play Console service account is unlinked.
 */
async function verifyWithGooglePlayApi(
  packageName: string,
  productId: string,
  purchaseToken: string,
  isSubscription: boolean
): Promise<PlayVerificationOutcome> {
  let accessToken: string | null | undefined;
  try {
    const client = await playAuth.getClient();
    const tokenResponse = await client.getAccessToken();
    accessToken = tokenResponse?.token;
  } catch (err: any) {
    return {
      verified: false,
      httpStatus: 503,
      reason: `Google Play API credentials unavailable: ${err?.message || "unconfigured service account"}`
    };
  }

  if (!accessToken) {
    return {
      verified: false,
      httpStatus: 503,
      reason: "Unable to acquire Google Play Developer API access token."
    };
  }

  if (isSubscription) {
    const url =
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}` +
      `/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`;
    const response = await fetch(url, {
      method: "GET",
      headers: { Authorization: `Bearer ${accessToken}` }
    });

    if (response.status === 401 || response.status === 403) {
      return {
        verified: false,
        httpStatus: 503,
        reason: "Google Play Console service account lacks permission to verify subscriptions."
      };
    }
    if (response.status === 404 || response.status === 410) {
      return {
        verified: false,
        httpStatus: 403,
        reason: "Subscription purchase token not found or expired in Google Play."
      };
    }
    if (!response.ok) {
      return {
        verified: false,
        httpStatus: 502,
        reason: `Google Play API returned HTTP ${response.status}.`
      };
    }

    const subJson: any = await response.json();
    const subState: string = subJson?.subscriptionState || "";
    const lineItems: any[] = Array.isArray(subJson?.lineItems) ? subJson.lineItems : [];
    const matchingLine = lineItems.find((item) => item?.productId === productId) || lineItems[0];
    const expiryIso: string | undefined = matchingLine?.expiryTime;
    const expiryMs = expiryIso ? Date.parse(expiryIso) : 0;

    // Only ACTIVE or IN_GRACE_PERIOD subscriptions whose expiry is in the future qualify.
    const isActiveState =
      subState === "SUBSCRIPTION_STATE_ACTIVE" ||
      subState === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD";

    if (!isActiveState || !Number.isFinite(expiryMs) || expiryMs <= Date.now()) {
      return {
        verified: false,
        httpStatus: 403,
        reason: `Subscription is not active (state: ${subState || "UNKNOWN"}).`
      };
    }

    return {
      verified: true,
      httpStatus: 200,
      expiryTimeMillis: expiryMs,
      orderId: subJson?.latestOrderId
    };
  } else {
    const url =
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}` +
      `/purchases/products/${encodeURIComponent(productId)}/tokens/${encodeURIComponent(purchaseToken)}`;
    const response = await fetch(url, {
      method: "GET",
      headers: { Authorization: `Bearer ${accessToken}` }
    });

    if (response.status === 401 || response.status === 403) {
      return {
        verified: false,
        httpStatus: 503,
        reason: "Google Play Console service account lacks permission to verify products."
      };
    }
    if (response.status === 404 || response.status === 410) {
      return {
        verified: false,
        httpStatus: 403,
        reason: "In-app purchase token was not found in Google Play."
      };
    }
    if (!response.ok) {
      return {
        verified: false,
        httpStatus: 502,
        reason: `Google Play API returned HTTP ${response.status}.`
      };
    }

    const prodJson: any = await response.json();
    // purchaseState: 0 = Purchased, 1 = Canceled, 2 = Pending
    const purchaseState = typeof prodJson?.purchaseState === "number" ? prodJson.purchaseState : -1;
    if (purchaseState !== 0) {
      return {
        verified: false,
        httpStatus: 403,
        paymentState: purchaseState,
        reason: purchaseState === 2
          ? "Payment is still pending confirmation."
          : "Purchase was cancelled or refunded in Google Play."
      };
    }

    return {
      verified: true,
      httpStatus: 200,
      paymentState: purchaseState,
      orderId: prodJson?.orderId
    };
  }
}

/**
 * HTTPS Endpoint: verifyGooglePlayPurchase
 * Authenticated via Firebase ID Token (and verified App Check if header present).
 * Checks androidpublisher v3 state, enforces cross-account purchaseToken SHA-256 lock,
 * credits VIP/Remove-Ads/currency/boosters inside a Firestore transaction, and writes /ledger.
 */
export const verifyGooglePlayPurchase = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).json({ verified: false, error: "Method Not Allowed" });
    return;
  }

  const authHeader = String(req.headers.authorization || "");
  if (!authHeader.startsWith("Bearer ")) {
    res.status(401).json({ verified: false, error: "Missing Firebase Authorization Bearer token." });
    return;
  }
  const idToken = authHeader.slice("Bearer ".length).trim();

  let uid: string;
  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    uid = decoded.uid;
  } catch (err: any) {
    res.status(401).json({ verified: false, error: "Invalid Firebase Auth token." });
    return;
  }

  const appCheckHeader = req.headers["x-firebase-appcheck"];
  if (typeof appCheckHeader === "string" && appCheckHeader.trim().length > 0) {
    try {
      await admin.appCheck().verifyToken(appCheckHeader.trim());
    } catch {
      res.status(401).json({ verified: false, error: "Invalid Firebase App Check token." });
      return;
    }
  }

  const { productId, purchaseToken, orderId, isSubscription, packageName } = req.body || {};
  if (
    typeof productId !== "string" ||
    typeof purchaseToken !== "string" ||
    !productId.trim() ||
    !purchaseToken.trim()
  ) {
    res.status(400).json({ verified: false, error: "Missing productId or purchaseToken." });
    return;
  }

  const effectivePackage = typeof packageName === "string" && packageName.trim()
    ? packageName.trim()
    : EXPECTED_PACKAGE_NAME;
  if (effectivePackage !== EXPECTED_PACKAGE_NAME) {
    res.status(403).json({ verified: false, error: "Unrecognized application packageName." });
    return;
  }

  const productConfig = VALID_PRODUCTS[productId];
  if (!productConfig) {
    res.status(400).json({ verified: false, error: `Unknown product SKU: ${productId}` });
    return;
  }

  const expectSub = Boolean(isSubscription) || Boolean(productConfig.isSubscription);
  const playResult = await verifyWithGooglePlayApi(
    effectivePackage,
    productId,
    purchaseToken.trim(),
    expectSub
  );

  if (!playResult.verified) {
    res.status(playResult.httpStatus).json({
      verified: false,
      error: playResult.reason || "Purchase verification failed."
    });
    return;
  }

  const tokenHash = crypto.createHash("sha256").update(purchaseToken.trim()).digest("hex");
  const resolvedOrderId = String(playResult.orderId || orderId || `GPA-${tokenHash.slice(0, 16)}`);
  const safeOrderDocId = resolvedOrderId.replace(/[^a-zA-Z0-9_\-]/g, "_").slice(0, 120);

  const userRef = db.collection("users").doc(uid);
  const purchaseRef = userRef.collection("purchases").doc(tokenHash);
  const tokenLockRef = db.collection("purchase_tokens").doc(tokenHash);
  const ledgerRef = userRef.collection("ledger").doc(safeOrderDocId);

  try {
    const txOutcome = await db.runTransaction(async (transaction) => {
      const lockDoc = await transaction.get(tokenLockRef);
      if (lockDoc.exists) {
        const lockOwner = lockDoc.data()?.userId;
        if (lockOwner && lockOwner !== uid) {
          return { conflict: true as const };
        }
        const existingPurchase = await transaction.get(purchaseRef);
        if (existingPurchase.exists && existingPurchase.data()?.status === "VERIFIED") {
          return {
            conflict: false as const,
            alreadyProcessed: true,
            vipExpiryTimestamp: existingPurchase.data()?.expiryTimeMillis || null
          };
        }
      }

      const userDoc = await transaction.get(userRef);
      const userData = userDoc.exists ? (userDoc.data() || {}) : {};
      const nowMs = Date.now();
      const updates: Record<string, any> = {
        lastCloudSyncTime: nowMs,
        updatedAt: FieldValue.serverTimestamp()
      };

      if (!userDoc.exists) {
        updates.username = "Player";
        updates.highestUnlockedLevel = 1;
        updates.coins = 500;
        updates.diamonds = 25;
        updates.isVipSubscribed = false;
        updates.hasRemovedAds = false;
        updates.isAdmin = false;
        updates.createdAt = FieldValue.serverTimestamp();
      }

      if (productConfig.coins) {
        updates.coins = (typeof userData.coins === "number" ? userData.coins : 500) + productConfig.coins;
      }
      if (productConfig.diamonds) {
        updates.diamonds = (typeof userData.diamonds === "number" ? userData.diamonds : 25) + productConfig.diamonds;
      }
      if (productConfig.removeAds) {
        updates.hasRemovedAds = true;
      }

      let resolvedVipExpiry: number | null = null;
      if (productConfig.isVip) {
        const spanMs = (productConfig.vipDays || 30) * 24 * 60 * 60 * 1000;
        resolvedVipExpiry = playResult.expiryTimeMillis || (Math.max(userData.vipExpiryTimestamp || 0, nowMs) + spanMs);
        updates.isVipSubscribed = true;
        updates.vipExpiryTimestamp = resolvedVipExpiry;
      }

      if (productConfig.type === "boosters") {
        updates.hammerCount = (userData.hammerCount || 3) + 3;
        updates.shuffleCount = (userData.shuffleCount || 2) + 3;
        updates.colorBlastCount = (userData.colorBlastCount || 2) + 3;
        updates.extraMovesCount = (userData.extraMovesCount || 2) + 3;
      }

      if (userDoc.exists) {
        transaction.update(userRef, updates);
      } else {
        transaction.set(userRef, updates);
      }

      transaction.set(tokenLockRef, {
        userId: uid,
        productId,
        orderId: resolvedOrderId,
        claimedAt: FieldValue.serverTimestamp()
      });

      transaction.set(purchaseRef, {
        orderId: resolvedOrderId,
        productId,
        purchaseTokenHash: tokenHash,
        isSubscription: expectSub,
        status: "VERIFIED",
        paymentState: playResult.paymentState ?? 0,
        expiryTimeMillis: resolvedVipExpiry ?? 0,
        serverVerified: true,
        verifiedAt: FieldValue.serverTimestamp()
      });

      transaction.set(ledgerRef, {
        orderId: resolvedOrderId,
        productId,
        purchaseTokenHash: tokenHash,
        userId: uid,
        amount: productConfig.coins || productConfig.diamonds || 1,
        type: productConfig.type,
        status: "VERIFIED_AND_CREDITED",
        timestamp: nowMs
      });

      return {
        conflict: false as const,
        alreadyProcessed: false,
        vipExpiryTimestamp: resolvedVipExpiry
      };
    });

    if (txOutcome.conflict) {
      res.status(409).json({ verified: false, error: "Purchase token already claimed by another player." });
      return;
    }

    res.status(200).json({
      verified: true,
      alreadyProcessed: txOutcome.alreadyProcessed,
      orderId: resolvedOrderId,
      productId,
      vipExpiryTimestamp: txOutcome.vipExpiryTimestamp
    });
  } catch (err: any) {
    console.error("verifyGooglePlayPurchase transaction failed:", err);
    res.status(500).json({ verified: false, error: "Server verification transaction failed." });
  }
});

/**
 * Audit guard on /users/{userId}/purchases/{purchaseId} in the named Firestore DB.
 * Clients cannot write purchases directly under firestore.rules; if any unverified record appears,
 * immediately reject it without granting balances or VIP.
 */
export const onPurchaseCreated = functions.firestore
  .database(FIRESTORE_DATABASE_ID)
  .document("users/{userId}/purchases/{purchaseId}")
  .onCreate(async (snap) => {
    const data = snap.data();
    if (data?.serverVerified === true && data?.status === "VERIFIED") {
      return null;
    }
    return snap.ref.update({
      status: "REJECTED_UNVERIFIED",
      processedAt: Date.now()
    });
  });

let cachedAdMobKeys: Record<string, string> | null = null;
let cachedAdMobKeysFetchedAt = 0;

async function getAdMobVerifierKeys(): Promise<Record<string, string>> {
  const now = Date.now();
  if (cachedAdMobKeys && now - cachedAdMobKeysFetchedAt < 6 * 60 * 60 * 1000) {
    return cachedAdMobKeys;
  }
  const resp = await fetch("https://www.gstatic.com/admob/reward/verifier-keys.json");
  if (!resp.ok) {
    throw new Error(`Failed fetching AdMob verifier keys: HTTP ${resp.status}`);
  }
  const json: any = await resp.json();
  const keysMap: Record<string, string> = {};
  for (const entry of json?.keys || []) {
    if (entry?.keyId != null && typeof entry?.pem === "string") {
      keysMap[String(entry.keyId)] = entry.pem;
    }
  }
  cachedAdMobKeys = keysMap;
  cachedAdMobKeysFetchedAt = now;
  return keysMap;
}

/**
 * Extracts the exact canonical signed substring before `&signature=` from a raw AdMob SSV URL.
 */
export function extractAdMobSignedPayload(originalUrl: string): { payload: string; signature: string } | null {
  const qIndex = originalUrl.indexOf("?");
  if (qIndex < 0) return null;
  const rawQuery = originalUrl.slice(qIndex + 1);
  const sigMarker = "&signature=";
  const sigIdx = rawQuery.indexOf(sigMarker);
  if (sigIdx < 0) return null;

  const payload = rawQuery.slice(0, sigIdx);
  const afterSig = rawQuery.slice(sigIdx + sigMarker.length);
  const nextAmp = afterSig.indexOf("&");
  const encodedSig = nextAmp >= 0 ? afterSig.slice(0, nextAmp) : afterSig;
  const signature = decodeURIComponent(encodedSig);
  return { payload, signature };
}

/**
 * Cloud Function: verifyAdMobSSV
 * Verifies Google AdMob ECDSA SHA256 signature against official verifier-keys.json
 * before crediting any rewarded-ad coins to the player.
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
    ad_unit: adUnit,
    key_id: keyId,
    signature
  } = req.query;

  if (!userId || !transactionId || !keyId || !signature) {
    res.status(400).send("Missing required SSV parameters (user_id, custom_data, key_id, signature).");
    return;
  }

  const parsed = extractAdMobSignedPayload(req.originalUrl || req.url || "");
  if (!parsed) {
    res.status(400).send("Malformed AdMob SSV query signature ordering.");
    return;
  }

  try {
    const verifierKeys = await getAdMobVerifierKeys();
    const pemKey = verifierKeys[String(keyId)];
    if (!pemKey) {
      res.status(403).send("Unknown AdMob verifier key_id.");
      return;
    }

    const verifier = crypto.createVerify("SHA256");
    verifier.update(parsed.payload);
    verifier.end();

    const sigNormalized = parsed.signature.replace(/-/g, "+").replace(/_/g, "/");
    const isValidSignature = verifier.verify(pemKey, Buffer.from(sigNormalized, "base64"));
    if (!isValidSignature) {
      res.status(403).send("Invalid AdMob SSV signature.");
      return;
    }
  } catch (err: any) {
    console.error("AdMob SSV signature check failed:", err);
    res.status(503).send("Unable to validate AdMob signature.");
    return;
  }

  const amount = Math.min(Math.max(parseInt(String(rewardAmountStr || "1"), 10) || 1, 1), 5);
  const safeTxId = String(transactionId).replace(/[^a-zA-Z0-9_\-]/g, "_").slice(0, 120);
  const userRef = db.collection("users").doc(String(userId));
  const ledgerRef = userRef.collection("ledger").doc(`ad_${safeTxId}`);
  const rewardRef = userRef.collection("reward_requests").doc(safeTxId);

  try {
    await db.runTransaction(async (transaction) => {
      const existingTx = await transaction.get(ledgerRef);
      if (existingTx.exists) return;

      const userDoc = await transaction.get(userRef);
      if (userDoc.exists) {
        const currentCoins = userDoc.data()?.coins || 0;
        transaction.update(userRef, {
          coins: currentCoins + (amount * 50),
          lastCloudSyncTime: Date.now(),
          updatedAt: FieldValue.serverTimestamp()
        });
      }

      transaction.set(rewardRef, {
        transactionId: safeTxId,
        amount,
        adUnit: String(adUnit || ""),
        verifiedAt: FieldValue.serverTimestamp()
      });

      transaction.set(ledgerRef, {
        orderId: `ad_${safeTxId}`,
        productId: "admob_rewarded_ssv",
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
 */
export const processAccountDeletion = functions.https.onCall(async (_data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required to delete account.");
  }

  const userId = context.auth.uid;
  const userRef = db.collection("users").doc(userId);

  try {
    const subcollections = ["level_progress", "ledger", "purchases", "reward_requests", "deletion_requests"];
    for (const subName of subcollections) {
      const snapshot = await userRef.collection(subName).get();
      if (!snapshot.empty) {
        const batch = db.batch();
        snapshot.docs.forEach((doc) => batch.delete(doc.ref));
        await batch.commit();
      }
    }

    await userRef.delete();
    await admin.auth().deleteUser(userId);

    return { status: "SUCCESS", message: "Account and associated data deleted permanently." };
  } catch (err: any) {
    console.error(`Failed to delete user account ${userId}:`, err);
    throw new functions.https.HttpsError("internal", `Account deletion failed: ${err.message}`);
  }
});

/**
 * Public HTTPS Endpoint for web deletion portal (docs/delete-account.html).
 * Eliminates unauthenticated public Firestore client writes and hardcoded API keys.
 */
export const requestWebAccountDeletion = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }
  if (req.method !== "POST") {
    res.status(405).json({ ok: false, error: "Method Not Allowed" });
    return;
  }

  const email = String(req.body?.email || "").trim().toLowerCase();
  const username = String(req.body?.username || "Player").trim().slice(0, 64);
  const reason = String(req.body?.reason || "User web request").trim().slice(0, 500);
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  if (!emailRegex.test(email) || email.length > 254) {
    res.status(400).json({ ok: false, error: "Please provide a valid Google Play / account email address." });
    return;
  }

  const reqId = `DEL-${Date.now().toString(36).toUpperCase()}-${crypto.randomBytes(3).toString("hex").toUpperCase()}`;

  try {
    await db.collection("account_deletion_requests").doc(reqId).set({
      requestId: reqId,
      email,
      username,
      reason,
      status: "PENDING",
      timestamp: Date.now(),
      createdAt: FieldValue.serverTimestamp()
    });

    res.status(200).json({ ok: true, requestId: reqId });
  } catch (err: any) {
    console.error("Failed registering web account deletion request:", err);
    res.status(500).json({ ok: false, error: "Unable to queue deletion request right now." });
  }
});

/**
 * Triggered on named database /account_deletion_requests/{requestId}.
 */
export const onWebDeletionRequestCreated = functions.firestore
  .database(FIRESTORE_DATABASE_ID)
  .document("account_deletion_requests/{requestId}")
  .onCreate(async (snap) => {
    const data = snap.data();
    const email = data?.email;

    if (!email) {
      return snap.ref.update({ status: "REJECTED_MISSING_EMAIL", processedAt: Date.now() });
    }

    try {
      const userRecord = await admin.auth().getUserByEmail(email);
      const userId = userRecord.uid;
      const userRef = db.collection("users").doc(userId);
      const subcollections = ["level_progress", "ledger", "purchases", "reward_requests", "deletion_requests"];
      for (const subName of subcollections) {
        const snapshot = await userRef.collection(subName).get();
        if (!snapshot.empty) {
          const batch = db.batch();
          snapshot.docs.forEach((doc) => batch.delete(doc.ref));
          await batch.commit();
        }
      }
      await userRef.delete();
      await admin.auth().deleteUser(userId);

      return snap.ref.update({
        status: "PROCESSED",
        userIdPurged: userId,
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
