package com.packpal.kids.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryRulesTest {

    @Test
    fun keepsLatestFifty() {
        val idsNewestFirst = (60L downTo 1L).toList()
        val prune = HistoryRules.idsToPrune(idsNewestFirst)
        assertEquals((10L downTo 1L).toList(), prune)
    }

    @Test
    fun nothingToPruneAtOrBelowLimit() {
        assertTrue(HistoryRules.idsToPrune((50L downTo 1L).toList()).isEmpty())
        assertTrue(HistoryRules.idsToPrune(emptyList()).isEmpty())
    }

    @Test
    fun historySnapshotIsIndependentOfLaterTemplateEdits() {
        val items = schoolItems.toMutableList()
        val snapshot = VerificationRules.snapshot(items)
        // A parent renames, re-icons and deletes items after the check started.
        items[0] = items[0].copy(name = "Maths book", iconKey = "book")
        items.removeAt(items.lastIndex)
        assertEquals("Notebook", snapshot.first().name)
        assertEquals("generic", snapshot.first().iconKey)
        assertEquals(7, snapshot.size)
        assertEquals("Keys", snapshot.last().name)
    }
}
