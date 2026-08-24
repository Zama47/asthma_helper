package com.example.asthmahelper.ui.breathing

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

/**
 * Линейный график динамики пикфлоуметрии.
 * Оси пока скрыты (упрощение), данные отображаются линией по значениям.
 */
@Composable
fun BreathingChart(
    measurements: List<Pair<Long, Float>>,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(measurements) {
        if (measurements.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            lineSeries {
                series(measurements.map { it.second })
            }
        }
    }

    if (measurements.isEmpty()) return

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer()
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
    )
}
