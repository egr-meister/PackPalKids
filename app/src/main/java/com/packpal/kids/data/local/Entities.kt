package com.packpal.kids.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val displayOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(
        entity = TemplateEntity::class, parentColumns = ["id"], childColumns = ["templateId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("templateId")],
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val name: String,
    val note: String,
    val category: String,
    val iconKey: String,
    val displayOrder: Int,
)

@Entity(
    tableName = "packing_sessions",
    foreignKeys = [ForeignKey(
        entity = TemplateEntity::class, parentColumns = ["id"], childColumns = ["templateId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["templateId"], unique = true)],
)
data class PackingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val startedAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "packing_item_states",
    primaryKeys = ["sessionId", "itemId"],
    foreignKeys = [
        ForeignKey(
            entity = PackingSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("itemId")],
)
data class PackingItemStateEntity(
    val sessionId: Long,
    val itemId: Long,
    val packed: Boolean,
)

@Entity(
    tableName = "verification_attempts",
    foreignKeys = [ForeignKey(
        entity = PackingSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["sessionId"], unique = true)],
)
data class VerificationAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val startedAt: Long,
    /** Position of the next item in the current round's queue. */
    val nextItemPosition: Int,
    /** 1 for the first pass, incremented by each "Recheck missing". */
    val round: Int,
    val templateNameSnapshot: String,
)

@Entity(
    tableName = "verification_items",
    primaryKeys = ["attemptId", "sourceItemId"],
    foreignKeys = [ForeignKey(
        entity = VerificationAttemptEntity::class, parentColumns = ["id"], childColumns = ["attemptId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class VerificationItemEntity(
    val attemptId: Long,
    /** Snapshot reference only (no foreign key) so the attempt is independent of later edits. */
    val sourceItemId: Long,
    val itemNameSnapshot: String,
    val categorySnapshot: String,
    val iconKeySnapshot: String,
    val verificationState: String,
    val displayOrder: Int,
    @ColumnInfo(defaultValue = "1") val inCurrentRound: Boolean,
)

@Entity(tableName = "check_history", indices = [Index("completedAt")])
data class CheckHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateNameSnapshot: String,
    val completedAt: Long,
    val totalItems: Int,
    val confirmedItems: Int,
    val missingItems: Int,
)

@Entity(
    tableName = "check_history_items",
    foreignKeys = [ForeignKey(
        entity = CheckHistoryEntity::class, parentColumns = ["id"], childColumns = ["historyId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("historyId")],
)
data class CheckHistoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val historyId: Long,
    val itemNameSnapshot: String,
    val categorySnapshot: String,
    val iconKeySnapshot: String,
    val verificationState: String,
    val displayOrder: Int,
)

data class TemplateWithCount(
    val id: Long,
    val name: String,
    val displayOrder: Int,
    val itemCount: Int,
)
