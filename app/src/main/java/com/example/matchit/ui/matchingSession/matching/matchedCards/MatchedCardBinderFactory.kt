package com.example.matchit.ui.matchingSession.matching.matchedCards

import com.bumptech.glide.Glide
import com.example.matchit.R
import com.example.matchit.data.model.session.MatchedItemDTO
import com.example.matchit.data.model.session.MovieCardData
import com.example.matchit.data.model.session.RestaurantCardData
import com.example.matchit.databinding.MatchedCardMediumBinding
import com.example.matchit.databinding.MatchedCardSmallBinding


interface MatchedCardBinder {
    fun bindMedium(
        binding: MatchedCardMediumBinding,
        item: MatchedItemDTO
    )

    fun bindSmall(
        binding: MatchedCardSmallBinding,
        item: MatchedItemDTO
    )
}

class MovieMatchedCardBinder : MatchedCardBinder {

    override fun bindMedium(
        binding: MatchedCardMediumBinding,
        item: MatchedItemDTO
    ) {
        val movie = item.cardData as MovieCardData
        binding.cardImage.transitionName = movie.cardId
        binding.cardTitle.text =
            "${movie.title} (${movie.year})"


        Glide.with(binding.cardImage)
            .load(movie.imageUrl)
            .error(R.drawable.no_image_placeholder)
            .into(binding.cardImage)
    }

    override fun bindSmall(
        binding: MatchedCardSmallBinding,
        item: MatchedItemDTO
    ) {
        val movie = item.cardData as MovieCardData
        binding.cardImage.transitionName = movie.cardId
        binding.cardTitle.text = movie.title


        Glide.with(binding.cardImage)
            .load(movie.imageUrl)
            .error(R.drawable.no_image_placeholder)
            .into(binding.cardImage)
    }
}

class RestaurantMatchedCardBinder : MatchedCardBinder {

    override fun bindMedium(
        binding: MatchedCardMediumBinding,
        item: MatchedItemDTO
    ) {
        val restaurant = item.cardData as RestaurantCardData
        binding.cardImage.transitionName = restaurant.cardId
        binding.cardTitle.text = restaurant.title

        val photoUrl = restaurant.placePhotos
            .firstOrNull()
            ?.url

        Glide.with(binding.cardImage)
            .load(photoUrl)
            .error(R.drawable.no_image_placeholder)
            .into(binding.cardImage)
    }

    override fun bindSmall(
        binding: MatchedCardSmallBinding,
        item: MatchedItemDTO
    ) {
        val restaurant = item.cardData as RestaurantCardData
        binding.cardImage.transitionName = restaurant.cardId
        binding.cardTitle.text = restaurant.title

        val photoUrl = restaurant.placePhotos
            .firstOrNull()
            ?.url

        Glide.with(binding.cardImage)
            .load(photoUrl)
            .error(R.drawable.no_image_placeholder)
            .into(binding.cardImage)
    }
}


object MatchedCardBinderFactory {

    fun get(item: MatchedItemDTO): MatchedCardBinder =
        when (item.cardData) {
            is MovieCardData -> MovieMatchedCardBinder()
            is RestaurantCardData -> RestaurantMatchedCardBinder()
            else -> error("Unsupported cardData type")
        }
}