package com.example.matchit.ui.matchingSession.matching.cards.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.matchit.databinding.FragmentMovieMatchCardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MovieMatchCardFragment : Fragment() {

    private var _binding: FragmentMovieMatchCardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMovieMatchCardBinding.inflate(inflater, container, false)
        return binding.root;
    }
}