package com.example.matchit.ui.matchingSession.initialisation.movies

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.data.model.session.MovieCardData
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.model.session.SessionUpdateRequestDTO
import com.example.matchit.data.model.session.UserVotingResult
import com.example.matchit.data.remote.client.Resource
import com.example.matchit.data.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InitVotingMoviesViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _initialVotingStartedEvent = MutableLiveData<SessionType>()
    val initialVotingData: LiveData<SessionType> = _initialVotingStartedEvent

    private val _sessionVoteUpdateEvent = MutableSharedFlow<Unit>()
    val sessionVoteUpdateEvent = _sessionVoteUpdateEvent.asSharedFlow()

    /**
     * Informs UI once the matching completed successfully (we found a match)
     */
    private val _matchingEndResult = MutableLiveData<Unit>()
    val matchingEndResult: LiveData<Unit> = _matchingEndResult

    /**
     * Holds information about how user voted
     */
    private var _lastVotingData: MutableList<UserVotingResult> = mutableListOf()

    private val _currentMatchingDeck = MutableLiveData<ArrayList<MovieCardData>>()
    val currentMatchingDeck: LiveData<ArrayList<MovieCardData>> = _currentMatchingDeck

    init {
        requestInitMovieCards()
    }

    private fun requestInitMovieCards() {
        viewModelScope.launch {

            when (val initMovieCardsData = sessionRepository.getInitMoviesCards()){
                is Resource.Success ->
                    _currentMatchingDeck.value = ArrayList(initMovieCardsData.data.movies);
                is Resource.Error ->
                    Log.e("initSession", initMovieCardsData.errorMessage)
                is Resource.GeneralError ->
                    Log.e("initSession", initMovieCardsData.message)
            }
        }
    }

    fun updateUserVoting(itemIndex: Int, swipeResult: Int) {

        _currentMatchingDeck.value?.getOrNull(itemIndex)?.let {
            _lastVotingData.add(UserVotingResult(it.cardId, swipeResult));

            viewModelScope.launch {
                _sessionVoteUpdateEvent.emit(Unit)
            }

            if (checkIfDeckFinished(itemIndex)) {
                updateStateAfterInitialisation()
                _matchingEndResult.postValue(Unit)
            }

        } ?: run {
            Log.e("nextSessionDataError", "Failed to load next session cards")
        }
    }

    fun removeLastUserVoting() {
        // simply remove the last voted item
        _lastVotingData.removeLast()
    }

    private fun updateStateAfterInitialisation() {
        viewModelScope.launch {
            sessionRepository.updateInitMovieResults(SessionUpdateRequestDTO("1", _lastVotingData, null))
        }
    }

    private fun checkIfDeckFinished(currentIndex: Int) : Boolean {
        return _currentMatchingDeck.value?.let {
            currentIndex == it.size - 1
        } ?: false
    }
}