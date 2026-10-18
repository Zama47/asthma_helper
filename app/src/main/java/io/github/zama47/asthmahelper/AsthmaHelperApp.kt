package io.github.zama47.asthmahelper

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.zama47.asthmahelper.ui.about.AboutScreen
import io.github.zama47.asthmahelper.ui.act.ActScreen
import io.github.zama47.asthmahelper.ui.breathing.BreathingScreen
import io.github.zama47.asthmahelper.ui.calendar.AsthmaCalendarScreen
import io.github.zama47.asthmahelper.ui.components.BottomNavigationBar
import io.github.zama47.asthmahelper.ui.dashboard.DashboardScreen
import io.github.zama47.asthmahelper.ui.medications.MedicationsScreen
import io.github.zama47.asthmahelper.ui.weather.WeatherScreen

@Composable
fun AsthmaHelperApp(startAddMeasurement: Boolean = false, onStartAddMeasurementHandled: () -> Unit = {}) {
    val navController = rememberNavController()

    // Запрос из виджета: перейти на главную и открыть диалог добавления замера.
    LaunchedEffect(startAddMeasurement) {
        if (startAddMeasurement) {
            navController.navigate("dashboard") { launchSingleTop = true }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    navController = navController,
                    startWithAddMeasurement = startAddMeasurement,
                    onStartAddMeasurementHandled = onStartAddMeasurementHandled
                )
            }
            composable("breathing") { BreathingScreen(navController) }
            composable("medications") { MedicationsScreen(navController) }
            composable("calendar") { AsthmaCalendarScreen(navController) }
            composable("weather") { WeatherScreen(navController) }
            composable("act") {
                ActScreen(
                    onClose = {
                        if (!navController.popBackStack()) {
                            navController.navigate("dashboard")
                        }
                    }
                )
            }
            composable("about") {
                AboutScreen(
                    onClose = {
                        if (!navController.popBackStack()) {
                            navController.navigate("dashboard")
                        }
                    }
                )
            }
        }
    }
}
