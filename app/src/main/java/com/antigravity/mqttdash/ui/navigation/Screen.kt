package com.antigravity.mqttdash.ui.navigation

sealed class Screen(val route: String) {
    /** Connection / Quick Connect screen shown when not connected */
    data object Connect : Screen("connect")

    /** Main dashboard screen shown after successful connection */
    data object Dashboard : Screen("dashboard/{brokerId}/{dashboardId}") {
        fun createRoute(brokerId: Long, dashboardId: Long) =
            "dashboard/$brokerId/$dashboardId"
    }
}
