package com.example.matchit.ui.matchingSession.initialisation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InitVotingAlertViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
): ViewModel() {

    private val _initialVotingStartedEvent = MutableLiveData<SessionType>()
    val initialVotingStartedEvent: LiveData<SessionType> = _initialVotingStartedEvent

    fun initializeVotingSession() {
        viewModelScope.launch {
            sessionRepository.lastSessionType.collect { state ->
                state?.let {
                    _initialVotingStartedEvent.value = it;
                }
            }
        }
    }

}