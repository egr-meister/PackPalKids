package com.packpal.kids.ui.parent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packpal.kids.AppContainer
import com.packpal.kids.domain.Category
import com.packpal.kids.domain.DraftErrors
import com.packpal.kids.domain.DraftItem
import com.packpal.kids.domain.Limits
import com.packpal.kids.domain.TemplateDraft
import com.packpal.kids.domain.Validation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class EditorState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val isNew: Boolean = true,
    val draft: TemplateDraft = TemplateDraft(null, "", emptyList()),
    val original: TemplateDraft = TemplateDraft(null, "", emptyList()),
    val errors: DraftErrors = DraftErrors(),
    val showErrors: Boolean = false,
    val confirmRestart: Boolean = false,
    val saving: Boolean = false,
) {
    val dirty: Boolean get() = !loading && draft != original
    val canAddItem: Boolean get() = draft.items.size < Limits.MAX_ITEMS
}

/** Draft editor: changes stay in memory (and SavedStateHandle) until the parent taps Save. */
class TemplateEditorViewModel(private val c: AppContainer, private val handle: SavedStateHandle) : ViewModel() {
    private val templateId: Long? = (handle.get<Long>("templateId") ?: -1L).takeIf { it >= 0 }
    private val _state = MutableStateFlow(EditorState(isNew = templateId == null))
    val state: StateFlow<EditorState> = _state.asStateFlow()
    private var otherNames: List<String> = emptyList()
    private var nextKey = -1L

    init {
        viewModelScope.launch {
            otherNames = c.templates.templateNames(excludingId = templateId)
            val original = if (templateId == null) TemplateDraft(null, "", emptyList()) else c.templates.getDraft(templateId)
            if (original == null) {
                _state.update { it.copy(loading = false, notFound = true) }
                return@launch
            }
            val restored = handle.get<String>(KEY_DRAFT)?.let { runCatching { DraftCodec.decode(it) }.getOrNull() }
            val draft = restored ?: original
            nextKey = minOf(-1L, (draft.items.minOfOrNull { it.key } ?: 0L) - 1)
            _state.update { it.copy(loading = false, original = original, draft = draft) }
        }
    }

    private fun edit(transform: (TemplateDraft) -> TemplateDraft) {
        _state.update { s ->
            val d = transform(s.draft)
            handle[KEY_DRAFT] = DraftCodec.encode(d)
            s.copy(draft = d, errors = if (s.showErrors) Validation.validate(d, otherNames) else s.errors)
        }
    }

    private fun editItem(key: Long, transform: (DraftItem) -> DraftItem) =
        edit { d -> d.copy(items = d.items.map { if (it.key == key) transform(it) else it }) }

    fun setName(v: String) = edit { it.copy(name = v.take(Limits.TEMPLATE_NAME_MAX + 10)) }
    fun setItemName(key: Long, v: String) = editItem(key) { it.copy(name = v.take(Limits.ITEM_NAME_MAX + 10)) }
    fun setItemNote(key: Long, v: String) = editItem(key) { it.copy(note = v.take(Limits.NOTE_MAX + 10)) }
    fun setItemIcon(key: Long, iconKey: String) = editItem(key) { it.copy(iconKey = iconKey) }

    fun addItem(category: Category) {
        if (!_state.value.canAddItem) return
        val item = DraftItem(nextKey--, null, "", "", category, defaultIcon(category))
        edit { d ->
            val lastIndex = d.items.indexOfLast { it.category == category }
            val list = d.items.toMutableList()
            if (lastIndex >= 0) list.add(lastIndex + 1, item) else list.add(item)
            d.copy(items = list)
        }
    }

    fun removeItem(key: Long) = edit { d -> d.copy(items = d.items.filterNot { it.key == key }) }

    /** Moves the item to the end of another compartment. */
    fun setItemCategory(key: Long, category: Category) = edit { d ->
        val item = d.items.firstOrNull { it.key == key } ?: return@edit d
        if (item.category == category) return@edit d
        val rest = d.items.filterNot { it.key == key }.toMutableList()
        val lastIndex = rest.indexOfLast { it.category == category }
        val moved = item.copy(category = category)
        if (lastIndex >= 0) rest.add(lastIndex + 1, moved) else rest.add(moved)
        d.copy(items = rest)
    }

    /** Reorders within the item's compartment: swaps with the previous/next item of the same compartment. */
    fun move(key: Long, up: Boolean) = edit { d ->
        val list = d.items.toMutableList()
        val i = list.indexOfFirst { it.key == key }
        if (i < 0) return@edit d
        val cat = list[i].category
        val j = if (up) (i - 1 downTo 0).firstOrNull { list[it].category == cat }
        else (i + 1 until list.size).firstOrNull { list[it].category == cat }
        if (j == null) return@edit d
        val tmp = list[i]; list[i] = list[j]; list[j] = tmp
        d.copy(items = list)
    }

    /** Validates, then saves, or asks for confirmation first when the template has active packing progress. */
    fun requestSave(onSaved: () -> Unit) {
        val s = _state.value
        if (s.saving || s.loading) return
        val errors = Validation.validate(s.draft, otherNames)
        if (!errors.isValid) {
            _state.update { it.copy(errors = errors, showErrors = true) }
            return
        }
        if (templateId == null && s.draft.templateId == null) {
            viewModelScope.launch {
                if (c.templates.count() >= Limits.MAX_TEMPLATES) {
                    _state.update { it.copy(errors = errors.copy(general = "You can have up to ${Limits.MAX_TEMPLATES} lists."), showErrors = true) }
                } else save(onSaved)
            }
            return
        }
        viewModelScope.launch {
            if (templateId != null && c.templates.hasActiveProgress(templateId)) {
                _state.update { it.copy(confirmRestart = true) }
            } else save(onSaved)
        }
    }

    fun dismissRestart() = _state.update { it.copy(confirmRestart = false) }

    fun confirmRestartAndSave(onSaved: () -> Unit) {
        _state.update { it.copy(confirmRestart = false) }
        viewModelScope.launch { save(onSaved) }
    }

    private suspend fun save(onSaved: () -> Unit) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        try {
            val id = c.templates.saveDraft(_state.value.draft)
            handle.remove<String>(KEY_DRAFT)
            if (templateId == null) c.preferences.setSelectedTemplate(id)
            _state.update { it.copy(original = it.draft, saving = false) }
            onSaved()
        } catch (e: IllegalStateException) {
            _state.update { it.copy(saving = false, showErrors = true, errors = it.errors.copy(general = "This list could not be saved. Please try again.")) }
        }
    }

    suspend fun duplicateSaved(): Long? = templateId?.let { c.templates.duplicate(it) }

    fun deleteTemplate(onDeleted: () -> Unit) {
        val id = templateId ?: return
        viewModelScope.launch {
            c.templates.delete(id)
            handle.remove<String>(KEY_DRAFT)
            onDeleted()
        }
    }

    companion object {
        private const val KEY_DRAFT = "draft"

        fun defaultIcon(category: Category): String = when (category) {
            Category.BOOKS -> "notebook"
            Category.FOOD -> "snack"
            Category.CLOTHES -> "shirt"
            Category.TOOLS -> "pencil"
            Category.IMPORTANT -> "keys"
        }
    }
}

/** Compact JSON encoding of the draft so unsaved edits survive process death. */
internal object DraftCodec {
    fun encode(d: TemplateDraft): String = JSONObject().apply {
        put("templateId", d.templateId ?: -1L)
        put("name", d.name)
        put("items", JSONArray().apply {
            d.items.forEach { i ->
                put(JSONObject().apply {
                    put("key", i.key); put("id", i.id ?: -1L); put("name", i.name); put("note", i.note)
                    put("category", i.category.name); put("icon", i.iconKey)
                })
            }
        })
    }.toString()

    fun decode(s: String): TemplateDraft {
        val o = JSONObject(s)
        val arr = o.getJSONArray("items")
        val items = (0 until arr.length()).map { idx ->
            val i = arr.getJSONObject(idx)
            DraftItem(
                key = i.getLong("key"),
                id = i.getLong("id").takeIf { it >= 0 },
                name = i.getString("name"),
                note = i.getString("note"),
                category = Category.fromKey(i.getString("category")),
                iconKey = i.getString("icon"),
            )
        }
        return TemplateDraft(o.getLong("templateId").takeIf { it >= 0 }, o.getString("name"), items)
    }
}
