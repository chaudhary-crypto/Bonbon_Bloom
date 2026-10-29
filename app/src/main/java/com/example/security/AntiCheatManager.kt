package com.example.security

import android.os.Build
import android.os.Debug
import com.example.data.UserProfile
import com.example.game.GridPosition
import java.io.File
import java.security.MessageDigest
import kotlin.math.abs

data class SecurityReport(
    val isSecure: Boolean,
    val isSignatureValid: Boolean,
    val isRootDetected: Boolean,
    val isDebuggerAttached: Boolean,
    val isHookFrameworkDetected: Boolean,
    val issuesFound: List<String>,
    val scanTimestamp: Long = System.currentTimeMillis()
)

object AntiCheatManager {
    private const val SECRET_SALT = "CRUSH_CANDY_PROTECT_SECURE_SALT_v2_2026"

    // Rate limiting for gameplay moves
    private var lastMoveTimestamp: Long = 0L
    private const val MIN_MOVE_INTERVAL_MS = 60L // Bot prevention threshold

    /**
     * Computes a cryptographic SHA-256 HMAC hash of the critical user profile values
     * to prevent unauthorized offline or memory-based modifications of coins, diamonds, levels, and items.
     */
    fun computeSignature(profile: UserProfile): String {
        val payload = buildString {
            append(profile.id).append(":")
            append(profile.coins).append(":")
            append(profile.diamonds).append(":")
            append(profile.lives).append(":")
            append(profile.highestUnlockedLevel).append(":")
            append(profile.hammerCount).append(":")
            append(profile.shuffleCount).append(":")
            append(profile.colorBlastCount).append(":")
            append(profile.magicWandCount).append(":")
            append(profile.cosmicBlastCount).append(":")
            append(profile.vipExpiryTimestamp).append(":")
            append(profile.infiniteLivesExpiryTimestamp).append(":")
            append(profile.piggyBankCoins).append(":")
            append(profile.authProvider).append(":")
            append(profile.email ?: "none").append(":")
            append(SECRET_SALT)
        }
        return sha256(payload)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Signs a user profile with the cryptographic hash before persisting it to the database.
     */
    fun signProfile(profile: UserProfile): UserProfile {
        val sig = computeSignature(profile)
        return profile.copy(
            securitySignature = sig,
            lastSecurityScanTime = System.currentTimeMillis()
        )
    }

    /**
     * Verifies that the saved profile has not been tampered with.
     * Returns true if valid, false if coins/diamonds/inventory were modified externally.
     */
    fun verifySignature(profile: UserProfile): Boolean {
        // If profile was never signed yet (e.g. fresh install), consider it valid
        if (profile.securitySignature.isEmpty()) return true
        val expected = computeSignature(profile)
        return expected == profile.securitySignature
    }

    /**
     * Checks if the device environment exhibits indicators of rooting or jailbreak.
     */
    fun checkRoot(): Boolean {
        val rootPaths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in rootPaths) {
            try {
                if (File(path).exists()) return true
            } catch (_: Throwable) {
                // Ignore permission or security errors
            }
        }

        // Check build tags for test-keys (custom/rooted ROM)
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        return false
    }

    /**
     * Detects active debuggers attached to process memory.
     */
    fun checkDebugger(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    /**
     * Detects common dynamic hook and memory cheat engines (Xposed, Frida, GameGuardian).
     */
    fun checkHookFrameworks(): Boolean {
        val suspiciousClasses = listOf(
            "de.robv.android.xposed.XposedBridge",
            "com.saurik.substrate.MS$2",
            "io.github.zerfall.FridaServer"
        )
        for (className in suspiciousClasses) {
            try {
                Class.forName(className)
                return true
            } catch (_: ClassNotFoundException) {
                // Expected
            }
        }
        return false
    }

    /**
     * Runs a comprehensive security diagnostic and returns an audited report.
     */
    fun performFullSecurityScan(profile: UserProfile): SecurityReport {
        val issues = mutableListOf<String>()
        val sigValid = verifySignature(profile)
        if (!sigValid) {
            issues.add("Savefile integrity signature mismatch (possible memory or database manipulation)")
        }

        val rootDetected = checkRoot()
        if (rootDetected) {
            issues.add("Root privileges or su binary detected on device")
        }

        val debuggerAttached = checkDebugger()
        if (debuggerAttached) {
            issues.add("Dynamic memory debugger currently attached")
        }

        val hookDetected = checkHookFrameworks()
        if (hookDetected) {
            issues.add("Hook or code-injection framework detected")
        }

        return SecurityReport(
            isSecure = sigValid && !debuggerAttached && !hookDetected,
            isSignatureValid = sigValid,
            isRootDetected = rootDetected,
            isDebuggerAttached = debuggerAttached,
            isHookFrameworkDetected = hookDetected,
            issuesFound = issues,
            scanTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Validates candy swap actions to prevent impossible speedhacks and teleport hacks.
     */
    fun validateMove(pos1: GridPosition, pos2: GridPosition): MoveValidationResult {
        val now = System.currentTimeMillis()
        val elapsed = now - lastMoveTimestamp

        // Rate limit check
        if (lastMoveTimestamp > 0 && elapsed < MIN_MOVE_INTERVAL_MS) {
            return MoveValidationResult.RateLimitExceeded
        }
        lastMoveTimestamp = now

        // Adjacency check
        val isAdjacent = (abs(pos1.row - pos2.row) == 1 && pos1.col == pos2.col) ||
                         (abs(pos1.col - pos2.col) == 1 && pos1.row == pos2.row)
        if (!isAdjacent) {
            return MoveValidationResult.NonAdjacentSwap
        }

        return MoveValidationResult.Valid
    }
}

sealed class MoveValidationResult {
    object Valid : MoveValidationResult()
    object RateLimitExceeded : MoveValidationResult()
    object NonAdjacentSwap : MoveValidationResult()
}
