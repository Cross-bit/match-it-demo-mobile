package com.example.matchit.ui.matchingSession.matching

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import com.example.matchit.R
import com.example.matchit.data.model.session.MovieCardData
import com.example.matchit.data.model.session.RestaurantCardData
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.databinding.FragmentSessionMatchingBinding
import com.example.matchit.ui.MainActivity
import com.example.matchit.ui.common.setupDoubleBackPress
import com.example.matchit.ui.matchingSession.common.BottomSheetFragment
import com.example.matchit.ui.matchingSession.creation.invitation.parametersSettings.RestaurantParametersFragment
import com.example.matchit.ui.matchingSession.matching.cards.BaseCardsAdapter
import com.example.matchit.ui.matchingSession.matching.cards.movies.MovieCardHandler
import com.example.matchit.ui.matchingSession.matching.cards.movies.MovieCardsAdapter
import com.example.matchit.ui.matchingSession.matching.cards.restaurants.RestaurantCardHandler
import com.example.matchit.ui.matchingSession.matching.cards.restaurants.RestaurantCardsAdapter
import com.example.matchit.ui.matchingSession.matching.cards.restaurants.RestaurantNavigator
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.snackbar.Snackbar
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
class SessionMatchingFragment : Fragment(), CardStackListener  {

    private var _binding: FragmentSessionMatchingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SessionMatchingViewModel by viewModels<SessionMatchingViewModel>()

    private lateinit var _loadingDataText: TextView;

    private var _cardsLoadingView: LinearLayout? = null

    /**
     * Cards swiping
     */
    private lateinit var cardStackView: CardStackView
    private lateinit var movieCardsAdapter: MovieCardsAdapter
    private lateinit var restaurantCardsAdapter: RestaurantCardsAdapter

    private var cardStackManager: CardStackLayoutManager? = null
    private var rewindBtnMenuItem: MenuItem? = null

    private val restaurantNavigator: RestaurantNavigator = RestaurantNavigator(this)
    private val restaurantCardHandler = RestaurantCardHandler(this, restaurantNavigator)

    private val movieCardHandler = MovieCardHandler(this)

    private var badge: BadgeDrawable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("FRAGMENT_LIFECYCLE", "onCreateView VIEW CREATED -> ${this.hashCode()}")
        _binding = FragmentSessionMatchingBinding.inflate(inflater, container, false)
        return binding.root;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("FRAGMENT_LIFECYCLE", "SessionMatchingFragment CREATED -> ${this.hashCode()}")
    }

    override fun onDestroy() {
        Log.d("FRAGMENT_LIFECYCLE", "SessionMatchingFragment DESTROYED -> ${this.hashCode()}")
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        movieCardsAdapter = MovieCardsAdapter(
        { _, item, cardView -> movieCardHandler.openCardDetail( item, cardView) })

        restaurantCardsAdapter = RestaurantCardsAdapter(
        requireContext(),
        { _, item, cardView -> restaurantCardHandler.openCardDetail(item, cardView)},
        { _, placePhoto -> restaurantCardHandler.showPhotoAttribution(placePhoto) },
        { _, placePhoto -> restaurantCardHandler.openGoogleMapsLink(placePhoto) })

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
        ) {
            viewModel.disconnectFromSession()
        }
    }

    private fun setupViewModelBinding() {

        _cardsLoadingView = binding.loadingCardsView
        _loadingDataText = binding.loadingDataText

        _loadingDataText.text =
            getString(R.string.loading_message_0) // at the beginning we want the text to be just loading

        viewModel.currentMatchingDeck.observe(viewLifecycleOwner) { newCards ->
            rewindBtnMenuItem?.setEnabled(true);

            updateCards(
                cards = newCards.filterIsInstance<MovieCardData>(),
                adapter = movieCardsAdapter
            )
            updateCards(
                cards = newCards.filterIsInstance<RestaurantCardData>(),
                adapter = restaurantCardsAdapter
            )
        }

        viewModel.showProgressBar.observe(viewLifecycleOwner) { showProgressBar ->
            _cardsLoadingView?.visibility = if (showProgressBar) View.VISIBLE else View.GONE
        }

        // --> NAVIGATE AFTER MATCH

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionEndedWithMatchEvent.collectLatest { matchResult ->
                if (matchResult.error != null) {
                    Toast.makeText(requireContext(), matchResult.error, Toast.LENGTH_SHORT).show()
                    viewModel.disconnectFromSession()
                    return@collectLatest
                }

                if (findNavController().currentDestination?.id != R.id.sessionMatchingFragment) {
                    return@collectLatest
                }

                when (matchResult.sessionType) {
                    SessionType.MOVIE -> {
                        findNavController().navigate(
                            R.id.action_sessionCardsFragment_to_cardMatchedFragment
                        )
                    }
                    SessionType.RESTAURANT -> {
                        findNavController().navigate(
                            R.id.action_sessionMatchingFragment_to_restaurantCardMatchedFragment
                        )
                    }
                    else -> {}
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userDisconnectedEvent.collectLatest {
                exitMatchingSessionActivity()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentDeckFinishedEvent.collectLatest {
                _loadingDataText.text = getString(R.string.waiting_for_other_members_message)
                rewindBtnMenuItem?.setEnabled(false);
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionTerminatedEvent.collectLatest { terminationMessageId ->
                Toast.makeText(
                    context,
                    getString(terminationMessageId),
                    Toast.LENGTH_LONG
                ).show()
                
                exitMatchingSessionActivity();
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showChatBadge.collectLatest { show ->
                badge?.isVisible = show
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.unreadChatMessagesInfoEvent.collectLatest { messageId ->
                Snackbar.make(binding.root, messageId, Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.clearBadge()
    }

    fun <T> updateCards(
        cards: List<T>,
        adapter: BaseCardsAdapter<T, *>
    )
    {
        if (cards.isEmpty()) return

        if (cardStackView.adapter != adapter) {
            cardStackView.adapter = adapter
        }

        adapter.setCards(cards)
        adapter.notifyDataSetChanged()

        // cards
        val index = viewModel.currentCardIndex
        cardStackView.post {
            if (index in cards.indices) {
                cardStackManager?.scrollToPosition(index)
            }
        }
    }

    /**
     *  Specific cards swiping methods
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

    override fun onCardSwiped(direction: Direction) {
        Log.d("CardStackView", "onCardSwiped: p = ${cardStackManager!!.topPosition}, d = $direction")

        // IMPORTANT -- tracks which card is on top of the deck
        viewModel.updateTopCardIndex(cardStackManager!!.topPosition)

        // top position starts from 1, for any cases...
        val cardIndex = max(cardStackManager!!.topPosition - 1, 0)

        when (direction) {
            Direction.Left -> viewModel.updateUserVoting(cardIndex, -1)
            Direction.Right -> viewModel.updateUserVoting(cardIndex, 1)
            Direction.Top -> viewModel.updateUserVoting(cardIndex, 0)
            Direction.Bottom -> viewModel.updateUserVoting(cardIndex, 0)
        }
    }

    override fun onCardRewound() {
        viewModel.updateTopCardIndex(cardStackManager!!.topPosition)
        viewModel.removeLastUserVoting()
    }

    override fun onCardCanceled() {
        Log.d("CardStackView", "onCardCanceled: ${cardStackManager!!.topPosition}")

        // reset the swipe color overlay
        cardStackManager!!.topView.findViewById<ImageView>(R.id.overlay_like)?.alpha = 0f
        cardStackManager!!.topView.findViewById<ImageView>(R.id.overlay_neutral)?.alpha = 0f
        cardStackManager!!.topView.findViewById<ImageView>(R.id.overlay_dislike)?.alpha = 0f
    }

    override fun onCardAppeared(view: View, position: Int) {
        val textView = view.findViewById<TextView>(R.id.card_title)
        Log.d("CardStackView", "onCardAppeared: ($position) ${textView.text}")

        // reset the swipe color overlay
        view.findViewById<ImageView>(R.id.overlay_like)?.alpha = 0f
        view.findViewById<ImageView>(R.id.overlay_dislike)?.alpha = 0f
        view.findViewById<ImageView>(R.id.overlay_neutral)?.alpha = 0f
    }

    override fun onCardDisappeared(view: View, position: Int) {
        val textView = view.findViewById<TextView>(R.id.card_title)
        Log.d("CardStackView", "onCardDisappeared: ($position) ${textView.text}")
        // reset the swipe color overlay
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

        val skipBtn = binding.skipButton

        skipBtn.setOnClickListener {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Left)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(AccelerateInterpolator())
                .build()
            cardStackManager?.setSwipeAnimationSetting(setting)
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.openSessionParametersEvent.collectLatest { sessionType ->
                when(sessionType) {
                    SessionType.MOVIE -> Unit // We don't have any params for MOVIES
                    SessionType.RESTAURANT -> {
                        val bottomSheet = BottomSheetFragment.newInstance(RestaurantParametersFragment::class.java)
                        bottomSheet.show(childFragmentManager, bottomSheet.tag)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionVoteUpdateEvent.collectLatest {
                childFragmentManager.findFragmentById(R.id.card_detail_container)
                    ?.let {
                        childFragmentManager.beginTransaction()
                            .remove(it)
                            .commit()
                    }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notifyMemberOnlineStatusChanged.collectLatest { onlineStatusUpdate ->
                val displayName = onlineStatusUpdate.username
                var message = getString(R.string.session_connection_lost_error, displayName)
                if (onlineStatusUpdate.isOnline) {
                    message = getString(R.string.session_connection_reconnect_info, displayName)
                }

                Toast.makeText(
                    context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    @ExperimentalBadgeUtils
    private fun setupTopToolbar() {
        val toolbar = binding.mainToolbar
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

        val menuHost = (requireActivity() as MenuHost)

        badge = BadgeDrawable.create(requireContext()).apply {
            isVisible = false
            backgroundColor = Color.RED
        }

        BadgeUtils.attachBadgeDrawable(
            badge!!,
            toolbar,
            R.id.chat_option
        )

        menuHost.addMenuProvider(object : MenuProvider {

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.matching_top_menu, menu)
                rewindBtnMenuItem = menu.findItem(R.id.rewind_card_option);

                viewLifecycleOwner.lifecycleScope.launch {

                    menu.findItem(R.id.session_parameters)?.isVisible = false
                    viewModel.showParametersOption.collectLatest { visibility ->
                        menu.findItem(R.id.session_parameters)?.isVisible = visibility
                    }
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

                    R.id.session_parameters -> {
                        viewModel.openSessionParametersPanel()
                        true
                    }

                    R.id.exit_session_option -> {

                        // we make sure user really wants to quit...
                        disconnectFromSessionDialog()

                        true
                    }

                    R.id.chat_option -> {

                        viewModel.clearBadge()

                        // we assure that user really wants to quit...

                        val sessionUUID = viewModel.getCurrentSessionUUID()

                        val action =
                            SessionMatchingFragmentDirections
                                .actionSessionMatchingFragmentToChatWindowFragment(sessionUUID)

                        findNavController().navigate(action)

                        true
                    }

                    else -> true
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }


    private fun disconnectFromSessionDialog() {
        val dialog = AlertDialog.Builder(this.context);

        dialog.setTitle(R.string.alert_message_title_0);
        dialog.setMessage(R.string.user_exit_session_alert_message);

        dialog.setPositiveButton("Yes") { dialog, which ->
            viewModel.disconnectFromSession()
            dialog.dismiss();
        }

        // move to resources
        dialog.setNegativeButton("No") { dialog, which -> dialog.dismiss(); }

        dialog.show()
    }

    private fun exitMatchingSessionActivity() {
        val intent = Intent(activity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onResume() {
        super.onResume()

        // NOTICE!! -- this is for the card detail view so it
        childFragmentManager.popBackStack(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }

    override fun onDestroyView() {
        Log.d("FRAGMENT_LIFECYCLE", "onDestroyView VIEW DESTROYED -> ${this.hashCode()}")
        super.onDestroyView()
        (requireActivity() as AppCompatActivity).setSupportActionBar(null)

        _binding = null
    }

}
