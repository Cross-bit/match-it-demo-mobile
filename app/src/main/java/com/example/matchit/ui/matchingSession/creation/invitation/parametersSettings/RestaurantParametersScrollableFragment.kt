package com.example.matchit.ui.matchingSession.creation.invitation.parametersSettings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.matchit.R
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class RestaurantParametersScrollableFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(
            R.layout.fragment_restaurant_parameters_scrollable,
            container,
            false
        )
    }
}