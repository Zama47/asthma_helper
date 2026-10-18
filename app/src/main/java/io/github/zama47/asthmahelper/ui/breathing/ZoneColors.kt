package io.github.zama47.asthmahelper.ui.breathing

import androidx.compose.ui.graphics.Color
import io.github.zama47.asthmahelper.domain.clinical.Zone

/** Цвет зоны ПСВ для UI. Логика зон — в domain.clinical.PeakFlowAnalytics. */
internal val Zone.color: Color
    get() = when (this) {
        Zone.GREEN -> Color(0xFF4CAF50)
        Zone.YELLOW -> Color(0xFFFFC107)
        Zone.RED -> Color(0xFFF44336)
    }
