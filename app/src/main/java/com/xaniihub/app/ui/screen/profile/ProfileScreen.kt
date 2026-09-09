package com.xaniihub.app.ui.screen.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.xaniihub.app.domain.model.BodyParams
import com.xaniihub.app.domain.model.GenderType
import com.xaniihub.app.ui.components.MiniTrendLine
import com.xaniihub.app.ui.components.PremiumCard
import com.xaniihub.app.localization.AppLanguage
import com.xaniihub.app.localization.AppLanguageController
import com.xaniihub.app.localization.appLocale
import com.xaniihub.app.localization.appString
import com.xaniihub.app.localization.appText
import com.xaniihub.app.ui.theme.AppThemeController
import com.xaniihub.app.ui.theme.AppThemePalette
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    paddingValues: PaddingValues,
    state: ProfileUiState,
    onSaveParams: (BodyParams) -> Unit,
    onWeightSave: (Float) -> Unit
) {
    var weight by remember(state.bodyParams.weightKg) { mutableStateOf(formatEditableNumber(state.bodyParams.weightKg)) }
    var height by remember(state.bodyParams.heightCm) { mutableStateOf(state.bodyParams.heightCm.toString()) }
    var age by remember(state.bodyParams.age) { mutableStateOf(state.bodyParams.age.toString()) }
    var activity by remember(state.bodyParams.activityMultiplier) { mutableStateOf(formatEditableNumber(state.bodyParams.activityMultiplier)) }
    var targetWeight by remember(state.bodyParams.targetWeightKg) { mutableStateOf(formatEditableNumber(state.bodyParams.targetWeightKg)) }
    var gender by remember(state.bodyParams.gender) { mutableStateOf(state.bodyParams.gender) }
    var newWeight by remember { mutableStateOf("") }
    var paramsSaveAttempted by remember { mutableStateOf(false) }
    var newWeightError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(appText("profile"), style = MaterialTheme.typography.headlineLarge)
        Text(
            appText("profile_subtitle"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        MyDayStepsCard(state = state, modifier = Modifier.fillMaxWidth())
        DynamicsStepsCard(state = state, modifier = Modifier.fillMaxWidth())

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(appText("body_parameters"), style = MaterialTheme.typography.titleLarge)
                val weightInvalid = weight.toFloatOrNull()?.let { it !in 25f..350f } ?: true
                val heightInvalid = height.toIntOrNull()?.let { it !in 100..250 } ?: true
                val ageInvalid = age.toIntOrNull()?.let { it !in 13..120 } ?: true
                val activityInvalid = activity.toFloatOrNull()?.let { it !in 1.2f..1.9f } ?: true
                val targetWeightInvalid = targetWeight.toFloatOrNull()?.let { it !in 25f..350f } ?: true
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = sanitizeDecimal(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appText("weight_kg")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = paramsSaveAttempted && weightInvalid,
                    supportingText = if (paramsSaveAttempted && weightInvalid) { { Text(appText("input_error")) } } else null
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appText("height_cm")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = paramsSaveAttempted && heightInvalid,
                    supportingText = if (paramsSaveAttempted && heightInvalid) { { Text(appText("input_error")) } } else null
                )
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appText("age")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = paramsSaveAttempted && ageInvalid,
                    supportingText = if (paramsSaveAttempted && ageInvalid) { { Text(appText("input_error")) } } else null
                )
                OutlinedTextField(
                    value = activity,
                    onValueChange = { activity = sanitizeDecimal(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appText("activity_coeff")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = paramsSaveAttempted && activityInvalid,
                    supportingText = if (paramsSaveAttempted && activityInvalid) { { Text(appText("input_error")) } } else null
                )
                OutlinedTextField(
                    value = targetWeight,
                    onValueChange = { targetWeight = sanitizeDecimal(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appText("target_weight")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = paramsSaveAttempted && targetWeightInvalid,
                    supportingText = if (paramsSaveAttempted && targetWeightInvalid) { { Text(appText("input_error")) } } else null
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GenderChip(
                        title = appText("male"),
                        selected = gender == GenderType.MALE,
                        onClick = { gender = GenderType.MALE },
                        modifier = Modifier.weight(1f)
                    )
                    GenderChip(
                        title = appText("female"),
                        selected = gender == GenderType.FEMALE,
                        onClick = { gender = GenderType.FEMALE },
                        modifier = Modifier.weight(1f)
                    )
                    GenderChip(
                        title = appText("other"),
                        selected = gender == GenderType.OTHER,
                        onClick = { gender = GenderType.OTHER },
                        modifier = Modifier.weight(1f)
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        paramsSaveAttempted = true
                        if (!weightInvalid && !heightInvalid && !ageInvalid && !activityInvalid && !targetWeightInvalid) {
                            onSaveParams(
                                BodyParams(
                                    weightKg = weight.toFloat(),
                                    heightCm = height.toInt(),
                                    age = age.toInt(),
                                    gender = gender,
                                    activityMultiplier = activity.toFloat(),
                                    targetWeightKg = targetWeight.toFloat()
                                )
                            )
                            paramsSaveAttempted = false
                        }
                    }
                ) {
                    Text(appText("save_params"))
                }
            }
        }

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(appText("metrics"), style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCell(
                        modifier = Modifier.weight(1f),
                        title = "BMI",
                        value = if (state.bmi <= 0f) "—" else "%.1f".format(state.bmi),
                        subtitle = bmiLabel(state.bmi)
                    )
                    StatCell(
                        modifier = Modifier.weight(1f),
                        title = "BMR",
                        value = "${state.bmr}",
                        subtitle = appText("kcal_per_day")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCell(
                        modifier = Modifier.weight(1f),
                        title = "TDEE",
                        value = "${state.tdee}",
                        subtitle = appText("kcal_per_day")
                    )
                    StatCell(
                        modifier = Modifier.weight(1f),
                        title = appText("burned"),
                        value = "${state.burnedToday}",
                        subtitle = appText("burned_today")
                    )
                }
            }
        }

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(appText("weight_dynamics"), style = MaterialTheme.typography.titleLarge)
                val language = AppLanguageController.language
                val weightFormatter = remember(language) { DateTimeFormatter.ofPattern("d MMM", appLocale(language)) }
                if (state.weightTrend.isEmpty()) {
                    Text(
                        text = appText("weight_trend_empty"),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    MiniTrendLine(
                        values = state.weightTrend.map { it.weightKg },
                        xLabels = state.weightTrend.map {
                            Instant.ofEpochMilli(it.timestamp)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(weightFormatter)
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = newWeight,
            onValueChange = {
                newWeight = sanitizeDecimal(it)
                newWeightError = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(appText("new_weight")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = newWeightError,
            supportingText = if (newWeightError) { { Text(appText("input_error")) } } else null
        )
        Button(
            onClick = {
                val parsed = newWeight.toFloatOrNull()?.takeIf { it in 25f..350f }
                newWeightError = parsed == null
                if (parsed != null) {
                    onWeightSave(parsed)
                    newWeight = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(appText("add_weight"))
        }

        ThemePaletteSelector(modifier = Modifier.fillMaxWidth())
        LanguageSelector(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }
}



@Composable
private fun LanguageSelector(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    PremiumCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(appText("language"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(appText("language_subtitle"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                LanguageChip(
                    title = "English",
                    selected = AppLanguageController.language == AppLanguage.EN,
                    onClick = { AppLanguageController.setLanguage(context, AppLanguage.EN) },
                    modifier = Modifier.weight(1f)
                )
                LanguageChip(
                    title = appText("russian"),
                    selected = AppLanguageController.language == AppLanguage.RU,
                    onClick = { AppLanguageController.setLanguage(context, AppLanguage.RU) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LanguageChip(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    ) { Text(title) }
}

@Composable
private fun ThemePaletteSelector(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    PremiumCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(appText("themes"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                appText("themes_subtitle"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppThemePalette.values().forEach { palette ->
                    val selected = AppThemeController.palette == palette
                    ThemePaletteRow(
                        title = palette.title,
                        palette = palette,
                        selected = selected,
                        onClick = { AppThemeController.setPalette(context, palette) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemePaletteRow(
    title: String,
    palette: AppThemePalette,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = when (palette) {
        AppThemePalette.MONO -> listOf(Color.White, Color(0xFF9E9E9E), Color(0xFF2A2A2A))
        AppThemePalette.CYAN -> listOf(Color(0xFF00D9FF), Color(0xFF3BE7FF), Color(0xFF083846))
        AppThemePalette.VIOLET -> listOf(Color(0xFFA78BFA), Color(0xFFE879F9), Color(0xFF33235E))
        AppThemePalette.EMERALD -> listOf(Color(0xFF5EF05D), Color(0xFFB8FF6A), Color(0xFF1D4C28))
        AppThemePalette.SUNSET -> listOf(Color(0xFFFF8A50), Color(0xFFFFC857), Color(0xFF5C2A1B))
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (selected) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}

@Composable
private fun MyDayStepsCard(
    state: ProfileUiState,
    modifier: Modifier = Modifier
) {
    val todaySteps = state.dashboardStats?.steps ?: 0
    val usualSteps = state.analyticsOverview?.averageSteps ?: state.stepHistory.takeLast(30).filter { it > 0 }.averageInt()
    val delta = todaySteps - usualSteps
    val progress = java.time.LocalTime.now().toSecondOfDay() / 86_400f
    val history = state.dashboardStats?.hourlySteps?.take(24)?.ifEmpty { null } ?: List(24) { 0 }

    PremiumCard(modifier = modifier) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(appText("my_day"), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MetricDropDownChip(appText("steps_metric"))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(appText("today_caps"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(formatInt(todaySteps), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Light)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(appText("usual_caps"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatInt(usualSteps), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Light)
                }
            }

            DayProgressChart(values = history, progress = progress, modifier = Modifier.fillMaxWidth())

            val message = when {
                usualSteps <= 0 -> appText("my_day_no_data")
                delta > 0 -> appText("my_day_more").format(formatInt(delta))
                delta == 0 -> appText("my_day_equal")
                else -> appText("my_day_less").format(formatInt(abs(delta)))
            }
            Text(
                message,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DynamicsStepsCard(
    state: ProfileUiState,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableIntStateOf(0) }
    val periods = listOf(appText("week"), appText("month"), appText("year"))
    val periodSize = when (selectedPeriod) {
        0 -> 7
        1 -> 30
        else -> 365
    }
    val currentValues = state.stepHistory.takeLast(periodSize).ifEmpty { listOf(0) }
    val previousValues = state.stepHistory.dropLast(periodSize).takeLast(periodSize)
    val currentAverage = currentValues.averageInt()
    val previousAverage = previousValues.averageInt()
    val delta = currentAverage - previousAverage
    val isPositive = delta >= 0

    PremiumCard(modifier = modifier) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(appText("dynamic"), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MetricDropDownChip(appText("steps_metric"))
            }

            SegmentedTabs(
                labels = periods,
                selected = selectedPeriod,
                onSelected = { selectedPeriod = it },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(formatInt(previousAverage), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(appText("previous_period"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatInt(currentAverage), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(appText("current_period"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    DeltaBadge(delta = delta, positive = isPositive)
                }
            }

            DynamicsLineChart(values = currentValues, modifier = Modifier.fillMaxWidth())

            val comparisonText = if (previousAverage == 0) {
                appText("dyn_no_compare").format(formatInt(currentAverage))
            } else if (isPositive) {
                appText("dyn_more").format(formatInt(currentAverage), formatInt(delta))
            } else {
                appText("dyn_less").format(formatInt(currentAverage), formatInt(abs(delta)))
            }
            Text(comparisonText, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

        }
    }
}

// Static metric label. There is currently only one metric (Steps), so this is a
// non-interactive badge — no dropdown chevron that would falsely imply tap-to-switch.
@Composable
private fun MetricDropDownChip(label: String) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SegmentedTabs(
    labels: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val active = selected == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelected(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DayProgressChart(
    values: List<Int>,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(170.dp)) {
            val baseline = size.height * 0.78f
            val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
            val points = values.mapIndexed { index, value ->
                val x = if (values.size <= 1) 0f else index.toFloat() / (values.lastIndex) * size.width
                val normalized = value.toFloat() / max
                Offset(x, baseline - normalized * size.height * 0.48f)
            }

            val lineStroke = 4.dp.toPx()
            drawLine(track, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = lineStroke, cap = StrokeCap.Round)
            drawLine(primary, Offset(0f, baseline), Offset(size.width * progress.coerceIn(0f, 1f), baseline), strokeWidth = lineStroke, cap = StrokeCap.Round)

            points.zipWithNext().forEach { (start, end) ->
                drawLine(muted, start, end, strokeWidth = lineStroke, cap = StrokeCap.Round)
            }

            val markerX = size.width * progress.coerceIn(0f, 1f)
            drawLine(muted, Offset(markerX, size.height * 0.22f), Offset(markerX, baseline + lineStroke), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(primary, radius = 9.dp.toPx(), center = Offset(markerX, baseline))
            drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(markerX, baseline))
            drawCircle(primary, radius = 3.dp.toPx(), center = Offset(markerX, baseline))
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Text("00:00", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterStart))
            Text("24:00", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterEnd))
            Text(
                appText("now"),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.offset(x = (maxWidth - 48.dp) * progress.coerceIn(0f, 1f))
            )
        }
    }
}

@Composable
private fun DeltaBadge(delta: Int, positive: Boolean) {
    val color = if (positive) Color(0xFF5EF05D) else Color(0xFFFFA726)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = if (positive) "+${formatInt(delta)}" else "-${formatInt(abs(delta))}",
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Text(if (positive) "↗" else "↘", color = Color.Black, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun DynamicsLineChart(
    values: List<Int>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = Color(0xFF5EF05D)
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val safeValues = values.ifEmpty { listOf(0, 0) }

    Canvas(
        modifier = modifier
            .height(126.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                    )
                )
            )
            .padding(8.dp)
    ) {
        val horizontalPadding = 18.dp.toPx()
        val topPadding = 18.dp.toPx()
        val bottomPadding = 22.dp.toPx()
        val chartWidth = size.width - horizontalPadding * 2f
        val chartHeight = size.height - topPadding - bottomPadding
        val max = safeValues.maxOrNull()?.coerceAtLeast(1) ?: 1
        val min = safeValues.minOrNull() ?: 0
        val range = (max - min).coerceAtLeast(1)

        repeat(3) { index ->
            val y = topPadding + chartHeight * index / 2f
            drawLine(grid, Offset(horizontalPadding, y), Offset(size.width - horizontalPadding, y), strokeWidth = 1.dp.toPx())
        }

        val points = safeValues.mapIndexed { index, value ->
            val x = if (safeValues.size == 1) horizontalPadding else horizontalPadding + (index.toFloat() / safeValues.lastIndex) * chartWidth
            val normalized = (value - min).toFloat() / range
            val y = topPadding + chartHeight - normalized * chartHeight
            Offset(x, y)
        }

        points.zipWithNext().forEach { (start, end) ->
            drawLine(primary.copy(alpha = 0.34f), Offset(start.x, size.height - bottomPadding), Offset(end.x, size.height - bottomPadding), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
            val fillOffset = 12.dp.toPx()
            drawLine(fill, Offset(start.x, start.y + fillOffset), Offset(end.x, end.y + fillOffset), strokeWidth = 9.dp.toPx(), cap = StrokeCap.Round)
            drawLine(primary, start, end, strokeWidth = 3.5.dp.toPx(), cap = StrokeCap.Round)
        }
        points.forEachIndexed { index, point ->
            val color = if (index == points.lastIndex) secondary else primary
            drawCircle(color.copy(alpha = 0.22f), radius = 7.5.dp.toPx(), center = point)
            drawCircle(color, radius = 3.dp.toPx(), center = point)
        }
    }
}

@Composable
private fun DynamicsMetric(
    change: String,
    title: String,
    value: String,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (positive) Color(0xFF5EF05D) else Color(0xFFFFA726)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("${if (positive) "↗" else "↘"} $change", color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DynamicsDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(96.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
    )
}

@Composable
private fun GenderChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(title)
    }
}

@Composable
private fun StatCell(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatEditableNumber(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else value.toString()

private fun sanitizeDecimal(value: String): String {
    val normalized = value.replace(',', '.')
    val separator = normalized.indexOf('.')
    return normalized.filterIndexed { index, char ->
        char.isDigit() || (char == '.' && index == separator)
    }
}

private fun List<Int>.averageInt(): Int =
    if (isEmpty()) 0 else (sum() / size.toFloat()).roundToInt()

private fun formatInt(value: Int): String =
    java.text.NumberFormat.getIntegerInstance(appLocale()).format(value)

private fun formatDistance(value: Float): String {
    val formatter = java.text.NumberFormat.getNumberInstance(appLocale()).apply {
        minimumFractionDigits = if (value < 10f) 1 else 0
        maximumFractionDigits = if (value < 10f) 1 else 0
    }
    return "${formatter.format(value)} ${appString("km")}" 
}

@Composable
private fun bmiLabel(bmi: Float): String = when {
    bmi <= 0f -> "—"
    bmi < 18.5f -> appText("underweight")
    bmi < 25f -> appText("normal")
    bmi < 30f -> appText("overweight")
    else -> appText("obesity")
}

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')} ${appString("hour_short")}" else "$m ${appString("min")}"
}

private fun estimateCalories(steps: Int, weightKg: Float): Int {
    return ((steps * 0.04f) * (weightKg / 70f)).roundToInt().coerceAtLeast(0)
}
