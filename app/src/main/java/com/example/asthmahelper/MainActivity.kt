package com.example.asthmahelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.asthmahelper.ui.theme.AsthmaHelperTheme
import com.example.asthmahelper.widget.PefWidgetProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PefWidgetProvider.refresh(this)
        setContent {
            AsthmaHelperTheme {
                AsthmaHelperApp()
            }
        }
    }
}
