package com.packpal.kids.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.packpal.kids.AppContainer
import com.packpal.kids.R
import com.packpal.kids.domain.Limits
import com.packpal.kids.domain.TemplateSummary
import com.packpal.kids.ui.components.ArtIcon
import com.packpal.kids.ui.components.ConfirmDialog
import com.packpal.kids.ui.components.PackPalScaffold
import com.packpal.kids.ui.components.SecondaryButton
import com.packpal.kids.ui.components.UiIcon
import com.packpal.kids.ui.components.rememberSnackbar
import com.packpal.kids.ui.components.templateIcon
import com.packpal.kids.ui.packPalFactory
import com.packpal.kids.ui.theme.PackPalColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ParentState(
    val templates: List<TemplateSummary> = emptyList(),
    val animationsEnabled: Boolean = true,
    val historyCount: Int = 0,
)

class ParentViewModel(private val c: AppContainer) : ViewModel() {
    val state = combine(c.templates.observeTemplates(), c.preferences.preferences, c.history.observeLatest()) { t, p, h ->
        ParentState(t, p.animationsEnabled, h.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ParentState())

    fun setAnimations(enabled: Boolean) = viewModelScope.launch { c.preferences.setAnimationsEnabled(enabled) }

    suspend fun duplicate(id: Long): Long? = c.templates.duplicate(id)

    fun delete(id: Long) = viewModelScope.launch { c.templates.delete(id) }

    suspend fun restoreStarters(): Int = c.templates.insertStarters()

    suspend fun clearAll() = c.clearAllLocalData()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentHomeRoute(
    onBack: () -> Unit,
    onEditTemplate: (Long?) -> Unit,
    onManageHistory: () -> Unit,
    onPrivacy: () -> Unit,
    onDataCleared: () -> Unit,
    vm: ParentViewModel = viewModel(factory = packPalFactory { c, _ -> ParentViewModel(c) }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = rememberSnackbar()
    var deleteTarget by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmClearAll by rememberSaveable { mutableStateOf(false) }
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    val atLimit = state.templates.size >= Limits.MAX_TEMPLATES

    PackPalScaffold(title = "Parents", onBack = onBack, backLabel = "Close parent area", backIcon = R.drawable.ic_ui_close, snackbarHostState = snackbar) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { SectionTitle("Packing lists (${state.templates.size} of ${Limits.MAX_TEMPLATES})") }
            if (state.templates.isEmpty()) {
                item { Text("No lists. Add a new list or restore the starter lists.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) }
            }
            items(state.templates, key = { it.id }) { t ->
                TemplateRow(
                    t,
                    onEdit = { onEditTemplate(t.id) },
                    onDuplicate = {
                        scope.launch {
                            val id = vm.duplicate(t.id)
                            snackbar.showSnackbar(if (id != null) "Copy of ${t.name} created." else "You can have up to ${Limits.MAX_TEMPLATES} lists.")
                        }
                    },
                    onDelete = { deleteTarget = t.id },
                    duplicateEnabled = !atLimit,
                )
            }
            item {
                FlowRow(
                    Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SecondaryButton("New list", { onEditTemplate(null) }, enabled = !atLimit, icon = R.drawable.ic_ui_add)
                    SecondaryButton("Restore starter lists", { confirmRestore = true }, enabled = !atLimit, icon = R.drawable.ic_ui_refresh)
                }
                if (atLimit) Text("You have reached the limit of ${Limits.MAX_TEMPLATES} lists.", style = MaterialTheme.typography.bodyMedium)
            }

            item { Spacer(Modifier.height(8.dp)); SectionTitle("Check history") }
            item {
                SettingsRow(
                    title = "Manage history",
                    subtitle = if (state.historyCount == 0) "No saved checks" else "${state.historyCount} saved checks (latest ${Limits.HISTORY_MAX} are kept)",
                    onClick = onManageHistory,
                )
            }

            item { Spacer(Modifier.height(8.dp)); SectionTitle("Preferences") }
            item {
                Row(
                    Modifier.widthIn(max = 640.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PackPalColors.White)
                        .border(1.5.dp, PackPalColors.NavySoft, RoundedCornerShape(16.dp)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Animations", style = MaterialTheme.typography.titleSmall)
                        Text("Short checkmark and pocket animations", style = MaterialTheme.typography.bodyMedium, color = PackPalColors.NavySoft)
                    }
                    Switch(
                        checked = state.animationsEnabled,
                        onCheckedChange = { vm.setAnimations(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = PackPalColors.TealDark),
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)); SectionTitle("Privacy and data") }
            item { SettingsRow("Privacy", "What PackPal Kids stores and where", onPrivacy) }
            item {
                SettingsRow("Clear all local data", "Delete all lists, progress and history", onClick = { confirmClearAll = true }, destructive = true)
            }
        }
    }

    deleteTarget?.let { id ->
        val name = state.templates.firstOrNull { it.id == id }?.name ?: "this list"
        ConfirmDialog(
            title = "Delete $name?",
            text = "The list, its items and its current packing progress will be deleted. Saved check history stays.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { deleteTarget = null; vm.delete(id) },
            onDismiss = { deleteTarget = null },
        )
    }
    if (confirmRestore) {
        ConfirmDialog(
            title = "Restore starter lists?",
            text = "School, Sport, Art Class, Weekend and Trip will be added again. Your lists and history are kept; names get a number if they already exist.",
            confirmLabel = "Restore",
            onConfirm = {
                confirmRestore = false
                scope.launch {
                    val added = vm.restoreStarters()
                    snackbar.showSnackbar(if (added == 0) "No room for more lists." else "Added $added starter lists.")
                }
            },
            onDismiss = { confirmRestore = false },
        )
    }
    if (confirmClearAll) {
        ConfirmDialog(
            title = "Clear all local data?",
            text = "This deletes every list, item, packing progress, unfinished check, saved check history and preference on this device. " +
                "The app then starts again with the five starter lists. This cannot be undone.",
            confirmLabel = "Delete everything",
            destructive = true,
            onConfirm = {
                confirmClearAll = false
                scope.launch { vm.clearAll(); onDataCleared() }
            },
            onDismiss = { confirmClearAll = false },
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().semantics { heading() },
    )
}

@Composable
fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit, destructive: Boolean = false) {
    Row(
        Modifier.widthIn(max = 640.dp).fillMaxWidth().defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(16.dp)).background(if (destructive) PackPalColors.CoralSoft else PackPalColors.White)
            .border(1.5.dp, PackPalColors.NavySoft, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = PackPalColors.NavySoft)
        }
        UiIcon(R.drawable.ic_ui_chevron, null, size = 20.dp)
    }
}

@Composable
private fun TemplateRow(t: TemplateSummary, onEdit: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit, duplicateEnabled: Boolean) {
    Row(
        Modifier.widthIn(max = 640.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PackPalColors.White)
            .border(1.5.dp, PackPalColors.NavySoft, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClickLabel = "edit ${t.name}", onClick = onEdit)
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtIcon(templateIcon(t.name), size = 34.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name, style = MaterialTheme.typography.titleSmall)
            Text(if (t.itemCount == 1) "1 item" else "${t.itemCount} items", style = MaterialTheme.typography.bodyMedium, color = PackPalColors.NavySoft)
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) { UiIcon(R.drawable.ic_ui_edit, "Edit ${t.name}") }
        IconButton(onClick = onDuplicate, enabled = duplicateEnabled, modifier = Modifier.size(48.dp)) {
            UiIcon(R.drawable.ic_ui_copy, "Duplicate ${t.name}", tint = if (duplicateEnabled) PackPalColors.Navy else PackPalColors.NavySoft)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) { UiIcon(R.drawable.ic_ui_delete, "Delete ${t.name}") }
    }
}

