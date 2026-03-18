package com.example.matchit.ui.matchingSession.initialisation.movies

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import com.example.matchit.R
import com.example.matchit.data.model.session.MovieCardData
import com.example.matchit.databinding.FragmentInitVotingMoviesBinding
import com.example.matchit.ui.MainActivity
import com.example.matchit.ui.common.setupDoubleBackPress
import com.example.matchit.ui.matchingSession.matching.SessionMatchingFragmentDirections
import com.example.matchit.ui.matchingSession.matching.cards.movies.MovieCardHandler
import com.example.matchit.ui.matchingSession.matching.cards.movies.MovieCardsAdapter
import com.example.matchit.ui.matchingSession.matching.cards.movies.MovieDetailFragment
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.Duration
import com.yuyakaido.android.cardstackview.StackFrom
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting
import com.yuyakaido.android.cardstackview.SwipeableMethod
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@AndroidEntryPoint
class InitVotingMoviesFragment : Fragment(), CardStackListener {

    private var _binding: FragmentInitVotingMoviesBinding? = null
    private val binding get() = _binding!!
    private val _viewModel: InitVotingMoviesViewModel by viewModels<InitVotingMoviesViewModel>()

    private var _cardsLoadingView: LinearLayout? = null

    private var rewindBtnMenuItem: MenuItem? = null

    /**
     * Cards swiping
     */
    private lateinit var cardStackView: CardStackView
    private lateinit var movieCardsAdapter: MovieCardsAdapter

    private var cardStackManager: CardStackLayoutManager? = null

    private val movieCardHandler = MovieCardHandler(this)

    companion object {
        fun newInstance() = InitVotingMoviesFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInitVotingMoviesBinding.inflate(inflater, container, false)
        return binding.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        movieCardsAdapter = MovieCardsAdapter(::onMovieItemClicked) //ArrayList(emptyList<MovieCardData>()),

        binding.loadingCardsView.visibility = View.VISIBLE
        cardStackView = binding.cardStackView

        setupViewModelBinding()
        setupTopToolbar()
        setupCardStackView()
        setupButton()
        initBackPressBehaviour()
    }

    private fun initBackPressBehaviour() {
        setupDoubleBackPress(
            messageRes = R.string.close_session_message_alert
        ) { }
    }

    private fun setupViewModelBinding() {

        _cardsLoadingView = _binding?.loadingCardsView

        _viewModel.currentMatchingDeck.observe(viewLifecycleOwner) { newCards ->
            binding.loadingCardsView.visibility = View.GONE
            movieCardsAdapter.setCards(newCards)
            movieCardsAdapter.notifyDataSetChanged()
        }

        _viewModel.matchingEndResult.observe(viewLifecycleOwner) {
            findNavController().navigate(R.id.action_initVotingMoviesFragment_to_initVotingCompletedFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            _viewModel.sessionVoteUpdateEvent.collectLatest {
            childFragmentManager.findFragmentById(R.id.card_detail_container)
                ?.let {
                    childFragmentManager.beginTransaction()
                        .remove(it)
                        .commit()
                }
            }
        }
    }

    private fun setupTopToolbar() {
        val toolbar = binding.mainToolbar

        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

        val menuHost = (requireActivity() as MenuHost)

        menuHost.addMenuProvider(object : MenuProvider {

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.matching_top_menu, menu)
                rewindBtnMenuItem = menu.findItem(R.id.rewind_card_option);

                viewLifecycleOwner.lifecycleScope.launch {
                    menu.findItem(R.id.session_parameters)?.isVisible = false
                    menu.findItem(R.id.chat_option)?.isVisible = false
                }
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {


                return when (item.itemId) {

                    R.id.rewind_card_option -> {

                        val setting = SwipeAnimationSetting.Builder()
                            .setDirection(Direction.Right)
                            .setDuration(Duration.Fast.duration)
                            .setInterpolator(AccelerateInterpolator())
                            .build()
                        cardStackManager!!.setSwipeAnimationSetting(setting)
                        cardStackView.rewind()

                        true
                    }

                    R.id.exit_session_option -> {
                        closeInitSessionDialog()
                        true
                    }


                    else -> true
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    fun onMovieItemClicked(position: Int, item: MovieCardData, sharedView: View) {
        try {
            movieCardHandler.openCardDetail( item, sharedView)

            movieCardsAdapter = MovieCardsAdapter(
                //ArrayList(emptyList<MovieCardData>()),
                { _, item, cardView -> movieCardHandler.openCardDetail(item, cardView) })

        } catch (e: Exception) {
            Log.e("error", "Crash on fragment creation", e)
        }
    }

    private fun closeInitSessionDialog() {
        val dialog = AlertDialog.Builder(this.context)
        dialog.setTitle(R.string.alert_message_title_0)
        dialog.setMessage(R.string.closing_init_session_matching)

        dialog.setPositiveButton(getString(R.string.dialog_accept_text)) { dialog, which ->
            exitMatchingSessionActivity()
            dialog.dismiss()
        }

        // move to resources
        dialog.setNegativeButton(getString(R.string.dialog_reject_text)) { dialog, which -> dialog.dismiss(); }

        dialog.show()
    }

    private fun exitMatchingSessionActivity() {
        val intent = Intent(activity, MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    /*
    *
    * Cards Swiping hooks
    */

    override fun onCardDragging(direction: Direction, ratio: Float) {

        Log.d("CardStackView", "onCardDragging: d = ${direction.name}, r = $ratio")

        val top = cardStackManager?.topView ?: return

        val like = top.findViewById<ImageView>(R.id.overlay_like)
        val dislike = top.findViewById<ImageView>(R.id.overlay_dislike)
        val neutral = top.findViewById<ImageView>(R.id.overlay_neutral)

        like.alpha = 0f
        dislike.alpha = 0f
        neutral.alpha = 0f

        val eased = min(1f, ratio * 2.2f)

        when (direction) {

            Direction.Right -> {
                like.alpha = eased
            }

            Direction.Left -> {
                dislike.alpha = eased
            }

            Direction.Top,
            Direction.Bottom -> {
                neutral.alpha = eased
            }

            else -> Unit
        }
    }

    override fun onCardSwiped(direction: Direction?) {
        Log.d("CardStackView", "onCardSwiped: p = ${cardStackManager!!.topPosition}, d = $direction")

        // top position starts from 1, for any cases...
        val cardIndex = max(cardStackManager!!.topPosition - 1, 0)

        when(direction) {
            Direction.Left -> _viewModel.updateUserVoting(cardIndex, -1)
            Direction.Right -> _viewModel.updateUserVoting(cardIndex, 1)
            Direction.Top -> _viewModel.updateUserVoting(cardIndex, 0)
            Direction.Bottom -> _viewModel.updateUserVoting(cardIndex, 0)
            else -> {

            }
        }
    }

    override fun onCardRewound() {
        _viewModel.removeLastUserVoting()

    }
    override fun onCardCanceled() {

        cardStackManager!!.topView.findViewById<ImageView>(R.id.overlay_like)?.alpha = 0f
        cardStackManager!!.topView.findViewById<ImageView>(R.id.overlay_neutral)?.alpha = 0f
        cardStackManager!!.topView.findViewById<ImageView>(R.id.overlay_dislike)?.alpha = 0f

    }
    override fun onCardAppeared(view: View, position: Int) {
        view.findViewById<ImageView>(R.id.overlay_like)?.alpha = 0f
        view.findViewById<ImageView>(R.id.overlay_dislike)?.alpha = 0f
        view.findViewById<ImageView>(R.id.overlay_neutral)?.alpha = 0f

    }
    override fun onCardDisappeared(view: View, position: Int) {

        view.findViewById<ImageView>(R.id.overlay_like)?.alpha = 0f
        view.findViewById<ImageView>(R.id.overlay_dislike)?.alpha = 0f
        view.findViewById<ImageView>(R.id.overlay_neutral)?.alpha = 0f

    }

    private fun setupCardStackView() {

        cardStackManager = CardStackLayoutManager(requireContext(), this)

        cardStackManager!!.setStackFrom(StackFrom.None)
        cardStackManager!!.setVisibleCount(3)
        cardStackManager!!.setTranslationInterval(8.0f)
        cardStackManager!!.setScaleInterval(0.95f)
        cardStackManager!!.setSwipeThreshold(0.3f)
        cardStackManager!!.setMaxDegree(20.0f)
        cardStackManager!!.setDirections(Direction.HORIZONTAL + Direction.Top)
        cardStackManager!!.setCanScrollHorizontal(true)
        cardStackManager!!.setCanScrollVertical(true)
        cardStackManager!!.setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
        cardStackManager!!.setOverlayInterpolator(LinearInterpolator())

        cardStackView.layoutManager = cardStackManager
        cardStackView.adapter = movieCardsAdapter
        cardStackView.itemAnimator.apply {
            if (this is DefaultItemAnimator) {
                supportsChangeAnimations = false
            }
        }
    }

    private fun setupButton() {

        val skipBtn =  binding.skipButton
        skipBtn.setOnClickListener {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Left)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(AccelerateInterpolator())
                .build()
            cardStackManager!!.setSwipeAnimationSetting(setting)
            cardStackView.swipe()
        }

        val hmBtn = binding.hmButton
        hmBtn.setOnClickListener {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Top)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(DecelerateInterpolator())
                .build()
            cardStackManager!!.setSwipeAnimationSetting(setting)
            cardStackView.swipe()
        }

        val likeBtn = binding.likeButton

        likeBtn.setOnClickListener {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Right)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(AccelerateInterpolator())
                .build()
            cardStackManager!!.setSwipeAnimationSetting(setting)
            cardStackView.swipe()
        }
    }



}