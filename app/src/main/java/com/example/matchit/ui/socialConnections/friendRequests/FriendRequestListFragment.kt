package com.example.matchit.ui.socialConnections.friendRequests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.matchit.R
import com.example.matchit.databinding.FragmentFriendRequestsListBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FriendRequestListFragment : Fragment() {

    private lateinit var requestRecyclerView: RecyclerView

    private val friendsViewModel: FriendRequestsListViewModel by viewModels<FriendRequestsListViewModel>()

    private lateinit var _backToFriendListBtn: FloatingActionButton

    private var _binding: FragmentFriendRequestsListBinding? = null

    private lateinit var _requestsInfo: TextView

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFriendRequestsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        _backToFriendListBtn = binding.backToFriendsBtn
        _requestsInfo = binding.requestsInfo

        requestRecyclerView = binding.friendRequestsRecyclerView

        requestRecyclerView.layoutManager = LinearLayoutManager(view.context)
        requestRecyclerView.setHasFixedSize(true)

        friendsViewModel.allRequestData.observe(viewLifecycleOwner,
            Observer {

                if (it.isNotEmpty()) {
                    requestRecyclerView.adapter = IncomingFriendsRequestsAdapter(ArrayList(it), friendsViewModel)

                    _requestsInfo.visibility = View.GONE
                }
                else {
                    _requestsInfo.visibility = View.VISIBLE
                }
            })

        // fetch all friend requests data
        friendsViewModel.updateAllFriendRequests()

        friendsViewModel.admitFriendRequestStatus.observe(viewLifecycleOwner, Observer { status ->
            // Show the status (not error) message to the user
            Snackbar.make(view, status, Snackbar.LENGTH_LONG).show()
            findNavController().navigate(R.id.action_friendRequestsFragment_to_friendsListFragment)
        })

        _backToFriendListBtn.setOnClickListener {
            findNavController().navigate(R.id.action_friendRequestsFragment_to_friendsListFragment)
        }
    }
}