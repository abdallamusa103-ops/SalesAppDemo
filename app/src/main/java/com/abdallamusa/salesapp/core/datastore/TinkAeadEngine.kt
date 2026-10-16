package com.abdallamusa.salesapp.core.datastore

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TinkAeadEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val aead: Aead

    init {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        aead = keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        val cipherBytes = aead.encrypt(plaintext.toByteArray(StandardCharsets.UTF_8), null)
        return Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
    }

    fun decrypt(ciphertext: String): String {
        if (ciphertext.isEmpty()) return ""
        return try {
            val decodedBytes = Base64.decode(ciphertext, Base64.NO_WRAP)
            val decryptedBytes = aead.decrypt(decodedBytes, null)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            "" // Fallback safely if decryption fails or data was corrupted
        }
    }

    companion object {
        private const val KEYSET_NAME = "odoo_keyset"
        private const val PREF_FILE_NAME = "odoo_tink_prefs"
        private const val MASTER_KEY_URI = "android-keystore://odoo_master_key"
    }
}