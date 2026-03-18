package com.example.matchit.ui.matchingSession.matching.cards.movies

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.matchit.R
import com.example.matchit.data.model.session.MovieCardData
import com.example.matchit.ui.matchingSession.matching.cards.BaseCardsAdapter
import com.google.android.material.chip.ChipGroup
import kotlin.math.min

class MovieCardsAdapter(
    private val onItemClick: (Int, MovieCardData, View) -> Unit
) : BaseCardsAdapter<MovieCardData, MovieCardsAdapter.CardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return CardViewHolder(inflater.inflate(R.layout.fragment_movie_match_card, parent, false))
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {

        val card = cards[position]

        holder.name.text = "${card.title} (${card.year})"

        val chipGroup = holder.tagsView

        val chipList = MoviesChipListManager(chipGroup.context, chipGroup)
        chipList.clearChips()

        val genresMax = card.genres.subList(0, min(card.genres.size, 3))
        chipList.appendGenreChips(genresMax)

        Glide.with(holder.image)
            .load(card.imageUrl)
            .error(R.drawable.no_image_placeholder)
            .into(holder.image)

        holder.itemView.setOnClickListener { v ->
            Toast.makeText(v.context, card.title, Toast.LENGTH_SHORT).show()
            onItemClick(position, card, holder.cardView)
        }

        holder.movieRating.text = card.ratingTMDB?.let { String.format("%.1f", it) } ?: "--"
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.card_title)
        var tagsView: ChipGroup = view.findViewById(R.id.type_chips_group)
        val cardView: CardView = view.findViewById(R.id.movie_card_view)
        var image: ImageView = view.findViewById(R.id.card_image)
        var movieRating: TextView = view.findViewById(R.id.movie_rating)
    }
}