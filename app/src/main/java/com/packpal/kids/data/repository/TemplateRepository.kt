package com.packpal.kids.data.repository

import androidx.room.withTransaction
import com.packpal.kids.data.local.ItemEntity
import com.packpal.kids.data.local.PackPalDatabase
import com.packpal.kids.data.local.TemplateEntity
import com.packpal.kids.domain.Category
import com.packpal.kids.domain.DraftItem
import com.packpal.kids.domain.Item
import com.packpal.kids.domain.Limits
import com.packpal.kids.domain.PackingRules
import com.packpal.kids.domain.StarterTemplates
import com.packpal.kids.domain.TemplateDraft
import com.packpal.kids.domain.TemplateSummary
import com.packpal.kids.domain.Validation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TemplateRepository(private val db: PackPalDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    private val dao = db.templateDao()
    private val packingDao = db.packingDao()

    fun observeTemplates(): Flow<List<TemplateSummary>> = dao.observeTemplates().map { list -> list.map { it.toDomain() } }

    fun observeItems(templateId: Long): Flow<List<Item>> = dao.observeItems(templateId).map { l -> l.map { it.toDomain() } }

    fun observeTemplateName(templateId: Long): Flow<String?> = dao.observeTemplate(templateId).map { it?.name }

    suspend fun getDraft(templateId: Long): TemplateDraft? {
        val t = dao.getTemplate(templateId) ?: return null
        val items = PackingRules.ordered(dao.getItems(templateId).map { it.toDomain() })
        return TemplateDraft(
            templateId = t.id,
            name = t.name,
            items = items.map { DraftItem(it.id, it.id, it.name, it.note, it.category, it.iconKey) },
        )
    }

    suspend fun templateNames(excludingId: Long?): List<String> =
        dao.getTemplates().filter { it.id != excludingId }.map { it.name }

    suspend fun count(): Int = dao.count()

    /** True when saving an edit would restart this template's current session. */
    suspend fun hasActiveProgress(templateId: Long): Boolean {
        val packed = packingDao.getPackedIds(templateId).toSet()
        val attempt = packingDao.getAttemptForTemplate(templateId)
        return PackingRules.hasActiveProgress(packed, attempt != null)
    }

    /**
     * Saves a validated draft in one transaction. Existing history snapshots are separate rows and never change.
     * When the template already had progress, its session is restarted (packed states and unfinished check cleared).
     */
    suspend fun saveDraft(draft: TemplateDraft): Long = db.withTransaction {
        val now = clock()
        val name = draft.name.trim()
        val templateId = if (draft.templateId == null) {
            check(dao.count() < Limits.MAX_TEMPLATES) { "Template limit reached" }
            dao.insertTemplate(TemplateEntity(name = name, displayOrder = dao.maxDisplayOrder() + 1, createdAt = now, updatedAt = now))
        } else {
            val existing = dao.getTemplate(draft.templateId) ?: error("Template not found")
            if (hasActiveProgress(existing.id)) {
                packingDao.deleteSessionForTemplate(existing.id)
            }
            dao.updateTemplate(existing.copy(name = name, updatedAt = now))
            existing.id
        }
        val existingIds = dao.getItems(templateId).map { it.id }.toSet()
        val keptIds = mutableSetOf<Long>()
        Category.entries.forEach { category ->
            draft.items.filter { it.category == category }.forEachIndexed { index, d ->
                val entity = ItemEntity(
                    id = d.id?.takeIf { it in existingIds } ?: 0,
                    templateId = templateId,
                    name = d.name.trim(),
                    note = d.note.trim(),
                    category = category.name,
                    iconKey = d.iconKey,
                    displayOrder = index,
                )
                if (entity.id != 0L) {
                    dao.updateItem(entity); keptIds += entity.id
                } else {
                    dao.insertItem(entity)
                }
            }
        }
        val removed = existingIds - keptIds
        if (removed.isNotEmpty()) dao.deleteItems(removed.toList())
        templateId
    }

    suspend fun duplicate(templateId: Long): Long? = db.withTransaction {
        if (dao.count() >= Limits.MAX_TEMPLATES) return@withTransaction null
        val source = dao.getTemplate(templateId) ?: return@withTransaction null
        val now = clock()
        val name = Validation.uniqueName("${source.name} copy", dao.getTemplates().map { it.name })
        val newId = dao.insertTemplate(TemplateEntity(name = name, displayOrder = dao.maxDisplayOrder() + 1, createdAt = now, updatedAt = now))
        dao.insertItems(dao.getItems(templateId).map { it.copy(id = 0, templateId = newId) })
        newId
    }

    suspend fun delete(templateId: Long) {
        dao.deleteTemplate(templateId) // cascades to items, session, packing states and unfinished check
    }

    /** Inserts all starter templates, keeping existing templates and history; names are made unique. */
    suspend fun insertStarters(): Int = db.withTransaction {
        val now = clock()
        val names = dao.getTemplates().map { it.name }.toMutableList()
        var order = dao.maxDisplayOrder() + 1
        var added = 0
        for (starter in StarterTemplates.all) {
            if (names.size >= Limits.MAX_TEMPLATES) break
            val name = Validation.uniqueName(starter.name, names)
            val id = dao.insertTemplate(TemplateEntity(name = name, displayOrder = order++, createdAt = now, updatedAt = now))
            val byCategory = starter.items.groupBy { it.category }
            dao.insertItems(byCategory.flatMap { (category, items) ->
                items.mapIndexed { index, it ->
                    ItemEntity(templateId = id, name = it.name, note = "", category = category.name, iconKey = it.iconKey, displayOrder = index)
                }
            })
            names += name
            added++
        }
        added
    }
}
