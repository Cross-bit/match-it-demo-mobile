package com.example.matchit.ui.matchingSession.initialisation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.matchit.databinding.FragmentInitVotingCompletedBinding
import com.example.matchit.ui.matchingSession.SessionActivity
import dagger.hilt.android.AndroidEntryPoint
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class InitVotingCompletedFragment : Fragment() {

    private var _binding: FragmentInitVotingCompletedBinding? = null
    private val binding get() = _binding!!

    private var _konfetti: KonfettiView? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentInitVotingCompletedBinding.inflate(inflater, container, false)
        return binding.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val continueBtn = binding.continueAfterInitialisationBtn;

        continueBtn.setOnClickListener {
            val intent = Intent(activity, SessionActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        _konfetti = _binding?.konfettiView

        launchParty()
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