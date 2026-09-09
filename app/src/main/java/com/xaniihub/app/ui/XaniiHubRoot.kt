package com.xaniihub.app.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xaniihub.app.localization.AppLanguageController
import com.xaniihub.app.localization.appLocale
import com.xaniihub.app.ui.components.AuroraBackground
import com.xaniihub.app.ui.navigation.NavDestination
import com.xaniihub.app.ui.screen.analytics.AnalyticsScreen
import com.xaniihub.app.ui.screen.analytics.AnalyticsViewModel
import com.xaniihub.app.ui.screen.home.HomeScreen
import com.xaniihub.app.ui.screen.home.HomeViewModel
import com.xaniihub.app.ui.screen.profile.ProfileScreen
import com.xaniihub.app.ui.screen.profile.ProfileViewModel

@Composable
fun XaniiHubRoot() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination

    AuroraBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                PremiumBottomBar(
                    selectedRoute = current?.route,
                    navController = navController
                )
            }
        ) { inner ->
            NavHost(
                navController = navController,
                startDestination = NavDestination.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(NavDestination.Home.route) {
                    val vm: HomeViewModel = hiltViewModel()
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    HomeScreen(
                        paddingValues = inner,
                        state = state,
                        onGoalChange = vm::setGoal,
                        onDateSelected = vm::selectDate
                    )
                }
                composable(NavDestination.Analytics.route) {
                    val vm: AnalyticsViewModel = hiltViewModel()
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    AnalyticsScreen(
                        paddingValues = inner,
                        state = state
                    )
                }
                composable(NavDestination.Profile.route) {
                    val vm: ProfileViewModel = hiltViewModel()
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    ProfileScreen(
                        paddingValues = inner,
                        state = state,
                        onSaveParams = vm::saveParams,
                        onWeightSave = vm::saveWeight
                    )
                }
            }
        }
    }
}

/**
 * Floating frosted-glass navigation pill. The selected destination expands into a
 * gradient capsule that reveals its label; the rest stay as quiet icons.
 */
@Composable
private fun PremiumBottomBar(
    selectedRoute: String?,
    navController: NavHostController
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .shadow(
                elevation = 26.dp,
                shape = CircleShape,
                ambientColor = scheme.primary.copy(alpha = 0.45f),
                spotColor = scheme.primary.copy(alpha = 0.55f)
            )
            .clip(CircleShape)
            .background(scheme.surface.copy(alpha = 0.82f))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.10f),
                        scheme.primary.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                )
            )
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.35f), scheme.primary.copy(alpha = 0.18f), Color.Transparent)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
            .height(68.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavDestination.items.forEach { item ->
            val selected = selectedRoute == item.route
            val weight by animateFloatAsState(
                targetValue = if (selected) 2.3f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "nav_weight"
            )
            BottomNavButton(
                item = item,
                selected = selected,
                modifier = Modifier.weight(weight),
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
private fun BottomNavButton(
    item: NavDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val language = AppLanguageController.language
    val label = remember(context, language, item.labelRes) {
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(appLocale(language))
        }
        context.createConfigurationContext(configuration).getString(item.labelRes)
    }
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "nav_icon_scale"
    )
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .height(50.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier
                        .background(Brush.horizontalGradient(listOf(scheme.primary, scheme.secondary)))
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)))
                } else Modifier
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(24.dp).scale(iconScale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = label,
                tint = if (selected) scheme.onPrimary else scheme.onSurface.copy(alpha = 0.62f),
                modifier = Modifier.size(22.dp)
            )
        }
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
