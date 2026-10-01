package com.packpal.kids.domain

data class StarterItem(val name: String, val category: Category, val iconKey: String)
data class StarterTemplate(val name: String, val items: List<StarterItem>)

object StarterTemplates {
    private fun i(name: String, category: Category, icon: String) = StarterItem(name, category, icon)

    val all: List<StarterTemplate> = listOf(
        StarterTemplate(
            "School", listOf(
                i("Notebook", Category.BOOKS, "notebook"),
                i("Workbook", Category.BOOKS, "workbook"),
                i("Water bottle", Category.FOOD, "water_bottle"),
                i("Lunchbox", Category.FOOD, "lunchbox"),
                i("Jacket", Category.CLOTHES, "jacket"),
                i("Pencil case", Category.TOOLS, "pencil_case"),
                i("Keys", Category.IMPORTANT, "keys"),
            )
        ),
        StarterTemplate(
            "Sport", listOf(
                i("Water bottle", Category.FOOD, "water_bottle"),
                i("Snack", Category.FOOD, "snack"),
                i("Sports clothes", Category.CLOTHES, "sports_clothes"),
                i("Trainers", Category.CLOTHES, "trainers"),
                i("Towel", Category.TOOLS, "towel"),
                i("Keys", Category.IMPORTANT, "keys"),
            )
        ),
        StarterTemplate(
            "Art Class", listOf(
                i("Sketchbook", Category.BOOKS, "sketchbook"),
                i("Water bottle", Category.FOOD, "water_bottle"),
                i("Apron", Category.CLOTHES, "apron"),
                i("Pencil case", Category.TOOLS, "pencil_case"),
                i("Brushes", Category.TOOLS, "brushes"),
                i("Keys", Category.IMPORTANT, "keys"),
            )
        ),
        StarterTemplate(
            "Weekend", listOf(
                i("Book", Category.BOOKS, "book"),
                i("Water bottle", Category.FOOD, "water_bottle"),
                i("Snack", Category.FOOD, "snack"),
                i("Jacket", Category.CLOTHES, "jacket"),
                i("Activity kit", Category.TOOLS, "activity_kit"),
                i("Keys", Category.IMPORTANT, "keys"),
            )
        ),
        StarterTemplate(
            "Trip", listOf(
                i("Activity book", Category.BOOKS, "book"),
                i("Water bottle", Category.FOOD, "water_bottle"),
                i("Lunchbox", Category.FOOD, "lunchbox"),
                i("Spare clothes", Category.CLOTHES, "shirt"),
                i("Jacket", Category.CLOTHES, "jacket"),
                i("Tissues", Category.TOOLS, "tissues"),
                i("Travel ticket", Category.IMPORTANT, "travel_ticket"),
                i("Keys", Category.IMPORTANT, "keys"),
            )
        ),
    )
}
