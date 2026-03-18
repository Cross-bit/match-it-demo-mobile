package com.example.matchit.ui.matchingSession.creation.confirmation.banners

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.matchit.databinding.FragmentInviteMovieBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InviteConfirmMovieFragment : Fragment() {
    private var _binding: FragmentInviteMovieBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInviteMovieBinding.inflate(inflater, container, false)
        return binding.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { }
}