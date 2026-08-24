package com.example.asthmahelper.domain.usecase

import com.example.asthmahelper.domain.model.BreathingNorm
import com.example.asthmahelper.domain.repository.BreathingNormRepository
import javax.inject.Inject

/**
 * Сохраняет норму дыхания.
 * formulaType: 0 - ручной ввод, 1 - по возрасту/росту (норма уже рассчитана вызывающей стороной).
 */
class SetBreathingNormUseCase @Inject constructor(
    private val repository: BreathingNormRepository
) {
    suspend operator fun invoke(minNormal: Float, maxNormal: Float, formulaType: Int) {
        require(minNormal > 0f && maxNormal > minNormal) {
            "Минимум должен быть больше нуля, а максимум больше минимума"
        }
        val existing = repository.getBreathingNorm()
        val norm = BreathingNorm(
            minNormal = minNormal,
            maxNormal = maxNormal,
            formulaType = formulaType
        )
        if (existing != null) repository.updateBreathingNorm(norm)
        else repository.insertBreathingNorm(norm)
    }

    companion object {
        /** Примерная формула нормы для взрослых. */
        fun calculateByAgeAndHeight(age: Int, heightCm: Float): Pair<Float, Float> {
            val mid = heightCm * 5.5f - age * 2.2f + 200f
            return (mid * 0.9f) to (mid * 1.1f)
        }
    }
}
