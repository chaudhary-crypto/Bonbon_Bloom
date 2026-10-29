package com.example.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.payment.PlayBillingManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.BANNER_AD_UNIT_ID
) {
    val context = LocalContext.current
    val isInEditMode = LocalInspectionMode.current
    val hasRemovedAds by PlayBillingManager.hasRemovedAds.collectAsState()

    // If player purchased Remove Ads, do not display or load banners
    if (hasRemovedAds) {
        Box(modifier = Modifier.size(0.dp))
        return
    }

    var isAdLoaded by remember { mutableStateOf(false) }
    var adLoadFailed by remember { mutableStateOf(false) }

    if (isInEditMode) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Google AdMob Banner (Preview)", fontSize = 12.sp, color = Color.Gray)
        }
        return
    }

    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                adViewRef?.destroy()
            } catch (_: Throwable) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF212121).copy(alpha = 0.04f))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                try {
                    val view = AdView(ctx)
                    adViewRef = view
                    view.setAdSize(AdSize.BANNER)
                    view.adUnitId = adUnitId
                    view.adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                            adLoadFailed = false
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isAdLoaded = false
                            adLoadFailed = true
                        }
                    }
                    view.loadAd(AdRequest.Builder().build())
                    view
                } catch (e: Throwable) {
                    Log.w("BannerAdView", "AdView creation skipped safely: ${e.message}")
                    adLoadFailed = true
                    android.view.View(ctx)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Subtle placeholder shown while banner is requesting or if offline
        if (!isAdLoaded && !adLoadFailed) {
            Text(
                text = "Sponsored • Google Ads",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        } else if (adLoadFailed) {
            Text(
                text = "Bonbon Bloom • Ad Space",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.LightGray
            )
        }
    }
}
