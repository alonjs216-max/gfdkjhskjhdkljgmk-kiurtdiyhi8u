package com.xaniihub.app.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaniihub.app.domain.model.AnalyticsOverview

import com.xaniihub.app.domain.model.BodyParams
import com.xaniihub.app.domain.model.DashboardStats
import com.xaniihub.app.domain.model.GenderType
import com.xaniihub.app.domain.model.WeightPoint
import com.xaniihub.app.domain.repository.XaniiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val bodyParams: BodyParams = BodyParams(),
    val weightTrend: List<WeightPoint> = emptyList(),
    val dashboardStats: DashboardStats? = null,
    val analyticsOverview: AnalyticsOverview? = null,
    val stepHistory: List<Int> = emptyList(),
    val bmi: Float = 0f,
    val bmr: Int = 0,
    val tdee: Int = 0,
    val burnedToday: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: XaniiRepository
) : ViewModel() {

    /** BMI, BMR and TDEE depend on the body parameters only. */
    private data class BodyMetrics(
        val params: BodyParams,
        val bmi: Float,
        val bmr: Int,
        val tdee: Int
    )

    // The dashboard flow emits on every sensor update, and the old single combine recomputed all
    // three formulas each time. Deriving them from the body parameters alone means they are
    // recalculated only when the user actually edits weight, height, age, sex or activity.
    private val bodyMetrics: Flow<BodyMetrics> = repository.observeBodyParams()
        .distinctUntilChanged()
        .map { params ->
            BodyMetrics(
                params = params,
                bmi = calculateBmi(params.weightKg, params.heightCm),
                bmr = calculateBmr(params),
                tdee = calculateTdee(params)
            )
        }

    val uiState: StateFlow<ProfileUiState> = combine(
        bodyMetrics,
        repository.observeWeightTrend(),
        repository.observeDashboardStats(),
        repository.observeAnalyticsOverview(),
        repository.observeDailyHeatMap()
    ) { metrics, trend, dashboard, analytics, heatMap ->
        ProfileUiState(
            bodyParams = metrics.params,
            weightTrend = trend,
            dashboardStats = dashboard,
            analyticsOverview = analytics,
            stepHistory = heatMap,
            bmi = metrics.bmi,
            bmr = metrics.bmr,
            tdee = metrics.tdee,
            burnedToday = dashboard.calories.toInt()
        )
    }.stateIn(
        scope = viewModelScope,
        // Keeps the state alive across configuration changes, but stops the upstream flows when
        // the profile screen is no longer being observed.
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState()
    )

    fun saveParams(params: BodyParams) {
        viewModelScope.launch {
            repository.saveBodyParams(params)
        }
    }

    fun saveWeight(weight: Float) {
        viewModelScope.launch {
            repository.saveWeight(weight)
        }
    }

    private fun calculateBmi(weightKg: Float, heightCm: Int): Float {
        if (weightKg <= 0f || heightCm <= 0) return 0f
        val heightM = heightCm / 100f
        return weightKg / (heightM * heightM)
    }


    private fun calculateBmr(params: BodyParams): Int {
        val base = (10f * params.weightKg) + (6.25f * params.heightCm) - (5f * params.age)
        val sexOffset = when (params.gender) {
            GenderType.MALE -> 5f
            GenderType.FEMALE -> -161f
            GenderType.OTHER -> -78f
        }
        return (base + sexOffset).toInt().coerceAtLeast(900)
    }

    private fun calculateTdee(params: BodyParams): Int {
        return (calculateBmr(params) * params.activityMultiplier).toInt().coerceAtLeast(1100)
    }
}
