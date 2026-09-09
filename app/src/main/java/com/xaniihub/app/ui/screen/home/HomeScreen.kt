package com.xaniihub.app.ui.screen.home

import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xaniihub.app.localization.AppLanguageController
import com.xaniihub.app.localization.appLocale
import com.xaniihub.app.localization.appString
import com.xaniihub.app.localization.appText
import com.xaniihub.app.ui.components.Eyebrow
import com.xaniihub.app.ui.components.GlassIconButton
import com.xaniihub.app.ui.components.GoalRing
import com.xaniihub.app.ui.components.GradientButton
import com.xaniihub.app.ui.components.MetricCard
import com.xaniihub.app.ui.components.PremiumCard
import com.xaniihub.app.ui.components.SectionHeader
import com.xaniihub.app.ui.components.TinyBarChart
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    state: HomeUiState,
    onGoalChange: (String) -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val stats = state.stats
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var showGoalDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var goalError by remember { mutableStateOf(false) }
    var goalDraft by remember(stats?.dailyGoal) { mutableStateOf(stats?.dailyGoal?.toString().orEmpty()) }
    val language = AppLanguageController.language
    val formatter = remember(language) { DateTimeFormatter.ofPattern("d MMMM yyyy", appLocale(language)) }
    val weekdayFormatter = remember(language) { DateTimeFormatter.ofPattern("EEEE", appLocale(language)) }
    val dateText = remember(state.selectedDate, language) { state.selectedDate.format(formatter) }
    val weekdayText = remember(state.selectedDate, language) {
        state.selectedDate.format(weekdayFormatter).replaceFirstChar { it.uppercase() }
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() <= LocalDate.now()
            }
        }
    )
    LaunchedEffect(state.selectedDate) {
        datePickerState.selectedDateMillis = state.selectedDate
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    // Only show real per-hour data. When the sensor hasn't produced hourly events
    // we render an empty chart with a placeholder instead of fabricating a distribution.
    val hourly = remember(stats?.hourlySteps, state.selectedDate) {
        stats?.hourlySteps.orEmpty().takeIf { it.any { v -> v > 0 } }.orEmpty()
    }
    val hasHourly = hourly.any { it > 0 }

    val springSpec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    val springSpecInt = spring<Int>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)

    val targetSteps = stats?.steps ?: 0
    val animatedSteps by animateIntAsState(targetSteps, springSpecInt, label = "steps_countup")
    val animatedDistance by animateFloatAsState(stats?.distanceKm ?: 0f, springSpec, label = "distance_anim")
    val animatedCalories by animateFloatAsState(stats?.calories ?: 0f, springSpec, label = "calories_anim")
    val animatedMinutes by animateIntAsState(stats?.activeMinutes ?: 0, springSpecInt, label = "minutes_anim")
    val progress = stats?.progress ?: 0f
    val goalReached = progress >= 1f
    val percent = (progress * 100f).roundToInt()

    val haptics = LocalHapticFeedback.current
    val feedbackPrefs = remember(context) {
        context.getSharedPreferences("ringwalk_feedback", Context.MODE_PRIVATE)
    }
    LaunchedEffect(goalReached, state.isToday, state.selectedDate) {
        val dateKey = state.selectedDate.toString()
        if (goalReached && state.isToday && feedbackPrefs.getString("goal_haptic_date", null) != dateKey) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            feedbackPrefs.edit().putString("goal_haptic_date", dateKey).apply()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            // ---- Header -------------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Eyebrow(weekdayText, color = scheme.primary)
                    Text(
                        if (state.isToday) appText("today") else appText("selected_day"),
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(dateText, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
                GlassIconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = appText("calendar"), tint = scheme.onSurface)
                }
            }

            // ---- Hero ring ----------------------------------------------------------------
            PremiumCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 36.dp) {
                Column(
                    modifier = Modifier.padding(top = 14.dp, bottom = 18.dp, start = 14.dp, end = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIconButton(size = 40.dp, onClick = { onDateSelected(state.selectedDate.minusDays(1)) }) {
                            Icon(Icons.Outlined.ChevronLeft, contentDescription = appText("previous_day"), tint = scheme.onSurface)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Eyebrow(if (state.isToday) appText("activity_today") else appText("activity_archive"))
                            Text(
                                dateText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                        GlassIconButton(
                            size = 40.dp,
                            onClick = { onDateSelected(state.selectedDate.plusDays(1).coerceAtMost(LocalDate.now())) }
                        ) {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = appText("next_day"), tint = scheme.onSurface)
                        }
                    }
                    GoalRing(
                        // Uncapped progress so each completed goal can start a new themed lap.
                        progress = progress,
                        centerText = formatInt(animatedSteps),
                        subtitle = appText("of_steps").format(formatInt(stats?.dailyGoal ?: 8000)),
                        eyebrow = "$percent%",
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    // Streak + goal chips under the ring.
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoChip(
                            icon = Icons.Outlined.Whatshot,
                            text = appText("streak_short").format(stats?.streakDays ?: 0),
                            accent = Color(0xFFFF8A50)
                        )
                        InfoChip(
                            icon = Icons.Outlined.Flag,
                            text = appText("widget_goal").format(formatInt(stats?.dailyGoal ?: 8000)),
                            accent = scheme.primary,
                            onClick = { showGoalDialog = true }
                        )
                    }
                }
            }

            // ---- Metrics ------------------------------------------------------------------
            val kcalLabel = appText("kcal")
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compactMetrics = maxWidth < 420.dp
                if (compactMetrics) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricCard(
                                title = appText("distance"),
                                value = formatDistance(animatedDistance),
                                icon = Icons.Outlined.Straighten,
                                accent = Color(0xFF00D9FF),
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = appText("calories"),
                                value = "${animatedCalories.roundToInt()} $kcalLabel",
                                icon = Icons.Outlined.LocalFireDepartment,
                                accent = Color(0xFFFFB74D),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        MetricCard(
                            title = appText("time"),
                            value = formatDuration(animatedMinutes),
                            icon = Icons.Outlined.Timer,
                            accent = Color(0xFF75F06D),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricCard(
                            title = appText("distance"),
                            value = formatDistance(animatedDistance),
                            icon = Icons.Outlined.Straighten,
                            accent = Color(0xFF00D9FF),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = appText("calories"),
                            value = "${animatedCalories.roundToInt()} $kcalLabel",
                            icon = Icons.Outlined.LocalFireDepartment,
                            accent = Color(0xFFFFB74D),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = appText("time"),
                            value = formatDuration(animatedMinutes),
                            icon = Icons.Outlined.Timer,
                            accent = Color(0xFF75F06D),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ---- Hourly activity ----------------------------------------------------------
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionHeader(
                        title = appText("hourly_activity"),
                        icon = Icons.Outlined.BarChart,
                        trailing = {
                            Text(
                                appText("hourly_peak").format(formatInt(hourly.maxOrNull() ?: 0)),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    )
                    if (hasHourly) {
                        TinyBarChart(
                            values = hourly,
                            highlightIndex = if (state.isToday) LocalTime.now().hour.coerceAtMost(hourly.lastIndex) else null
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Eyebrow("00", modifier = Modifier.align(Alignment.CenterStart))
                            Eyebrow("12", modifier = Modifier.align(Alignment.Center))
                            Eyebrow("24", modifier = Modifier.align(Alignment.CenterEnd))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(scheme.onSurface.copy(alpha = 0.04f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                appText("no_hourly_data"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }

            // ---- Lifetime -----------------------------------------------------------------
            PremiumCard(modifier = Modifier.fillMaxWidth(), accent = scheme.tertiary) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Eyebrow(appText("lifetime_steps"))
                        Text(
                            formatInt(stats?.lifetimeSteps ?: 0L),
                            style = MaterialTheme.typography.displaySmall,
                            color = scheme.onSurface
                        )
                        Text(appText("achievements"), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(scheme.tertiary.copy(alpha = 0.35f), scheme.primary.copy(alpha = 0.12f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = scheme.tertiary, modifier = Modifier.size(30.dp))
                    }
                }
            }

            // ---- Daily goal ---------------------------------------------------------------
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Eyebrow(appText("daily_goal"))
                        Text(
                            appText("steps_value").format(formatInt(stats?.dailyGoal ?: 8000)),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    GradientButton(text = appText("change"), onClick = { showGoalDialog = true })
                }
            }
            Spacer(Modifier.height(96.dp))
        }
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.background.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = scheme.primary, trackColor = scheme.primary.copy(alpha = 0.15f))
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onDateSelected(picked.coerceAtMost(LocalDate.now()))
                        }
                        showDatePicker = false
                    }
                ) { Text(appText("select")) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(appText("cancel")) } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = goalDraft.toIntOrNull()
                        goalError = parsed == null || parsed !in 1_000..100_000
                        if (!goalError) {
                            onGoalChange(goalDraft)
                            showGoalDialog = false
                        }
                    }
                ) { Text(appText("save")) }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text(appText("cancel")) }
            },
            title = { Text(appText("daily_goal")) },
            text = {
                OutlinedTextField(
                    value = goalDraft,
                    onValueChange = {
                        goalDraft = it.filter(Char::isDigit)
                        goalError = false
                    },
                    label = { Text(appText("steps_per_day")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = goalError,
                    supportingText = if (goalError) {
                        { Text(appText("input_error")) }
                    } else null
                )
            }
        )
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    accent: Color,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.12f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

private fun LocalDate.coerceAtMost(max: LocalDate): LocalDate = if (isAfter(max)) max else this
private fun formatInt(value: Int): String =
    java.text.NumberFormat.getIntegerInstance(appLocale()).format(value)
private fun formatInt(value: Long): String =
    java.text.NumberFormat.getIntegerInstance(appLocale()).format(value)
private fun formatDistance(value: Float): String {
    val formatter = java.text.NumberFormat.getNumberInstance(appLocale()).apply {
        minimumFractionDigits = if (value < 10f) 1 else 0
        maximumFractionDigits = if (value < 10f) 1 else 0
    }
    return "${formatter.format(value)} ${appString("km")}"
}
private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')} ${appString("hour_short")}" else "$m ${appString("min")}"
}
