package com.example.matchit.ui.matchingSession

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.matchit.R
import com.example.matchit.data.network.NetworkLiveData
import com.example.matchit.databinding.ActivitySessionBinding
import com.example.matchit.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class SessionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionBinding
    private val _sessionActivityViewModel: SessionViewModel by viewModels<SessionViewModel>()

    private lateinit var _navController: NavController

    private var isFirstObservation = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.session_nav_host_fragment) as NavHostFragment
        _navController = navHostFragment.navController

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                _sessionActivityViewModel.openInviteConfirmEvent.collect {
                    _navController.navigate(R.id.action_sessionInviteFragment_to_sessionInviteConfirmFragment)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                _sessionActivityViewModel.closeSessionActivityEvent.collect {
                    Toast.makeText(baseContext, it, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }


        _sessionActivityViewModel.sessionIsInitialisedEvent.observe(this) { state ->
            state.error?.let {
                Toast.makeText(this.baseContext, R.string.invitation_message, Toast.LENGTH_LONG).show()
            } ?: run {
                // If user did not perform initialisation phase, we redirect him to do so.
                if (state.result == ActivityOpenResultState.ResultState.NOT_INITIALISED) {
                    _navController.navigate(R.id.initVotingAlert)
                }
            }
        }

        val networkLiveData = NetworkLiveData(this)

        networkLiveData.observe(this) { isConnected ->
            if (isFirstObservation) {
                isFirstObservation = false
                return@observe
            }

            _sessionActivityViewModel.userNetworkStateChanged(isConnected)
        }
    }

    private fun navigateMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStop() {
        _sessionActivityViewModel.clearOutSession()
        
        super.onStop()
    }
}