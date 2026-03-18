package com.example.matchit.ui.matchingSession.creation.invitation

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.matchit.R
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.databinding.FragmentSessionInviteBinding
import com.example.matchit.ui.MainActivity
import com.example.matchit.ui.matchingSession.SessionViewModel
import com.example.matchit.ui.matchingSession.creation.invitation.parametersSettings.RestaurantParametersScrollableFragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SessionInviteFragment : Fragment() {

    private var _binding: FragmentSessionInviteBinding? = null
    private val binding get() = _binding!!

    private val sessionViewModel: SessionViewModel by activityViewModels()

    private val sessionInviteViewModel: SessionInviteViewModel by activityViewModels()

    private lateinit var invitationMessageTextView: TextView

    private var isActiveFormValid: Boolean = true

    private val tabFragments = mutableListOf<Fragment>()
    private val tabTitles = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSessionInviteBinding.inflate(inflater, container, false)
        return binding.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.requestLocationPermission()

        setupTextViews()
        setupButtons()
        setupViewModelBinding()

        viewLifecycleOwner.lifecycleScope.launch {
            sessionInviteViewModel.currentSessionType
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { type ->
                    setupTabsForSessionType(type)
                    installFragments()
                    setupTabLayout()
                }
        }

        // fetch all friends data
        sessionInviteViewModel.loadAllFriendsData()

        viewLifecycleOwner.lifecycleScope.launch {
            sessionViewModel.activeForm
                .flatMapLatest { form ->
                    form?.isValidFlow ?: flowOf(true)
                }
                .collect { isValid ->
                    isActiveFormValid = isValid
                    updateTabsAvailability(isValid)
                }
        }

        initBackPressBehaviour()
    }

    private fun updateTabsAvailability(isValid: Boolean) {
        val tabs = binding.sessionTabs
        val inviteTab = tabs.getTabAt(1) ?: return

        //inviteTab.view.isEnabled = isValid
        inviteTab.view.alpha = if (isValid) 1f else 0.4f
    }


    private fun initBackPressBehaviour() {
        val backPressCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                sessionInviteViewModel.cancelInvitation()
                sessionInviteViewModel.clearSessionState()
                 this@SessionInviteFragment.navigateMainActivity()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback)
    }

    private fun setupTabLayout() {
        val tabs = binding.sessionTabs
        tabs.removeAllTabs()

        tabTitles.forEach { title ->
            tabs.addTab(tabs.newTab().setText(title))
        }

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {

                val isInviteTab = tab.position > 0

                if (isInviteTab && !isActiveFormValid) {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.fill_form_first),
                        Snackbar.LENGTH_SHORT
                    ).show()

                    tabs.selectTab(tabs.getTabAt(0))
                    return
                }

                childFragmentManager.beginTransaction().apply {
                    tabFragments.forEach { hide(it) }
                    show(tabFragments[tab.position])
                }.commit()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupTabsForSessionType(type: SessionType) {
        tabFragments.clear()
        tabTitles.clear()

        when (type) {
            SessionType.RESTAURANT -> {
                tabFragments += RestaurantParametersScrollableFragment()
                tabTitles += getString(R.string.invitation_tab_ses_options)

                tabFragments += SessionInviteMembersListFragment()
                tabTitles += getString(R.string.invitation_tab_mem)
            }

            SessionType.MOVIE -> {
                tabFragments += SessionInviteMembersListFragment()
                tabTitles += getString(R.string.invitation_tab_mem)
            }
        }
    }

    private fun installFragments() {
        val tx = childFragmentManager.beginTransaction()

        tabFragments.forEachIndexed { index, fragment ->
            val tag = "tab_$index"

            // Ensure fragment is not already there
            val existing = childFragmentManager.findFragmentByTag(tag)

            val target = if (existing != null) {
                existing
            } else {
                fragment
            }

            // IMPORTANT: rewrite to real cur fragment
            tabFragments[index] = target

            if (existing == null) {
                tx.add(R.id.tabContentContainer, target, tag)
                if (index != 0) tx.hide(target)
            }
        }

        tx.commitNowAllowingStateLoss()
    }


    private fun setupTextViews() {
        invitationMessageTextView = binding.invitingMembersMessage
        invitationMessageTextView.visibility = View.GONE
    }

    private fun setupButtons() {

        val exitSessionBtn = binding.exitSessionButton

        exitSessionBtn.setOnClickListener {
            sessionInviteViewModel.cancelInvitation()
            sessionInviteViewModel.clearSessionState()
            this.navigateMainActivity()
        }
    }

    private fun checkLocationPermission() : Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
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

    private fun setupViewModelBinding() {

        /**
         * WHEN EVER ENTIRE LIST OF USERS CHANGES
         */

        viewLifecycleOwner.lifecycleScope.launch {
            sessionInviteViewModel.sessionInvitationStatusUpdated.collectLatest { isInviting ->
                invitationMessageTextView.visibility = if (isInviting) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            sessionInviteViewModel.sessionTerminatedEvent.collectLatest { reasonMessage ->
                Toast.makeText(context, getString(reasonMessage), Toast.LENGTH_SHORT).show()
                navigateMainActivity()
            }
        }
    }

    private fun navigateMainActivity() {
        // navigate to the dashboard
        val intent = Intent(activity, MainActivity::class.java)
        startActivity(intent)
        activity?.finish() // finish current activity
    }

}


