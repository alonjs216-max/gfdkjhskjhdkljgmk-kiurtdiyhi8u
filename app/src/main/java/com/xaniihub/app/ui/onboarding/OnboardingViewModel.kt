package com.xaniihub.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xaniihub.app.domain.model.BodyParams
import com.xaniihub.app.domain.repository.XaniiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: XaniiRepository
) : ViewModel() {

    fun complete(params: BodyParams, dailyGoal: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.saveBodyParams(params)
            repository.setDailyGoal(dailyGoal)
            onComplete()
        }
    }
}