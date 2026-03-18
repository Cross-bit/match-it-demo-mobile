package com.example.matchit.ui.matchingSession.matching.cards.movies

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import com.example.matchit.ui.matchingSession.matching.cards.restaurants.PlaceTypeConfig
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class MoviesChipListManager(
    private val ctx: Context,
    private val chipGroupTypes: ChipGroup
    ) {

        fun clearChips() {
            chipGroupTypes.removeAllViews()
        }

        // Create a base styled chip
        private fun createChip(
            text: String,
            bgColorHex: String,
            textColorHex: String
        ): Chip {

            return Chip(ctx).apply {
                id = View.generateViewId()
                this.text = text
                isClickable = false
                isCheckable = false

                // Background
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(bgColorHex))

                // Text color
                setTextColor(Color.parseColor(textColorHex))

                chipCornerRadius = 40f
                chipStrokeWidth = 0f
                chipStrokeColor = ColorStateList.valueOf(Color.TRANSPARENT)


                textSize = 17f
                setPadding(24, 12, 24, 12)
            }
        }

    fun appendGenreChips(allTypes: List<String>) {

        val filtered = allTypes
            .filterNot { PlaceTypeConfig.ignoredTypes.contains(it) }
            .map { type ->
                PlaceTypeConfig.map[type]
                    ?: PlaceTypeConfig.default.copy(label = type)
            }
            .sortedBy { it.priority }
            .take(3)

        filtered.forEach { meta ->
            val chip = createChip(
                text = (meta.emoji?.let { "$it " } ?: "") + meta.label,
                bgColorHex = "#7D4CFF",
                textColorHex = "#FFFFFF"
            )
            chipGroupTypes.addView(chip)
        }
    }

}
