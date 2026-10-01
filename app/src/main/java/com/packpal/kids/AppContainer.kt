package com.packpal.kids

import android.content.Context
import com.packpal.kids.data.local.PackPalDatabase
import com.packpal.kids.data.prefs.PreferencesRepository
import com.packpal.kids.data.repository.HistoryRepository
import com.packpal.kids.data.repository.PackingRepository
import com.packpal.kids.data.repository.TemplateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Manual dependency container, created once by [PackPalApp]. */
class AppContainer(context: Context) {
    val database: PackPalDatabase = PackPalDatabase.build(context)
    val preferences = PreferencesRepository(context)
    val templates = TemplateRepository(database)
    val packing = PackingRepository(database)
    val history = HistoryRepository(database)

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val seedLock = Mutex()

    fun start() {
        appScope.launch { seedIfNeeded() }
    }

    /** Seeds starter templates exactly once. Deleted templates are never reseeded on later launches. */
    suspend fun seedIfNeeded() = seedLock.withLock {
        if (preferences.current().seeded) return@withLock
        if (templates.count() == 0) templates.insertStarters()
        preferences.setSeeded(true)
    }

    /** Deletes every template, session, check and history entry, resets preferences, then reseeds. */
    suspend fun clearAllLocalData() = seedLock.withLock {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        preferences.clear()
        templates.insertStarters()
        preferences.setSeeded(true)
    }
}
