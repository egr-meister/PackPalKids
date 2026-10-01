package com.packpal.kids.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.packpal.kids.AppContainer
import com.packpal.kids.R
import com.packpal.kids.data.repository.HistoryDetail
import com.packpal.kids.data.repository.HistorySummary
import com.packpal.kids.domain.VerificationState
import com.packpal.kids.ui.components.ArtIcon
import com.packpal.kids.ui.components.EmptyMessage
import com.packpal.kids.ui.components.ItemIcons
import com.packpal.kids.ui.components.PackPalScaffold
import com.packpal.kids.ui.components.UiIcon
import com.packpal.kids.ui.packPalFactory
import com.packpal.kids.ui.theme.PackPalColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Absolute timestamps rendered in the device's current time zone. */
fun formatLocalDateTime(epochMillis: Long): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))

data class HistoryListState(val loaded: Boolean = false, val entries: List<HistorySummary> = emptyList())

class HistoryViewModel(c: AppContainer) : ViewModel() {
    val state: StateFlow<HistoryListState> = c.history.observeLatest().map { HistoryListState(true, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryListState())
}

class HistoryDetailViewModel(c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val id: Long = checkNotNull(handle["historyId"])
    val state: StateFlow<HistoryDetail?> = c.history.observeDetail(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun StatusChip(allChecked: Boolean) {
    Row(
        Modifier.clip(RoundedCornerShape(12.dp))
            .background(if (allChecked) PackPalColors.TealLight else PackPalColors.CoralSoft)
            .border(1.5.dp, PackPalColors.Navy, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UiIcon(if (allChecked) R.drawable.ic_ui_check else R.drawable.ic_ui_missing, null, size = 18.dp)
        Spacer(Modifier.width(4.dp))
        Text(if (allChecked) "All checked" else "Missing items", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun HistoryRow(entry: HistorySummary, onClick: (() -> Unit)?, trailing: @Composable () -> Unit = {}) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 76.dp)
            .clip(shape)
            .background(PackPalColors.White)
            .border(2.dp, PackPalColors.Navy, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = "open check", onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { }) {
            Text(entry.templateName, style = MaterialTheme.typography.titleMedium)
            Text(formatLocalDateTime(entry.completedAt), style = MaterialTheme.typography.bodyMedium, color = PackPalColors.NavySoft)
            Spacer(Modifier.padding(top = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${entry.confirmed} of ${entry.total} confirmed", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(10.dp))
                StatusChip(entry.allChecked)
            }
        }
        trailing()
    }
}

@Composable
fun HistoryRoute(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    vm: HistoryViewModel = viewModel(factory = packPalFactory { c, _ -> HistoryViewModel(c) }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    PackPalScaffold(title = "Check history", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.loaded && state.entries.isEmpty()) {
                item { EmptyMessage("No checks saved yet. Finish a \"Check before leaving\" and save it to see it here.", Modifier.padding(24.dp)) }
            }
            items(state.entries, key = { it.id }) { e -> HistoryRow(e, onClick = { onOpen(e.id) }) {
                UiIcon(R.drawable.ic_ui_chevron, null, size = 22.dp)
            } }
        }
    }
}

@Composable
fun HistoryDetailRoute(
    onBack: () -> Unit,
    vm: HistoryDetailViewModel = viewModel(factory = packPalFactory { c, h -> HistoryDetailViewModel(c, h) }),
) {
    val detail by vm.state.collectAsStateWithLifecycle()
    PackPalScaffold(title = detail?.summary?.templateName ?: "Check", onBack = onBack) { padding ->
        val d = detail
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (d == null) return@LazyColumn
            item { HistoryRow(d.summary, onClick = null) }
            items(d.items) { item ->
                val missing = item.state == VerificationState.MISSING
                Row(
                    Modifier.widthIn(max = 640.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(if (missing) PackPalColors.CoralSoft else PackPalColors.White)
                        .border(1.5.dp, PackPalColors.Navy, RoundedCornerShape(16.dp)).padding(12.dp)
                        .semantics(mergeDescendants = true) { },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtIcon(ItemIcons.res(item.iconKey), size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text(item.category.label, style = MaterialTheme.typography.bodyMedium, color = PackPalColors.NavySoft)
                    }
                    UiIcon(if (missing) R.drawable.ic_ui_missing else R.drawable.ic_ui_check, null, size = 22.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        when (item.state) {
                            VerificationState.CONFIRMED -> "Confirmed"
                            VerificationState.MISSING -> "Missing"
                            VerificationState.UNREVIEWED -> "Not checked"
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
