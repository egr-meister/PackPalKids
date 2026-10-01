package com.packpal.kids.domain

enum class Category(val label: String) {
    BOOKS("Books"),
    FOOD("Food"),
    CLOTHES("Clothes"),
    TOOLS("Tools"),
    IMPORTANT("Important");

    companion object {
        fun fromKey(key: String): Category = entries.firstOrNull { it.name == key } ?: IMPORTANT
    }
}

enum class VerificationState {
    UNREVIEWED,
    CONFIRMED,
    MISSING;

    companion object {
        fun fromKey(key: String): VerificationState = entries.firstOrNull { it.name == key } ?: UNREVIEWED
    }
}

data class TemplateSummary(
    val id: Long,
    val name: String,
    val displayOrder: Int,
    val itemCount: Int,
)

data class Item(
    val id: Long,
    val templateId: Long,
    val name: String,
    val note: String,
    val category: Category,
    val iconKey: String,
    val displayOrder: Int,
)

data class Progress(val done: Int, val total: Int) {
    val isComplete: Boolean get() = total > 0 && done == total
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total
    val remaining: Int get() = total - done
}

/** One item inside a final-check attempt. Values are snapshots taken when the check started. */
data class VerifyEntry(
    val sourceItemId: Long,
    val name: String,
    val category: Category,
    val iconKey: String,
    val state: VerificationState,
    val inCurrentRound: Boolean,
    val order: Int,
)

data class VerifyResult(
    val total: Int,
    val confirmed: Int,
    val missing: Int,
    val reviewed: Int,
) {
    /** Ready only when every item was confirmed; never while anything is missing or unreviewed. */
    val allChecked: Boolean get() = total > 0 && confirmed == total
}
