package com.example.matchit.ui.sessionHistoryChats

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.R
import com.example.matchit.data.model.session.SessionState
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.remote.model.SessionApi.SessionSummaryApiResponse
import com.example.matchit.data.remote.model.UserProfileApiResponse
import com.example.matchit.data.session.SessionRepository
import com.example.matchit.data.users.UsersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.logging.Logger
import javax.inject.Inject


data class HistoryDataUpdate(
    val error: Int? = null,
    val data: List<HistorySessionItem> = emptyList()
)


@HiltViewModel
class SessionHistoryChatsViewModel @Inject constructor(
    val sessionRepository: SessionRepository,
    val usersRepository: UsersRepository
) : ViewModel() {

    /**
     * Hols information about all the historical sessions
     */
    private val _historySessions = MutableLiveData<HistoryDataUpdate>()
    val historySessions: LiveData<HistoryDataUpdate> =  _historySessions

    init {
        viewModelScope.launch {
            // load all history sessions
            loadAllHistorySessions()
        }
    }

    private suspend fun loadAllHistorySessions() {
        val sessHist = when(val result = sessionRepository.getAllUserHistorySessions()) {
            is Resource.Success -> {
                result.data
            }
            else -> {
                _historySessions.value = HistoryDataUpdate(error = R.string.sessions_history_load_failed)
                return
            }
        }

        val uniqueUserUuids =
            sessHist
                .flatMapTo(mutableSetOf()) { it.users }
                .toList()

        val profiles: List<UserProfileApiResponse> = when (val res = usersRepository.fetchUserDataBatch(uniqueUserUuids)) {
            is Resource.Success -> res.data
            else -> {
                _historySessions.value = HistoryDataUpdate(error = R.string.sessions_history_load_failed)
                return
            }
        }

        val profileMap: Map<String, UserProfileApiResponse> =
            profiles.associateBy { it.uuid.toString() }

        val historyItems: List<HistorySessionItem> =
            sessHist
                .sortedByDescending { it.createdAt }
                .map { session ->

                val userProfiles = session.users.mapNotNull { uuid ->
                    profileMap[uuid]
                }

                HistorySessionItem(
                    sessionUUID = session.sessionUUID,
                    sessionType = session.sessionType,
                    state = session.state,
                    createdAt = session.createdAt,
                    size = session.size,
                    users = userProfiles
                )
            }

        _historySessions.value = HistoryDataUpdate(data=historyItems)

    }



}