package com.xaniihub.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaniihub.app.domain.model.DashboardStats
import com.xaniihub.app.domain.repository.XaniiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val stats: DashboardStats? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true,
    /**
     * Current calendar day, kept in the state so that [isToday] follows midnight. It used to be
     * captured once, when the state was created, so a screen left open overnight kept claiming
     * that yesterday was today.
     */
    val today: LocalDate = selectedDate
) {
    val isToday: Boolean get() = selectedDate == today
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
        // One ticker for the whole app: this screen used to run a second, slightly different copy
        // of the same midnight loop that the repository already maintains.
        repository.observeCurrentDate()
            .onEach { today ->
                _uiState.update { it.copy(today = today) }
                if (followsCurrentDay) selectedDate.value = today
            }
            .launchIn(viewModelScope)

        selectedDate
            .flatMapLatest { date -> repository.observeDashboardStatsForDate(date) }
            .onEach { stats ->
                _uiState.update {
                    it.copy(
                        stats = stats,
                        selectedDate = stats.date,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun selectDate(date: LocalDate) {
        followsCurrentDay = date == _uiState.value.today
        selectedDate.value = date
    }

    fun setGoal(value: String) {
        val parsed = value.toIntOrNull() ?: return
        viewModelScope.launch {
            repository.setDailyGoal(parsed)
        }
    }
}
