package com.packpal.kids.ui.verification

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.packpal.kids.R
import com.packpal.kids.domain.Item
import com.packpal.kids.domain.VerifyEntry
import com.packpal.kids.ui.components.ArtIcon
import com.packpal.kids.ui.components.ConfirmDialog
import com.packpal.kids.ui.components.ItemIcons
import com.packpal.kids.ui.components.PackPalScaffold
import com.packpal.kids.ui.components.PrimaryButton
import com.packpal.kids.ui.components.SecondaryButton
import com.packpal.kids.ui.components.UiIcon
import com.packpal.kids.ui.packPalFactory
import com.packpal.kids.ui.theme.LocalAnimationsEnabled
import com.packpal.kids.ui.theme.PackPalColors

@Composable
fun VerificationRoute(
    onBackToBackpack: () -> Unit,
    vm: VerificationViewModel = viewModel(factory = packPalFactory { c, h -> VerificationViewModel(c, h) }),
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    var confirmDiscardFor by rememberSaveable { mutableStateOf<Long?>(null) }

    // Back (toolbar or system) simply leaves: every answer is already stored, so the check can be resumed.
    PackPalScaffold(title = "Check before leaving", onBack = onBackToBackpack, backLabel = "Back to backpack") { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val content = Modifier.widthIn(max = 560.dp).fillMaxWidth()
            when (val s = ui) {
                VerifyUi.Loading -> Spacer(Modifier.height(1.dp))
                VerifyUi.NoItems -> {
                    Text("This list has no items yet. Ask a parent to add some.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = content)
                    Spacer(Modifier.height(20.dp))
                    PrimaryButton("Back to backpack", onBackToBackpack, modifier = content)
                }
                is VerifyUi.Precheck -> Precheck(s.unpacked, onContinuePacking = onBackToBackpack, onCheckAnyway = vm::checkAnyway, modifier = content)
                is VerifyUi.Reviewing -> {
                    Reviewing(s, onAnswer = { here -> vm.answer(s.attemptId, s.entry, here) }, modifier = content)
                    Spacer(Modifier.height(24.dp))
                    TextButton(onClick = { confirmDiscardFor = s.attemptId }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                        Text("Discard check", color = PackPalColors.Navy)
                    }
                }
                is VerifyUi.Summary -> {
                    Summary(s, onRecheck = { vm.recheckMissing(s.attemptId) }, onSave = { vm.save(s) }, modifier = content)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { confirmDiscardFor = s.attemptId }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                        Text("Discard check", color = PackPalColors.Navy)
                    }
                }
                is VerifyUi.Saved -> Saved(s, onBack = onBackToBackpack, onNewPack = { vm.startNewPack(onBackToBackpack) }, modifier = content)
            }
        }
    }

    confirmDiscardFor?.let { id ->
        ConfirmDialog(
            title = "Discard this check?",
            text = "Your answers in this check will be removed and nothing is saved to history.",
            confirmLabel = "Discard",
            destructive = true,
            onConfirm = { confirmDiscardFor = null; vm.discard(id, onBackToBackpack) },
            onDismiss = { confirmDiscardFor = null },
        )
    }
}

@Composable
private fun Precheck(unpacked: List<Item>, onContinuePacking: () -> Unit, onCheckAnyway: () -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UiIcon(R.drawable.ic_ui_missing, null, size = 32.dp, tint = PackPalColors.Navy)
            Spacer(Modifier.width(10.dp))
            Text("Some things are still unpacked.", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        }
        unpacked.forEach { item ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PackPalColors.CoralSoft)
                    .border(2.dp, PackPalColors.Navy, RoundedCornerShape(16.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtIcon(ItemIcons.res(item.iconKey), size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(item.category.label, style = MaterialTheme.typography.bodyMedium, color = PackPalColors.NavySoft)
                }
                Text("Not packed", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Continue packing", onContinuePacking, modifier = Modifier.fillMaxWidth(), icon = R.drawable.ic_ui_back)
        SecondaryButton("Check anyway", onCheckAnyway, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Reviewing(s: VerifyUi.Reviewing, onAnswer: (Boolean) -> Unit, modifier: Modifier) {
    val animate = LocalAnimationsEnabled.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (s.round > 1) {
            Text("Rechecking missing items", style = MaterialTheme.typography.titleMedium, color = PackPalColors.NavySoft)
            Spacer(Modifier.height(4.dp))
        }
        Text(
            "Item ${s.position} of ${s.roundTotal}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (s.result.total == 0) 0f else s.result.reviewed.toFloat() / s.result.total },
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
            color = PackPalColors.TealDark,
            trackColor = PackPalColors.CreamDeep,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Text("${s.result.reviewed} of ${s.result.total} checked", style = MaterialTheme.typography.bodyMedium, color = PackPalColors.NavySoft)
        Spacer(Modifier.height(16.dp))

        AnimatedContent(
            targetState = s.entry,
            transitionSpec = {
                if (animate) (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.96f)) togetherWith fadeOut(tween(120))
                else fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            },
            contentKey = { it.sourceItemId to s.round },
            label = "item",
        ) { entry -> ItemFocusCard(entry) }

        Spacer(Modifier.height(20.dp))
        PrimaryButton("It's here", onClick = { onAnswer(true) }, icon = R.drawable.ic_ui_check, modifier = Modifier.fillMaxWidth().height(68.dp))
        Spacer(Modifier.height(12.dp))
        SecondaryButton(
            "Still missing", onClick = { onAnswer(false) }, icon = R.drawable.ic_ui_missing,
            accent = PackPalColors.Navy, container = PackPalColors.CoralSoft,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        )
    }
}

@Composable
private fun ItemFocusCard(entry: VerifyEntry) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(PackPalColors.White)
            .border(3.dp, PackPalColors.Navy, RoundedCornerShape(28.dp)).padding(24.dp)
            .semantics(mergeDescendants = true) { },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(140.dp).clip(CircleShape).background(PackPalColors.Cream).border(2.dp, PackPalColors.Navy, CircleShape),
            contentAlignment = Alignment.Center,
        ) { ArtIcon(ItemIcons.res(entry.iconKey), size = 96.dp) }
        Spacer(Modifier.height(16.dp))
        Text(entry.name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            entry.category.label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(PackPalColors.TealLight).padding(horizontal = 12.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text("Is it in your bag?", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Summary(s: VerifyUi.Summary, onRecheck: () -> Unit, onSave: () -> Unit, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (s.result.allChecked) {
            Box(
                Modifier.size(120.dp).clip(CircleShape).background(PackPalColors.Yellow).border(3.dp, PackPalColors.Navy, CircleShape),
                contentAlignment = Alignment.Center,
            ) { UiIcon(R.drawable.ic_ui_check, null, size = 72.dp) }
            Text("Everything checked!", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite })
            Text("All ${s.result.total} items are in the bag.", style = MaterialTheme.typography.bodyLarge)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UiIcon(R.drawable.ic_ui_missing, null, size = 34.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (s.missing.size == 1) "1 thing is missing" else "${s.missing.size} things are missing",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite },
                )
            }
            Text("${s.result.confirmed} of ${s.result.total} checked", style = MaterialTheme.typography.bodyLarge)
            s.missing.forEach { e ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PackPalColors.CoralSoft)
                        .border(2.dp, PackPalColors.Navy, RoundedCornerShape(16.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtIcon(ItemIcons.res(e.iconKey), size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name, style = MaterialTheme.typography.titleMedium)
                        Text(e.category.label, style = MaterialTheme.typography.bodyMedium, color = PackPalColors.NavySoft)
                    }
                    Text("Missing", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(4.dp))
            PrimaryButton("Recheck missing", onRecheck, icon = R.drawable.ic_ui_refresh, modifier = Modifier.fillMaxWidth())
        }
        if (s.result.allChecked) {
            PrimaryButton("Save check", onSave, modifier = Modifier.fillMaxWidth())
        } else {
            SecondaryButton("Save check", onSave, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Saved(s: VerifyUi.Saved, onBack: () -> Unit, onNewPack: () -> Unit, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        UiIcon(R.drawable.ic_ui_history, null, size = 56.dp)
        Text("Check saved", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite })
        Text(
            if (s.result.allChecked) "All ${s.result.total} items checked."
            else "${s.result.confirmed} of ${s.result.total} checked. ${s.result.missing} missing.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Back to backpack", onBack, modifier = Modifier.fillMaxWidth())
        SecondaryButton("Start a new pack", onNewPack, icon = R.drawable.ic_ui_refresh, modifier = Modifier.fillMaxWidth())
    }
}
