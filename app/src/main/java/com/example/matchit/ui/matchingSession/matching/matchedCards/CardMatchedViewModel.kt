package com.example.matchit.ui.matchingSession.matching.matchedCards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.data.model.session.MatchedItemDTO
import com.example.matchit.data.model.session.MovieCardData
import com.example.matchit.data.model.session.RestaurantCardData
import com.example.matchit.data.notifications.pushNotifications.NotificationDispatcher
import com.example.matchit.data.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CardMatchedViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val notificationDispatcher: NotificationDispatcher,
) : ViewModel() {

    val matchedItems: StateFlow<List<MatchedItemDTO>> =
        sessionRepository.matchedItems

    init {
        // from now on we can receive invitation other sessions
        notificationDispatcher.invalidateSessionInvite()
    }
}