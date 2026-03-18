package com.example.matchit.ui.socialConnections.peopleSearch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.matchit.R
import com.example.matchit.databinding.FragmentSearchUserBinding

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class FragmentSearchUserFragment : Fragment() {
    private lateinit var _recyclerView: RecyclerView
    private lateinit var _progressBar: ProgressBar

    private lateinit var _searchBarText: TextView
    private lateinit var _searchBtn: Button

    private lateinit var _searchPersonAdapter: SearchPersonAdapter

    private lateinit var _backToFriendListBtn: FloatingActionButton

    private val _searchPeopleViewModel: SearchPeopleViewModel by viewModels<SearchPeopleViewModel>()

    private var _binding: FragmentSearchUserBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _recyclerView = binding.searchPersonRecycleView
        _progressBar = binding.progressCircular

        _searchBarText = binding.searchInputText
        _searchBtn = binding.searchSubmitBtn

        _backToFriendListBtn = binding.backToFriendsBtn

        _searchPersonAdapter = SearchPersonAdapter(_searchPeopleViewModel)

        _recyclerView.layoutManager = LinearLayoutManager(view.context)
        _recyclerView.setHasFixedSize(true)

        _recyclerView.adapter = _searchPersonAdapter



        _searchBtn.setOnClickListener {
            val query = _searchBarText.text.toString();

            if (query.isNotEmpty())
                 _searchPeopleViewModel.updateSearchPersonByEmail(query)
        }

        _searchPeopleViewModel.searchResult.observe(viewLifecycleOwner, Observer { searchResult ->
            _progressBar.visibility = View.GONE

            _searchPersonAdapter.apply {
                data = if (searchResult.data != null) listOf(searchResult.data) else emptyList()
            }

            searchResult.data?.let {
                _searchPersonAdapter.apply {
                    data = listOf(it)
                }
            }

            searchResult.error?.let{
                Snackbar.make(view, it, Snackbar.LENGTH_SHORT).show()
            }
        })

        _searchPeopleViewModel.friendRequestStatus.observe(viewLifecycleOwner, Observer { status ->
            // Show the status (not error) message to the user
            Snackbar.make(view, status, Snackbar.LENGTH_LONG).show()
        })

        _backToFriendListBtn.setOnClickListener {
            findNavController().navigate(R.id.action_searchPersonFragment_to_friendsListFragment)
        }
    }

}