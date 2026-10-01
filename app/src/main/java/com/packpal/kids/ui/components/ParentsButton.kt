package com.packpal.kids.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.packpal.kids.R
import com.packpal.kids.ui.theme.PackPalColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val HOLD_MILLIS = 3000

/**
 * "Parents" control: press and hold for three seconds. A short tap (or the accessibility action) opens a
 * dialog with an alternative way in. This only prevents accidental entry; it is not a security boundary.
 */
@Composable
fun ParentsHoldButton(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val open by rememberUpdatedState(onOpen)
    var showAlternative by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp, minWidth = 120.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PackPalColors.White)
            .border(2.dp, PackPalColors.Navy, RoundedCornerShape(18.dp))
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Parents. Press and hold for 3 seconds, or double tap for another way in."
                onClick(label = "open the parent check") { showAlternative = true; true }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var completed = false
                    val job: Job = scope.launch {
                        progress.snapTo(0f)
                        progress.animateTo(1f, tween(HOLD_MILLIS, easing = LinearEasing))
                        completed = true
                        progress.snapTo(0f)
                        open()
                    }
                    val up = waitForUpOrCancellation()
                    if (!completed) {
                        job.cancel()
                        val wasShortTap = progress.value < 0.15f
                        scope.launch { progress.animateTo(0f, tween(150)) }
                        if (up != null && wasShortTap) showAlternative = true
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .background(PackPalColors.YellowSoft)
        )
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            UiIcon(R.drawable.ic_ui_parents, null, size = 24.dp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Parents", style = MaterialTheme.typography.labelLarge)
                Text("Hold 3 sec", style = MaterialTheme.typography.labelSmall, color = PackPalColors.NavySoft)
            }
        }
    }

    if (showAlternative) {
        ParentCheckDialog(
            onSuccess = { showAlternative = false; open() },
            onDismiss = { showAlternative = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParentCheckDialog(onSuccess: () -> Unit, onDismiss: () -> Unit) {
    val a = rememberSaveable { Random.nextInt(6, 10) }
    val b = rememberSaveable { Random.nextInt(4, 9) }
    val answer = a + b
    val choices = rememberSaveable { listOf(answer, answer + 2, answer - 3, answer + 5).shuffled() }
    var wrong by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PackPalColors.Cream,
        title = { Text("For parents") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Press and hold the Parents button for 3 seconds, or answer this question:", style = MaterialTheme.typography.bodyLarge)
                Text("What is $a + $b?", style = MaterialTheme.typography.titleLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    choices.forEach { c ->
                        SecondaryButton(text = c.toString(), onClick = { if (c == answer) { onSuccess() } else { wrong = true } })
                    }
                }
                if (wrong) Text("Not quite. Try again.", color = PackPalColors.Navy, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) { Text("Cancel", color = PackPalColors.Navy) } },
    )
}
