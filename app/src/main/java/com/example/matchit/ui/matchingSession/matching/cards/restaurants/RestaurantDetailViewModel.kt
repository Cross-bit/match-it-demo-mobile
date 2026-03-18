package com.example.matchit.ui.matchingSession.matching.cards.restaurants

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.data.deviceSensors.LocationRepository
import com.example.matchit.data.model.session.GpsCoordinates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class RestaurantDetailViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _userLocation = MutableLiveData<GpsCoordinates>()
    val userLocation: LiveData<GpsCoordinates> = _userLocation

    init {
        viewModelScope.launch {
            val location = locationRepository.getCurrentLocation()

            if (location.isSuccess) {
                location.getOrNull()?.let {
                    _userLocation.postValue(it)
                }
            }
        }
    }
}
