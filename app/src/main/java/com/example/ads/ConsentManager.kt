package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * ConsentManager handles Google User Messaging Platform (UMP) SDK for EEA/UK compliance:
 * - Requests consent information
 * - Shows consent form if required
 * - Exposes Privacy Options form for Settings
 * - General audience: tagForUnderAgeOfConsent = false
 */
object ConsentManager {
    private const val TAG = "ConsentManager"
    private var consentInformation: ConsentInformation? = null

    val canRequestAds: Boolean
        get() = consentInformation?.canRequestAds() ?: true

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation?.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun requestConsent(activity: Activity, onConsentCompleted: () -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false) // General audience (not child-directed)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation?.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.errorCode}: ${formError.message}")
                    }
                    onConsentCompleted()
                }
            },
            { requestConsentError ->
                Log.w(TAG, "Consent update failed: ${requestConsentError.errorCode}: ${requestConsentError.message}")
                onConsentCompleted()
            }
        )
    }

    fun showPrivacyOptionsForm(activity: Activity, onDismiss: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy options form error: ${formError.message}")
            }
            onDismiss()
        }
    }
}
