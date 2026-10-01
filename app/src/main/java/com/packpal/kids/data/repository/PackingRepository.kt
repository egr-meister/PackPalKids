package com.packpal.kids.data.repository

import androidx.room.withTransaction
import com.packpal.kids.data.local.CheckHistoryEntity
import com.packpal.kids.data.local.CheckHistoryItemEntity
import com.packpal.kids.data.local.PackPalDatabase
import com.packpal.kids.data.local.PackingItemStateEntity
import com.packpal.kids.data.local.PackingSessionEntity
import com.packpal.kids.data.local.VerificationAttemptEntity
import com.packpal.kids.domain.HistoryRules
import com.packpal.kids.domain.VerificationRules
import com.packpal.kids.domain.VerifyEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class ActiveCheck(
    val attemptId: Long,
    val templateName: String,
    val round: Int,
    val entries: List<VerifyEntry>,
)

class PackingRepository(private val db: PackPalDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    private val dao = db.packingDao()
    private val templateDao = db.templateDao()
    private val historyDao = db.historyDao()

    fun observePackedIds(templateId: Long): Flow<Set<Long>> = dao.observePackedIds(templateId).map { it.toSet() }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeActiveCheck(templateId: Long): Flow<ActiveCheck?> =
        dao.observeAttemptForTemplate(templateId).flatMapLatest { attempt ->
            if (attempt == null) flowOf(null)
            else dao.observeVerificationItems(attempt.id).map { items ->
                ActiveCheck(attempt.id, attempt.templateNameSnapshot, attempt.round, items.map { it.toDomain() })
            }
        }

    private suspend fun ensureSession(templateId: Long): Long {
        dao.getSession(templateId)?.let { return it.id }
        val now = clock()
        return dao.insertSession(PackingSessionEntity(templateId = templateId, startedAt = now, updatedAt = now))
    }

    suspend fun setPacked(templateId: Long, itemId: Long, packed: Boolean) = db.withTransaction {
        val sessionId = ensureSession(templateId)
        dao.upsertState(PackingItemStateEntity(sessionId, itemId, packed))
        dao.touchSession(sessionId, clock())
    }

    /** Clears this template's packed states and unfinished check. Completed history is not touched. */
    suspend fun startFresh(templateId: Long) = db.withTransaction {
        dao.deleteSessionForTemplate(templateId)
        val now = clock()
        dao.insertSession(PackingSessionEntity(templateId = templateId, startedAt = now, updatedAt = now))
    }

    /** Starts a final check from the template's current items, or returns the existing unfinished one. */
    suspend fun startCheck(templateId: Long): Long? = db.withTransaction {
        dao.getAttemptForTemplate(templateId)?.let { return@withTransaction it.id }
        val template = templateDao.getTemplate(templateId) ?: return@withTransaction null
        val items = templateDao.getItems(templateId).map { it.toDomain() }
        if (items.isEmpty()) return@withTransaction null
        val sessionId = ensureSession(templateId)
        val attemptId = dao.insertAttempt(
            VerificationAttemptEntity(
                sessionId = sessionId, startedAt = clock(), nextItemPosition = 0, round = 1,
                templateNameSnapshot = template.name,
            )
        )
        dao.insertVerificationItems(VerificationRules.snapshot(items).map { it.toEntity(attemptId) })
        attemptId
    }

    /** Records one answer and mirrors it to the packed state. Persisted immediately. */
    suspend fun answer(templateId: Long, attemptId: Long, sourceItemId: Long, isHere: Boolean) = db.withTransaction {
        val attempt = dao.getAttempt(attemptId) ?: return@withTransaction
        val entries = dao.getVerificationItems(attemptId).map { it.toDomain() }
        val updated = VerificationRules.answer(entries, sourceItemId, isHere)
        dao.updateVerificationItems(updated.map { it.toEntity(attemptId) })
        val next = VerificationRules.currentIndex(updated) ?: VerificationRules.roundQueue(updated).size
        dao.updateAttempt(attempt.copy(nextItemPosition = next))
        // The source item may have been deleted meanwhile only if the session was restarted, which removes the attempt.
        if (templateDao.getItems(templateId).any { it.id == sourceItemId }) {
            dao.upsertState(PackingItemStateEntity(attempt.sessionId, sourceItemId, isHere))
            dao.touchSession(attempt.sessionId, clock())
        }
    }

    suspend fun recheckMissing(attemptId: Long) = db.withTransaction {
        val attempt = dao.getAttempt(attemptId) ?: return@withTransaction
        val entries = dao.getVerificationItems(attemptId).map { it.toDomain() }
        if (VerificationRules.missing(entries).isEmpty()) return@withTransaction
        dao.updateVerificationItems(VerificationRules.recheckMissing(entries).map { it.toEntity(attemptId) })
        dao.updateAttempt(attempt.copy(nextItemPosition = 0, round = attempt.round + 1))
    }

    suspend fun discardCheck(attemptId: Long) {
        dao.deleteAttempt(attemptId)
    }

    /**
     * Saves the finished attempt to history and removes it, atomically. A second call for the same attempt
     * finds nothing and returns null, so repeated taps cannot create duplicate entries.
     */
    suspend fun saveCheck(attemptId: Long): Long? = db.withTransaction {
        val attempt = dao.getAttempt(attemptId) ?: return@withTransaction null
        val entries = dao.getVerificationItems(attemptId).map { it.toDomain() }
        if (!VerificationRules.canSave(entries)) return@withTransaction null
        val result = VerificationRules.result(entries)
        val historyId = historyDao.insertEntry(
            CheckHistoryEntity(
                templateNameSnapshot = attempt.templateNameSnapshot,
                completedAt = clock(),
                totalItems = result.total,
                confirmedItems = result.confirmed,
                missingItems = result.missing,
            )
        )
        historyDao.insertItems(entries.sortedBy { it.order }.map {
            CheckHistoryItemEntity(
                historyId = historyId,
                itemNameSnapshot = it.name,
                categorySnapshot = it.category.name,
                iconKeySnapshot = it.iconKey,
                verificationState = it.state.name,
                displayOrder = it.order,
            )
        })
        dao.deleteAttempt(attemptId)
        val prune = HistoryRules.idsToPrune(historyDao.getIdsNewestFirst())
        if (prune.isNotEmpty()) historyDao.deleteEntries(prune)
        historyId
    }
}
