package com.autowallpaper.changer.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Schedule : Screen("schedule")
    data object Library : Screen("library")
    data object Settings : Screen("settings")
}
