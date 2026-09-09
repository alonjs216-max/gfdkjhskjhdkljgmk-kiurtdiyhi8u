package com.xaniihub.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.xaniihub.app.R

sealed class NavDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    data object Home : NavDestination("home", R.string.nav_home, Icons.Outlined.Home)
    data object Analytics : NavDestination("analytics", R.string.nav_analytics, Icons.Outlined.Analytics)
    data object Profile : NavDestination("profile", R.string.nav_profile, Icons.Outlined.Person)

    companion object {
        val items = listOf(Home, Analytics, Profile)
    }
}
