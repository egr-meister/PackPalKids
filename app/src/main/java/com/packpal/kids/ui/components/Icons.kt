package com.packpal.kids.ui.components

import androidx.annotation.DrawableRes
import com.packpal.kids.R
import com.packpal.kids.domain.Category

data class IconOption(val key: String, val label: String, @DrawableRes val res: Int)

/** Curated, bundled item icons for the parent icon picker. */
object ItemIcons {
    val options: List<IconOption> = listOf(
        IconOption("notebook", "Notebook", R.drawable.ic_item_notebook),
        IconOption("workbook", "Workbook", R.drawable.ic_item_workbook),
        IconOption("book", "Book", R.drawable.ic_item_book),
        IconOption("sketchbook", "Sketchbook", R.drawable.ic_item_sketchbook),
        IconOption("pencil", "Pencil", R.drawable.ic_item_pencil),
        IconOption("pencil_case", "Pencil case", R.drawable.ic_item_pencil_case),
        IconOption("ruler", "Ruler", R.drawable.ic_item_ruler),
        IconOption("scissors", "Scissors", R.drawable.ic_item_scissors),
        IconOption("brushes", "Brushes", R.drawable.ic_item_brushes),
        IconOption("paints", "Paints", R.drawable.ic_item_paints),
        IconOption("water_bottle", "Water bottle", R.drawable.ic_item_water_bottle),
        IconOption("lunchbox", "Lunchbox", R.drawable.ic_item_lunchbox),
        IconOption("snack", "Snack", R.drawable.ic_item_snack),
        IconOption("sandwich", "Sandwich", R.drawable.ic_item_sandwich),
        IconOption("jacket", "Jacket", R.drawable.ic_item_jacket),
        IconOption("shirt", "Shirt", R.drawable.ic_item_shirt),
        IconOption("sports_clothes", "Sports clothes", R.drawable.ic_item_sports_clothes),
        IconOption("trainers", "Shoes", R.drawable.ic_item_trainers),
        IconOption("apron", "Apron", R.drawable.ic_item_apron),
        IconOption("hat", "Hat", R.drawable.ic_item_hat),
        IconOption("towel", "Towel", R.drawable.ic_item_towel),
        IconOption("tissues", "Tissues", R.drawable.ic_item_tissues),
        IconOption("activity_kit", "Activity kit", R.drawable.ic_item_activity_kit),
        IconOption("keys", "Keys", R.drawable.ic_item_keys),
        IconOption("travel_ticket", "Ticket", R.drawable.ic_item_travel_ticket),
        IconOption("glasses", "Glasses", R.drawable.ic_item_glasses),
        IconOption("ball", "Ball", R.drawable.ic_item_ball),
        IconOption("umbrella", "Umbrella", R.drawable.ic_item_umbrella),
        IconOption("headphones", "Headphones", R.drawable.ic_item_headphones),
        IconOption("wallet", "Wallet", R.drawable.ic_item_wallet),
        IconOption("generic", "Other item", R.drawable.ic_item_generic),
    )
    private val byKey = options.associateBy { it.key }

    const val DEFAULT_KEY = "generic"

    @DrawableRes
    fun res(key: String): Int = (byKey[key] ?: byKey.getValue(DEFAULT_KEY)).res

    fun label(key: String): String = (byKey[key] ?: byKey.getValue(DEFAULT_KEY)).label
}

@DrawableRes
fun categoryIcon(category: Category): Int = when (category) {
    Category.BOOKS -> R.drawable.ic_cat_books
    Category.FOOD -> R.drawable.ic_cat_food
    Category.CLOTHES -> R.drawable.ic_cat_clothes
    Category.TOOLS -> R.drawable.ic_cat_tools
    Category.IMPORTANT -> R.drawable.ic_cat_important
}

/** Simple context icon for a list, chosen from its name. */
@DrawableRes
fun templateIcon(name: String): Int {
    val n = name.lowercase()
    return when {
        "school" in n -> R.drawable.ic_tpl_school
        "sport" in n || "swim" in n || "football" in n -> R.drawable.ic_tpl_sport
        "art" in n || "paint" in n || "draw" in n -> R.drawable.ic_tpl_art
        "weekend" in n -> R.drawable.ic_tpl_weekend
        "trip" in n || "travel" in n || "holiday" in n -> R.drawable.ic_tpl_trip
        else -> R.drawable.ic_tpl_custom
    }
}
