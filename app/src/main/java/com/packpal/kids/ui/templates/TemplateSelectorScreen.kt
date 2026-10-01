package com.packpal.kids.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.packpal.kids.AppContainer
import com.packpal.kids.R
import com.packpal.kids.domain.TemplateSummary
import com.packpal.kids.ui.components.ArtIcon
import com.packpal.kids.ui.components.EmptyMessage
import com.packpal.kids.ui.components.PackPalScaffold
import com.packpal.kids.ui.components.UiIcon
import com.packpal.kids.ui.components.templateIcon
import com.packpal.kids.ui.packPalFactory
import com.packpal.kids.ui.theme.PackPalColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SelectorState(val templates: List<TemplateSummary> = emptyList(), val selectedId: Long? = null)

class TemplateSelectorViewModel(private val c: AppContainer) : ViewModel() {
    val state = combine(c.templates.observeTemplates(), c.preferences.preferences) { t, p ->
        SelectorState(t, (t.firstOrNull { it.id == p.selectedTemplateId } ?: t.firstOrNull())?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SelectorState())

    fun select(id: Long, then: () -> Unit) {
        viewModelScope.launch {
            c.preferences.setSelectedTemplate(id)
            then()
        }
    }
}

private val TagRowShape = GenericShape { size, _ ->
    val n = size.height * 0.28f
    moveTo(n, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height); lineTo(n, size.height)
    lineTo(0f, size.height - n); lineTo(0f, n); close()
}

@Composable
fun TemplateSelectorRoute(
    onBack: () -> Unit,
    vm: TemplateSelectorViewModel = viewModel(factory = packPalFactory { c, _ -> TemplateSelectorViewModel(c) }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    PackPalScaffold(title = "Choose a list", onBack = onBack) { padding ->
        if (state.templates.isEmpty()) {
            EmptyMessage("Ask a parent to add a list.", Modifier.padding(padding).padding(24.dp))
            return@PackPalScaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(state.templates, key = { it.id }) { t ->
                val selected = t.id == state.selectedId
                Row(
                    Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 84.dp)
                        .clip(TagRowShape)
                        .background(if (selected) PackPalColors.YellowSoft else PackPalColors.White)
                        .border(if (selected) 3.dp else 2.dp, PackPalColors.Navy, TagRowShape)
                        .clickable(role = Role.Button, onClickLabel = "choose ${t.name}") { vm.select(t.id, onBack) }
                        .semantics(mergeDescendants = true) { this.selected = selected }
                        .padding(start = 26.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(PackPalColors.Cream).border(2.dp, PackPalColors.Navy, CircleShape))
                    Spacer(Modifier.width(14.dp))
                    ArtIcon(templateIcon(t.name), size = 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            when (t.itemCount) { 0 -> "No items yet"; 1 -> "1 item"; else -> "${t.itemCount} items" },
                            style = MaterialTheme.typography.bodyMedium, color = PackPalColors.NavySoft,
                        )
                    }
                    if (selected) {
                        UiIcon(R.drawable.ic_ui_check, "Current list", size = 28.dp, tint = PackPalColors.TealDark)
                    }
                }
            }
        }
    }
}
