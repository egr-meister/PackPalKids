package com.packpal.kids.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query(
        """SELECT t.id, t.name, t.displayOrder, (SELECT COUNT(*) FROM items i WHERE i.templateId = t.id) AS itemCount
           FROM templates t ORDER BY t.displayOrder, t.id"""
    )
    fun observeTemplates(): Flow<List<TemplateWithCount>>

    @Query("SELECT * FROM templates ORDER BY displayOrder, id")
    suspend fun getTemplates(): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplate(id: Long): TemplateEntity?

    @Query("SELECT * FROM templates WHERE id = :id")
    fun observeTemplate(id: Long): Flow<TemplateEntity?>

    @Query("SELECT COALESCE(MAX(displayOrder), -1) FROM templates")
    suspend fun maxDisplayOrder(): Int

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun count(): Int

    @Insert
    suspend fun insertTemplate(t: TemplateEntity): Long

    @Update
    suspend fun updateTemplate(t: TemplateEntity)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Query("SELECT * FROM items WHERE templateId = :templateId ORDER BY displayOrder, id")
    fun observeItems(templateId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE templateId = :templateId ORDER BY displayOrder, id")
    suspend fun getItems(templateId: Long): List<ItemEntity>

    @Insert
    suspend fun insertItem(item: ItemEntity): Long

    @Insert
    suspend fun insertItems(items: List<ItemEntity>)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("DELETE FROM items WHERE id IN (:ids)")
    suspend fun deleteItems(ids: List<Long>)
}

@Dao
interface PackingDao {
    @Query("SELECT * FROM packing_sessions WHERE templateId = :templateId")
    suspend fun getSession(templateId: Long): PackingSessionEntity?

    @Query("SELECT * FROM packing_sessions WHERE templateId = :templateId")
    fun observeSession(templateId: Long): Flow<PackingSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(s: PackingSessionEntity): Long

    @Query("UPDATE packing_sessions SET updatedAt = :now WHERE id = :sessionId")
    suspend fun touchSession(sessionId: Long, now: Long)

    @Query("DELETE FROM packing_sessions WHERE templateId = :templateId")
    suspend fun deleteSessionForTemplate(templateId: Long)

    @Query(
        """SELECT s.itemId FROM packing_item_states s JOIN packing_sessions p ON s.sessionId = p.id
           WHERE p.templateId = :templateId AND s.packed = 1"""
    )
    fun observePackedIds(templateId: Long): Flow<List<Long>>

    @Query(
        """SELECT s.itemId FROM packing_item_states s JOIN packing_sessions p ON s.sessionId = p.id
           WHERE p.templateId = :templateId AND s.packed = 1"""
    )
    suspend fun getPackedIds(templateId: Long): List<Long>

    @Upsert
    suspend fun upsertState(state: PackingItemStateEntity)

    // --- Verification attempts ---

    @Query(
        """SELECT a.* FROM verification_attempts a JOIN packing_sessions p ON a.sessionId = p.id
           WHERE p.templateId = :templateId"""
    )
    fun observeAttemptForTemplate(templateId: Long): Flow<VerificationAttemptEntity?>

    @Query(
        """SELECT a.* FROM verification_attempts a JOIN packing_sessions p ON a.sessionId = p.id
           WHERE p.templateId = :templateId"""
    )
    suspend fun getAttemptForTemplate(templateId: Long): VerificationAttemptEntity?

    @Query("SELECT * FROM verification_attempts WHERE id = :id")
    suspend fun getAttempt(id: Long): VerificationAttemptEntity?

    @Insert
    suspend fun insertAttempt(a: VerificationAttemptEntity): Long

    @Update
    suspend fun updateAttempt(a: VerificationAttemptEntity)

    @Query("DELETE FROM verification_attempts WHERE id = :id")
    suspend fun deleteAttempt(id: Long)

    @Query("SELECT * FROM verification_items WHERE attemptId = :attemptId ORDER BY displayOrder")
    fun observeVerificationItems(attemptId: Long): Flow<List<VerificationItemEntity>>

    @Query("SELECT * FROM verification_items WHERE attemptId = :attemptId ORDER BY displayOrder")
    suspend fun getVerificationItems(attemptId: Long): List<VerificationItemEntity>

    @Insert
    suspend fun insertVerificationItems(items: List<VerificationItemEntity>)

    @Update
    suspend fun updateVerificationItems(items: List<VerificationItemEntity>)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM check_history ORDER BY completedAt DESC, id DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<CheckHistoryEntity>>

    @Query("SELECT id FROM check_history ORDER BY completedAt DESC, id DESC")
    suspend fun getIdsNewestFirst(): List<Long>

    @Query("SELECT * FROM check_history WHERE id = :id")
    fun observeEntry(id: Long): Flow<CheckHistoryEntity?>

    @Query("SELECT * FROM check_history_items WHERE historyId = :id ORDER BY displayOrder")
    fun observeEntryItems(id: Long): Flow<List<CheckHistoryItemEntity>>

    @Insert
    suspend fun insertEntry(e: CheckHistoryEntity): Long

    @Insert
    suspend fun insertItems(items: List<CheckHistoryItemEntity>)

    @Query("DELETE FROM check_history WHERE id IN (:ids)")
    suspend fun deleteEntries(ids: List<Long>)

    @Query("DELETE FROM check_history")
    suspend fun clear()
}
