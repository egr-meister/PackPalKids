package com.packpal.kids.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TemplateEntity::class,
        ItemEntity::class,
        PackingSessionEntity::class,
        PackingItemStateEntity::class,
        VerificationAttemptEntity::class,
        VerificationItemEntity::class,
        CheckHistoryEntity::class,
        CheckHistoryItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PackPalDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun packingDao(): PackingDao
    abstract fun historyDao(): HistoryDao

    companion object {
        const val NAME = "packpal.db"

        fun build(context: Context): PackPalDatabase =
            Room.databaseBuilder(context, PackPalDatabase::class.java, NAME)
                // Future schema changes add entries from Migrations.ALL; destructive migration is never used.
                .addMigrations(*Migrations.ALL)
                .build()
    }
}

object Migrations {
    /** Version 1 is the first schema. Add Migration(n, n + 1) objects here for each schema change. */
    val ALL: Array<androidx.room.migration.Migration> = emptyArray()
}
