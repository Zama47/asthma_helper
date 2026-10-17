package com.example.asthmahelper.ui.breathing

import androidx.compose.ui.graphics.Color
import com.example.asthmahelper.domain.clinical.Zone

/** Цвет зоны ПСВ для UI. Логика зон — в domain.clinical.PeakFlowAnalytics. */
internal val Zone.color: Color
    get() = when (this) {
        Zone.GREEN -> Color(0xFF4CAF50)
        Zone.YELLOW -> Color(0xFFFFC107)
        Zone.RED -> Color(0xFFF44336)
    }
