package com.example.matchit.ui.sessionHistoryChats

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.matchit.R
import com.example.matchit.databinding.FragmentChatWindowBinding
import com.example.matchit.databinding.FragmentSessionHistoryChatsBinding
import com.example.matchit.ui.chatting.ChatMessagesAdapter
import com.example.matchit.ui.chatting.MessagesLoadUpdate
import com.example.matchit.ui.matchingSession.matching.SessionMatchingFragmentDirections
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SessionHistoryChatsFragment : Fragment() {

    companion object {
        fun newInstance() = SessionHistoryChatsFragment()
    }

    private var _binding: FragmentSessionHistoryChatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionHistoryChatsViewModel by viewModels<SessionHistoryChatsViewModel>()

    private lateinit var historySessionsAdapter: SessionHistoryChatsAdapter;
    private lateinit var historySessionsRecyclerview: RecyclerView

    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSessionHistoryChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        progressBar = binding.sessionsLoadProgressBar
        progressBar.visibility = View.VISIBLE
        binding.noDataMessage.visibility = View.GONE

        historySessionsRecyclerview = binding.historySessionsRecyclerView
        historySessionsRecyclerview.layoutManager = LinearLayoutManager(requireContext())

        historySessionsAdapter = SessionHistoryChatsAdapter()
        { item ->

            val action = SessionHistoryChatsFragmentDirections.
            actionSessionHistoryChatsFragmentToChatWindowFragment(item.sessionUUID)

            findNavController().navigate(action)
        }
        historySessionsRecyclerview.adapter = historySessionsAdapter
        historySessionsRecyclerview.setHasFixedSize(true)
        historySessionsRecyclerview.itemAnimator = null

        viewModel.historySessions.observe(viewLifecycleOwner) { newData ->

            if (newData.error != null) {
                val message = getString(newData.error)
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                binding.errorMessage.visibility = View.VISIBLE
                return@observe
            }

            if (newData.data.isEmpty()) {
                binding.noDataMessage.visibility = View.VISIBLE
            }

            historySessionsAdapter.submitList(newData.data)
            progressBar.visibility = View.GONE
            binding.errorMessage.visibility = View.GONE
        }
    }
}