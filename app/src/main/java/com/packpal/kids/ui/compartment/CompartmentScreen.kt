package com.packpal.kids.ui.compartment

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.packpal.kids.AppContainer
import com.packpal.kids.R
import com.packpal.kids.domain.Category
import com.packpal.kids.domain.Item
import com.packpal.kids.domain.PackingRules
import com.packpal.kids.ui.components.ArtIcon
import com.packpal.kids.ui.components.EmptyMessage
import com.packpal.kids.ui.components.ItemIcons
import com.packpal.kids.ui.components.PackPalScaffold
import com.packpal.kids.ui.components.UiIcon
import com.packpal.kids.ui.components.categoryIcon
import com.packpal.kids.ui.packPalFactory
import com.packpal.kids.ui.theme.LocalAnimationsEnabled
import com.packpal.kids.ui.theme.PackPalColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CompartmentState(val loaded: Boolean = false, val items: List<Item> = emptyList(), val packed: Set<Long> = emptySet())

class CompartmentViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val templateId: Long = checkNotNull(handle["templateId"])
    val category: Category = Category.fromKey(checkNotNull(handle["category"]))

    val state = combine(c.templates.observeItems(templateId), c.packing.observePackedIds(templateId)) { items, packed ->
        CompartmentState(true, items.filter { it.category == category }.sortedBy { it.displayOrder }, packed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompartmentState())

    fun toggle(item: Item, packed: Boolean) {
        viewModelScope.launch { c.packing.setPacked(templateId, item.id, packed) }
    }
}

@Composable
fun CompartmentRoute(
    onBack: () -> Unit,
    vm: CompartmentViewModel = viewModel(factory = packPalFactory { c, h -> CompartmentViewModel(c, h) }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val progress = PackingRules.progress(state.items, state.packed)
    PackPalScaffold(title = vm.category.label, onBack = onBack, backLabel = "Back to backpack") { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "header") {
                Column(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ArtIcon(categoryIcon(vm.category), size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (state.items.isEmpty()) "No items" else "${progress.done} of ${progress.total} packed",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                    if (state.items.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress.fraction },
                            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                            color = PackPalColors.TealDark,
                            trackColor = PackPalColors.CreamDeep,
                            strokeCap = StrokeCap.Round,
                            gapSize = 0.dp,
                            drawStopIndicator = {},
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Tap an item when it is in your bag.", style = MaterialTheme.typography.bodyLarge, color = PackPalColors.NavySoft)
                    }
                }
            }
            if (state.loaded && state.items.isEmpty()) {
                item(key = "empty") { EmptyMessage("Nothing goes in this pocket for this list.", Modifier.padding(24.dp)) }
            }
            items(state.items, key = { it.id }) { item ->
                PackItemCard(item, item.id in state.packed, onToggle = { vm.toggle(item, it) })
            }
        }
    }
}

@Composable
fun PackItemCard(item: Item, packed: Boolean, onToggle: (Boolean) -> Unit) {
    val animate = LocalAnimationsEnabled.current
    val bg by animateColorAsState(if (packed) PackPalColors.TealLight else PackPalColors.White, if (animate) tween(220) else snap(), label = "card")
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 96.dp)
            .clip(shape)
            .background(bg)
            .border(if (packed) 3.dp else 2.dp, PackPalColors.Navy, shape)
            .toggleable(value = packed, role = Role.Checkbox, onValueChange = onToggle)
            .semantics { stateDescription = if (packed) "Packed" else "Not packed" }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(PackPalColors.Cream).border(1.5.dp, PackPalColors.Navy, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) { ArtIcon(ItemIcons.res(item.iconKey), size = 46.dp) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleLarge)
            if (item.note.isNotBlank()) {
                Text(item.note, style = MaterialTheme.typography.bodyLarge, color = PackPalColors.NavySoft)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(if (packed) PackPalColors.Yellow else PackPalColors.Cream)
                    .border(2.dp, PackPalColors.Navy, CircleShape),
                contentAlignment = Alignment.Center,
            ) { if (packed) UiIcon(R.drawable.ic_ui_check, null, size = 26.dp) }
            Text(if (packed) "Packed" else "Not packed", style = MaterialTheme.typography.labelMedium)
        }
    }
}
