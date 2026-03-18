package com.example.matchit.ui.matchingSession.matching.cards.restaurants

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.matchit.R
import com.example.matchit.data.model.googlePlaces.PlacePhoto
import com.example.matchit.data.model.session.RestaurantCardData
import com.example.matchit.ui.matchingSession.common.BottomSheetFragment

/**
* ==================================
* DESCRIPTION
* ==================================
* Handles UI actions triggered from a restaurant card during matching.
* Keeps navigation logic out of the adapter and the main fragment.
* Central place for restaurant-specific click behavior inside the matching session.
* */

class RestaurantCardHandler(
    private val fragment: Fragment,
    private val restaurantNavigator: RestaurantNavigator
) {

    /**
     * Opens the restaurant detail screen using a shared element transition.
     */
    fun openCardDetail(item: RestaurantCardData, view: View) {
        val detailFragment = RestaurantDetailFragment.newInstance(item)

        fragment.childFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .addSharedElement(view, view.transitionName)
            .replace(R.id.card_detail_container, detailFragment)
           //.addToBackStack(null)
            .commit()
    }

    /**
     * Shows the photo attribution bottom sheet for the selected restaurant.
     */
    fun showPhotoAttribution(photo: PlacePhoto) {

        val args = Bundle().apply {
            putParcelable(PhotoAttributionsFragment.ATTRIBUTIONS, photo)
        }

        BottomSheetFragment.newInstance(
            PhotoAttributionsFragment::class.java,
            args
        ).show(fragment.childFragmentManager, "restaurant_attr")
    }

    /**
     * Shows the photo attribution bottom sheet for the selected restaurant.
     */
    fun openGoogleMapsLink(cardData: RestaurantCardData) {
        this.restaurantNavigator.openGoogleMaps(cardData)
    }
}