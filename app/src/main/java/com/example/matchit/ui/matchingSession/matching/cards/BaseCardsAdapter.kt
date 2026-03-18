package com.example.matchit.ui.matchingSession.matching.cards

import androidx.recyclerview.widget.RecyclerView

abstract class BaseCardsAdapter<T, VH : RecyclerView.ViewHolder> :
    RecyclerView.Adapter<VH>() {

    protected val cards = mutableListOf<T>()

    fun setCards(newItems: List<T>) {
        cards.clear()
        cards.addAll(newItems)
    }

    override fun getItemCount(): Int = cards.size
}