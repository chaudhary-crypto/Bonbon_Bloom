import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.bonbonbloom.game"
    minSdk = 24
    targetSdk = 36
    versionCode = 121116
    versionName = "12.11.16"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
    buildConfigField("String", "PRIVACY_POLICY_URL", "\"https://www.earnvero.app/privacy\"")
    buildConfigField("String", "TERMS_URL", "\"https://www.earnvero.app/terms\"")
  }

  signingConfigs {
    create("release") {
      val customKeystore = System.getenv("KEYSTORE_PATH")?.let { file(it) }
      val defaultUploadKeystore = file("${rootDir}/my-upload-key.jks")
      val debugKeystore = file("${rootDir}/debug.keystore")
      storeFile = when {
        customKeystore?.exists() == true -> customKeystore
        defaultUploadKeystore.exists() -> defaultUploadKeystore
        else -> debugKeystore
      }
      storePassword = System.getenv("STORE_PASSWORD") ?: "android"
      keyAlias = System.getenv("KEY_ALIAS") ?: (if (defaultUploadKeystore.exists() || customKeystore?.exists() == true) "upload" else "androiddebugkey")
      keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
      enableV1Signing = true
      enableV2Signing = true
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
      enableV1Signing = true
      enableV2Signing = true
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")

      val envFile = rootProject.file(".env")
      val envProps = Properties()
      if (envFile.exists()) {
          val stream = envFile.inputStream()
          try {
              envProps.load(stream)
          } finally {
              stream.close()
          }
      }

      val defaultProdAppId = "ca-app-pub-9950623744691218~4994758991"
      val defaultProdBanner = "ca-app-pub-9950623744691218/3912118329"
      val defaultProdInterstitial = "ca-app-pub-9950623744691218/9288568089"
      val defaultProdRewardedInterstitial = "ca-app-pub-9950623744691218/7505860934"
      val defaultProdRewarded = "ca-app-pub-9950623744691218/7505860934"

      val prodAppId = (project.findProperty("ADMOB_APP_ID") as? String)
          ?: envProps.getProperty("ADMOB_APP_ID") ?: System.getenv("ADMOB_APP_ID") ?: defaultProdAppId
      val prodBannerId = (project.findProperty("ADMOB_BANNER_ID") as? String)
          ?: envProps.getProperty("ADMOB_BANNER_ID") ?: System.getenv("ADMOB_BANNER_ID") ?: defaultProdBanner
      val prodInterstitialId = (project.findProperty("ADMOB_INTERSTITIAL_ID") as? String)
          ?: envProps.getProperty("ADMOB_INTERSTITIAL_ID") ?: System.getenv("ADMOB_INTERSTITIAL_ID") ?: defaultProdInterstitial
      val prodRewardedInterstitialId = (project.findProperty("ADMOB_REWARDED_INTERSTITIAL_ID") as? String)
          ?: envProps.getProperty("ADMOB_REWARDED_INTERSTITIAL_ID") ?: System.getenv("ADMOB_REWARDED_INTERSTITIAL_ID") ?: defaultProdRewardedInterstitial
      val prodRewardedId = (project.findProperty("ADMOB_REWARDED_ID") as? String)
          ?: envProps.getProperty("ADMOB_REWARDED_ID") ?: System.getenv("ADMOB_REWARDED_ID") ?: defaultProdRewarded

      val testAppId = "ca-app-pub-3940256099942544~3347511713"
      val allowTestInRelease = (project.findProperty("ALLOW_TEST_ADS_IN_RELEASE") as? String
          ?: System.getenv("ALLOW_TEST_ADS_IN_RELEASE")) == "true"

      val isReleaseTarget = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }

      val appIdRegex = Regex("^ca-app-pub-\\d{16}~\\d{10}$")
      val adUnitRegex = Regex("^ca-app-pub-\\d{16}/\\d{10}$")

      if (isReleaseTarget && !allowTestInRelease) {
          if (prodAppId.isEmpty() || prodAppId == testAppId || !appIdRegex.matches(prodAppId)) {
              throw GradleException(
                  "PRODUCTION ADMOB ERROR: A valid production ADMOB_APP_ID is required for release builds (format: ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX). " +
                  "Never use sample test IDs in production releases. For development staging, set ALLOW_TEST_ADS_IN_RELEASE=true."
              )
          }
          if (prodBannerId.isNotEmpty() && !adUnitRegex.matches(prodBannerId)) {
              throw GradleException("Invalid ADMOB_BANNER_ID format: $prodBannerId")
          }
          if (prodInterstitialId.isNotEmpty() && !adUnitRegex.matches(prodInterstitialId)) {
              throw GradleException("Invalid ADMOB_INTERSTITIAL_ID format: $prodInterstitialId")
          }
          if (prodRewardedInterstitialId.isNotEmpty() && !adUnitRegex.matches(prodRewardedInterstitialId)) {
              throw GradleException("Invalid ADMOB_REWARDED_INTERSTITIAL_ID format: $prodRewardedInterstitialId")
          }
          if (prodRewardedId.isNotEmpty() && !adUnitRegex.matches(prodRewardedId)) {
              throw GradleException("Invalid ADMOB_REWARDED_ID format: $prodRewardedId")
          }
      }

      val effectiveAppId = if (prodAppId.isNotEmpty()) prodAppId else defaultProdAppId
      val effectiveBanner = if (prodBannerId.isNotEmpty()) prodBannerId else defaultProdBanner
      val effectiveInterstitial = if (prodInterstitialId.isNotEmpty()) prodInterstitialId else defaultProdInterstitial
      val effectiveRewardedInterstitial = if (prodRewardedInterstitialId.isNotEmpty()) prodRewardedInterstitialId else defaultProdRewardedInterstitial
      val effectiveRewarded = if (prodRewardedId.isNotEmpty()) prodRewardedId else defaultProdRewarded

      manifestPlaceholders["admobAppId"] = effectiveAppId
      buildConfigField("String", "ADMOB_BANNER_ID", "\"$effectiveBanner\"")
      buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$effectiveInterstitial\"")
      buildConfigField("String", "ADMOB_REWARDED_INTERSTITIAL_ID", "\"$effectiveRewardedInterstitial\"")
      buildConfigField("String", "ADMOB_REWARDED_ID", "\"$effectiveRewarded\"")

      val defaultUnityGameId = "800427695"
      val defaultUnityRewarded = "BP_Rewarded_Android"
      val defaultUnityInterstitial = "BP_Interstitial_Android"
      val defaultUnityBanner = "BP_Banner_Android"

      val unityGameId = (project.findProperty("UNITY_GAME_ID") as? String)
          ?: envProps.getProperty("UNITY_GAME_ID") ?: System.getenv("UNITY_GAME_ID") ?: defaultUnityGameId
      val unityRewardedPlacement = (project.findProperty("UNITY_REWARDED_PLACEMENT") as? String)
          ?: envProps.getProperty("UNITY_REWARDED_PLACEMENT") ?: System.getenv("UNITY_REWARDED_PLACEMENT") ?: defaultUnityRewarded
      val unityInterstitialPlacement = (project.findProperty("UNITY_INTERSTITIAL_PLACEMENT") as? String)
          ?: envProps.getProperty("UNITY_INTERSTITIAL_PLACEMENT") ?: System.getenv("UNITY_INTERSTITIAL_PLACEMENT") ?: defaultUnityInterstitial
      val unityBannerPlacement = (project.findProperty("UNITY_BANNER_PLACEMENT") as? String)
          ?: envProps.getProperty("UNITY_BANNER_PLACEMENT") ?: System.getenv("UNITY_BANNER_PLACEMENT") ?: defaultUnityBanner

      buildConfigField("String", "UNITY_GAME_ID", "\"$unityGameId\"")
      buildConfigField("String", "UNITY_REWARDED_PLACEMENT", "\"$unityRewardedPlacement\"")
      buildConfigField("String", "UNITY_INTERSTITIAL_PLACEMENT", "\"$unityInterstitialPlacement\"")
      buildConfigField("String", "UNITY_BANNER_PLACEMENT", "\"$unityBannerPlacement\"")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
      manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
      buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
      buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
      buildConfigField("String", "ADMOB_REWARDED_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/5354046379\"")
      buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")

      buildConfigField("String", "UNITY_GAME_ID", "\"800427695\"")
      buildConfigField("String", "UNITY_REWARDED_PLACEMENT", "\"BP_Rewarded_Android\"")
      buildConfigField("String", "UNITY_INTERSTITIAL_PLACEMENT", "\"BP_Interstitial_Android\"")
      buildConfigField("String", "UNITY_BANNER_PLACEMENT", "\"BP_Banner_Android\"")
    }

  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  lint {
    checkReleaseBuilds = false
    abortOnError = false
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

base {
  archivesName.set("BonbonBloom")
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation("androidx.fragment:fragment-ktx:1.8.5")
  implementation("androidx.appcompat:appcompat:1.7.0")
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation("androidx.work:work-runtime-ktx:2.9.0")
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  implementation("com.google.firebase:firebase-analytics")
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.ads)
  // Google Mobile Ads Unity Ads Mediation Adapter & Core SDK
  implementation("com.google.ads.mediation:unity:4.12.5.0")
  implementation("com.unity3d.ads:unity-ads:4.12.5")
  implementation(libs.play.billing)
  implementation(libs.user.messaging.platform)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
