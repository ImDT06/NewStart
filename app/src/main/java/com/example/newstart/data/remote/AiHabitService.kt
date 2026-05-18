package com.example.newstart.data.remote

import com.example.newstart.util.AppConstants
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiHabitService @Inject constructor() {
    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash", 
        apiKey = AppConstants.GEMINI_API_KEY,
        safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
        )
    )

    private val systemPrompt = """
        Bạn là trợ lý AI cho ứng dụng quản lý thói quen NewStart. 
        Nhiệm vụ của bạn là phân tích câu nói của người dùng và trả về kết quả định dạng JSON duy nhất.
        
        Các hành động hỗ trợ:
        1. "ADD": Thêm thói quen. Cần: name (tên), icon (emoji phù hợp), time (HH:mm), minsBefore (số phút nhắc trước).
        2. "DELETE": Xóa thói quen. Cần: name (tên thói quen muốn xóa).

        Định dạng trả về:
        {
          "action": "ADD" | "DELETE" | "UNKNOWN",
          "data": {
             "name": "tên thói quen",
             "icon": "emoji",
             "time": "HH:mm",
             "minsBefore": 0
          }
        }
        Nếu không hiểu hành động, trả về action "UNKNOWN".
        Chỉ trả về JSON, không giải thích gì thêm.
    """.trimIndent()

    suspend fun processCommand(input: String): JSONObject {
        return try {
            val response = model.generateContent(
                content {
                    text(systemPrompt)
                    text("Câu lệnh của người dùng: $input")
                }
            )
            
            val jsonStr = response.text?.trim()
            if (jsonStr.isNullOrEmpty()) {
                val reason = response.candidates.firstOrNull()?.finishReason?.name ?: "Unknown"
                throw Exception("AI không trả về văn bản. Lý do: $reason")
            }
            
            // Làm sạch chuỗi JSON để loại bỏ markdown
            val cleanJson = jsonStr.removeSurrounding("```json", "```").removeSurrounding("```").trim()
            
            val finalJson = if (cleanJson.contains("{")) {
                cleanJson.substring(cleanJson.indexOf("{"), cleanJson.lastIndexOf("}") + 1)
            } else {
                cleanJson
            }
            
            JSONObject(finalJson)
        } catch (e: Exception) {
            // Log lỗi chi tiết để debug
            android.util.Log.e("AiHabitService", "Lỗi xử lý AI: ${e.message}", e)
            
            // Xử lý lỗi đặc thù của thư viện Google khi gặp lỗi 404/400
            val message = when {
                e.message?.contains("404") == true -> "Không tìm thấy Model AI. Vui lòng kiểm tra lại vùng hỗ trợ hoặc cập nhật SDK."
                e.message?.contains("MissingFieldException") == true -> "Lỗi phản hồi từ Google AI. Thư viện gặp trục trặc khi đọc dữ liệu lỗi."
                else -> e.localizedMessage ?: "Lỗi kết nối AI"
            }
            throw Exception(message)
        }
    }
}
