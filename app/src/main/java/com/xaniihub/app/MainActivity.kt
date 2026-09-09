package com.xaniihub.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.xaniihub.app.tracking.InactivityWorker
import com.xaniihub.app.tracking.StepTrackingService
import com.xaniihub.app.ui.XaniiHubRoot
import com.xaniihub.app.ui.theme.AppThemeController
import com.xaniihub.app.ui.theme.XaniiHubTheme
import com.xaniihub.app.localization.AppLanguage
import com.xaniihub.app.localization.AppLanguageController
import com.xaniihub.app.localization.appText
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import com.xaniihub.app.domain.goals.GoalTypesController
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedTextField
import androidx.hilt.navigation.compose.hiltViewModel
import com.xaniihub.app.domain.model.BodyParams
import com.xaniihub.app.domain.model.GenderType
import com.xaniihub.app.ui.onboarding.OnboardingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The preference reads are synchronous; initializing before composition prevents a
        // first-frame flash with the default language and palette.
        AppLanguageController.init(this)
        AppThemeController.init(this)
        enableEdgeToEdge()
        setContent {
            XaniiHubTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    LanguageGate {
                        PermissionGate(
                            onReady = {
                                StepTrackingService.start(this)
                                InactivityWorker.schedule(this)
                            }
                        ) {
                            XaniiHubRoot()
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun LanguageGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    when {
        !AppLanguageController.configured -> {
            LanguageSetupScreen(
                onSelect = { language -> AppLanguageController.setLanguage(context, language) }
            )
        }
        !AppLanguageController.creatorInfoSeen -> {
            CreatorInfoScreen(
                onContinue = { AppLanguageController.markCreatorInfoSeen(context) }
            )
        }
        !AppLanguageController.onboarded -> {
            OnboardingScreen(
                onFinish = { AppLanguageController.markOnboarded(context) }
            )
        }
        else -> content()
    }
}


@Composable
private fun CreatorInfoScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(
                Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("RingWalk", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(appText("creator_title"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(appText("creator_name"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Text(appText("creator_age"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = appText("creator_story"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            onClick = onContinue
        ) { Text(appText("start_app")) }
    }
}

@Composable
private fun LanguageSetupScreen(onSelect: (AppLanguage) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(
                Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("RingWalk", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
        Text(
            text = appText("choose_language"),
            modifier = Modifier.padding(top = 24.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = appText("choose_language_subtitle"),
            modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LanguageButton(title = "English", onClick = { onSelect(AppLanguage.EN) })
        LanguageButton(title = "Русский", onClick = { onSelect(AppLanguage.RU) })
    }
}

@Composable
private fun LanguageButton(title: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f))
    ) { Text(title, color = MaterialTheme.colorScheme.onSurface) }
}

@Composable
private fun PermissionGate(
    onReady: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("permission_gate", android.content.Context.MODE_PRIVATE)
    }
    var activityPermissionRequested by remember {
        mutableStateOf(preferences.getBoolean("activity_permission_requested", false))
    }
    val requestedPermissions = remember {
        buildList {
            add(Manifest.permission.ACTIVITY_RECOGNITION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Notifications improve the experience but are not required to count steps.
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }
    var activityPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val hasStepCounterSensor = remember(context) {
        (context.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager)
            ?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                activityPermissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val activity = context as? MainActivity
    val shouldShowActivityRationale = activity?.shouldShowRequestPermissionRationale(
        Manifest.permission.ACTIVITY_RECOGNITION
    ) ?: false
    val needsSettings = !activityPermissionGranted &&
        activityPermissionRequested &&
        !shouldShowActivityRationale

    lateinit var requestPermissions: () -> Unit
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            activityPermissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        }
    )
    requestPermissions = {
        activityPermissionRequested = true
        preferences.edit().putBoolean("activity_permission_requested", true).apply()
        launcher.launch(requestedPermissions)
    }

    LaunchedEffect(Unit) {
        if (!activityPermissionGranted && !activityPermissionRequested) {
            requestPermissions()
        }
    }

    LaunchedEffect(activityPermissionGranted, hasStepCounterSensor) {
        if (activityPermissionGranted && hasStepCounterSensor) {
            onReady()
        }
    }

    if (activityPermissionGranted && hasStepCounterSensor) {
        content()
    } else if (activityPermissionGranted) {
        SensorUnavailableScreen()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = appText(if (needsSettings) "permission_settings" else "permission"),
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                modifier = Modifier.padding(top = 16.dp),
                onClick = {
                    if (needsSettings) {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                    } else {
                        requestPermissions()
                    }
                }
            ) {
                Text(appText(if (needsSettings) "open_settings" else "allow"))
            }
        }
    }
}

@Composable
private fun SensorUnavailableScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "RingWalk",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black
        )
        Text(
            text = appText("step_sensor_unavailable"),
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val viewModel: OnboardingViewModel = hiltViewModel()
    var step by remember { mutableStateOf(0) }
    val lastStep = 5

    var gender by remember { mutableStateOf(GenderType.OTHER) }
    var age by remember { mutableStateOf("27") }
    var height by remember { mutableStateOf("175") }
    var weight by remember { mutableStateOf("70") }
    var activity by remember { mutableStateOf(1.2f) }
    var stepGoal by remember { mutableStateOf(8000f) }
    val finishOnboarding: () -> Unit = {
        val goal = stepGoal.toInt()
        viewModel.complete(
            params = BodyParams(
                weightKg = weight.toFloatOrNull() ?: 70f,
                heightCm = height.toIntOrNull() ?: 175,
                age = age.toIntOrNull() ?: 27,
                gender = gender,
                activityMultiplier = activity,
                targetWeightKg = weight.toFloatOrNull() ?: 70f
            ),
            dailyGoal = goal,
            onComplete = {
                GoalTypesController.setStepGoal(context, goal)
                onFinish()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        val title = when (step) {
            0 -> appText("onb_welcome_title")
            1 -> appText("onb_track_title")
            2 -> appText("onb_profile_title")
            3 -> appText("onb_activity_title")
            4 -> appText("onb_goal_title")
            else -> appText("onb_ready_title")
        }
        val text = when (step) {
            0 -> appText("onb_welcome_text")
            1 -> appText("onb_track_text")
            2 -> appText("onb_profile_text")
            3 -> appText("onb_activity_text")
            4 -> appText("onb_goal_text")
            else -> appText("onb_ready_text")
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        when (step) {
            2 -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    appText("onb_gender"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OnbChip(appText("onb_gender_male"), gender == GenderType.MALE, Modifier.weight(1f)) { gender = GenderType.MALE }
                    OnbChip(appText("onb_gender_female"), gender == GenderType.FEMALE, Modifier.weight(1f)) { gender = GenderType.FEMALE }
                    OnbChip(appText("onb_gender_other"), gender == GenderType.OTHER, Modifier.weight(1f)) { gender = GenderType.OTHER }
                }
                OnbNumberField(appText("onb_age"), age, appText("unit_years")) { age = it }
                OnbNumberField(appText("onb_height"), height, appText("unit_cm")) { height = it }
                OnbNumberField(appText("onb_weight"), weight, appText("unit_kg")) { weight = it }
            }
            3 -> {
                Spacer(Modifier.height(8.dp))
                val options = listOf(
                    1.2f to appText("act_sedentary"),
                    1.375f to appText("act_light"),
                    1.55f to appText("act_moderate"),
                    1.725f to appText("act_active"),
                    1.9f to appText("act_very")
                )
                options.forEach { (value, label) ->
                    OnbActivityOption(label, value, activity == value) { activity = value }
                }
            }
            4 -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stepGoal.toInt().toString() + " " + appText("unit_steps"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = stepGoal,
                    onValueChange = { stepGoal = it },
                    valueRange = 3000f..20000f,
                    steps = 16,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (step < lastStep) step += 1 else finishOnboarding()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (step < lastStep) appText("onb_next") else appText("onb_start"))
        }

        if (step < lastStep) {
            TextButton(onClick = finishOnboarding) {
                Text(appText("onb_skip"))
            }
        }

        if (step in 1..lastStep) {
            TextButton(onClick = { step -= 1 }) {
                Text(appText("onb_back"))
            }
        }
    }
}

@Composable
private fun OnbChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun OnbNumberField(
    label: String,
    value: String,
    unit: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        trailingIcon = { Text(unit) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun OnbActivityOption(
    label: String,
    value: Float,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                "×" + value,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}