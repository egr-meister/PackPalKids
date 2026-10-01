package com.packpal.kids.domain

fun item(id: Long, name: String, category: Category, order: Int = 0, icon: String = "generic") =
    Item(id = id, templateId = 1, name = name, note = "", category = category, iconKey = icon, displayOrder = order)

val schoolItems = listOf(
    item(1, "Notebook", Category.BOOKS, 0),
    item(2, "Workbook", Category.BOOKS, 1),
    item(3, "Water bottle", Category.FOOD, 0),
    item(4, "Lunchbox", Category.FOOD, 1),
    item(5, "Jacket", Category.CLOTHES, 0),
    item(6, "Pencil case", Category.TOOLS, 0),
    item(7, "Keys", Category.IMPORTANT, 0),
)
