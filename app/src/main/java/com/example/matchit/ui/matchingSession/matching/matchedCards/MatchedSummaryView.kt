package com.example.matchit.ui.matchingSession.matching.matchedCards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.example.matchit.R
import com.example.matchit.data.model.session.CardData
import com.example.matchit.data.model.session.MatchedItemDTO
import com.example.matchit.databinding.MatchedCardSmallBinding
import com.example.matchit.databinding.MatchedSummaryViewBinding

class MatchedSummaryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding =
        MatchedSummaryViewBinding.inflate(LayoutInflater.from(context), this)

    fun bindCards(
        cards: List<CardData>,
        onItemClick: (MatchedItemDTO, View) -> Unit
    ) {
        val items = cards.map { card ->
            MatchedItemDTO(
                itemId = card.cardId,
                rank = 0,
                run = 0,
                score = 0.0,
                cardData = card
            )
        }

        bind(items, onItemClick)
    }

    fun bind(
        items: List<MatchedItemDTO>,
        onItemClick: (MatchedItemDTO, View) -> Unit
    ) {
        bindMain(items.first(), onItemClick)
        bindOthers(items.drop(1), onItemClick)
    }

    private fun bindMain(item: MatchedItemDTO,
             onClick: (MatchedItemDTO, View) -> Unit
    ) {
        val cardBinding = binding.mainCard

        MatchedCardBinderFactory.get(item)
            .bindMedium(cardBinding, item)

        val cardRoot = cardBinding.cardViewContainer
        val sharedImage = cardBinding.cardImage

        setupClick(cardRoot, sharedImage, item, onClick)
    }

    private fun bindOthers(
        items: List<MatchedItemDTO>,
        onClick: (MatchedItemDTO, View) -> Unit
    ) {
        binding.otherCards.removeAllViews()

        items.take(3).forEach { item ->
            val cardView = LayoutInflater.from(context)
                .inflate(R.layout.matched_card_small, binding.otherCards, false)

            val params = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )

            cardView.layoutParams = params
            cardView.minimumHeight =
                resources.getDimensionPixelSize(R.dimen.small_card_height)

            val cardBinding = MatchedCardSmallBinding.bind(cardView)

            MatchedCardBinderFactory.get(item)
                .bindSmall(cardBinding, item)

            val cardRoot = cardBinding.cardViewContainer
            val sharedImage = cardBinding.cardImage

            setupClick(cardRoot, sharedImage, item, onClick)

            playFlipAnimation(cardView,  null)

            binding.otherCards.addView(cardView)
        }
    }

    private fun setupClick(
        cardRoot: View,
        sharedImage: View,
        item: MatchedItemDTO,
        onClick: (MatchedItemDTO, View) -> Unit
    ) {
        cardRoot.setOnClickListener {
            playFlipAnimation(cardRoot) {
                onClick(item, sharedImage)
            }
        }
    }

    private fun playFlipAnimation(
        cardView: View,
        onEnd: (() -> Unit)?
    ) {
        val scale = cardView.resources.displayMetrics.density
        cardView.cameraDistance = 8000 * scale

        cardView.animate()
            .rotationYBy(360f)
            .setDuration(350)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction {
                cardView.rotationY = 0f
                onEnd?.invoke()
            }
            .start()
    }
}
