package com.packpal.kids.domain

object Limits {
    const val MAX_TEMPLATES = 20
    const val MAX_ITEMS = 40
    const val TEMPLATE_NAME_MAX = 30
    const val ITEM_NAME_MAX = 40
    const val NOTE_MAX = 100
    const val HISTORY_MAX = 50
}

data class DraftItem(
    /** Stable editor key: the item id for existing items, a negative number for new ones. */
    val key: Long,
    val id: Long?,
    val name: String,
    val note: String,
    val category: Category,
    val iconKey: String,
)

data class TemplateDraft(
    val templateId: Long?,
    val name: String,
    val items: List<DraftItem>,
)

data class DraftErrors(
    val templateName: String? = null,
    val itemNames: Map<Long, String> = emptyMap(),
    val notes: Map<Long, String> = emptyMap(),
    val general: String? = null,
) {
    val isValid: Boolean get() = templateName == null && itemNames.isEmpty() && notes.isEmpty() && general == null
}

object Validation {

    fun templateNameError(raw: String, otherTemplateNames: Collection<String>): String? {
        val name = raw.trim()
        return when {
            name.isEmpty() -> "Please enter a list name."
            name.length > Limits.TEMPLATE_NAME_MAX -> "Use ${Limits.TEMPLATE_NAME_MAX} characters or fewer."
            otherTemplateNames.any { it.trim().equals(name, ignoreCase = true) } -> "Another list already uses this name."
            else -> null
        }
    }

    fun itemNameError(raw: String, siblingNamesInCompartment: Collection<String>): String? {
        val name = raw.trim()
        return when {
            name.isEmpty() -> "Please enter an item name."
            name.length > Limits.ITEM_NAME_MAX -> "Use ${Limits.ITEM_NAME_MAX} characters or fewer."
            siblingNamesInCompartment.any { it.trim().equals(name, ignoreCase = true) } ->
                "This compartment already has an item with this name."
            else -> null
        }
    }

    fun noteError(raw: String): String? =
        if (raw.trim().length > Limits.NOTE_MAX) "Notes can be up to ${Limits.NOTE_MAX} characters." else null

    fun validate(draft: TemplateDraft, otherTemplateNames: Collection<String>): DraftErrors {
        val itemErrors = mutableMapOf<Long, String>()
        val noteErrors = mutableMapOf<Long, String>()
        draft.items.forEachIndexed { index, item ->
            // Only earlier items in the same compartment count, so the first of two duplicates stays valid.
            val earlierSiblings = draft.items.take(index).filter { it.category == item.category }.map { it.name }
            itemNameError(item.name, earlierSiblings)?.let { itemErrors[item.key] = it }
            noteError(item.note)?.let { noteErrors[item.key] = it }
        }
        val general = when {
            draft.items.isEmpty() -> "Add at least one item so this list can be packed."
            draft.items.size > Limits.MAX_ITEMS -> "A list can have up to ${Limits.MAX_ITEMS} items."
            else -> null
        }
        return DraftErrors(
            templateName = templateNameError(draft.name, otherTemplateNames),
            itemNames = itemErrors,
            notes = noteErrors,
            general = general,
        )
    }

    /** Returns [base] or "base (2)", "base (3)"... that does not clash (case-insensitive) and fits the limit. */
    fun uniqueName(base: String, existing: Collection<String>): String {
        val taken = existing.map { it.trim().lowercase() }.toSet()
        val trimmed = base.trim().take(Limits.TEMPLATE_NAME_MAX)
        if (trimmed.lowercase() !in taken) return trimmed
        var n = 2
        while (true) {
            val suffix = " ($n)"
            val candidate = trimmed.take(Limits.TEMPLATE_NAME_MAX - suffix.length).trimEnd() + suffix
            if (candidate.lowercase() !in taken) return candidate
            n++
        }
    }
}

object HistoryRules {
    /** Given history ids ordered newest first, returns the ids that exceed the retention limit. */
    fun idsToPrune(idsNewestFirst: List<Long>, keep: Int = Limits.HISTORY_MAX): List<Long> = idsNewestFirst.drop(keep)
}
