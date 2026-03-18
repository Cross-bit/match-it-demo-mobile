package com.example.matchit.ui.launch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.matchit.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApplicationBootingFragment : Fragment() {

    companion object {
        fun newInstance() = ApplicationBootingFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_application_loading, container, false)
    }
}