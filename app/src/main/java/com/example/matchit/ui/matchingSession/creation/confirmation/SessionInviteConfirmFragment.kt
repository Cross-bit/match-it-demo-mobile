package com.example.matchit.ui.matchingSession.creation.confirmation

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.matchit.R
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.databinding.FragmentSessionInviteConfirmBinding
import com.example.matchit.ui.MainActivity
import com.example.matchit.ui.common.setupDoubleBackPress
import com.example.matchit.ui.launch.LaunchActivity
import com.example.matchit.ui.matchingSession.creation.confirmation.banners.InviteConfirmMovieFragment
import com.example.matchit.ui.matchingSession.creation.confirmation.banners.InviteConfirmRestaurantFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


/**
 * Session fragment where users confirms their session invitation
 */

@AndroidEntryPoint
class SessionInviteConfirmFragment : Fragment() {

    private var _binding: FragmentSessionInviteConfirmBinding? = null
    private val binding get() = _binding!!

    private val sessionConfirmationViewModel: SessionInviteConfirmViewModel by viewModels<SessionInviteConfirmViewModel>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var invitationBanner: FragmentContainerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSessionInviteConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        sessionConfirmationViewModel.initOnViewCreated()

        val acceptInviteBtn = binding.sessionAcceptBtn
        val rejectInviteBtn = binding.sessionRejectBtn
        val progressBar = binding.sessionProgressBar
        invitationBanner = binding.bannerContainer
        //sessionOptionsBtn = binding.sessionOptionsBtn

        viewLifecycleOwner.lifecycleScope.launch {
            sessionConfirmationViewModel.sessionCreatorName.collectLatest {
                binding.invitationMessage.text = String.format(getString(R.string.invitation_message), it);
            }
        }

        acceptInviteBtn.setOnClickListener {
           sessionConfirmationViewModel.acceptSessionInvite()
            acceptInviteBtn.isEnabled = false
            rejectInviteBtn.isEnabled = false

            progressBar.visibility = View.VISIBLE
        }

        rejectInviteBtn.setOnClickListener {
            sessionConfirmationViewModel.rejectSessionInvite()
            rejectInviteBtn.isEnabled = false
            acceptInviteBtn.isEnabled = false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            sessionConfirmationViewModel.closeConfirmEvent.collectLatest {
                returnToMainActivity()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {

            sessionConfirmationViewModel.sessionConnectionMessageEvent.collectLatest {
                Toast.makeText(
                    context,
                    it,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {

            sessionConfirmationViewModel.sessionTerminatedEvent.collectLatest {
                Toast.makeText(
                    context,
                    it,
                    Toast.LENGTH_SHORT
                ).show()

                returnToMainActivity()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            /**
             * Session has started (we got notification that creator of the session has started it)
             */
            sessionConfirmationViewModel.sessionStartedEvent.collectLatest {
                it.let { sessionStartRes ->
                    Toast.makeText(
                        context,
                        it,
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigate(R.id.action_sessionInviteConfirmFragment_to_sessionMatchingFragment)
                }

            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionConfirmationViewModel.sessionType.collectLatest { state ->
                    when (state) {
                        SessionType.MOVIE -> setupMovieInviteFragment()
                        SessionType.RESTAURANT -> setupRestaurantInviteFragment()
                        null -> Unit
                    }
                }
            }
        }

        initBackPressBehaviour()
    }

    private fun initBackPressBehaviour() {
        setupDoubleBackPress(
            messageRes = R.string.session_invite_confirm_double_press
        ) {
            sessionConfirmationViewModel.rejectSessionInvite()
        }
    }

    private fun setupRestaurantInviteFragment() {

        // make sure we have location permissions
        requestLocationPermission()

        /*sessionOptionsBtn.visibility = View.VISIBLE

        sessionOptionsBtn.setOnClickListener {
            val bottomSheet = BottomSheetFragment.newInstance(RestaurantParametersFragment::class.java)
            bottomSheet.show(childFragmentManager, bottomSheet.tag)
        }*/

        childFragmentManager.beginTransaction()
            .add(invitationBanner.id, InviteConfirmRestaurantFragment())
            .commit()
    }

    private fun setupMovieInviteFragment() {
        childFragmentManager.beginTransaction()
            .add(invitationBanner.id, InviteConfirmMovieFragment()).commit()
    }

    private fun requestLocationPermission() {
        if (!checkLocationPermission()) {
            ActivityCompat.requestPermissions(
                this.requireActivity(),
                arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
        }
    }

    private fun checkLocationPermission() : Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }


    /**
     * Redirects user back to the main activity
     */
    private fun returnToMainActivity() {
        val intent = Intent(requireContext(), LaunchActivity::class.java)
        startActivity(intent)
        requireActivity().finish()

        /*val intent = Intent(activity, MainActivity::class.java) // we actually open launch activity to ensure user is still authenticated
        startActivity(intent)
        requireActivity().finish()*/
    }

}