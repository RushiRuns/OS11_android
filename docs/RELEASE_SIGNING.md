# OS11 Android — Release Signing & Keystore Management

> **CRITICAL REQUIREMENT:** The release keystore is the cryptographic identity of the application. If the private key is lost or corrupted, existing installations **can never be updated**.

## 1. Keystore Generation Procedure

Generate the release signing key using the standard Java `keytool` utility:

```bash
keytool -genkeypair \
  -v \
  -keystore os11-release.jks \
  -alias os11-release \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storetype PKCS12 \
  -dname "CN=OS11 Release, OU=Mobile, O=OS11, C=US"
```

Parameters explained:
- **Algorithm**: RSA 4096-bit (or EC secp384r1) provides long-term cryptographic resistance.
- **Validity**: 10,000 days (~27 years) ensures key will not expire during the active lifecycle of the OS11 ecosystem.
- **Storetype**: PKCS12 is the modern industry standard for keystore storage.

## 2. Mandatory Dual Backup Locations (Architecture Requirement)

In accordance with Phase 20 requirements, the release keystore and its recovery credentials must be backed up in **at least two independent, durable locations**:

1. **Primary Backup (Offline Hardware Cold Storage)**:
   - Encrypted USB drive (BitLocker / LUKS) stored in an offsite or fireproof vault.
   - Includes `os11-release.jks` and an encrypted text file with alias and passwords.

2. **Secondary Backup (Air-gapped Secure Vault / Password Manager)**:
   - Encrypted attachment inside the team/maintainer's zero-knowledge password vault (e.g. 1Password / Bitwarden / KeePassXC).
   - Recovery key printed and held in secure documentation archive.

## 3. Environment-Based CI / Local Build Configuration

Release signing parameters are read from environment variables to keep credentials out of version control:

```kotlin
// In app/build.gradle.kts
signingConfigs {
    create("release") {
        val keystorePath = System.getenv("OS11_RELEASE_KEYSTORE")
        if (!keystorePath.isNullOrBlank() && file(keystorePath).exists()) {
            storeFile = file(keystorePath)
            storePassword = System.getenv("OS11_RELEASE_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("OS11_RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("OS11_RELEASE_KEY_PASSWORD")
        }
    }
}
```

If release environment variables are not supplied, Gradle builds gracefully fall back to the debug keystore for automated verification and CI smoke testing.
