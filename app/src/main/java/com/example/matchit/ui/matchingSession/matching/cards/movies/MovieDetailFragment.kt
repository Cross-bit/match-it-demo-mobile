package com.example.matchit.ui.matchingSession.matching.cards.movies

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.matchit.R
import com.example.matchit.data.model.session.MovieCardData
import com.example.matchit.data.model.session.WatchProviderData
import com.example.matchit.databinding.FragmentMovieDetailBinding
import com.example.matchit.ui.matchingSession.matching.cards.restaurants.RestaurantImageCarouselAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.min

@AndroidEntryPoint
class MovieDetailFragment : Fragment() {

    companion object {
        const val ARG_ITEM = "item"

        fun newInstance(item: MovieCardData) = MovieDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_ITEM, item)
            }
        }
    }

    private var _binding: FragmentMovieDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var carouselAdapter: RestaurantImageCarouselAdapter
    private lateinit var carouseRecyclerView: RecyclerView
    private lateinit var titleView: TextView

    private lateinit var youTubePlayerView: YouTubePlayerView
    private var youTubePlayer: YouTubePlayer? = null

    private var cardData: MovieCardData? = null

    private val viewModel: MovieDetailViewModel by viewModels()

    // providers
    private lateinit var movieWatchProvidersContainer: LinearLayout
    private lateinit var movieAvailableFromTitle: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_ITEM, MovieCardData::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_ITEM)
        }

        titleView = binding.cardTitle
        titleView.text = cardData?.title ?: "Missing movie name."

        if (cardData?.actors.isNullOrEmpty()) {
            binding.movieActorsTitle.visibility = View.GONE
            binding.movieActorsList.visibility = View.GONE
        } else {
            binding.movieActorsList.text = cardData!!.actors.joinToString(", ")

            binding.movieActorsTitle.visibility = View.VISIBLE
            binding.movieActorsList.visibility = View.VISIBLE
        }

        carouseRecyclerView = binding.carouselRecyclerView

        val movieRating = binding.movieRating
        val movieDesc = binding.movieDescription
        val movieYear = binding.movieYear

        movieRating.text = MovieFormatUtils.formatRating(cardData?.ratingTMDB)
        movieDesc.text =  cardData?.description.toString()
        movieYear.text = cardData?.year.toString()

        val chipGroup = binding.typeChipsGroup

        val chipList = MoviesChipListManager(chipGroup.context, chipGroup)
        chipList.clearChips()

        val genresMax = cardData?.genres?.subList(0, min(cardData?.genres?.size ?: 0, 3))
        genresMax?.let {chipList.appendGenreChips(it)}

        youTubePlayerView = binding.youtubePlayerView
        lifecycle.addObserver(youTubePlayerView);

        movieWatchProvidersContainer = binding.movieWatchProvidersContainer
        movieAvailableFromTitle = binding.movieAvailableFromTitle

        cardData?.watchProviders?.let{ bindWatchProviders(it) }

        setupCardPanelOpen()

        youTubePlayerView.viewTreeObserver.addOnGlobalLayoutListener {
            Log.d("YT", "player size = ${youTubePlayerView.width}x${youTubePlayerView.height}")
        }
    }

    private fun setupCardPanelOpen() {
        val cardView = binding.movieDetailView
        val behavior = BottomSheetBehavior.from(cardView)

        cardView.translationY = resources.displayMetrics.heightPixels.toFloat()

        behavior.isDraggable = false
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED

        cardView.post {
            cardView.animate()
                .translationY(0f)
                .setDuration(150)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    behavior.isDraggable = true
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED

                   // startYoutubePlayer()
                }
                .start()
        }

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {

                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    startYoutubePlayer()
                }

                if (newState == BottomSheetBehavior.STATE_COLLAPSED ||
                    newState == BottomSheetBehavior.STATE_HIDDEN) {

                    stopYoutubePlayer()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

    fun bindWatchProviders(
        providers: List<WatchProviderData>
    ) {
        movieWatchProvidersContainer.removeAllViews()

        if (providers.isEmpty()) {
            movieWatchProvidersContainer.visibility = View.GONE
            movieAvailableFromTitle.visibility = View.GONE
            return
        }

        movieWatchProvidersContainer.visibility = View.VISIBLE
        movieAvailableFromTitle.visibility = View.VISIBLE

        providers.forEach { provider ->
            val view = LayoutInflater.from(requireContext())
                .inflate(R.layout.view_watch_provider, movieWatchProvidersContainer, false)

            val logo = view.findViewById<ImageView>(R.id.provider_logo)
            val type = view.findViewById<TextView>(R.id.provider_type)

            // logo
            if (provider.logo != null) {
                Glide.with(view)
                    .load(provider.logo)
                    .into(logo)
            } else {
                logo.visibility = View.GONE
            }

            // type (Flat / Rent / Buy)
            type.text = when (provider.type) {
                "flatrate" -> "Included"
                "rent" -> "Rent"
                "buy" -> "Buy"
                else -> provider.type
            }

            movieWatchProvidersContainer.addView(view)
        }
    }

    private var isPlayerInitialized = false

    private fun startYoutubePlayer() {
        if (isPlayerInitialized) return
        val url = cardData?.trailerUrl ?: return
        val key = extractYoutubeKey(url)
        if (key.isBlank()) return

        isPlayerInitialized = true

        youTubePlayerView.postDelayed({

            youTubePlayerView.getYouTubePlayerWhenReady(
                object : YouTubePlayerCallback {
                    override fun onYouTubePlayer(player: YouTubePlayer) {
                        youTubePlayer = player
                        player.cueVideo(key, 0f)
                    }
                }
            )
        }, 500)
    }

    private fun stopYoutubePlayer() {
        youTubePlayer?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        youTubePlayerView.release()
        _binding = null
    }

    private fun extractYoutubeKey(url: String): String {
        return Uri.parse(url).getQueryParameter("v") ?: ""
    }
}
