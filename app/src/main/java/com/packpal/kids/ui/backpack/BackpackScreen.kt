package com.packpal.kids.ui.backpack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.packpal.kids.R
import com.packpal.kids.domain.Category
import com.packpal.kids.ui.components.ConfirmDialog
import com.packpal.kids.ui.components.ParentsHoldButton
import com.packpal.kids.ui.components.PrimaryButton
import com.packpal.kids.ui.components.SecondaryButton
import com.packpal.kids.ui.packPalFactory
import com.packpal.kids.ui.theme.PackPalColors

@Composable
fun BackpackRoute(
    onOpenCompartment: (templateId: Long, Category) -> Unit,
    onOpenTemplates: () -> Unit,
    onStartCheck: (templateId: Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenParents: () -> Unit,
    vm: BackpackViewModel = viewModel(factory = packPalFactory { c, _ -> BackpackViewModel(c) }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    BackpackScreen(
        state = state,
        onOpenCompartment = { c -> state.template?.let { onOpenCompartment(it.id, c) } },
        onOpenTemplates = onOpenTemplates,
        onStartCheck = { state.template?.let { onStartCheck(it.id) } },
        onOpenHistory = onOpenHistory,
        onOpenParents = onOpenParents,
        onStartFresh = vm::startFresh,
        onDiscardCheck = vm::discardCheck,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackpackScreen(
    state: BackpackUiState,
    onOpenCompartment: (Category) -> Unit,
    onOpenTemplates: () -> Unit,
    onStartCheck: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenParents: () -> Unit,
    onStartFresh: () -> Unit,
    onDiscardCheck: () -> Unit,
) {
    var confirmFresh by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(PackPalColors.Cream)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (state.loading) return@Box
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("PackPal", style = MaterialTheme.typography.titleSmall, color = PackPalColors.NavySoft, modifier = Modifier.align(Alignment.Start))

            val check = state.activeCheck
            if (check != null) {
                ResumeCard(onResume = onStartCheck, onDiscard = { confirmDiscard = true })
                Spacer(Modifier.height(4.dp))
            }

            OpenBackpack(
                templateName = state.template?.name,
                items = state.items,
                packedIds = state.packedIds,
                onTagClick = onOpenTemplates,
                onCompartmentClick = onOpenCompartment,
            )

            Spacer(Modifier.height(16.dp))
            val maxW = Modifier.widthIn(max = 520.dp).fillMaxWidth()
            when {
                !state.hasTemplate -> NoticeText("Ask a parent to add a list.", maxW)
                state.items.isEmpty() -> NoticeText("This list has no items yet. Ask a parent to add some.", maxW)
                else -> PrimaryButton(
                    text = if (check != null) "Resume check" else "Check before leaving",
                    onClick = onStartCheck,
                    icon = R.drawable.ic_ui_check,
                    modifier = maxW,
                )
            }

            Spacer(Modifier.height(16.dp))
            FlowRow(
                modifier = maxW,
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SecondaryButton("History", onOpenHistory, icon = R.drawable.ic_ui_history)
                if (state.hasTemplate && state.items.isNotEmpty()) {
                    SecondaryButton(
                        "Start fresh",
                        onClick = { if (state.hasProgress) { confirmFresh = true } else { onStartFresh() } },
                        icon = R.drawable.ic_ui_refresh,
                    )
                }
                ParentsHoldButton(onOpen = onOpenParents)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmFresh) {
        ConfirmDialog(
            title = "Start a fresh pack?",
            text = "Every item in this list will be unpacked again. Any unfinished check is cleared. Saved history stays.",
            confirmLabel = "Start fresh",
            onConfirm = { confirmFresh = false; onStartFresh() },
            onDismiss = { confirmFresh = false },
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = "Discard this check?",
            text = "The unfinished check will be removed. Nothing is saved to history. Packed items stay packed.",
            confirmLabel = "Discard",
            destructive = true,
            onConfirm = { confirmDiscard = false; onDiscardCheck() },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun NoticeText(text: String, modifier: Modifier) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PackPalColors.CoralSoft)
            .border(2.dp, PackPalColors.Navy, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ResumeCard(onResume: () -> Unit, onDiscard: () -> Unit) {
    Column(
        Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PackPalColors.YellowSoft)
            .border(2.dp, PackPalColors.Navy, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text("You have an unfinished check.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Resume check", onResume, modifier = Modifier.weight(1f))
            SecondaryButton("Discard", onDiscard, modifier = Modifier.width(130.dp))
        }
    }
}
