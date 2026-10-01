package com.packpal.kids.ui.parent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.packpal.kids.R
import com.packpal.kids.domain.Category
import com.packpal.kids.domain.DraftItem
import com.packpal.kids.domain.Limits
import com.packpal.kids.ui.components.ArtIcon
import com.packpal.kids.ui.components.ConfirmDialog
import com.packpal.kids.ui.components.ItemIcons
import com.packpal.kids.ui.components.PackPalScaffold
import com.packpal.kids.ui.components.SecondaryButton
import com.packpal.kids.ui.components.UiIcon
import com.packpal.kids.ui.components.categoryIcon
import com.packpal.kids.ui.components.rememberSnackbar
import com.packpal.kids.ui.packPalFactory
import com.packpal.kids.ui.theme.PackPalColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplateEditorRoute(
    onClose: () -> Unit,
    vm: TemplateEditorViewModel = viewModel(factory = packPalFactory { c, h -> TemplateEditorViewModel(c, h) }),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = rememberSnackbar()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var iconPickerFor by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoryPickerFor by rememberSaveable { mutableStateOf<Long?>(null) }

    val requestClose: () -> Unit = { if (s.dirty) { confirmDiscard = true } else { onClose() } }
    BackHandler(enabled = s.dirty) { confirmDiscard = true }

    PackPalScaffold(
        title = if (s.isNew) "New list" else "Edit list",
        onBack = requestClose,
        backLabel = "Cancel",
        backIcon = R.drawable.ic_ui_close,
        snackbarHostState = snackbar,
        actions = {
            TextButton(
                onClick = { vm.requestSave(onClose) },
                enabled = !s.loading && !s.saving && !s.notFound,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) { Text("Save", style = MaterialTheme.typography.labelLarge, color = PackPalColors.Navy) }
        },
    ) { padding ->
        if (s.loading) return@PackPalScaffold
        if (s.notFound) {
            Text("This list no longer exists.", modifier = Modifier.padding(padding).padding(24.dp), style = MaterialTheme.typography.bodyLarge)
            return@PackPalScaffold
        }
        val itemsByCategory = s.draft.items.groupBy { it.category }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "name") {
                val err = if (s.showErrors) s.errors.templateName else null
                OutlinedTextField(
                    value = s.draft.name,
                    onValueChange = vm::setName,
                    label = { Text("List name") },
                    singleLine = true,
                    isError = err != null,
                    supportingText = { Text(err ?: "${s.draft.name.trim().length} / ${Limits.TEMPLATE_NAME_MAX}") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    colors = fieldColors(),
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                )
            }
            val general = if (s.showErrors) s.errors.general else null
            if (general != null) {
                item(key = "general") {
                    Text(general, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth())
                }
            }
            item(key = "count") {
                Text(
                    "${s.draft.items.size} of ${Limits.MAX_ITEMS} items",
                    style = MaterialTheme.typography.bodyMedium, color = PackPalColors.NavySoft,
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                )
            }
            Category.entries.forEach { category ->
                val list = itemsByCategory[category].orEmpty()
                item(key = "header-${category.name}") {
                    Row(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ArtIcon(categoryIcon(category), size = 30.dp)
                        Spacer(Modifier.width(8.dp))
                        SectionTitle("${category.label} (${list.size})")
                    }
                }
                items(list, key = { it.key }) { item ->
                    val idx = list.indexOf(item)
                    ItemEditorCard(
                        item = item,
                        nameError = if (s.showErrors) s.errors.itemNames[item.key] else null,
                        noteError = if (s.showErrors) s.errors.notes[item.key] else null,
                        canMoveUp = idx > 0,
                        canMoveDown = idx < list.size - 1,
                        onName = { vm.setItemName(item.key, it) },
                        onNote = { vm.setItemNote(item.key, it) },
                        onPickIcon = { iconPickerFor = item.key },
                        onPickCategory = { categoryPickerFor = item.key },
                        onMoveUp = { vm.move(item.key, up = true) },
                        onMoveDown = { vm.move(item.key, up = false) },
                        onRemove = { vm.removeItem(item.key) },
                    )
                }
                item(key = "add-${category.name}") {
                    SecondaryButton(
                        "Add to ${category.label}", { vm.addItem(category) },
                        enabled = s.canAddItem, icon = R.drawable.ic_ui_add,
                        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                    )
                }
            }
            if (!s.isNew) {
                item(key = "actions") {
                    Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle("This list")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton("Duplicate list", {
                                scope.launch {
                                    if (s.dirty) snackbar.showSnackbar("Save or cancel your changes first.")
                                    else {
                                        val id = vm.duplicateSaved()
                                        snackbar.showSnackbar(if (id != null) "Copy created." else "You can have up to ${Limits.MAX_TEMPLATES} lists.")
                                    }
                                }
                            }, icon = R.drawable.ic_ui_copy)
                            SecondaryButton("Delete list", { confirmDelete = true }, icon = R.drawable.ic_ui_delete, container = PackPalColors.CoralSoft)
                        }
                    }
                }
            }
            item(key = "spacer") { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = "Discard changes?",
            text = "Your unsaved changes to this list will be lost.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            destructive = true,
            onConfirm = { confirmDiscard = false; onClose() },
            onDismiss = { confirmDiscard = false },
        )
    }
    if (s.confirmRestart) {
        ConfirmDialog(
            title = "Restart packing for this list?",
            text = "This list has packing progress or an unfinished check. Saving your changes will restart its packing session: " +
                "every item becomes unpacked and the unfinished check is cleared. Saved history does not change.",
            confirmLabel = "Save and restart",
            onConfirm = { vm.confirmRestartAndSave(onClose) },
            onDismiss = vm::dismissRestart,
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this list?",
            text = "The list, its items and its current packing progress will be deleted. Saved check history stays.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { confirmDelete = false; vm.deleteTemplate(onClose) },
            onDismiss = { confirmDelete = false },
        )
    }
    iconPickerFor?.let { key ->
        IconPickerDialog(
            selected = s.draft.items.firstOrNull { it.key == key }?.iconKey,
            onPick = { vm.setItemIcon(key, it); iconPickerFor = null },
            onDismiss = { iconPickerFor = null },
        )
    }
    categoryPickerFor?.let { key ->
        CategoryPickerDialog(
            selected = s.draft.items.firstOrNull { it.key == key }?.category,
            onPick = { vm.setItemCategory(key, it); categoryPickerFor = null },
            onDismiss = { categoryPickerFor = null },
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PackPalColors.Navy,
    unfocusedBorderColor = PackPalColors.NavySoft,
    focusedLabelColor = PackPalColors.Navy,
    cursorColor = PackPalColors.Navy,
    focusedContainerColor = PackPalColors.White,
    unfocusedContainerColor = PackPalColors.White,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemEditorCard(
    item: DraftItem,
    nameError: String?,
    noteError: String?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onName: (String) -> Unit,
    onNote: (String) -> Unit,
    onPickIcon: () -> Unit,
    onPickCategory: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val label = item.name.ifBlank { "new item" }
    Column(
        Modifier.widthIn(max = 640.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PackPalColors.CreamDeep)
            .border(1.5.dp, PackPalColors.NavySoft, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.padding(top = 6.dp).size(60.dp).clip(RoundedCornerShape(14.dp)).background(PackPalColors.White)
                    .border(1.5.dp, PackPalColors.Navy, RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClickLabel = "choose picture for $label", onClick = onPickIcon),
                contentAlignment = Alignment.Center,
            ) { ArtIcon(ItemIcons.res(item.iconKey), size = 40.dp, contentDescription = "Picture: ${ItemIcons.label(item.iconKey)}. Change") }
            Spacer(Modifier.width(10.dp))
            OutlinedTextField(
                value = item.name,
                onValueChange = onName,
                label = { Text("Item name") },
                singleLine = true,
                isError = nameError != null,
                supportingText = { Text(nameError ?: "${item.name.trim().length} / ${Limits.ITEM_NAME_MAX}") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                colors = fieldColors(),
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = item.note,
            onValueChange = onNote,
            label = { Text("Note for your child (optional)") },
            isError = noteError != null,
            supportingText = { Text(noteError ?: "${item.note.trim().length} / ${Limits.NOTE_MAX}") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(verticalArrangement = Arrangement.Center, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onPickCategory, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                Text("Pocket: ${item.category.label}", color = PackPalColors.Navy, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(48.dp)) {
                UiIcon(R.drawable.ic_ui_up, "Move $label up", tint = if (canMoveUp) PackPalColors.Navy else PackPalColors.NavySoft.copy(alpha = 0.4f))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(48.dp)) {
                UiIcon(R.drawable.ic_ui_down, "Move $label down", tint = if (canMoveDown) PackPalColors.Navy else PackPalColors.NavySoft.copy(alpha = 0.4f))
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                UiIcon(R.drawable.ic_ui_delete, "Remove $label")
            }
        }
    }
}

@Composable
private fun IconPickerDialog(selected: String?, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PackPalColors.Cream,
        title = { Text("Choose a picture") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(84.dp),
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ItemIcons.options, key = { it.key }) { opt ->
                    val isSel = opt.key == selected
                    Column(
                        Modifier.clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) PackPalColors.YellowSoft else PackPalColors.White)
                            .border(if (isSel) 2.5.dp else 1.dp, PackPalColors.Navy, RoundedCornerShape(12.dp))
                            .selectable(selected = isSel, role = Role.RadioButton) { onPick(opt.key) }
                            .padding(6.dp)
                            .semantics(mergeDescendants = true) { },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ArtIcon(opt.res, size = 40.dp)
                        Text(opt.label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = PackPalColors.Navy) } },
    )
}

@Composable
private fun CategoryPickerDialog(selected: Category?, onPick: (Category) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PackPalColors.Cream,
        title = { Text("Which pocket?") },
        text = {
            LazyColumn {
                items(Category.entries) { c ->
                    Row(
                        Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
                            .selectable(selected = c == selected, role = Role.RadioButton) { onPick(c) }
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = c == selected, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        ArtIcon(categoryIcon(c), size = 28.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(c.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = PackPalColors.Navy) } },
    )
}
