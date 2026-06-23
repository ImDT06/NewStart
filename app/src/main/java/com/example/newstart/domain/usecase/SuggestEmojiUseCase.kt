package com.example.newstart.domain.usecase

import com.example.newstart.data.service.GeminiEmojiService
import javax.inject.Inject

class SuggestEmojiUseCase @Inject constructor(
    private val geminiEmojiService: GeminiEmojiService
) {
    suspend operator fun invoke(text: String): List<String> {
        return geminiEmojiService.suggestEmojis(text)
    }
}
