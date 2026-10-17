package com.example.asthmahelper.ui.act

import androidx.compose.ui.graphics.Color
import com.example.asthmahelper.domain.clinical.ActLevel

/** Текстовая расшифровка уровня контроля ACT. */
val ActLevel.label: String
    get() = when (this) {
        ActLevel.WELL_CONTROLLED -> "Хороший контроль"
        ActLevel.PARTIAL -> "Частичный контроль"
        ActLevel.UNCONTROLLED -> "Нет контроля"
    }

/** Цвет уровня контроля ACT. */
val ActLevel.color: Color
    get() = when (this) {
        ActLevel.WELL_CONTROLLED -> Color(0xFF4CAF50)
        ActLevel.PARTIAL -> Color(0xFFFFC107)
        ActLevel.UNCONTROLLED -> Color(0xFFF44336)
    }
