package com.packpal.kids.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificationRulesTest {

    private fun answerAll(entries: List<VerifyEntry>, missingIds: Set<Long>): List<VerifyEntry> {
        var e = entries
        while (true) {
            val idx = VerificationRules.currentIndex(e) ?: break
            val current = VerificationRules.roundQueue(e)[idx]
            e = VerificationRules.answer(e, current.sourceItemId, isHere = current.sourceItemId !in missingIds)
        }
        return e
    }

    @Test
    fun snapshotStartsUnreviewedInCompartmentOrder() {
        val s = VerificationRules.snapshot(schoolItems.shuffled())
        assertEquals(7, s.size)
        assertTrue(s.all { it.state == VerificationState.UNREVIEWED && it.inCurrentRound })
        assertEquals((0 until 7).toList(), s.map { it.order })
        assertEquals("Notebook", s.first().name)
        assertEquals("Keys", s.last().name)
        assertEquals(0, VerificationRules.currentIndex(s))
    }

    @Test
    fun answersMoveThroughItemsAndRecordState() {
        var s = VerificationRules.snapshot(schoolItems)
        s = VerificationRules.answer(s, 1, isHere = true)
        assertEquals(1, VerificationRules.currentIndex(s))
        s = VerificationRules.answer(s, 2, isHere = false)
        assertEquals(2, VerificationRules.currentIndex(s))
        val r = VerificationRules.result(s)
        assertEquals(1, r.confirmed)
        assertEquals(1, r.missing)
        assertEquals(2, r.reviewed)
        assertFalse(r.allChecked)
    }

    @Test
    fun allConfirmedIsReady() {
        val done = answerAll(VerificationRules.snapshot(schoolItems), emptySet())
        assertNull(VerificationRules.currentIndex(done))
        assertTrue(VerificationRules.result(done).allChecked)
        assertTrue(VerificationRules.canSave(done))
    }

    @Test
    fun neverReadyWhileAnythingIsMissing() {
        val done = answerAll(VerificationRules.snapshot(schoolItems), setOf(5L))
        val r = VerificationRules.result(done)
        assertFalse(r.allChecked)
        assertEquals(6, r.confirmed)
        assertEquals(listOf("Jacket"), VerificationRules.missing(done).map { it.name })
        assertTrue(VerificationRules.canSave(done))
    }

    @Test
    fun cannotSaveUnfinishedCheck() {
        val s = VerificationRules.answer(VerificationRules.snapshot(schoolItems), 1, true)
        assertFalse(VerificationRules.canSave(s))
        assertFalse(VerificationRules.canSave(emptyList()))
    }

    @Test
    fun recheckRevisitsOnlyMissingItemsInSameAttempt() {
        val first = answerAll(VerificationRules.snapshot(schoolItems), setOf(3L, 7L))
        val recheck = VerificationRules.recheckMissing(first)
        val queue = VerificationRules.roundQueue(recheck)
        assertEquals(listOf(3L, 7L), queue.map { it.sourceItemId })
        assertEquals(7, recheck.size)
        // Confirmed items keep their result.
        assertEquals(5, recheck.count { it.state == VerificationState.CONFIRMED })

        var s = VerificationRules.answer(recheck, 3, isHere = true)
        // Items outside the round cannot change.
        s = VerificationRules.answer(s, 1, isHere = false)
        assertEquals(VerificationState.CONFIRMED, s.first { it.sourceItemId == 1L }.state)
        s = VerificationRules.answer(s, 7, isHere = false)

        assertNull(VerificationRules.currentIndex(s))
        val r = VerificationRules.result(s)
        assertEquals(6, r.confirmed)
        assertEquals(1, r.missing)
        assertEquals(listOf(7L), VerificationRules.missing(s).map { it.sourceItemId })
    }

    @Test
    fun recheckAfterEverythingFoundIsReady() {
        val first = answerAll(VerificationRules.snapshot(schoolItems), setOf(4L))
        val final = answerAll(VerificationRules.recheckMissing(first), emptySet())
        assertTrue(VerificationRules.result(final).allChecked)
    }
}
