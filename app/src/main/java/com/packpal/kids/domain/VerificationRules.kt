package com.packpal.kids.domain

object VerificationRules {

    /** Creates the immutable snapshot used for a new final check. */
    fun snapshot(items: List<Item>): List<VerifyEntry> =
        PackingRules.ordered(items).mapIndexed { index, item ->
            VerifyEntry(
                sourceItemId = item.id,
                name = item.name,
                category = item.category,
                iconKey = item.iconKey,
                state = VerificationState.UNREVIEWED,
                inCurrentRound = true,
                order = index,
            )
        }

    fun roundQueue(entries: List<VerifyEntry>): List<VerifyEntry> =
        entries.filter { it.inCurrentRound }.sortedBy { it.order }

    /** Index (in the current round queue) of the next item to review, or null when the round is done. */
    fun currentIndex(entries: List<VerifyEntry>): Int? =
        roundQueue(entries).indexOfFirst { it.state == VerificationState.UNREVIEWED }.takeIf { it >= 0 }

    fun isRoundComplete(entries: List<VerifyEntry>): Boolean = currentIndex(entries) == null

    /** "It's here" -> CONFIRMED, "Still missing" -> MISSING. Only items of the current round can change. */
    fun answer(entries: List<VerifyEntry>, sourceItemId: Long, isHere: Boolean): List<VerifyEntry> =
        entries.map {
            if (it.sourceItemId == sourceItemId && it.inCurrentRound) {
                it.copy(state = if (isHere) VerificationState.CONFIRMED else VerificationState.MISSING)
            } else it
        }

    /** Starts a new round over the same attempt that revisits only the missing items. */
    fun recheckMissing(entries: List<VerifyEntry>): List<VerifyEntry> =
        entries.map {
            if (it.state == VerificationState.MISSING) it.copy(state = VerificationState.UNREVIEWED, inCurrentRound = true)
            else it.copy(inCurrentRound = false)
        }

    fun result(entries: List<VerifyEntry>): VerifyResult = VerifyResult(
        total = entries.size,
        confirmed = entries.count { it.state == VerificationState.CONFIRMED },
        missing = entries.count { it.state == VerificationState.MISSING },
        reviewed = entries.count { it.state != VerificationState.UNREVIEWED },
    )

    fun missing(entries: List<VerifyEntry>): List<VerifyEntry> =
        entries.filter { it.state == VerificationState.MISSING }.sortedBy { it.order }

    /** A check can be saved only when every item has an answer. */
    fun canSave(entries: List<VerifyEntry>): Boolean =
        entries.isNotEmpty() && entries.none { it.state == VerificationState.UNREVIEWED }
}
