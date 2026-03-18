package com.example.matchit.ui.common

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

/**
 * Extension function that enables double-tap back press handling for a Fragment.
*/
fun Fragment.setupDoubleBackPress(
    @StringRes messageRes: Int,
    timeoutMs: Long = 2000,
    onFirstPress: (() -> Unit)? = null,
    onConfirmed: () -> Unit
) = setupDoubleBackPress(
    message = getString(messageRes),
    timeoutMs = timeoutMs,
    onConfirmed = onConfirmed,
    onFirstPress = onFirstPress
)

/**
 * Extension function that enables double-tap back press handling for a Fragment.
 */
fun Fragment.setupDoubleBackPress(
    message: String = "Press back again to continue...",
    timeoutMs: Long = 2000,
    onFirstPress: (() -> Unit)? = null,
    onConfirmed: () -> Unit
) {
    var pressedOnce = false

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

            if (pressedOnce) {
                onConfirmed()
                return
            }

            onFirstPress?.invoke()

            pressedOnce = true
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                pressedOnce = false
            }, timeoutMs)
        }
    }

    requireActivity().onBackPressedDispatcher.addCallback(
        viewLifecycleOwner,
        callback
    )
}

/**
 * Extension function that enables double-tap back press handling for a Fragment.
 */
fun AppCompatActivity.setupDoubleBackPress(
    @StringRes messageRes: Int,
    timeoutMs: Long = 2000,
    onFirstPress: (() -> Unit)? = null,
    onConfirmed: () -> Unit
) = setupDoubleBackPress(
    message = getString(messageRes),
    timeoutMs = timeoutMs,
    onConfirmed = onConfirmed,
    onFirstPress = onFirstPress
)

/**
 * Extension function that enables double-tap back press handling for a AppCompatActivity.
 *
 */
fun AppCompatActivity.setupDoubleBackPress(
    message: String = "Press back again to continue...",
    timeoutMs: Long = 2000,
    onFirstPress: (() -> Unit)? = null,
    onConfirmed: () -> Unit
) {
    var pressedOnce = false

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

            if (pressedOnce) {
                onConfirmed()
                return
            }

            onFirstPress?.invoke()

            pressedOnce = true
            Toast.makeText(this@setupDoubleBackPress, message, Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                pressedOnce = false
            }, timeoutMs)
        }
    }

    onBackPressedDispatcher.addCallback(this, callback)
}