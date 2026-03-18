package com.example.matchit.ui.matchingSession.creation.invitation.parametersSettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matchit.R
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.data.model.session.SessionParameters
import com.example.matchit.data.session.SessionParametersRepository
import com.example.matchit.ui.matchingSession.common.SessionForm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParamsValidationResult(
    val minNumberOfCategoriesSelectedMessage: Int? = null, // null means ok, otherwise error message is returned
    val searchCityIsNotEmptyMessage: Int? = null,
    val isValid: Boolean
)

data class RestaurantParametersUiState(
    val params: SessionParameters.RestaurantParameters,
    val validation: ParamsValidationResult
)

@HiltViewModel
class RestaurantParametersFragmentViewModel @Inject constructor(
    private val paramsRepository: SessionParametersRepository
) : ViewModel(), SessionForm {

    private var lastSnapshot: SessionParameters.RestaurantParameters? = null
    private var lastValidationResult: Boolean = true

    private val _params =
        MutableStateFlow<SessionParameters.RestaurantParameters?>(null)

    val uiState: StateFlow<RestaurantParametersUiState?> =
        _params
            .filterNotNull()
            .map { params ->
                RestaurantParametersUiState(
                    params = params,
                    validation = validateRestaurantParams(params)
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                null
            )

    override val isValidFlow: StateFlow<Boolean> =
        uiState
            .map { it?.validation?.isValid == true }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                false
            )

    init {
        viewModelScope.launch {
            _params.value = paramsRepository.getSessionParameters(SessionType.RESTAURANT)
                        as? SessionParameters.RestaurantParameters
                    ?: SessionParametersFactory.createDefault(SessionType.RESTAURANT)
                            as SessionParameters.RestaurantParameters

            // we store last state of the form
            createSnapshot()
        }
    }

    private fun validateRestaurantParams(params: SessionParameters.RestaurantParameters): ParamsValidationResult {
        val cityValid = params.searchCity.isNotBlank()
        val categoriesValid = params.selectedCategories.isNotEmpty()

        lastValidationResult = cityValid && categoriesValid

        return ParamsValidationResult(
            if (categoriesValid) null else R.string.no_category_selected,
            if (cityValid) null else R.string.city_empty_error,
            isValid = lastValidationResult
        )
    }

    override fun createSnapshot() {
        lastSnapshot = _params.value?.copy()
    }

    private fun recallLastSnapshot() {
        if (lastSnapshot != null)
            _params.value = lastSnapshot

        viewModelScope.launch {
            paramsRepository.updateSessionParameters(
                _params.value as SessionParameters,
                    SessionType.RESTAURANT
            )
        }
    }

    override fun isValid(): Boolean {
        return lastValidationResult
    }

    override fun recoverIfNeeded() {
        recallLastSnapshot()
    }


    fun onCityChanged(city: String) =
        update { it.copy(searchCity = city) }

    fun onPriceChanged(price: Float) =
        update { it.copy(restaurantPriceRange = price) }

    fun onCategoryToggled(
        category: SessionParameters.RestaurantParameters.Category,
        checked: Boolean
    ) = update {
        val newSet = it.selectedCategories.toMutableSet().apply {
            if (checked) add(category) else remove(category)
        }
        it.copy(selectedCategories = newSet)
    }

    private fun update(
        reducer: (SessionParameters.RestaurantParameters) -> SessionParameters.RestaurantParameters
    ) {
        val current = _params.value ?: return
        val updated = reducer(current)

        _params.value = updated

        viewModelScope.launch {
            paramsRepository.updateSessionParameters(updated, SessionType.RESTAURANT)
        }
    }

}