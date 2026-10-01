package com.packpal.kids.data.repository

import com.packpal.kids.data.local.CheckHistoryEntity
import com.packpal.kids.data.local.PackPalDatabase
import com.packpal.kids.domain.Category
import com.packpal.kids.domain.Limits
import com.packpal.kids.domain.VerificationState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class HistorySummary(
    val id: Long,
    val templateName: String,
    val completedAt: Long,
    val total: Int,
    val confirmed: Int,
    val missing: Int,
) {
    val allChecked: Boolean get() = missing == 0 && confirmed == total
}

data class HistoryItem(val name: String, val category: Category, val iconKey: String, val state: VerificationState)

data class HistoryDetail(val summary: HistorySummary, val items: List<HistoryItem>)

private fun CheckHistoryEntity.toSummary() =
    HistorySummary(id, templateNameSnapshot, completedAt, totalItems, confirmedItems, missingItems)

class HistoryRepository(db: PackPalDatabase) {
    private val dao = db.historyDao()

    fun observeLatest(): Flow<List<HistorySummary>> =
        dao.observeLatest(Limits.HISTORY_MAX).map { l -> l.map { it.toSummary() } }

    fun observeDetail(id: Long): Flow<HistoryDetail?> =
        combine(dao.observeEntry(id), dao.observeEntryItems(id)) { entry, items ->
            entry?.let {
                HistoryDetail(it.toSummary(), items.map { i ->
                    HistoryItem(i.itemNameSnapshot, Category.fromKey(i.categorySnapshot), i.iconKeySnapshot, VerificationState.fromKey(i.verificationState))
                })
            }
        }

    suspend fun delete(id: Long) = dao.deleteEntries(listOf(id))

    suspend fun clear() = dao.clear()
}
