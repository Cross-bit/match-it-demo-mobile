package com.example.matchit.ui.matchingSession.matching.cards.restaurants

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.matchit.data.model.googlePlaces.PlacePhoto
import com.example.matchit.databinding.FragmentPhotoAttributionsBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class PhotoAttributionsFragment : Fragment() {

    companion object {
        const val ATTRIBUTIONS = "attributions"

        fun newInstance(item: PlacePhoto) = PhotoAttributionsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ATTRIBUTIONS, item)
            }
        }
    }

    private var _binding: FragmentPhotoAttributionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPhotoAttributionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val authorAttributions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ATTRIBUTIONS, PlacePhoto::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ATTRIBUTIONS)
        }

        val authorName = binding.imageAuthor
        val authorURI = binding.authorProfileUri
        val openURI = binding.openAuthorsUri

        authorAttributions?.let {
            authorName.text = authorAttributions.authorAttributions?.displayName ?: "unknown"
            authorURI.text = authorAttributions.authorAttributions?.uri ?: "unknown"
        }

        openURI.setOnClickListener {
            authorAttributions?.authorAttributions?.uri?.let {
                openUriInBrowser(it)
            } ?: run {
                Toast.makeText(requireContext(), "Unable to open author's URI", Toast.LENGTH_SHORT).show()
            }
        }


    }

    fun openUriInBrowser(uriString: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(uriString)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {

             Toast.makeText(requireContext(), "No browser found to open the URI", Toast.LENGTH_SHORT).show()
        }
    }

}