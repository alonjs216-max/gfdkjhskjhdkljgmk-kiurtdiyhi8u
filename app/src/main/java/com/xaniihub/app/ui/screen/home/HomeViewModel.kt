package com.xaniihub.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaniihub.app.domain.model.DashboardStats
import com.xaniihub.app.domain.repository.XaniiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class HomeUiState(
    val stats: DashboardStats? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true
) {
    val isToday: Boolean = selectedDate == LocalDate.now()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: XaniiRepository
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private var followsCurrentDay = true
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeCurrentDate()
            .onEach { today ->
                if (followsCurrentDay) selectedDate.value = today
            }
            .launchIn(viewModelScope)

        selectedDate
            .flatMapLatest { date -> repository.observeDashboardStatsForDate(date) }
            .onEach { stats ->
                _uiState.value = _uiState.value.copy(
                    stats = stats,
                    selectedDate = stats.date,
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)
    }

    fun selectDate(date: LocalDate) {
        followsCurrentDay = date == LocalDate.now()
        selectedDate.value = date
    }

    private fun observeCurrentDate() = flow {
        while (true) {
            val today = LocalDate.now()
            emit(today)
            val nextMidnight = today.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            delay((nextMidnight - System.currentTimeMillis()).coerceAtLeast(1_000L))
        }
    }.distinctUntilChanged()

    fun setGoal(value: String) {
        val parsed = value.toIntOrNull() ?: return
        viewModelScope.launch {
            repository.setDailyGoal(parsed)
        }
    }
}
