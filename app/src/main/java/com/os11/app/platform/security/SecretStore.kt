package com.os11.app.platform.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.os11.domain.result.AppError
import com.os11.domain.result.AppResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for securely storing cryptographic material and sensitive tokens.
 *
 * Rules per ARCHITECTURE.md:
 * Secrets (sync pairing token, sync private material, PIN hash and salt) are
 * wrapped with an Android Keystore key (AES-GCM) via SecretStore. Never stored
 * in plain DataStore, plain SQLite, or logs.
 */
interface SecretStore {
    suspend fun storeSecret(key: String, secret: ByteArray): AppResult<Unit>
    suspend fun getSecret(key: String): AppResult<ByteArray?>
    suspend fun storeString(key: String, secret: String): AppResult<Unit>
    suspend fun getString(key: String): AppResult<String?>
    suspend fun removeSecret(key: String): AppResult<Unit>
    suspend fun hasSecret(key: String): Boolean
}

@Singleton
@Suppress("TooGenericExceptionCaught", "ReturnCount")
class KeystoreSecretStore @Inject constructor(@param:ApplicationContext private val context: Context) : SecretStore {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }

    private fun getOrCreateMasterKey(): SecretKey {
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER,
            )
            val spec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(MASTER_KEY_SIZE)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    override suspend fun storeSecret(key: String, secret: ByteArray): AppResult<Unit> = try {
        val masterKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(secret)

        // Combined IV (12 bytes for GCM) + ciphertext
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

        val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)
        prefs.edit().putString(key, encoded).apply()
        AppResult.ok(Unit)
    } catch (e: Exception) {
        AppResult.err(AppError.Unexpected(e))
    }

    override suspend fun getSecret(key: String): AppResult<ByteArray?> {
        return try {
            val encoded = prefs.getString(key, null) ?: return AppResult.ok(null)
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) {
                return AppResult.err(AppError.Validation("secret", "Corrupted secret payload"))
            }

            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            val cipherTextLength = combined.size - GCM_IV_LENGTH
            val cipherText = ByteArray(cipherTextLength)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherTextLength)

            val masterKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
            val decrypted = cipher.doFinal(cipherText)
            AppResult.ok(decrypted)
        } catch (e: Exception) {
            AppResult.err(AppError.Unexpected(e))
        }
    }

    override suspend fun storeString(key: String, secret: String): AppResult<Unit> =
        storeSecret(key, secret.toByteArray(Charsets.UTF_8))

    override suspend fun getString(key: String): AppResult<String?> = when (val res = getSecret(key)) {
        is AppResult.Ok -> {
            val bytes = res.value
            if (bytes == null) AppResult.ok(null) else AppResult.ok(String(bytes, Charsets.UTF_8))
        }
        is AppResult.Err -> res
    }

    override suspend fun removeSecret(key: String): AppResult<Unit> {
        prefs.edit().remove(key).apply()
        return AppResult.ok(Unit)
    }

    override suspend fun hasSecret(key: String): Boolean = prefs.contains(key)

    companion object {
        private const val PREFS_NAME = "os11_secure_storage"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "os11_master_secret_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val MASTER_KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
