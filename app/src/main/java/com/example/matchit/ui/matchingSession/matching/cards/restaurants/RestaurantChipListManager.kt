package com.example.matchit.ui.matchingSession.matching.cards.restaurants

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

data class PlaceTypeMeta(
    val label: String,
    val emoji: String? = null,
    val bgColor: String,
    val textColor: String = "#000000",
    val priority: Int
)

object PlaceTypeConfig {

    // Types we want to completely ignore
    val ignoredTypes = setOf(
        "store",
        "point_of_interest",
        "establishment",
        "food_store",
        "meal_delivery",
        "meal_takeaway",
        "coffee_shop",
        "food"
    )

    // Human-readable and styled tags
    val map: Map<String, PlaceTypeMeta> = mapOf(

        "pizza_restaurant" to PlaceTypeMeta(
            label = "Pizza",
            emoji = "🍕",
            bgColor = "#FFE5E5",
            textColor = "#7A0000",
            priority = 1
        ),

        "italian_restaurant" to PlaceTypeMeta(
            label = "Italian",
            emoji = "\uD83C\uDF5D",
            bgColor = "#FFEDE5",
            textColor = "#7A0000",
            priority = 1
        ),

        "sushi_restaurant" to PlaceTypeMeta(
            label = "Sushi",
            emoji = "🍣",
            bgColor = "#E5F1FF",
            textColor = "#003366",
            priority = 1
        ),

        "hamburger_restaurant" to PlaceTypeMeta(
            label = "Burger",
            emoji = "🍔",
            bgColor = "#FFF0DA",
            textColor = "#5E3500",
            priority = 1
        ),

        "cafe" to PlaceTypeMeta(
            label = "Café",
            emoji = "☕",
            bgColor = "#FFF7E6",
            textColor = "#5B3A00",
            priority = 2
        ),

        "bar" to PlaceTypeMeta(
            label = "Bar",
            emoji = "🍸",
            bgColor = "#F0E5FF",
            textColor = "#4B2E7F",
            priority = 2
        ),

        "fast_food_restaurant" to PlaceTypeMeta(
            label = "Fast food",
            emoji = null,
            bgColor = "#FFF3D9",
            textColor = "#5A4000",
            priority = 3
        ),

        // fallback
        "restaurant" to PlaceTypeMeta(
            label = "Restaurant",
            emoji = null,
            bgColor = "#EAE2FF",
            textColor = "#4A2FBD",
            priority = 10
        )
    )

    // Default fallback for unknown types
    val default = PlaceTypeMeta(
        label = "Food",
        emoji = null,
        bgColor = "#EAE2FF",
        textColor = "#4A2FBD",
        priority = 99
    )
}

class RestaurantChipListManger(
    private val ctx: Context,
    private val chipGroupTypes: ChipGroup,
    private val chipGroupFeatures: ChipGroup
) {

    fun clearChips() {
        chipGroupTypes.removeAllViews()
        chipGroupFeatures.removeAllViews()
    }

    // Create a base styled chip
    private fun createChip(text: String, bg: String, tc: String): Chip {
        return Chip(ctx).apply {
            id = View.generateViewId()
            this.text = text
            isClickable = false
            isCheckable = false

            chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(bg))
            setTextColor(Color.parseColor("#000000")) //Color.parseColor(tc)
            chipCornerRadius = 40f
            chipStrokeWidth = 0f
            chipStrokeColor = ColorStateList.valueOf(Color.TRANSPARENT)
            textSize = 17f
            setPadding(16, 8, 16, 8)
        }
    }

    // Make types readable
    private fun String.toReadable(): String {
        return this.replace("_", " ")
            .replace("restaurant", "")
            .trim()
            .replaceFirstChar { it.uppercase() }
    }

    fun appendTypesChips(allTypes: List<String>) {

        val filtered = allTypes
            .filterNot { PlaceTypeConfig.ignoredTypes.contains(it) } // remove garbage
            .map { type ->
                PlaceTypeConfig.map[type]
                    ?: PlaceTypeConfig.default.copy(label = type.toReadable())
            }
            .sortedBy { it.priority }
            .take(3)

        filtered.forEach { meta ->
            val chip = createChip(
                text = (meta.emoji?.let { "$it " } ?: "") + meta.label,
                bg = meta.bgColor,
                tc = meta.textColor
            )
            chipGroupTypes.addView(chip)
        }
    }

    // ------------------------------------------------------------
    //  FEATURES
    // ------------------------------------------------------------
    fun appendVegetarianChip(isVegetarian: Boolean) {
        if (!isVegetarian) return

        val chip = createChip(
            text = "Vegetarian 🌱",
            bg = "#E5FFE8",
            tc = "#006622"
        )

        chipGroupFeatures.addView(chip)
    }

    fun appendTakeoutChip(takeout: Boolean) {
        if (!takeout) return

        val chip = createChip(
            text = "Takeout ✅",
            bg = "#FFF1DA",
            tc = "#7A4F00"
        )
        chipGroupFeatures.addView(chip)
    }

    fun appendPriceLevel(level: Int) {
        val clamped = level.coerceIn(1, 3)
        val txt = "$".repeat(clamped)

        val chip = createChip(
            text = txt,
            bg = "#F2F2F2",
            tc = "#555555"
        )

        chipGroupFeatures.addView(chip)
    }
}
