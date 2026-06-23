package com.example.newstart.data.preferences

import androidx.datastore.core.Serializer
import com.example.newstart.util.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class EncryptedSerializer<T>(
    private val cryptoManager: CryptoManager,
    private val kSerializer: KSerializer<T>,
    override val defaultValue: T
) : Serializer<T> {

    override suspend fun readFrom(input: InputStream): T = withContext(Dispatchers.IO) {
        val bytes = input.readBytes()
        if (bytes.isEmpty()) return@withContext defaultValue
        
        try {
            val decryptedBytes = cryptoManager.decrypt(bytes)
            Json.decodeFromString(kSerializer, decryptedBytes.decodeToString())
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: T, output: OutputStream) = withContext(Dispatchers.IO) {
        val jsonString = Json.encodeToString(kSerializer, t)
        val encryptedBytes = cryptoManager.encrypt(jsonString.encodeToByteArray())
        output.write(encryptedBytes)
    }
}
