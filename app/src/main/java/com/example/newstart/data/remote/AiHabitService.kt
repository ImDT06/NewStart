package com.example.newstart.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiHabitService @Inject constructor(
    private val client: OkHttpClient // Sử dụng OkHttpClient đã có trong project
) {
    // ĐƯỜNG LINK VERCEL ĐÃ ĐƯỢC CẬP NHẬT TỪ PROJECT CỦA BẠN
    private val VERCEL_URL = "https://vercel-new-start.vercel.app/api/process"

    suspend fun processCommand(input: String, currentTime: String): JSONObject {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("prompt", input)
                    put("currentTime", currentTime)
                }

                val request = Request.Builder()
                    .url(VERCEL_URL)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseStr = response.body?.string()?.trim() ?: "{}"
                    android.util.Log.d("AiHabitService", "Response: $responseStr")
                    
                    if (!response.isSuccessful) {
                        android.util.Log.e("AiHabitService", "Server Error: ${response.code} - $responseStr")
                        throw Exception("Lỗi Server (${response.code}): $responseStr")
                    }
                    
                    if (responseStr.startsWith("[")) {
                        JSONObject().put("results", org.json.JSONArray(responseStr))
                    } else {
                        val parsedJson = JSONObject(responseStr)
                        if (parsedJson.has("results")) {
                            parsedJson
                        } else {
                            JSONObject().put("results", org.json.JSONArray().put(parsedJson))
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AiHabitService", "Lỗi: ${e.message}", e)
                throw Exception("Lỗi: ${e.localizedMessage}")
            }
        }
    }
}
