package com.example.matchit.ui.matchingSession.initialisation

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.matchit.R
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.databinding.FragmentInitVotingAlertBinding
import com.example.matchit.ui.MainActivity
import com.example.matchit.ui.matchingSession.creation.confirmation.SessionInviteConfirmViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * If we find out user did not voted in given category, we show him
 * */

@AndroidEntryPoint
class InitVotingAlertFragment : Fragment() {

    private val _viewModel: InitVotingAlertViewModel by viewModels<InitVotingAlertViewModel>()
    private val _sessionInviteConfirmationViewModel: SessionInviteConfirmViewModel by viewModels<SessionInviteConfirmViewModel>()
    private var _binding: FragmentInitVotingAlertBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentInitVotingAlertBinding.inflate(inflater, container, false)
        return binding.root;
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val acceptSessionInitBtn = binding.acceptSessionInitialisationBtn;
        val rejectSessionInitBtn = binding.rejectSessionInitialisationBtn;


        acceptSessionInitBtn.setOnClickListener {
            _viewModel.initializeVotingSession();
        }

       _viewModel.initialVotingStartedEvent.observe(viewLifecycleOwner) {

            val navController = findNavController()


            it?.let { currentSesType ->

                when (currentSesType) {
                    SessionType.MOVIE -> {
                        navController.navigate(R.id.action_initVotingAlert_to_initVotingMoviesFragment)
                    }
                    SessionType.RESTAURANT -> {
                    }
                    else ->
                        throw Exception("Wrong session type");
                }
            }
        }


        rejectSessionInitBtn.setOnClickListener {
            val navController = findNavController()

            // Define your specific destination ID;
            val inviteConfirmationFragment = R.id.sessionInviteConfirmFragment
            val previousFragment = navController.previousBackStackEntry

            // if previous destination is confirmation/rejection of invitation to a session (but user haven't gone through init phase so far)
            if (previousFragment?.destination?.id == inviteConfirmationFragment) {
                showAlertSecondConfirmation();
            }
            else {
                // if we just got here ... from else where ... we go back to main activity directly
                navigateMainActivity();
            }

        }

        initBackPressBehaviour()

    }

    private fun initBackPressBehaviour() {
        val backPressCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                this@InitVotingAlertFragment.navigateMainActivity()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback)
    }

    private fun showAlertSecondConfirmation()
    {
        val dialog = AlertDialog.Builder(this.context);
        dialog.setTitle(R.string.alert_message_title_0);
        dialog.setMessage(R.string.alert_message_init_rejection);

        dialog.setPositiveButton("Yes") { dialog, which ->
            _sessionInviteConfirmationViewModel.rejectSessionInvite()
            navigateMainActivity();
            dialog.dismiss();
        }

        // move to resources
        dialog.setNegativeButton("No") { dialog, which ->
            dialog.dismiss();
        }

        dialog.show()

    }

    private fun navigateMainActivity() {
        val intent = Intent(this.context, MainActivity::class.java)
        startActivity(intent);
        requireActivity().finish()
    }

}