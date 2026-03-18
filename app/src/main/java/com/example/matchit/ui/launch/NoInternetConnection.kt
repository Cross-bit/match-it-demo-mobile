package com.example.matchit.ui.launch

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.matchit.R
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class NoInternetConnection : Fragment() {

    companion object {
        fun newInstance() = ApplicationBootingFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_no_internet_connection, container, false)
    }
}