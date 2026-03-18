package com.example.matchit.ui.matchingSession.creation.invitation

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.example.matchit.R
import com.example.matchit.data.model.session.AlgorithmType
import com.example.matchit.databinding.FragmentSessionInviteMembersListBinding
import com.example.matchit.ui.MainActivity
import com.example.matchit.ui.matchingSession.creation.invitation.friendList.FriendInviteAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SessionInviteMembersListFragment : Fragment() {

    private var _binding: FragmentSessionInviteMembersListBinding? = null
    private val binding get() = _binding!!

    private val sessionInviteViewModel: SessionInviteViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var _adapter: FriendInviteAdapter

    private lateinit var inviteMembersBtn: Button
    private lateinit var startSessionBtn: Button
    private lateinit var cancelInvitationBtn: Button

    private lateinit var friendsLoadProgressBar: ProgressBar
    private lateinit var noMembersToInviteText: TextView



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionInviteMembersListBinding.inflate(inflater, container, false)
        return binding.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
        setupRecyclerViews()
        setupLoadingBars()

        setupViewModelBinding()

        // fetch all friends data
        sessionInviteViewModel.loadAllFriendsData()

        setupAlgorithmConfig()
    }

    private fun setupAlgorithmConfig() {

        val spinner = binding.algorithmSelector.algorithmSpinner

        val algorithms = listOf(
            "Sync (S1)" to AlgorithmType.SYNC,
            "Async (A3)" to AlgorithmType.ASYNC,
            "Hybrid (H1)" to AlgorithmType.HYBRID
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            algorithms.map { it.first }
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {

                    val algorithm = algorithms[position].second
                    sessionInviteViewModel.setAlgorithm(algorithm)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun setupRecyclerViews() {

        recyclerView = binding.friendsInviteRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this.context)
        recyclerView.setHasFixedSize(true)

        _adapter = FriendInviteAdapter(this.context) {
                friendData, selected ->
            if (selected) {
                sessionInviteViewModel.addMemberToSelection(friendData.uuid)
            }
            else {
                sessionInviteViewModel.removeMemberFromSelection(friendData.uuid)
            }
        }

        recyclerView.adapter = _adapter
    }

    private fun setupLoadingBars() {
        friendsLoadProgressBar = binding.friendsLoadProgressBar
        noMembersToInviteText = binding.noMembersToInvite
    }

    private fun setupButtons() {

        inviteMembersBtn = binding.inviteSelectedMemberBtn;
        inviteMembersBtn.isEnabled = false;
        startSessionBtn = binding.sessionStartBtn;
        cancelInvitationBtn = binding.cancelMemberInvitation

        inviteMembersBtn.setOnClickListener {

            inviteMembersBtn.isEnabled = false
            inviteMembersBtn.visibility = View.GONE
            cancelInvitationBtn.visibility = View.VISIBLE
            cancelInvitationBtn.isEnabled = true

            sessionInviteViewModel.inviteMembersAndCreateSession()
        }

        startSessionBtn.setOnClickListener {
            sessionInviteViewModel.startMatchingSession()
        }

        cancelInvitationBtn.setOnClickListener {
            sessionInviteViewModel.cancelInvitation()
        }

    }

    private fun setupViewModelBinding() {

        /**
         * WHEN EVER ENTIRE LIST OF USERS CHANGES
         */
        sessionInviteViewModel.allFriends.observe(viewLifecycleOwner) {
            val invitableFriendsData = it ?: return@observe

            friendsLoadProgressBar.visibility = View.GONE

            if (invitableFriendsData.isEmpty()) {
                noMembersToInviteText.visibility = View.VISIBLE
            }
            else
                noMembersToInviteText.visibility = View.GONE

            _adapter.apply {
                dataList = invitableFriendsData
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            sessionInviteViewModel.friendDataUpdate.collectLatest { index ->
                // if item updates its state we update the recycler view
                recyclerView.adapter?.notifyItemChanged(index)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            /**
             *  WHEN EVER USER IS SELECTED(positively) from the invitation list
             */
            sessionInviteViewModel.userSelectedEvent.collectLatest {
                it.let {
                    inviteMembersBtn.isEnabled = true
                }
            }
        }

        /**
         *  WHEN EVER USER SELECTS/DESELECTS A CLIENT from the invitation list
         */
        sessionInviteViewModel.numberOfClientsSelected.observe(viewLifecycleOwner) { numberOfClients ->
            numberOfClients?.let {
                if (numberOfClients == 0) // this is actually needed when we deselect all the users, but we could put this info into userSelectedEvent
                    inviteMembersBtn.isEnabled = false
            }
        }


        viewLifecycleOwner.lifecycleScope.launch {
            /**
             * SESSION START PART (after user starts the session)
             */
            sessionInviteViewModel.sessionStarted.collectLatest {
                if (it.isReady) {
                    inviteMembersBtn.isEnabled = false;
                    startSessionBtn.isEnabled = false;
                    this@SessionInviteMembersListFragment.findNavController().navigate(R.id.action_sessionInviteFragment_to_sessionCardsFragment)
                }
                else {
                    it.error?.let { e -> Toast.makeText(getApplicationContext(), e, Toast.LENGTH_LONG).show() }
                    this@SessionInviteMembersListFragment.navigateMainActivity()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            /**
             * SESSION IS READY TO BE STARTED (once the user invited members and enough of them confirmed the invitation)
             */
            sessionInviteViewModel.sessionIsReadyToBeStarted.collectLatest {

                if (it.isReady) {
                    inviteMembersBtn.visibility = View.GONE
                    startSessionBtn.visibility = View.VISIBLE
                    cancelInvitationBtn.visibility = View.GONE

                }
                else {
                    it.error?.let { e -> Toast.makeText(context, e, Toast.LENGTH_LONG).show() }
                    navigateMainActivity()
                }
            }
        }


        viewLifecycleOwner.lifecycleScope.launch {
            /**
             * INVITATION CANCELED
             */
            sessionInviteViewModel.sessionInvitationCanceledEvent.collectLatest {
                cancelInvitationBtn.visibility = View.GONE
                inviteMembersBtn.visibility = View.VISIBLE
            }
        }
    }


    private fun navigateMainActivity() {
        // navigate to the dashboard
        val intent = Intent(activity, MainActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }

}