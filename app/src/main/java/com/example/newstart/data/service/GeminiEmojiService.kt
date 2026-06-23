package com.example.newstart.data.service

import com.example.newstart.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiEmojiService @Inject constructor() {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    suspend fun suggestEmojis(text: String): List<String> = withContext(Dispatchers.IO) {
        if (text.isBlank() || BuildConfig.GEMINI_API_KEY.isBlank()) {
            return@withContext emptyList()
        }

        val prompt = """
            Bạn là trợ lý ảo phân tích cảm xúc nhật ký tiếng Việt. Hãy phân tích đoạn nhật ký sau và trả về chính xác từ 4 đến 5 ký tự emoji phù hợp nhất với tâm trạng hoặc nội dung của bài viết.
            
            Yêu cầu bắt buộc:
            - Phân tách các emoji bằng khoảng trắng (ví dụ: 😊 🥰 🚵 🔥).
            - Không trả về thêm bất kỳ chữ, giải thích hay ký tự nào khác ngoài các emoji này.
            
            Nhật ký: "$text"
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(prompt)
            val responseText = response.text?.trim() ?: ""
            if (responseText.isNotEmpty()) {
                responseText.split(Regex("\\s+")).filter { it.isNotEmpty() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiEmojiService", "Lỗi khi gọi Gemini API gợi ý emoji: ${e.message}", e)
            emptyList()
        }
    }
}
