package com.example.matchit.ui.launch

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.net.NetworkCapabilities
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.matchit.R
import com.example.matchit.data.network.NetworkLiveData
import com.example.matchit.databinding.ActivityLaunchBinding
import com.example.matchit.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LaunchActivity : AppCompatActivity() {

    private lateinit var _binding: ActivityLaunchBinding
    private lateinit var _navController: NavController

    private val launchViewModel: LaunchViewModel by viewModels<LaunchViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityLaunchBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        _navController = Navigation.findNavController(this, R.id.launch_hosting_fragment)

        enforceDayModeOnly();

        if (!checkInternetConnection(this)) { // for the start
            _navController.navigate(R.id.noInternetConnection)
        }

        val networkLiveData = NetworkLiveData(this)
        networkLiveData.observe(this) { isConnected ->
            if (isConnected) {
                // standard initialisation flow
                initApplication()
            } else {
                _navController.navigate(R.id.noInternetConnection)
            }
        }
    }

    private fun checkInternetConnection(context: Context) : Boolean
    {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        } else {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    private fun initApplication() {
        authenticateUser()
    }

    private fun authenticateUser()
    {
        launchViewModel.authenticationResult.observe(this)
        { isAuthenticated ->
            if (isAuthenticated) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                this.finish()
            }
            else { // Authentication with current user tokens failed => we redirect to login fragment
                _navController.navigate(R.id.loginFragment)
            }
        }

        launchViewModel.tryAuthenticateUser()
    }

    private fun enforceDayModeOnly()
    {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }



}