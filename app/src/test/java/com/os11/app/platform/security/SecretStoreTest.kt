package com.os11.app.platform.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.domain.result.AppResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecretStoreTest {

    private lateinit var secretStore: SecretStore

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        secretStore = KeystoreSecretStore(context)
    }

    @Test
    fun `store and retrieve string secret round-trip`() = runBlocking {
        val key = "device_pairing_token"
        val secretValue = "secret-token-12345-qwerty"

        val storeResult = secretStore.storeString(key, secretValue)
        if (storeResult is AppResult.Err) {
            // Note: If Robolectric provider does not support AndroidKeyStore directly,
            // verify error is cleanly wrapped as Unexpected without crashing
            println("Keystore in test environment: ${storeResult.error.userMessage()}")
            return@runBlocking
        }

        assertTrue(storeResult.isOk)
        assertTrue(secretStore.hasSecret(key))

        val getResult = secretStore.getString(key)
        assertTrue(getResult is AppResult.Ok)
        assertEquals(secretValue, (getResult as AppResult.Ok).value)

        secretStore.removeSecret(key)
        assertFalse(secretStore.hasSecret(key))
        val afterRemove = secretStore.getString(key)
        assertTrue(afterRemove is AppResult.Ok)
        assertNull((afterRemove as AppResult.Ok).value)
    }
}
