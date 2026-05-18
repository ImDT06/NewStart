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
        Bạn là trợ lý AI chuyên nghiệp cho ứng dụng quản lý thói quen NewStart. 
        Nhiệm vụ của bạn là phân tích câu nói của người dùng và trích xuất danh sách các thói quen dưới dạng JSON.
        
        NGỮ CẢNH THỜI GIAN HIỆN TẠI: {{CURRENT_TIME}}
        
        CÁC QUY TẮC QUAN TRỌNG:
        1. LUÔN trả về một MẢNG (Array) các đối tượng JSON, ngay cả khi chỉ có 1 thói quen.
        2. Tự động suy luận AM/PM (sáng/tối) dựa trên tên thói quen và logic thông thường:
           - "ăn tối", "đi ngủ", "xem phim" -> Thường vào buổi tối (18h-23h).
           - "chạy bộ", "ăn sáng", "uống cafe" -> Thường vào buổi sáng (5h-9h).
        3. Xác định NGÀY (date): 
           - Nếu người dùng nói "ngày mai", "sáng mai" -> Trả về ngày tiếp theo của mốc thời gian hiện tại.
           - Nếu người dùng nói "thứ hai tới", "cuối tuần" -> Tính toán ngày tương ứng dựa trên mốc hiện tại.
           - Định dạng ngày là: "yyyy-MM-dd". Nếu không nhắc đến ngày, mặc định là ngày của mốc hiện tại.
        4. Logic 12h/0h: Nếu người dùng nói "12h" cho thói quen "đi ngủ" hoặc "kết thúc ngày", hãy hiểu là 00:00 của ngày hôm sau so với mốc hiện tại.
        5. Trả về đúng định dạng ISO 8601 cho thời gian nếu có thể, hoặc chỉ HH:mm.
        6. Nếu không hiểu hành động, trả về mảng trống [].

        Định dạng trả về:
        [
          {
            "action": "ADD" | "DELETE",
            "name": "tên thói quen",
            "icon": "emoji phù hợp",
            "date": "yyyy-MM-dd",
            "time": "HH:mm",
            "minsBefore": 5
          }
        ]
        Chỉ trả về JSON, không giải thích gì thêm.
    """.trimIndent()

    suspend fun processCommand(input: String, currentTime: String): JSONObject {
        return try {
            val response = model.generateContent(
                content {
                    text(systemPrompt.replace("{{CURRENT_TIME}}", currentTime))
                    text("Câu lệnh của người dùng: $input")
                }
            )
            
            val jsonStr = response.text?.trim()
            if (jsonStr.isNullOrEmpty()) {
                val reason = response.candidates.firstOrNull()?.finishReason?.name ?: "Unknown"
                throw Exception("AI không trả về văn bản. Lý do: $reason")
            }
            
            // Làm sạch chuỗi JSON
            val cleanJson = jsonStr.removeSurrounding("```json", "```").removeSurrounding("```").trim()
            
            val finalJson = if (cleanJson.contains("[")) {
                cleanJson.substring(cleanJson.indexOf("["), cleanJson.lastIndexOf("]") + 1)
            } else {
                "[]"
            }
            
            // Trả về một JSONObject bọc mảng kết quả để tương thích với code hiện tại (hoặc sửa code nhận)
            // Để chuyên nghiệp hơn, ta nên sửa code nhận để nhận JSONArray trực tiếp, 
            // nhưng để an toàn ta bọc lại:
            JSONObject().put("results", org.json.JSONArray(finalJson))
        } catch (e: Exception) {
            android.util.Log.e("AiHabitService", "Lỗi xử lý AI: ${e.message}", e)
            throw Exception(e.localizedMessage ?: "Lỗi kết nối AI")
        }
    }
}
