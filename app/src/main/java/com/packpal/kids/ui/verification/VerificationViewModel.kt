package com.packpal.kids.ui.verification

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packpal.kids.AppContainer
import com.packpal.kids.domain.Item
import com.packpal.kids.domain.PackingRules
import com.packpal.kids.domain.VerificationRules
import com.packpal.kids.domain.VerifyEntry
import com.packpal.kids.domain.VerifyResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface VerifyUi {
    data object Loading : VerifyUi
    data object NoItems : VerifyUi
    data class Precheck(val unpacked: List<Item>) : VerifyUi
    data class Reviewing(
        val attemptId: Long,
        val entry: VerifyEntry,
        val position: Int,
        val roundTotal: Int,
        val round: Int,
        val result: VerifyResult,
    ) : VerifyUi
    data class Summary(val attemptId: Long, val result: VerifyResult, val missing: List<VerifyEntry>) : VerifyUi
    data class Saved(val result: VerifyResult) : VerifyUi
}

class VerificationViewModel(private val c: AppContainer, private val handle: SavedStateHandle) : ViewModel() {
    val templateId: Long = checkNotNull(handle["templateId"])

    /** The child chose "Check anyway" despite unpacked items. */
    private val proceedAnyway = handle.getStateFlow(KEY_PROCEED, false)
    /** Set while leaving after a discard so no new check is started automatically. */
    private val closing = MutableStateFlow(false)
    private val flags = combine(proceedAnyway, closing) { p, cl -> p to cl }
    private val saved = MutableStateFlow<VerifyResult?>(null)
    private var busy = false

    val state: StateFlow<VerifyUi> = combine(
        c.templates.observeItems(templateId),
        c.packing.observePackedIds(templateId),
        c.packing.observeActiveCheck(templateId),
        flags,
        saved,
    ) { items, packed, check, (proceed, isClosing), savedResult ->
        when {
            isClosing -> VerifyUi.Loading
            savedResult != null -> VerifyUi.Saved(savedResult)
            check != null -> {
                val entries = check.entries
                val idx = VerificationRules.currentIndex(entries)
                val result = VerificationRules.result(entries)
                if (idx != null) {
                    val queue = VerificationRules.roundQueue(entries)
                    VerifyUi.Reviewing(check.attemptId, queue[idx], idx + 1, queue.size, check.round, result)
                } else {
                    VerifyUi.Summary(check.attemptId, result, VerificationRules.missing(entries))
                }
            }
            items.isEmpty() -> VerifyUi.NoItems
            else -> {
                val unpacked = PackingRules.unpacked(items, packed)
                if (unpacked.isNotEmpty() && !proceed) VerifyUi.Precheck(unpacked)
                else {
                    startIfNeeded()
                    VerifyUi.Loading
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VerifyUi.Loading)

    private fun startIfNeeded() {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try { c.packing.startCheck(templateId) } finally { busy = false }
        }
    }

    fun checkAnyway() {
        handle[KEY_PROCEED] = true
    }

    fun answer(attemptId: Long, entry: VerifyEntry, isHere: Boolean) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try { c.packing.answer(templateId, attemptId, entry.sourceItemId, isHere) } finally { busy = false }
        }
    }

    fun recheckMissing(attemptId: Long) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try { c.packing.recheckMissing(attemptId) } finally { busy = false }
        }
    }

    /** Duplicate-safe: ignored while a save is running, and the repository refuses a second save of the same attempt. */
    fun save(summary: VerifyUi.Summary) {
        if (busy || saved.value != null) return
        busy = true
        viewModelScope.launch {
            try {
                val id = c.packing.saveCheck(summary.attemptId)
                if (id != null) saved.value = summary.result
            } finally { busy = false }
        }
    }

    fun discard(attemptId: Long, then: () -> Unit) {
        closing.value = true
        viewModelScope.launch {
            c.packing.discardCheck(attemptId)
            then()
        }
    }

    fun startNewPack(then: () -> Unit) {
        viewModelScope.launch {
            c.packing.startFresh(templateId)
            then()
        }
    }

    private companion object {
        const val KEY_PROCEED = "proceedAnyway"
    }
}
