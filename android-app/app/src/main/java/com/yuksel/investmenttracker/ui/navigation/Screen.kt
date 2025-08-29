package com.yuksel.investmenttracker.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Assets : Screen("assets")
    object History : Screen("history")
    object Settings : Screen("settings")
    object AddAcquisition : Screen("add_acquisition")
}