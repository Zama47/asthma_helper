package com.example.asthmahelper.domain.model

/**
 * Норма дыхания с тремя зонами (как в классической пикфлоуметрии):
 *  - Зелёная: 80–100% от личного максимума (Pef) — всё хорошо;
 *  - Жёлтая: 50–80% — предупреждение;
 *  - Красная: < 50% — опасно, нужна помощь врача.
 *
 * minNormal/maxNormal — границы жёлтой зоны (в л/мин).
 */
data class BreathingNorm(
    val minNormal: Float,
    val maxNormal: Float,
    val formulaType: Int // 0 - manual, 1 - by age/height
) {
    /** Нижняя граница красной зоны (л/мин): 50% от maxNormal. */
    val redZoneThreshold: Float
        get() = maxNormal * 0.5f
}
