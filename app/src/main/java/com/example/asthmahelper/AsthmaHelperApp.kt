package com.example.asthmahelper

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.asthmahelper.ui.components.BottomNavigationBar
import com.example.asthmahelper.ui.breathing.BreathingScreen
import com.example.asthmahelper.ui.calendar.AsthmaCalendarScreen
import com.example.asthmahelper.ui.dashboard.DashboardScreen
import com.example.asthmahelper.ui.medications.MedicationsScreen
import com.example.asthmahelper.ui.weather.WeatherScreen

@Composable
fun AsthmaHelperApp() {
    val navController = rememberNavController()
    
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
            composable("dashboard") { DashboardScreen(navController) }
            composable("breathing") { BreathingScreen(navController) }
            composable("medications") { MedicationsScreen(navController) }
            composable("calendar") { AsthmaCalendarScreen(navController) }
            composable("weather") { WeatherScreen(navController) }
        }
    }
}