package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local indexed audit & deduplication ledger for Google Play Billing purchases.
 * Replaces SharedPreferences string-sets so pending, verified, consumed, and restored
 * transactions are durable, queryable, and crash-resilient.
 */
@Entity(
    tableName = "purchase_records",
    indices = [
        Index(value = ["purchaseToken"], unique = true),
        Index(value = ["orderId"]),
        Index(value = ["productId"]),
        Index(value = ["status"]),
        Index(value = ["timestamp"])
    ]
)
data class PurchaseRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseToken: String,
    val orderId: String,
    val productId: String,
    val isSubscription: Boolean,
    val status: String, // "PENDING", "VERIFIED_LOCAL", "VERIFIED_SERVER", "ACKNOWLEDGED", "CONSUMED", "REVOKED", "REJECTED"
    val entitledApplied: Boolean = false,
    val verificationSource: String = "PLAY_BILLING", // "PLAY_BILLING" or "CLOUD_FUNCTION"
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
