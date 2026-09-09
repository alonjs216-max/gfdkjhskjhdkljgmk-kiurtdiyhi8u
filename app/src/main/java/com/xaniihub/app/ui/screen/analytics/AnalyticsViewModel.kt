package com.xaniihub.app.ui.screen.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaniihub.app.domain.model.AnalyticsOverview
import com.xaniihub.app.domain.repository.XaniiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class AnalyticsUiState(
    val overview: AnalyticsOverview? = null,
    val heatMap: List<Int> = emptyList(),
    val hourlySteps: List<Int> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    repository: XaniiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    private var overviewLoaded = false
    private var heatMapLoaded = false
    private var dashboardLoaded = false

    init {
        repository.observeAnalyticsOverview()
            .onEach { overview ->
                overviewLoaded = true
                _uiState.value = _uiState.value.copy(
                    overview = overview,
                    isLoading = !allDataLoaded()
                )
            }
            .launchIn(viewModelScope)

        repository.observeDailyHeatMap()
            .onEach { map ->
                heatMapLoaded = true
                _uiState.value = _uiState.value.copy(
                    heatMap = map,
                    isLoading = !allDataLoaded()
                )
            }
            .launchIn(viewModelScope)

        repository.observeDashboardStats()
            .onEach { dashboard ->
                dashboardLoaded = true
                _uiState.value = _uiState.value.copy(
                    hourlySteps = dashboard.hourlySteps.take(24),
                    isLoading = !allDataLoaded()
                )
            }
            .launchIn(viewModelScope)
    }

    private fun allDataLoaded(): Boolean = overviewLoaded && heatMapLoaded && dashboardLoaded
}