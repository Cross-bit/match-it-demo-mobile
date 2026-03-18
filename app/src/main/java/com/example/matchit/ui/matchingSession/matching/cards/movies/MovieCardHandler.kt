package com.example.matchit.ui.matchingSession.matching.cards.movies

import android.view.View
import androidx.fragment.app.Fragment
import com.example.matchit.R
import com.example.matchit.data.model.session.MovieCardData

/**
* ==================================
* DESCRIPTION
* ==================================
* Handles UI actions triggered from a movie card during matching.
* Keeps navigation logic out of the adapter and the main fragment.
* Central place for movie-specific click behavior inside
* the matching session.
*
* */

    class MovieCardHandler(
        private val fragment: Fragment
    ) {
        /**
         * Opens the restaurant detail screen using a shared element transition.
         */
        fun openCardDetail(item: MovieCardData, view: View) {
            val detailFragment =
                MovieDetailFragment.newInstance(item)

            fragment.childFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .addSharedElement(view, view.transitionName)
                .replace(R.id.card_detail_container, detailFragment)
                .commit()
        }
    }