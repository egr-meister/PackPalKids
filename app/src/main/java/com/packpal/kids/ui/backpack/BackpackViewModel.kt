package com.packpal.kids.ui.backpack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packpal.kids.AppContainer
import com.packpal.kids.data.repository.ActiveCheck
import com.packpal.kids.domain.Item
import com.packpal.kids.domain.TemplateSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BackpackUiState(
    val loading: Boolean = true,
    val template: TemplateSummary? = null,
    val items: List<Item> = emptyList(),
    val packedIds: Set<Long> = emptySet(),
    val activeCheck: ActiveCheck? = null,
) {
    val hasTemplate: Boolean get() = template != null
    val hasProgress: Boolean get() = packedIds.isNotEmpty() || activeCheck != null
}

@OptIn(ExperimentalCoroutinesApi::class)
class BackpackViewModel(private val c: AppContainer) : ViewModel() {

    private val selected = combine(c.preferences.preferences, c.templates.observeTemplates()) { prefs, templates ->
        if (!prefs.seeded) null to false
        else (templates.firstOrNull { it.id == prefs.selectedTemplateId } ?: templates.firstOrNull()) to true
    }.distinctUntilChanged()

    val state: StateFlow<BackpackUiState> = selected.flatMapLatest { (template, ready) ->
        when {
            !ready -> flowOf(BackpackUiState(loading = true))
            template == null -> flowOf(BackpackUiState(loading = false))
            else -> combine(
                c.templates.observeItems(template.id),
                c.packing.observePackedIds(template.id),
                c.packing.observeActiveCheck(template.id),
            ) { items, packed, check ->
                val itemIds = items.map { it.id }.toSet()
                BackpackUiState(false, template, items, packed.intersect(itemIds), check)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackpackUiState())

    fun startFresh() {
        val id = state.value.template?.id ?: return
        viewModelScope.launch { c.packing.startFresh(id) }
    }

    fun discardCheck() {
        val attempt = state.value.activeCheck?.attemptId ?: return
        viewModelScope.launch { c.packing.discardCheck(attempt) }
    }

}
