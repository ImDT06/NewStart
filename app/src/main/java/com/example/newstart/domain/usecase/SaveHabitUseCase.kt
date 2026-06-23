package com.example.newstart.domain.usecase

import com.example.newstart.domain.model.Habit
import com.example.newstart.domain.repository.HabitRepository
import javax.inject.Inject

class SaveHabitUseCase @Inject constructor(
    private val habitRepository: HabitRepository
) {
    suspend operator fun invoke(habit: Habit): Result<Unit> {
        return habitRepository.saveHabit(habit)
    }
}
