package com.example.matchit.ui.matchingSession.creation.invitation.parametersSettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.matchit.data.model.session.SessionParameters
import com.example.matchit.databinding.FragmentRestaurantMatchingParametersBinding
import com.example.matchit.ui.matchingSession.SessionViewModel
import com.example.matchit.ui.matchingSession.common.BottomSheetFragment
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class RestaurantParametersFragment : Fragment() {

    private lateinit var _binding: FragmentRestaurantMatchingParametersBinding
    private lateinit var priceRangeSlider: Slider
    private lateinit var sessionOptions: FlexboxLayout
    private lateinit var searchCityInputField: TextInputEditText
    private lateinit var maxPriceRangeText: TextView

    // we set custom label
    private val priceLabels = arrayOf("\$", "\$\$", "\$\$\$", "\$\$\$\$", "\$\$\$\$\$")

    private val sessActivityViewModel: SessionViewModel by activityViewModels()

    private val categoryCheckboxes =
        mutableMapOf<SessionParameters.RestaurantParameters.Category, CheckBox>()

    private val viewModel: RestaurantParametersFragmentViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRestaurantMatchingParametersBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchCityInputField = _binding.searchCityInputField
        priceRangeSlider = _binding.priceRangeSlider
        sessionOptions = _binding.categoryCheckboxContainer
        maxPriceRangeText = _binding.maxPriceRangeValue

        priceRangeSlider.setLabelFormatter { it -> priceLabels[it.toInt()] }

        SessionParameters.RestaurantParameters.Category.entries.forEach { category ->
            val checkBox = CheckBox(requireContext()).apply {
                text = getString(category.toResId())
                textSize = 14f

                setOnCheckedChangeListener { _, isChecked ->
                    viewModel.onCategoryToggled(category, isChecked)
                }
            }

            categoryCheckboxes[category] = checkBox
            sessionOptions.addView(checkBox)
        }

        priceRangeSlider.addOnChangeListener { _, value, _ ->
            viewModel.onPriceChanged(value)
            maxPriceRangeText.text = priceLabels[value.toInt()]
        }

        searchCityInputField.doAfterTextChanged {
            viewModel.onCityChanged(it.toString())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state ?: return@collect

                    if (searchCityInputField.text.toString() != state.params.searchCity) {
                        searchCityInputField.setText(state.params.searchCity)
                    }

                    priceRangeSlider.value = state.params.restaurantPriceRange

                    // checkboxes
                    categoryCheckboxes.forEach { (category, checkBox) ->
                        checkBox.setOnCheckedChangeListener(null)
                        checkBox.isChecked =
                            state.params.selectedCategories.contains(category)
                        checkBox.setOnCheckedChangeListener { _, isChecked ->
                            viewModel.onCategoryToggled(category, isChecked)
                        }
                    }

                    // validation
                    if(state.validation.searchCityIsNotEmptyMessage != null)
                        searchCityInputField.error = state.validation.searchCityIsNotEmptyMessage.let(::getString)
                }
            }
        }

        // ensure that specific parent fragments know about the params

        (requireParentFragment() as? BottomSheetFragment)
            ?.registerRecoverableForm(viewModel)


        sessActivityViewModel.setActiveForm(viewModel)

    }

}