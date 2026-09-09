package com.xaniihub.app.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaniihub.app.domain.model.AnalyticsOverview

import com.xaniihub.app.domain.model.BodyParams
import com.xaniihub.app.domain.model.DashboardStats
import com.xaniihub.app.domain.model.WeightPoint
import com.xaniihub.app.domain.repository.XaniiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        combine(
            repository.observeBodyParams(),
            repository.observeWeightTrend(),
            repository.observeDashboardStats(),
            repository.observeAnalyticsOverview(),
            repository.observeDailyHeatMap()
        ) { params, trend, dashboard, analytics, heatMap ->
            val bmi = calculateBmi(params.weightKg, params.heightCm)
            ProfileUiState(
                bodyParams = params,
                weightTrend = trend,
                dashboardStats = dashboard,
                analyticsOverview = analytics,
                stepHistory = heatMap,
                bmi = bmi,
                bmr = calculateBmr(params),
                tdee = calculateTdee(params),
                burnedToday = dashboard.calories.toInt()
            )
        }.onEach {
            _uiState.value = it
        }.launchIn(viewModelScope)
    }

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
            com.xaniihub.app.domain.model.GenderType.MALE -> 5f
            com.xaniihub.app.domain.model.GenderType.FEMALE -> -161f
            com.xaniihub.app.domain.model.GenderType.OTHER -> -78f
        }
        return (base + sexOffset).toInt().coerceAtLeast(900)
    }

    private fun calculateTdee(params: BodyParams): Int {
        return (calculateBmr(params) * params.activityMultiplier).toInt().coerceAtLeast(1100)
    }
}
