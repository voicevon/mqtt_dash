package com.antigravity.mqttdash.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.antigravity.mqttdash.ui.navigation.Screen
import com.antigravity.mqttdash.ui.screen.connect.ConnectScreen
import com.antigravity.mqttdash.ui.screen.dashboard.DashboardScreen

@Composable
fun MqttDashApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Connect.route
    ) {
        composable(Screen.Connect.route) {
            ConnectScreen(
                onConnected = { brokerId, dashboardId ->
                    navController.navigate(
                        Screen.Dashboard.createRoute(brokerId, dashboardId)
                    ) {
                        // Clear back stack so Back doesn't return to connect screen while connected
                        popUpTo(Screen.Connect.route) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Screen.Dashboard.route,
            arguments = listOf(
                navArgument("brokerId") { type = NavType.LongType },
                navArgument("dashboardId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val brokerId = backStackEntry.arguments?.getLong("brokerId") ?: return@composable
            val dashboardId = backStackEntry.arguments?.getLong("dashboardId") ?: return@composable
            DashboardScreen(
                brokerId = brokerId,
                dashboardId = dashboardId,
                onDisconnect = {
                    navController.popBackStack(Screen.Connect.route, inclusive = false)
                }
            )
        }
    }
}
