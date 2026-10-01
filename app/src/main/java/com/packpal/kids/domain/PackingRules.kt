package com.packpal.kids.domain

object PackingRules {

    /** packed item count / total item count. Items with no stored state count as unpacked. */
    fun progress(items: List<Item>, packedIds: Set<Long>): Progress =
        Progress(done = items.count { it.id in packedIds }, total = items.size)

    fun categoryProgress(items: List<Item>, packedIds: Set<Long>, category: Category): Progress =
        progress(items.filter { it.category == category }, packedIds)

    /** Items in a stable, compartment-grouped order (compartment order, then parent-defined order). */
    fun ordered(items: List<Item>): List<Item> =
        items.sortedWith(compareBy<Item>({ it.category.ordinal }, { it.displayOrder }, { it.id }))

    fun unpacked(items: List<Item>, packedIds: Set<Long>): List<Item> =
        ordered(items).filter { it.id !in packedIds }

    /** True when a template has packing progress or an unfinished check that a reset would discard. */
    fun hasActiveProgress(packedIds: Set<Long>, hasUnfinishedCheck: Boolean): Boolean =
        packedIds.isNotEmpty() || hasUnfinishedCheck

    /** Result of "Start fresh": every item unpacked and no unfinished check. History is untouched. */
    fun freshSession(): Pair<Set<Long>, Boolean> = emptySet<Long>() to false
}
