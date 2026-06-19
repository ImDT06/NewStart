package com.example.newstart.util

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext context: Context
) {
    init {
        AeadConfig.register()
    }

    private val keysetName = "master_keyset"
    private val prefFileName = "master_key_preference"
    private val masterKeyUri = "android-keystore://master_key"

    private val aead: Aead = AndroidKeysetManager.Builder()
        .withSharedPref(context, keysetName, prefFileName)
        .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
        .withMasterKeyUri(masterKeyUri)
        .build()
        .keysetHandle
        .getPrimitive(Aead::class.java)

    fun encrypt(data: ByteArray): ByteArray {
        return aead.encrypt(data, null)
    }

    fun decrypt(encryptedData: ByteArray): ByteArray {
        return aead.decrypt(encryptedData, null)
    }
}
