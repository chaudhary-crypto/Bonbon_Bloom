package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ads.ConsentManager
import com.example.audio.HapticManager
import com.example.audio.SoundManager
import com.example.data.UserProfile
import com.example.payment.PlayBillingManager

@Composable
fun SettingsDialog(
    profile: UserProfile,
    activity: Activity?,
    onDismiss: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onDeleteMyData: () -> Unit,
    onSecretAdminUnlocked: (() -> Unit)? = null,
    onSandboxAddCoins: (() -> Unit)? = null,
    onSandboxAddDiamonds: (() -> Unit)? = null,
    onSandboxRefillLives: (() -> Unit)? = null,
    onSandboxGrantInfiniteLives: (() -> Unit)? = null,
    onSandboxGrantBoosters: (() -> Unit)? = null,
    onSandboxUnlockAllLevels: (() -> Unit)? = null,
    onSandboxRemoveAds: (() -> Unit)? = null,
    onSandboxGrantVip: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPrivacyReader by remember { mutableStateOf(false) }
    var showTermsReader by remember { mutableStateOf(false) }
    var showDevSandbox by remember { mutableStateOf(false) }
    var showSecretAdminAuthDialog by remember { mutableStateOf(false) }
    var devTapCount by remember { mutableIntStateOf(0) }
    var restoreStatus by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    var selectedLang by remember { mutableStateOf(profile.languageCode.ifEmpty { "en" }) }

    val languages = listOf(
        "en" to "English 🇺🇸",
        "hi" to "हिंदी 🇮🇳",
        "es" to "Español 🇪🇸",
        "pt" to "Português 🇧🇷",
        "id" to "Bahasa Indonesia 🇮🇩",
        "ar" to "العربية 🇸🇦",
        "fr" to "Français 🇫🇷",
        "de" to "Deutsch 🇩🇪"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E102F)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚙️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SETTINGS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD54F)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.clickable(onClick = onDismiss)
                        ) {
                            Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Audio & Game Feel
                SettingsSectionHeader("AUDIO & HAPTICS")

                SettingToggleRow(
                    icon = "🔊",
                    title = "Sound Effects",
                    checked = profile.soundEnabled,
                    onCheckedChange = {
                        SoundManager.soundEnabled = it
                        onToggleSound(it)
                    }
                )

                SettingToggleRow(
                    icon = "🎵",
                    title = "Background Music",
                    checked = profile.musicEnabled,
                    onCheckedChange = {
                        SoundManager.musicEnabled = it
                        onToggleMusic(it)
                    }
                )

                SettingToggleRow(
                    icon = "📳",
                    title = "Haptic Vibrations",
                    checked = HapticManager.hapticsEnabled,
                    onCheckedChange = {
                        HapticManager.hapticsEnabled = it
                        onToggleHaptics(it)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Purchases & Account
                SettingsSectionHeader("PURCHASES & STORE")

                Button(
                    onClick = {
                        isRestoring = true
                        PlayBillingManager.queryAndRestorePurchases { success, items ->
                            isRestoring = false
                            restoreStatus = if (items.isNotEmpty()) {
                                "✓ Restored ${items.size} active purchases: ${items.joinToString(", ")}"
                            } else {
                                "No prior active purchases found on this Google Play account."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                    enabled = !isRestoring
                ) {
                    Text(if (isRestoring) "Restoring..." else "🔄 Restore Purchases", fontWeight = FontWeight.Bold)
                }

                restoreStatus?.let { status ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = status,
                        fontSize = 11.sp,
                        color = Color(0xFF81C784),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Language
                SettingsSectionHeader("LANGUAGE")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(
                            text = languages.firstOrNull { it.first == selectedLang }?.second ?: "English 🇺🇸",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        languages.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name, fontSize = 13.sp) },
                                onClick = {
                                    selectedLang = code
                                    expanded = false
                                    onLanguageSelected(code)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Privacy & Compliance
                SettingsSectionHeader("PRIVACY & LEGAL")

                if (activity != null) {
                    OutlinedButton(
                        onClick = {
                            ConsentManager.showPrivacyOptionsForm(activity)
                        },
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF80D8FF))
                    ) {
                        Text("🛡️ Privacy Options (Ad Consent)", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showPrivacyReader = true },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text("Privacy Policy", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { showTermsReader = true },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text("Terms", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@earnvero.app")
                                putExtra(Intent.EXTRA_SUBJECT, "Bonbon Bloom Player Support [v${com.example.BuildConfig.VERSION_NAME}]")
                            }
                            context.startActivity(intent)
                        } catch (_: Throwable) {}
                    },
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD54F))
                ) {
                    Text("✉️ Support & Feedback (support@earnvero.app)", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Delete My Data (GDPR / Play Store mandatory compliance)
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("🗑️ Delete My Data", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Text(
                    text = "Bonbon Bloom v${com.example.BuildConfig.VERSION_NAME} • Encrypted Sync & Fair Play",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .let { mod ->
                            if (com.example.BuildConfig.DEBUG) {
                                mod.clickable {
                                    devTapCount++
                                    if (devTapCount >= 7) {
                                        devTapCount = 0
                                        HapticManager.performHapticFeedback(context, com.example.audio.HapticType.Heavy)
                                        showSecretAdminAuthDialog = true
                                    }
                                }
                            } else {
                                mod
                            }
                        }
                )
            }
        }
    }

    // Confirmation Alert for Data Deletion
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete All Data?", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) },
            text = {
                Text(
                    "This will permanently erase all local game progress, level stars, coin balances, diamonds, and boosters on this device. This cannot be undone.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteMyData()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // In-App Privacy Policy Reader
    if (showPrivacyReader) {
        InAppPolicyReaderDialog(
            context = context,
            onDismiss = { showPrivacyReader = false }
        )
    }

    // In-App Terms of Service Reader
    if (showTermsReader) {
        InAppTermsReaderDialog(
            context = context,
            onDismiss = { showTermsReader = false }
        )
    }

    // QA & Developer Sandbox Dialog (Strictly Debug builds only)
    if (com.example.BuildConfig.DEBUG && showDevSandbox) {
        DeveloperSandboxDialog(
            activity = activity,
            onDismiss = { showDevSandbox = false },
            onAddCoins = { onSandboxAddCoins?.invoke() },
            onAddDiamonds = { onSandboxAddDiamonds?.invoke() },
            onRefillLives = { onSandboxRefillLives?.invoke() },
            onGrantInfiniteLives = { onSandboxGrantInfiniteLives?.invoke() },
            onGrantBoosters = { onSandboxGrantBoosters?.invoke() },
            onUnlockAllLevels = { onSandboxUnlockAllLevels?.invoke() },
            onRemoveAds = { onSandboxRemoveAds?.invoke() },
            onGrantVip = { onSandboxGrantVip?.invoke() }
        )
    }

    if (com.example.BuildConfig.DEBUG && showSecretAdminAuthDialog) {
        var authError by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showSecretAdminAuthDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF150A26)),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👑 Owner Debug Verification", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFFFD54F))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sign in with the verified owner Google account (${com.example.auth.AdminAuthManager.OWNER_EMAIL}) to unlock debug QA tools.",
                        fontSize = 12.sp,
                        color = Color(0xFFCE93D8),
                        textAlign = TextAlign.Center
                    )

                    authError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(err, color = Color(0xFFFF5252), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (com.example.auth.AdminAuthManager.isVerifiedFirebaseOwnerSession()) {
                                SoundManager.playLevelWin()
                                onSecretAdminUnlocked?.invoke()
                                showSecretAdminAuthDialog = false
                                onDismiss()
                            } else {
                                authError = "Access denied: Please sign in via Google as the verified owner first."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Text("Verify Owner Session", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun InAppPolicyReaderDialog(
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E102F)),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔒 Privacy Policy",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFD54F)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Text("✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Bonbon Bloom is built by Novaro Technologies with player privacy and data security at its core.\n\n" +
                                "1. ADVERTISING & GOOGLE ADMOB\n" +
                                "• We partner with Google AdMob to display banner, interstitial, and optional rewarded ads.\n" +
                                "• Google AdMob may process your device's Android Advertising ID (AD_ID), general IP location, and performance metrics to deliver ads and prevent fraud.\n" +
                                "• Learn how Google uses information from apps: https://policies.google.com/technologies/partner-sites\n\n" +
                                "2. USER CONSENT & PRIVACY CONTROLS (EEA / UK / GDPR)\n" +
                                "• Players in the EEA/UK are presented with Google's Certified User Messaging Platform (UMP) consent dialog.\n" +
                                "• Review, modify, or revoke ad consent anytime in Settings > 'Privacy Options (Ad Consent)'.\n\n" +
                                "3. GOOGLE SIGN-IN, CLOUD SYNC & APP CHECK\n" +
                                "• Playing as Guest stores progress locally on your device in an encrypted-signed Room database.\n" +
                                "• Signing in with Google syncs your profile, level progress, and verified purchase ledger via encrypted Firebase Firestore and Firebase App Check.\n\n" +
                                "4. GOOGLE PLAY BILLING & VERIFICATION\n" +
                                "• All real-money purchases and VIP subscriptions are processed exclusively by Google Play Billing and verified server-side via Google Play Developer API.\n" +
                                "• We never collect or store your payment card or bank credentials.\n\n" +
                                "5. CHILDREN'S PRIVACY (COPPA)\n" +
                                "Bonbon Bloom is intended for general audiences (13+). We do not knowingly collect personal information from children under 13.\n\n" +
                                "6. ACCOUNT & DATA DELETION\n" +
                                "• Delete all local and cloud account data anytime via Settings > 'Delete My Data' or at https://chaudhary-crypto.github.io/Bonbon_Bloom/delete-account.html.\n" +
                                "• Contact privacy & support: support@earnvero.app",
                        fontSize = 12.sp,
                        color = Color(0xFFE0E0E0),
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://chaudhary-crypto.github.io/Bonbon_Bloom/privacy.html"))
                                context.startActivity(intent)
                            } catch (_: Throwable) {}
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF80D8FF))
                    ) {
                        Text("🌐 Open Web", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun InAppTermsReaderDialog(
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E102F)),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📜 Terms of Service",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFD54F)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Text("✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "TERMS OF SERVICE — BONBON BLOOM\n\n" +
                                "1. ACCEPTANCE\n" +
                                "By installing or playing Bonbon Bloom, you agree to these Terms and our Privacy Policy.\n\n" +
                                "2. VIRTUAL ITEMS & CURRENCY\n" +
                                "Coins, gems, lives, and boosters are virtual digital items with no real-world monetary value. They cannot be transferred or exchanged for real cash.\n\n" +
                                "3. GOOGLE PLAY PURCHASES, TAXES & REFUNDS\n" +
                                "All purchases and VIP subscriptions are billed solely by Google Play. Applicable regional taxes/GST are included or calculated by Google Play at checkout. Refunds are governed by Google Play's Refund Policy and mandatory statutory consumer protection laws.\n\n" +
                                "4. AUTO-RENEWING SUBSCRIPTIONS (VIP GOLD PASS)\n" +
                                "Weekly and Monthly VIP Gold Passes renew automatically at the price displayed on Google Play unless cancelled at least 24 hours before the end of the current period in Google Play Store > Payments & subscriptions > Subscriptions.\n\n" +
                                "5. FAIR PLAY & ANTI-CHEAT\n" +
                                "Players may not tamper with saves, forge purchase receipts, or reverse-engineer game clients. Verified violations forfeit illegally obtained items and suspend cloud sync.\n\n" +
                                "6. CONTACT & SUPPORT\n" +
                                "Support, refund inquiries, and legal notices: support@earnvero.app",
                        fontSize = 12.sp,
                        color = Color(0xFFE0E0E0),
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://chaudhary-crypto.github.io/Bonbon_Bloom/terms.html"))
                                context.startActivity(intent)
                            } catch (_: Throwable) {}
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF80D8FF))
                    ) {
                        Text("🌐 Open Web", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DeveloperSandboxDialog(
    activity: Activity?,
    onDismiss: () -> Unit,
    onAddCoins: () -> Unit,
    onAddDiamonds: () -> Unit,
    onRefillLives: () -> Unit,
    onGrantInfiniteLives: () -> Unit,
    onGrantBoosters: () -> Unit,
    onUnlockAllLevels: () -> Unit,
    onRemoveAds: () -> Unit,
    onGrantVip: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140B24)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧪", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "QA / DEV SANDBOX",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00E676)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Text("✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Test in-game economy, progression, AdMob ads, and billing features instantly.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Economy Testing
                SettingsSectionHeader("CURRENCY & LIVES")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAddCoins,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                    ) {
                        Text("+5,000 🟡", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
                    }

                    Button(
                        onClick = onAddDiamonds,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF))
                    ) {
                        Text("+200 💎", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRefillLives,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                    ) {
                        Text("Full Lives ❤️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onGrantInfiniteLives,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                    ) {
                        Text("2h Infinite ♾️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progression & Boosters
                SettingsSectionHeader("PROGRESSION & BOOSTERS")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onUnlockAllLevels,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Unlock 1,000 🗺️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onGrantBoosters,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                    ) {
                        Text("+5 Boosters 🚀", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Billing & Subscriptions Simulation
                SettingsSectionHeader("PURCHASE SIMULATION")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRemoveAds,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                    ) {
                        Text("Grant No Ads 🚫", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onGrantVip,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
                    ) {
                        Text("Grant VIP 👑", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Spacer(modifier = Modifier.height(14.dp))

                // AdMob Ads Controls & Mode Switcher
                val isTestAdsActive by com.example.ads.AdManager.isTestAdsMode.collectAsState()
                SettingsSectionHeader("ADMIN AD CONTROLS")
                
                // Status card explaining AdMob approval status and test ads
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTestAdsActive) Color(0xFF3E2723) else Color(0xFF1B5E20)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isTestAdsActive) "🟡 TEST ADS ACTIVE" else "🟢 PRODUCTION ADS ACTIVE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isTestAdsActive) Color(0xFFFFD54F) else Color(0xFFB9F6CA)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isTestAdsActive)
                                "Account Approval Pending: Google test ads serve on all banners & rewarded videos so you can test smoothly. Your production IDs (pub-9950623744691218) are preserved."
                            else
                                "Serving live production ads from your AdMob account (pub-9950623744691218).",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Toggle Button for Admin
                Button(
                    onClick = {
                        val currentContext = activity ?: context
                        com.example.ads.AdManager.toggleTestAdsMode(currentContext)
                    },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTestAdsActive) Color(0xFFFFA000) else Color(0xFF43A047)
                    )
                ) {
                    Text(
                        text = if (isTestAdsActive) "Switch to Live Ads (AdMob pub-9950623744691218)" else "Switch to Test Ads (Safe Testing)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (activity != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                com.example.ads.AdManager.showInterstitial(activity, currentLevelId = 11, forceShow = true)
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF80D8FF))
                        ) {
                            Text("🎬 Test Interstitial", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                com.example.ads.AdManager.showRewarded(
                                    activity = activity,
                                    onRewardEarned = {
                                        onAddCoins()
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB9F6CA))
                        ) {
                            Text("📺 Test Rewarded (+150 🪙)", fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF311B92))
                ) {
                    Text("Close Sandbox", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFFFFD54F),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
    )
}

@Composable
fun SettingToggleRow(
    icon: String,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFFFD54F),
                checkedTrackColor = Color(0xFF7C4DFF)
            )
        )
    }
}
