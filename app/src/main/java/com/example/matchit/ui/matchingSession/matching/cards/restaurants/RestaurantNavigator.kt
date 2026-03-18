package com.example.matchit.ui.matchingSession.matching.cards.restaurants

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import com.example.matchit.data.model.session.RestaurantCardData

/**
 * ==================================
 * DESCRIPTION
 * ==================================
 * Provides shared navigation actions for the restaurant domain (e.g., opening maps or external links).
 * Keeps intent/navigation logic out of adapters, handlers, and fragments.
 * Reusable from both card views and detail screens.
*/

class RestaurantNavigator(
    private val fragment: Fragment
) {

    /**
     * Tries to open google maps activity. If not installed redirects user to play store google maps page.
     */
    fun openGoogleMaps(cardData: RestaurantCardData) {
        val loc = cardData.location
        val context = fragment.requireContext()

        val uri = Uri.parse("geo:${loc.lat},${loc.long}?q=" + Uri.encode(cardData.title))
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val playStoreUri = Uri.parse("market://details?id=com.google.android.apps.maps")
            context.startActivity(Intent(Intent.ACTION_VIEW, playStoreUri))
        }
    }

}