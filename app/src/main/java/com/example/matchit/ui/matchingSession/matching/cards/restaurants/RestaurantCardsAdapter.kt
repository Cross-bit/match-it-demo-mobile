package com.example.matchit.ui.matchingSession.matching.cards.restaurants

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.matchit.R
import com.example.matchit.data.model.googlePlaces.PlacePhoto
import com.example.matchit.data.model.session.RestaurantCardData
import com.example.matchit.ui.matchingSession.matching.cards.BaseCardsAdapter
import com.google.android.material.chip.ChipGroup

class RestaurantCardsAdapter (
    private val context: Context,
    private val onItemClick: (Int, RestaurantCardData, View) -> Unit,
    private val onAttributionClick: (Int, PlacePhoto) -> Unit,
    private val onGoogleMapsLinkClick: (Int, RestaurantCardData) -> Unit
    ) : BaseCardsAdapter<RestaurantCardData, RestaurantCardsAdapter.CardViewHolder>()
{
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RestaurantCardsAdapter.CardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return CardViewHolder(inflater.inflate(R.layout.fragment_restaurant_match_card, parent, false))
    }

    override fun onBindViewHolder(holder: RestaurantCardsAdapter.CardViewHolder, position: Int) {
        val cardData = cards[position]

        holder.cardView.transitionName = "card_transition_$position"

        val bannerPhoto = if (cardData.placePhotos.isNotEmpty()) cardData.placePhotos[0] else null

        Glide.with(holder.image)
            .load(bannerPhoto?.url ?: "")
            .error(R.drawable.no_image_placeholder)
            .into(holder.image)

        holder.title.text = cardData.title

        holder.itemView.setOnClickListener {
            onItemClick(position, cardData, holder.cardView)
        }

        holder.attributionsBtn.setOnClickListener {
            bannerPhoto?.let {  //onItemClickListener.onAttributionItemClicked(position, bannerPhoto)
                onAttributionClick(position, bannerPhoto)
            }
        }

        val chipListManger = RestaurantChipListManger(context, holder.typeChips, holder.featuresChips)

        chipListManger.clearChips() // always make sure chips are empty (the view is recycled)

        chipListManger.appendTypesChips(cardData.type)
        chipListManger.appendVegetarianChip(cardData.servesVegetarian)
        chipListManger.appendTakeoutChip(cardData.takeout)
        chipListManger.appendPriceLevel(cardData.priceLevel)

        val gpsLoc = cardData.location
        holder.googleMapsBtn.setOnClickListener {
            onGoogleMapsLinkClick(position, cardData)
        }

        val rating = cardData.rating
        if (rating in 0f..5f)
            holder.googleRatings.text = rating.toString()
        else {
            holder.googleRatings.text = "-"
        }
    }

    class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.card_title)
        var image: ImageView = view.findViewById(R.id.card_image)
        val cardView: CardView = itemView.findViewById(R.id.restaurant_card_view)
        var typeChips: ChipGroup = view.findViewById(R.id.type_chips_group)
        var featuresChips: ChipGroup = view.findViewById(R.id.features_chips_group)
        val googleMapsBtn: TextView = view.findViewById(R.id.open_in_maps_btn)
        val attributionsBtn: Button = view.findViewById(R.id.attributions_btn)
        val googleRatings: TextView = view.findViewById(R.id.google_rating)
    }

}
