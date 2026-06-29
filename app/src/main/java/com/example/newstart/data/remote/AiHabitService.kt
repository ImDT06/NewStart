package com.example.newstart.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiHabitService @Inject constructor(
    private val client: OkHttpClient // Sử dụng OkHttpClient đã có trong project
) {
    // ĐƯỜNG LINK RAILWAY MỚI
    private val SERVER_URL = "https://newstart-backend-production.up.railway.app/api/ai/process"

    suspend fun processCommand(input: String, currentTime: String): JSONObject {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("prompt", "Current time: $currentTime. User input: $input")
                }

                val request = Request.Builder()
                    .url(SERVER_URL)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseStr = response.body?.string()?.trim() ?: ""
                    android.util.Log.d("AiHabitService", "Response from Railway: $responseStr")
                    
                    if (!response.isSuccessful) {
                        throw Exception("Lỗi Server Railway: $responseStr")
                    }
                    
                    // Clean markdown code blocks from response if present
                    var cleanJson = responseStr
                    if (cleanJson.startsWith("```")) {
                        cleanJson = cleanJson.substringAfter("\n")
                    }
                    if (cleanJson.endsWith("```")) {
                        cleanJson = cleanJson.substringBeforeLast("```")
                    }
                    cleanJson = cleanJson.trim()
                    if (cleanJson.startsWith("json")) {
                        cleanJson = cleanJson.substring(4).trim()
                    }

                    val resultsArray = try {
                        JSONArray(cleanJson)
                    } catch (e: Exception) {
                        try {
                            JSONArray().put(JSONObject(cleanJson))
                        } catch (ex: Exception) {
                            JSONArray()
                        }
                    }

                    // Giữ nguyên logic trả về kết quả cho App
                    val result = JSONObject()
                    result.put("results", resultsArray)
                    result
                }
            } catch (e: Exception) {
                android.util.Log.e("AiHabitService", "Lỗi: ${e.message}", e)
                throw Exception("Lỗi: ${e.localizedMessage}")
            }
        }
    }
}
