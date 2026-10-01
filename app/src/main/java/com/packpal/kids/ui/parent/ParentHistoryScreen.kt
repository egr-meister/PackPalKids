package com.packpal.kids.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.packpal.kids.AppContainer
import com.packpal.kids.R
import com.packpal.kids.ui.components.ConfirmDialog
import com.packpal.kids.ui.components.EmptyMessage
import com.packpal.kids.ui.components.PackPalScaffold
import com.packpal.kids.ui.components.SecondaryButton
import com.packpal.kids.ui.components.UiIcon
import com.packpal.kids.ui.history.HistoryRow
import com.packpal.kids.ui.history.HistoryViewModel
import com.packpal.kids.ui.packPalFactory
import kotlinx.coroutines.launch

class ParentHistoryActions(private val c: AppContainer) : ViewModel() {
    fun delete(id: Long) = viewModelScope.launch { c.history.delete(id) }
    fun clear() = viewModelScope.launch { c.history.clear() }
}

@Composable
fun ParentHistoryRoute(
    onBack: () -> Unit,
    listVm: HistoryViewModel = viewModel(factory = packPalFactory { c, _ -> HistoryViewModel(c) }),
    actions: ParentHistoryActions = viewModel(factory = packPalFactory { c, _ -> ParentHistoryActions(c) }),
) {
    val state by listVm.state.collectAsStateWithLifecycle()
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    PackPalScaffold(title = "Manage history", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.loaded && state.entries.isEmpty()) {
                item { EmptyMessage("No saved checks.", Modifier.padding(24.dp)) }
            } else if (state.entries.isNotEmpty()) {
                item {
                    SecondaryButton("Clear history", { confirmClear = true }, icon = R.drawable.ic_ui_delete, modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth())
                }
            }
            items(state.entries, key = { it.id }) { e ->
                HistoryRow(e, onClick = null) {
                    IconButton(onClick = { deleteId = e.id }, modifier = Modifier.size(48.dp)) {
                        UiIcon(R.drawable.ic_ui_delete, "Delete this check")
                    }
                }
            }
        }
    }

    deleteId?.let { id ->
        ConfirmDialog(
            title = "Delete this check?",
            text = "This saved check will be removed from history.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { deleteId = null; actions.delete(id) },
            onDismiss = { deleteId = null },
        )
    }
    if (confirmClear) {
        ConfirmDialog(
            title = "Clear all history?",
            text = "Every saved check will be removed. Lists and packing progress are not affected.",
            confirmLabel = "Clear history",
            destructive = true,
            onConfirm = { confirmClear = false; actions.clear() },
            onDismiss = { confirmClear = false },
        )
    }
}
