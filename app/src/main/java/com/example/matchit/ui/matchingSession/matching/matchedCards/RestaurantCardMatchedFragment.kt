package com.example.matchit.ui.matchingSession.matching.matchedCards

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.matchit.data.model.session.MatchedItemDTO
import com.example.matchit.data.model.session.MovieCardData
import com.example.matchit.data.model.session.RestaurantCardData
import com.example.matchit.databinding.FragmentCardMatchedRestaurantBinding
import com.example.matchit.ui.MainActivity
import com.example.matchit.ui.matchingSession.matching.cards.movies.MovieCardHandler
import com.example.matchit.ui.matchingSession.matching.cards.restaurants.RestaurantCardHandler
import com.example.matchit.ui.matchingSession.matching.cards.restaurants.RestaurantNavigator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class RestaurantCardMatchedFragment : Fragment() {

    private var _binding: FragmentCardMatchedRestaurantBinding? = null
    private val binding get() = _binding!!

    private var _closeBtn: Button? = null
    private var _konfetti: KonfettiView? = null
    private lateinit var  _matchTitle: CardView

    private val _matchedCardViewModel: CardMatchedViewModel by viewModels<CardMatchedViewModel>()

    private val restaurantNavigator: RestaurantNavigator = RestaurantNavigator(this)
    private val restaurantCardHandler = RestaurantCardHandler(this, restaurantNavigator)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCardMatchedRestaurantBinding.inflate(inflater, container, false)
        return binding.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialiseBindings()
        initBackPressBehaviour()
        launchParty()
    }

    private fun initBackPressBehaviour() {
        val backPressCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {  }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback)
    }

    private fun initialiseBindings() {

        _konfetti = binding.konfettiView
        _closeBtn = binding.closeBtn
        _matchTitle = binding.matchedViewTitle

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                _matchedCardViewModel.matchedItems.collect { items: List<MatchedItemDTO> ->
                    binding.matchedSummaryView.bind(
                        items = items
                    ) { it, view ->
                        restaurantCardHandler.openCardDetail(it.cardData as RestaurantCardData, view)
                    }
                }
            }
        }

        _matchTitle.setOnClickListener() {
            launchParty()
        }

        _closeBtn?.setOnClickListener() {
            finalizeMatching()
        }
    }

    private fun finalizeMatching() {
        val intent = Intent(this.context, MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun launchParty()
    {
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
            position = Position.Relative(0.5, 0.3)
        )
        _konfetti?.start(party)
    }


}