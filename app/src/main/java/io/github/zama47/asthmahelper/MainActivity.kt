package io.github.zama47.asthmahelper

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import io.github.zama47.asthmahelper.ui.theme.AsthmaHelperTheme
import io.github.zama47.asthmahelper.widget.AsthmaWidgetProvider

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Запрос «сразу открыть добавление замера» (приходит из виджета). */
    private var startAddMeasurement by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startAddMeasurement = intent.getBooleanExtra(EXTRA_ADD_MEASUREMENT, false)
        AsthmaWidgetProvider.refresh(this)
        setContent {
            AsthmaHelperTheme {
                AsthmaHelperApp(
                    startAddMeasurement = startAddMeasurement,
                    onStartAddMeasurementHandled = { startAddMeasurement = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startAddMeasurement = intent.getBooleanExtra(EXTRA_ADD_MEASUREMENT, false)
    }

    companion object {
        const val EXTRA_ADD_MEASUREMENT = "io.github.zama47.asthmahelper.extra.ADD_MEASUREMENT"
    }
}
