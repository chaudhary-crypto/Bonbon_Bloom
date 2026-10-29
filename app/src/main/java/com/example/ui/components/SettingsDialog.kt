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

                Spacer(modifier = Modifier.height(16.dp))

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
                    text = "Bonbon Bloom v1.0.0 • Offline-First Match-3 Engine",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .clickable {
                            devTapCount++
                            if (devTapCount >= 5) {
                                devTapCount = 0
                                HapticManager.performHapticFeedback(context, com.example.audio.HapticType.Heavy)
                                showDevSandbox = true
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

    // QA & Developer Sandbox Dialog
    if (showDevSandbox) {
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
                        text = "Bonbon Bloom is built with an offline-first privacy design. All gameplay, levels, stars, scores, and inventory are stored locally on your device.\n\n" +
                                "1. DATA COLLECTED LOCALLY\n" +
                                "• Saved game progress, coins, gems, high scores, boosters, and audio settings are kept on your device storage.\n" +
                                "• Cryptographic integrity signatures (HMAC-SHA256) protect against local memory tampering.\n\n" +
                                "2. THIRD-PARTY SDKs\n" +
                                "• Google AdMob: Non-personalized and personalized banner, interstitial, and rewarded video ads when online.\n" +
                                "• Google Play Billing: In-app purchases, subscriptions, and receipt verification.\n" +
                                "• Android WorkManager: Energy-efficient on-device local notifications (lives full, daily streak).\n\n" +
                                "3. CHILDREN'S PRIVACY\n" +
                                "Bonbon Bloom is rated for General Audience (13+). We do not knowingly collect personal information from children.\n\n" +
                                "4. YOUR CHOICES & DATA DELETION\n" +
                                "You can delete all locally saved game progress at any time via Settings > 'Delete My Data'. European users can revoke ad consent via Settings > 'Privacy Options'.",
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
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bonbonbloom.game/privacy"))
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
                                "Coins, gems, lives, and boosters are virtual game items with no real-world cash value. They cannot be refunded or exchanged for real currency except where required by law.\n\n" +
                                "3. SUBSCRIPTIONS (VIP GOLD PASS)\n" +
                                "VIP Gold Pass is an auto-renewing subscription managed through Google Play. Payment will be charged to your Google Play account upon confirmation. Cancel anytime via Google Play Store > Subscriptions at least 24 hours prior to renewal.\n\n" +
                                "4. FAIR PLAY & INTEGRITY\n" +
                                "Players may not modify, reverse engineer, or use unauthorized tools to tamper with saved progress or game balances. Tampered saves are subject to balance reset.\n\n" +
                                "5. NO WARRANTIES\n" +
                                "The game is provided 'as is' for your personal entertainment.",
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
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bonbonbloom.game/terms"))
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

                // AdMob Ads Testing
                if (activity != null) {
                    SettingsSectionHeader("ADMOB AD SIMULATION")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                com.example.ads.AdManager.showInterstitial(activity, force = true)
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF80D8FF))
                        ) {
                            Text("🎬 Interstitial", fontSize = 11.sp)
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
                            Text("📺 Rewarded", fontSize = 11.sp)
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
