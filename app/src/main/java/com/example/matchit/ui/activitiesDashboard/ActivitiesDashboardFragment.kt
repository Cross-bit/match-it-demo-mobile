package com.example.matchit.ui.activitiesDashboard

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.matchit.R
import com.example.matchit.data.model.session.SessionType
import com.example.matchit.databinding.FragmentDashboardBinding
import com.example.matchit.ui.MainViewModel
import com.example.matchit.ui.matchingSession.SessionActivity
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ActivitiesDashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val _dashboardViewModel: ActivitiesDashboardViewModel by viewModels<ActivitiesDashboardViewModel>()
    val _mainActivityViewModel: MainViewModel by activityViewModels()

    private lateinit var _profilePicture: ImageView

    private lateinit var _moviesMatchActivityCard: androidx.cardview.widget.CardView
    private lateinit var _moviesMatchActivityCardBack: androidx.cardview.widget.CardView
    private lateinit var _moviesMatchingSessionBtn: Button

    private lateinit var flip_card_front_animator: AnimatorSet
    private lateinit var flip_card_back_animator: AnimatorSet

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialiseBindings()

        val scale = resources.displayMetrics.density
        _moviesMatchActivityCard.cameraDistance = 8000 * scale
        _moviesMatchActivityCardBack.cameraDistance = 8000 * scale

        flip_card_front_animator = AnimatorInflater.loadAnimator(context, R.animator.flip_card_front_animator) as AnimatorSet
        flip_card_back_animator = AnimatorInflater.loadAnimator(context, R.animator.flip_card_back_animator) as AnimatorSet

        _moviesMatchActivityCard.setOnClickListener {
            flip_card_front_animator.setTarget(_moviesMatchActivityCard)
            flip_card_back_animator.setTarget(_moviesMatchActivityCardBack)
            flip_card_front_animator.start()
            flip_card_back_animator.start()
            _moviesMatchActivityCard.elevation = 0f
            _moviesMatchActivityCardBack.elevation = 2f
            _moviesMatchActivityCard.isClickable = false
            _moviesMatchActivityCardBack.isClickable = true
        }

        _moviesMatchActivityCardBack.setOnClickListener {
            flip_card_front_animator.setTarget(_moviesMatchActivityCardBack)
            flip_card_back_animator.setTarget(_moviesMatchActivityCard)
            flip_card_back_animator.start()
            flip_card_front_animator.start()
            _moviesMatchActivityCardBack.isClickable = false
            _moviesMatchActivityCard.isClickable = true

            _moviesMatchActivityCard.elevation = 2f
            _moviesMatchActivityCardBack.elevation = 0f
        }

        val overflow = view.findViewById<ImageButton>(R.id.btn_overflow)

        overflow.setOnClickListener {
            val popup = PopupMenu(requireContext(), overflow)
            popup.menuInflater.inflate(R.menu.top_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.top_menu_logout -> {
                        _mainActivityViewModel.logOut()
                        true
                    }
                    R.id.top_menu_preferences -> {
                        findNavController().navigate(R.id.userPreferencesFragment)
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        }
    }

    private fun initialiseBindings() {

        _moviesMatchActivityCard = binding.movieMatchActivity
        _moviesMatchActivityCardBack = binding.movieMatchActivityBack
        _moviesMatchingSessionBtn = binding.startMoviesSessionBtn

        val restaurantMatchActivityCard = binding.restaurantMatchActivity

        _moviesMatchingSessionBtn.setOnClickListener {
            launchSessionInvite(SessionType.MOVIE)
        }

        restaurantMatchActivityCard.setOnClickListener {
            launchSessionInvite(SessionType.RESTAURANT)
        }

        initialiseProfilePic()
        initialiseUsername()
    }

    private fun launchSessionInvite(sessionType: SessionType) {
        _dashboardViewModel.openSessionInvitationCreation(sessionType);

        val intent = Intent(activity, SessionActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun initialiseUsername() {

        val title = binding.dashboardTitle
        _profilePicture = binding.profilePictureView
        val defaultUsername = getString(R.string.default_username)

        // default (než přijde user)
        title.text = getString(
            R.string.dashboard_welcome_title,
            defaultUsername
        )

        _dashboardViewModel.userName.observe(viewLifecycleOwner) { userName ->
            title.text = getString(
                R.string.dashboard_welcome_title,
                userName
            )
        }
    }

    private fun initialiseProfilePic(){
        _dashboardViewModel.updateUserProfilePic()
        _dashboardViewModel.profilePictureUrl.observe(viewLifecycleOwner) { url ->

            Glide.with(_profilePicture)
                .load(url)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(_profilePicture)
        }
    }

}