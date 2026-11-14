package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.asStateFlow

/**
 * ConsentManager coordinates Google User Messaging Platform (UMP) SDK for EEA, UK,
 * and global privacy compliance:
 * 1. Requests consent information updates.
 * 2. Loads and displays the required consent form if needed.
 * 3. Evaluates ConsentInformation.canRequestAds() for EEA/UK jurisdictions.
 * 4. Initializes MobileAds and preloads ads.
 * 5. Handles activity recreation idempotently.
 * 6. Exposes privacy options form for settings re-consent.
 */
object ConsentManager {
    private const val TAG = "ConsentManager"

    private var consentInformation: ConsentInformation? = null
    private val isMobileAdsInitialized = AtomicBoolean(false)
    private val isConsentGatheringInProgress = AtomicBoolean(false)

    private val _canRequestAdsState = kotlinx.coroutines.flow.MutableStateFlow(false)
    val canRequestAdsState: kotlinx.coroutines.flow.StateFlow<Boolean> = _canRequestAdsState.asStateFlow()

    /**
     * Indicates whether ads can be requested based on UMP consent evaluation.
     * Guaranteed to permit ads globally unless explicitly revoked or blocked in EEA.
     */
    val canRequestAds: Boolean
        get() {
            val info = consentInformation ?: return false
            if (info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) {
                return info.canRequestAds()
            }
            return info.canRequestAds()
        }

    /**
     * Whether the user must be presented with Privacy Options (e.g. EEA/UK re-consent in Settings).
     */
    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation?.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Requests UMP consent information update and displays form if required.
     * Guaranteed to trigger onConsentCompleted with the boolean indicating whether ads are allowed.
     */
    fun requestConsent(activity: Activity, onConsentCompleted: (canRequest: Boolean) -> Unit = {}) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false) // General audience app (not child-directed)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        // Initialize immediately if consent is already valid or not required
        if (canRequestAds) {
            initializeMobileAds(activity)
        }

        if (isConsentGatheringInProgress.compareAndSet(false, true)) {
            consentInformation?.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        isConsentGatheringInProgress.set(false)
                        if (formError != null) {
                            Log.w(TAG, "Consent form error [${formError.errorCode}]: ${formError.message}")
                        }

                        val canRequest = canRequestAds
                        _canRequestAdsState.value = canRequest
                        Log.i(TAG, "UMP consent evaluation complete. canRequestAds=$canRequest")
                        if (canRequest) {
                            initializeMobileAds(activity)
                        }
                        onConsentCompleted(canRequest)
                    }
                },
                { requestConsentError ->
                    isConsentGatheringInProgress.set(false)
                    Log.w(TAG, "Consent update failed [${requestConsentError.errorCode}]: ${requestConsentError.message}")
                    val canRequest = canRequestAds
                    _canRequestAdsState.value = canRequest
                    if (canRequest) {
                        initializeMobileAds(activity)
                    }
                    onConsentCompleted(canRequest)
                }
            )
        } else {
            val canRequest = canRequestAds
            _canRequestAdsState.value = canRequest
            onConsentCompleted(canRequest)
        }
    }

    /**
     * Initializes Google Mobile Ads SDK ensuring idempotency.
     */
    fun initializeMobileAds(context: Context) {
        if (isMobileAdsInitialized.compareAndSet(false, true)) {
            try {
                val requestConfig = MobileAds.getRequestConfiguration().toBuilder()
                    .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
                    .build()
                MobileAds.setRequestConfiguration(requestConfig)

                MobileAds.initialize(context.applicationContext) { initStatus ->
                    Log.i(TAG, "Google Mobile Ads SDK successfully initialized: $initStatus")
                    AdManager.preloadAds(context.applicationContext)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "MobileAds initialization encountered an error: ${e.message}")
            }
        } else {
            // Already initialized, ensure ads are preloaded
            AdManager.preloadAds(context.applicationContext)
        }
    }

    /**
     * Presents privacy options form allowing players to update or revoke their consent.
     */
    fun showPrivacyOptionsForm(activity: Activity, onDismiss: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy options form error: ${formError.message}")
            }
            val canRequest = canRequestAds
            if (canRequest) {
                initializeMobileAds(activity)
            }
            onDismiss()
        }
    }
}
