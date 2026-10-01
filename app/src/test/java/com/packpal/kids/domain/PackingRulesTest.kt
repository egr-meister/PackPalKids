package com.packpal.kids.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackingRulesTest {

    @Test
    fun progressCountsPackedOverTotal() {
        val p = PackingRules.progress(schoolItems, setOf(1L, 3L, 7L))
        assertEquals(3, p.done)
        assertEquals(7, p.total)
        assertFalse(p.isComplete)
        assertEquals(4, p.remaining)
    }

    @Test
    fun packedIdsOfDeletedItemsAreIgnored() {
        val p = PackingRules.progress(schoolItems, setOf(1L, 99L))
        assertEquals(1, p.done)
    }

    @Test
    fun emptyTemplateIsNeverComplete() {
        val p = PackingRules.progress(emptyList(), emptySet())
        assertEquals(0, p.total)
        assertFalse(p.isComplete)
        assertEquals(0f, p.fraction)
    }

    @Test
    fun emptyCompartmentDoesNotAffectProgress() {
        val sport = listOf(item(1, "Bottle", Category.FOOD), item(2, "Keys", Category.IMPORTANT))
        assertEquals(0, PackingRules.categoryProgress(sport, emptySet(), Category.BOOKS).total)
        assertTrue(PackingRules.progress(sport, setOf(1L, 2L)).isComplete)
    }

    @Test
    fun categoryProgressOnlyCountsThatCompartment() {
        val p = PackingRules.categoryProgress(schoolItems, setOf(1L, 3L), Category.BOOKS)
        assertEquals(Progress(1, 2), p)
    }

    @Test
    fun unpackedListIsInCompartmentOrder() {
        val names = PackingRules.unpacked(schoolItems.reversed(), setOf(2L, 4L)).map { it.name }
        assertEquals(listOf("Notebook", "Water bottle", "Jacket", "Pencil case", "Keys"), names)
    }

    @Test
    fun sessionResetClearsPackedAndUnfinishedCheck() {
        assertTrue(PackingRules.hasActiveProgress(setOf(1L), false))
        assertTrue(PackingRules.hasActiveProgress(emptySet(), true))
        val (packed, unfinished) = PackingRules.freshSession()
        assertTrue(packed.isEmpty())
        assertFalse(unfinished)
        assertFalse(PackingRules.hasActiveProgress(packed, unfinished))
    }
}
