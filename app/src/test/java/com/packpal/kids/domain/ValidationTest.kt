package com.packpal.kids.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTest {

    private fun d(key: Long, name: String, c: Category, note: String = "") = DraftItem(key, null, name, note, c, "generic")

    @Test
    fun templateNameRules() {
        assertNotNull(Validation.templateNameError("   ", emptyList()))
        assertNotNull(Validation.templateNameError("x".repeat(31), emptyList()))
        assertNull(Validation.templateNameError("  ${"x".repeat(30)}  ", emptyList()))
        assertNotNull(Validation.templateNameError(" school ", listOf("School")))
        assertNull(Validation.templateNameError("School 2", listOf("School")))
    }

    @Test
    fun duplicateItemNamesRejectedOnlyWithinSameCompartment() {
        val draft = TemplateDraft(null, "Test", listOf(
            d(-1, "Bottle", Category.FOOD),
            d(-2, " bottle", Category.FOOD),
            d(-3, "Bottle", Category.TOOLS),
        ))
        val errors = Validation.validate(draft, emptyList())
        assertFalse(errors.isValid)
        assertEquals(setOf(-2L), errors.itemNames.keys)
    }

    @Test
    fun itemNameAndNoteLimits() {
        assertNotNull(Validation.itemNameError("", emptyList()))
        assertNotNull(Validation.itemNameError("y".repeat(41), emptyList()))
        assertNull(Validation.itemNameError("y".repeat(40), emptyList()))
        assertNull(Validation.noteError("n".repeat(100)))
        assertNotNull(Validation.noteError("n".repeat(101)))
    }

    @Test
    fun templateNeedsAtLeastOneItemAndAtMostForty() {
        assertNotNull(Validation.validate(TemplateDraft(null, "Empty", emptyList()), emptyList()).general)
        val many = (1..41).map { d(-it.toLong(), "Item $it", Category.TOOLS) }
        assertNotNull(Validation.validate(TemplateDraft(null, "Big", many), emptyList()).general)
        assertTrue(Validation.validate(TemplateDraft(null, "Ok", many.take(40)), emptyList()).isValid)
    }

    @Test
    fun uniqueNamesForRestoredStarters() {
        assertEquals("School", Validation.uniqueName("School", listOf("Sport")))
        assertEquals("School (2)", Validation.uniqueName("School", listOf("school")))
        assertEquals("School (3)", Validation.uniqueName("School", listOf("School", "School (2)")))
        val long = "L".repeat(30)
        val unique = Validation.uniqueName(long, listOf(long))
        assertTrue(unique.length <= Limits.TEMPLATE_NAME_MAX)
        assertTrue(unique.endsWith("(2)"))
    }

    @Test
    fun starterTemplatesAreValid() {
        assertEquals(listOf("School", "Sport", "Art Class", "Weekend", "Trip"), StarterTemplates.all.map { it.name })
        StarterTemplates.all.forEach { t ->
            val draft = TemplateDraft(null, t.name, t.items.mapIndexed { i, s -> d(-i - 1L, s.name, s.category) })
            assertTrue(t.name, Validation.validate(draft, emptyList()).isValid)
        }
        assertTrue(StarterTemplates.all.first { it.name == "Sport" }.items.none { it.category == Category.BOOKS })
    }
}
