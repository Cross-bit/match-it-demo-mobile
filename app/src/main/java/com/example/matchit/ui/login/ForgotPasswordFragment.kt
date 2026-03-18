package com.example.matchit.ui.login

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.matchit.R
import com.example.matchit.databinding.ActivityMainBinding
import com.example.matchit.databinding.FragmentForgotPasswordBinding
import com.example.matchit.databinding.FragmentLoginBinding

class ForgotPasswordFragment : Fragment() {
    private lateinit var _binding: FragmentForgotPasswordBinding

    private lateinit var viewModel: ForgotPasswordViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backBtn = _binding.goBackBtn

        backBtn.setOnClickListener() {
            findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
        }
    }
}