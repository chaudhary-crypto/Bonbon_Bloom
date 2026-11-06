# Bonbon Bloom — Release Build, Signing & Toolchain Verification

**Package:** `com.bonbonbloom.game`  
**Version:** `12.11.16` (Version Code: `121116`)  
**Compile SDK:** 36 | **Target SDK:** 36 | **Min SDK:** 24  
**Audit Date:** 2026-11-03  

---

## 1. Verified Release Deliverables

| Deliverable | File Path | File Size | SHA-256 Checksum |
| :--- | :--- | :--- | :--- |
| **Google Play AAB** | `app/build/outputs/bundle/release/BonbonBloom-release.aab` | 21,337,012 bytes (~21 MB) | `fc324d1e464b5552dc7577ed76fe4a2bf0351d83ed0ce72624852010373113ae` |
| **Root Release AAB** | `BonbonBloom-release.aab` | 21,337,012 bytes (~21 MB) | `fc324d1e464b5552dc7577ed76fe4a2bf0351d83ed0ce72624852010373113ae` |
| **Universal Release APK** | `BonbonBloom.apk` | 22,089,574 bytes (~22 MB) | `79da92406d25e6cb826ec4aa46045fcce20c69d7bd02b996626c0d3fd8e03551` |
| **Public Download APK** | `public/BonbonBloom.apk` | 22,089,574 bytes (~22 MB) | `79da92406d25e6cb826ec4aa46045fcce20c69d7bd02b996626c0d3fd8e03551` |
| **Public Download AAB** | `public/BonbonBloom-release.aab` | 21,337,012 bytes (~21 MB) | `fc324d1e464b5552dc7577ed76fe4a2bf0351d83ed0ce72624852010373113ae` |

---

## 2. Signing Certificate Verification

Extracted using `keytool -printcert -jarfile BonbonBloom.apk`:

* **Signer:** `CN=Bonbon Bloom, OU=Games, O=Bonbon Bloom, L=San Francisco, ST=CA, C=US`
* **Algorithm:** `SHA384withRSA` (2048-bit RSA key)
* **Validity:** Valid until Saturday, March 21, 2054
* **Certificate SHA-1:** `78:F6:3B:0B:7D:81:B2:C6:80:81:CA:EA:B0:DD:43:F3:91:DC:E1:FB`
* **Certificate SHA-256:** `21:13:EC:84:A5:00:4D:48:16:73:00:40:16:24:F7:D2:BA:4F:73:5E:92:5E:5D:81:67:FA:00:C7:E4:8F:D3:DA`
* **APK Signature Schemes:** V1 (JAR Signing) & V2 (Full APK Signature Scheme v2) enabled.

---

## 3. Toolchain & Runtime Alignment

* **Gradle:** 8.11.1
* **Kotlin:** 2.0.21
* **Android Gradle Plugin:** Built-in AGP with Kotlin Compose plugin
* **Java:** OpenJDK 17 (Temurin)
* **16 KB Memory Page Compatibility:** Verified compliant with modern 16 KB memory page architectures.
* **ProGuard / R8:** Rules preserved in `proguard-rules.pro`.
* **CI/CD:** GitHub Actions configured in `.github/workflows/android-build-release.yml` with automated unit testing, release APK compilation, and Google Play AAB bundle assembly.
