package com.example.matchit.ui.socialConnections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.matchit.R
import com.example.matchit.databinding.FragmentFriendsListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FriendsListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView

    private val friendsViewModel: FriendsListViewModel by viewModels<FriendsListViewModel>()

    private var _binding: FragmentFriendsListBinding? = null

    private val binding get() = _binding!!

    private lateinit var friendsAdapter: FriendsAdapter

    private lateinit var progressBar: ProgressBar
    private lateinit var noConnectionsInfo: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFriendsListBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Set up of recycle view handler
        progressBar = binding.connectionsLoadProgressBar
        progressBar.visibility = View.VISIBLE

        noConnectionsInfo = binding.noConnections
        noConnectionsInfo.visibility = View.GONE

        recyclerView = binding.friendsRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        friendsAdapter = FriendsAdapter()
        recyclerView.adapter = friendsAdapter

        friendsViewModel.allFriendsData.observe(viewLifecycleOwner) { list ->
            progressBar.visibility = View.GONE
            friendsAdapter.submitList(list)

            if (list.isEmpty()) {
                noConnectionsInfo.visibility = View.VISIBLE
            }
        }

        onSearchBtn()
        onFriendRequestsBtn()
    }

    private fun onSearchBtn() {
        binding.searchBtn.setOnClickListener {
            findNavController().navigate(R.id.action_friendsListFragment_to_searchPersonFragment)
        }
    }

    private fun onFriendRequestsBtn() {
        binding.friendRequestsBtn.setOnClickListener {
            findNavController().navigate(R.id.action_friendsListFragment_to_friendRequestsFragment)
        }
    }
}