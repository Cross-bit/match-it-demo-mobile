package com.example.matchit.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.matchit.R
import com.example.matchit.data.network.NetworkLiveData
import com.example.matchit.databinding.ActivityMainBinding
import com.example.matchit.ui.common.setupDoubleBackPress
import com.example.matchit.ui.launch.LaunchActivity
import com.example.matchit.ui.matchingSession.SessionActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var _binding: ActivityMainBinding
    private val binding get() = _binding!!

    private lateinit var navController: NavController
    private lateinit var bottomNavigation: BottomNavigationView

    private val mainActivityViewModel: MainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        bottomNavigation = _binding.bottomNavigation

        bottomNavigation.setOnItemSelectedListener { item ->
            navController.navigate(
                item.itemId,
                null,
                NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setPopUpTo(
                        navController.graph.startDestinationId,
                        inclusive = false,
                        saveState = true
                    )
                    .build()
            )
            true
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.main_nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        NavigationUI.setupWithNavController(bottomNavigation, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.userPreferencesFragment) {
                bottomNavigation.visibility = View.GONE
            }
            else if (destination.id == R.id.chatWindowFragment){
                bottomNavigation.visibility = View.GONE
            }
            else {
                bottomNavigation.visibility = View.VISIBLE
            }
        }

        requestNotificationPermission()

        val networkLiveData = NetworkLiveData(this)
        networkLiveData.observe(this) { isConnected ->
            if (!isConnected) {
                val intent = Intent(this, LaunchActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                this.finish()
            }
        }

        initiate()

        enforceDayModeOnly()

        initBackPressBehaviour()
    }

    private fun initBackPressBehaviour() {
        setupDoubleBackPress(
            messageRes = R.string.exit_app_on_double_backpress_msg,
            onFirstPress = {
                if (navController.currentDestination?.id != R.id.activitiesDashboardFragment) {
                    bottomNavigation.selectedItemId = R.id.activitiesDashboardFragment
                }
            }
        ) {
            this@MainActivity.finish()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun initiate() {
        mainActivityViewModel.userName.observe(this, Observer {
            Toast.makeText(
                this,
                it,
                Toast.LENGTH_LONG
            ).show()
        })

        mainActivityViewModel.userLogout.observe(this) {
            val intent = Intent(this, LaunchActivity::class.java)
            startActivity(intent)
            this.finish()
        }

        mainActivityViewModel.newFriendRequest.observe(this) {
            bottomNavigation.selectedItemId = R.id.friendsListFragment // we first must set the proper bottom navigation card...
            navController.navigate(R.id.action_friendsListFragment_to_friendRequestsFragment)
        }

        mainActivityViewModel.safeDestroy.observe(this) {
            super.onDestroy()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainActivityViewModel.newSessionInvitationEvent.collect {
                    val intent = Intent(this@MainActivity, SessionActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.top_menu_logout -> {
                mainActivityViewModel.logOut()
                true
            }
            R.id.top_menu_preferences -> {
                navController.navigate(R.id.userPreferencesFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun enforceDayModeOnly() { }

    override fun onDestroy() {
        mainActivityViewModel.cleanUpSessionConnection();
        super.onDestroy()
    }

    /**
     * We need user to allow notifications for our app (so anything related to sessions can work...)
     */
    private fun requestNotificationPermission() {

        /**
         * It turned out that this is rather costly operation (at least on ADVs...)
         * and it was causing main thread to be blocked => error
         * so we offload it to IO thread... (this could cause some issues with late FCM settings permissions settings...
         * but we are willing to risk it....)
         */

        CoroutineScope(Dispatchers.IO).launch {

            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU){
                val hasPermission = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if(!hasPermission){
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        0
                    )
                }
            }
        }
    }
}