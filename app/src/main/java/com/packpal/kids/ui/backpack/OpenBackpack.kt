package com.packpal.kids.ui.backpack

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.packpal.kids.R
import com.packpal.kids.domain.Category
import com.packpal.kids.domain.Item
import com.packpal.kids.domain.PackingRules
import com.packpal.kids.domain.Progress
import com.packpal.kids.ui.components.ArtIcon
import com.packpal.kids.ui.components.ItemIcons
import com.packpal.kids.ui.components.UiIcon
import com.packpal.kids.ui.components.categoryIcon
import com.packpal.kids.ui.components.stitched
import com.packpal.kids.ui.theme.LocalAnimationsEnabled
import com.packpal.kids.ui.theme.PackPalColors

private val BodyShape = RoundedCornerShape(topStart = 56.dp, topEnd = 56.dp, bottomStart = 36.dp, bottomEnd = 36.dp)

/**
 * The home illustration: an open backpack with five compartment panels, a luggage tag on the handle
 * and a vertical stitched progress strip along the edge.
 */
@Composable
fun OpenBackpack(
    templateName: String?,
    items: List<Item>,
    packedIds: Set<Long>,
    onTagClick: () -> Unit,
    onCompartmentClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = PackingRules.progress(items, packedIds)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val wide = maxWidth >= 600.dp
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = if (wide) 900.dp else 520.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HandleWithTag(templateName, onTagClick)
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(BodyShape)
                    .background(PackPalColors.Teal)
                    .border(3.dp, PackPalColors.Navy, BodyShape)
                    .stitched(inset = 8.dp, corner = 48.dp)
            ) {
                Column(Modifier.padding(start = 12.dp, end = 14.dp, top = 14.dp, bottom = 18.dp)) {
                    BackpackOpening()
                    Spacer(Modifier.height(8.dp))
                    ProgressLabel(progress)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        ProgressStrip(progress, Modifier.fillMaxHeight().width(26.dp))
                        Spacer(Modifier.width(10.dp))
                        Compartments(items, packedIds, wide, onCompartmentClick, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HandleWithTag(templateName: String?, onTagClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(92.dp)) {
        Canvas(Modifier.align(Alignment.BottomCenter).size(width = 120.dp, height = 70.dp)) {
            val stroke = 9.dp.toPx()
            drawArc(
                color = PackPalColors.Navy, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(stroke, stroke), size = Size(size.width - 2 * stroke, (size.height - stroke) * 2),
                style = Stroke(width = stroke + 5.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = PackPalColors.TealDark, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(stroke, stroke), size = Size(size.width - 2 * stroke, (size.height - stroke) * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        // Tag string from the handle to the tag.
        Canvas(Modifier.align(Alignment.BottomCenter).offset(x = 52.dp).size(40.dp, 60.dp)) {
            drawLine(PackPalColors.Navy, Offset(0f, size.height * 0.55f), Offset(size.width, size.height * 0.35f), strokeWidth = 2.5.dp.toPx())
        }
        LuggageTag(
            text = templateName ?: "No list",
            onClick = onTagClick,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp).offset(y = (-2).dp),
        )
    }
}

private val TagShape: Shape = GenericShape { size, _ ->
    val notch = size.height * 0.32f
    moveTo(notch, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(notch, size.height)
    lineTo(0f, size.height - notch)
    lineTo(0f, notch)
    close()
}

@Composable
fun LuggageTag(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .widthIn(max = 220.dp)
            .defaultMinSize(minHeight = 52.dp)
            .clip(TagShape)
            .background(PackPalColors.YellowSoft)
            .border(2.dp, PackPalColors.Navy, TagShape)
            .clickable(role = Role.Button, onClickLabel = "change list") { onClick() }
            .semantics(mergeDescendants = true) { contentDescription = "List: $text. Tap to change list." }
            .padding(start = 14.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(PackPalColors.Cream).border(2.dp, PackPalColors.Navy, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(6.dp))
        UiIcon(R.drawable.ic_ui_chevron, null, size = 20.dp)
    }
}

/** The open top of the backpack: a darker lining with the unzipped edges. */
@Composable
private fun BackpackOpening() {
    Canvas(Modifier.fillMaxWidth().height(26.dp).clearAndSetSemantics { }) {
        val h = size.height
        drawRoundRect(PackPalColors.TealDark, topLeft = Offset(0f, 0f), size = Size(size.width, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2))
        drawRoundRect(PackPalColors.Navy, topLeft = Offset(0f, 0f), size = Size(size.width, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2), style = Stroke(2.dp.toPx()))
        // Zipper teeth along the opening.
        val y = h / 2
        var x = h / 2
        while (x < size.width - h / 2) {
            drawLine(PackPalColors.Cream, Offset(x, y - 3.dp.toPx()), Offset(x, y + 3.dp.toPx()), strokeWidth = 2.dp.toPx())
            x += 9.dp.toPx()
        }
    }
}

@Composable
private fun ProgressLabel(progress: Progress) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PackPalColors.Cream)
            .border(2.dp, PackPalColors.Navy, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (progress.isComplete) {
            UiIcon(R.drawable.ic_ui_check, null, size = 22.dp, tint = PackPalColors.TealDark)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = when {
                progress.total == 0 -> "No items yet"
                progress.isComplete -> "All ${progress.total} packed!"
                else -> "${progress.done} of ${progress.total} packed"
            },
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** Vertical stitched strip; purely decorative for accessibility (the text label carries the value). */
@Composable
private fun ProgressStrip(progress: Progress, modifier: Modifier) {
    val animate = LocalAnimationsEnabled.current
    val fraction by animateFloatAsState(progress.fraction, if (animate) tween(350) else snap(), label = "strip")
    Canvas(modifier.clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        val r = androidx.compose.ui.geometry.CornerRadius(w / 2)
        drawRoundRect(PackPalColors.Cream, size = Size(w, h), cornerRadius = r)
        val filled = h * fraction
        if (filled > 0f) {
            drawRoundRect(PackPalColors.Yellow, topLeft = Offset(0f, h - filled), size = Size(w, filled), cornerRadius = r)
        }
        drawRoundRect(PackPalColors.Navy, size = Size(w, h), cornerRadius = r, style = Stroke(2.dp.toPx()))
        // Stitches down the middle; a solid stitch marks each packed item segment boundary.
        drawLine(
            PackPalColors.Navy, Offset(w / 2, 6.dp.toPx()), Offset(w / 2, h - 6.dp.toPx()),
            strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
        )
        if (progress.total in 1..40) {
            for (i in 1 until progress.total) {
                val y = h - h * i / progress.total
                drawLine(PackPalColors.Navy.copy(alpha = 0.45f), Offset(4.dp.toPx(), y), Offset(w - 4.dp.toPx(), y), strokeWidth = 1.dp.toPx())
            }
        }
    }
}

@Composable
private fun Compartments(
    items: List<Item>,
    packedIds: Set<Long>,
    wide: Boolean,
    onClick: (Category) -> Unit,
    modifier: Modifier,
) {
    @Composable
    fun pocket(c: Category, m: Modifier, minHeight: Dp) =
        CompartmentPanel(c, items.filter { it.category == c }.sortedBy { it.displayOrder }, packedIds, { onClick(c) }, m, minHeight)

    val gap = 10.dp
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        if (wide) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                pocket(Category.BOOKS, Modifier.weight(1.4f).fillMaxHeight(), 150.dp)
                pocket(Category.CLOTHES, Modifier.weight(1f).fillMaxHeight(), 150.dp)
            }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                pocket(Category.FOOD, Modifier.weight(1f).fillMaxHeight(), 130.dp)
                pocket(Category.TOOLS, Modifier.weight(0.75f).fillMaxHeight(), 130.dp)
                pocket(Category.IMPORTANT, Modifier.weight(1.3f).fillMaxHeight(), 130.dp)
            }
        } else {
            pocket(Category.BOOKS, Modifier.fillMaxWidth(), 128.dp)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                pocket(Category.FOOD, Modifier.weight(1.25f).fillMaxHeight(), 132.dp)
                pocket(Category.TOOLS, Modifier.weight(0.85f).fillMaxHeight(), 132.dp)
            }
            pocket(Category.CLOTHES, Modifier.fillMaxWidth(), 120.dp)
            pocket(Category.IMPORTANT, Modifier.fillMaxWidth().padding(horizontal = 8.dp), 120.dp)
        }
    }
}

private data class PocketStyle(val color: Color, val shape: RoundedCornerShape)

private fun styleFor(c: Category): PocketStyle = when (c) {
    Category.BOOKS -> PocketStyle(PackPalColors.PocketBooks, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
    Category.FOOD -> PocketStyle(PackPalColors.PocketFood, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 26.dp, bottomEnd = 26.dp))
    Category.CLOTHES -> PocketStyle(PackPalColors.PocketClothes, RoundedCornerShape(16.dp))
    Category.TOOLS -> PocketStyle(PackPalColors.PocketTools, RoundedCornerShape(12.dp))
    Category.IMPORTANT -> PocketStyle(PackPalColors.PocketImportant, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 30.dp, bottomEnd = 30.dp))
}

/** Decorative detail that makes each compartment recognisable as a different part of the bag. */
private fun Modifier.pocketDetail(c: Category): Modifier = drawBehind {
    when (c) {
        Category.BOOKS -> { // Rear pocket: a flap seam near the top.
            val y = 10.dp.toPx()
            drawLine(PackPalColors.TealDark.copy(alpha = 0.5f), Offset(18.dp.toPx(), y), Offset(size.width - 18.dp.toPx(), y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        Category.CLOTHES -> { // Folded fabric: soft diagonal folds.
            val step = 34.dp.toPx()
            var x = -size.height
            while (x < size.width) {
                drawLine(PackPalColors.White.copy(alpha = 0.45f), Offset(x, size.height), Offset(x + size.height * 0.6f, 0f), strokeWidth = 6.dp.toPx())
                x += step
            }
        }
        Category.TOOLS -> { // Organizer: vertical slots along the bottom.
            val slots = 4
            val slotW = size.width / (slots + 1)
            for (i in 1..slots) {
                val x = slotW * i
                drawLine(PackPalColors.TealDark.copy(alpha = 0.45f), Offset(x, size.height - 30.dp.toPx()), Offset(x, size.height - 8.dp.toPx()), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        Category.IMPORTANT -> { // Zipped front pocket: zipper along the top with a pull tab.
            val y = 9.dp.toPx()
            drawLine(PackPalColors.Navy, Offset(16.dp.toPx(), y), Offset(size.width - 16.dp.toPx(), y), strokeWidth = 5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
            val px = size.width - 34.dp.toPx()
            drawRoundRect(PackPalColors.Yellow, topLeft = Offset(px, y - 4.dp.toPx()), size = Size(12.dp.toPx(), 20.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
            drawRoundRect(PackPalColors.Navy, topLeft = Offset(px, y - 4.dp.toPx()), size = Size(12.dp.toPx(), 20.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()), style = Stroke(2.dp.toPx()))
        }
        Category.FOOD -> { // Small pocket: a curved bottom seam.
            drawArc(PackPalColors.TealDark.copy(alpha = 0.45f), 20f, 140f, false, topLeft = Offset(14.dp.toPx(), size.height - 40.dp.toPx()),
                size = Size(size.width - 28.dp.toPx(), 30.dp.toPx()), style = Stroke(3.dp.toPx()))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompartmentPanel(
    category: Category,
    items: List<Item>,
    packedIds: Set<Long>,
    onClick: () -> Unit,
    modifier: Modifier,
    minHeight: Dp,
) {
    val style = styleFor(category)
    val progress = PackingRules.progress(items, packedIds)
    val animate = LocalAnimationsEnabled.current
    val bg by animateColorAsState(
        if (progress.isComplete) PackPalColors.YellowSoft else style.color,
        if (animate) tween(300) else snap(), label = "pocket",
    )
    val description = buildString {
        append(category.label).append(" compartment. ")
        if (items.isEmpty()) append("No items.")
        else {
            append("${progress.done} of ${progress.total} packed.")
            if (progress.isComplete) append(" All packed.")
        }
    }
    Box(
        modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(style.shape)
            .background(bg)
            .pocketDetail(category)
            .border(2.dp, PackPalColors.Navy, style.shape)
            .clickable(role = Role.Button, onClickLabel = "open ${category.label}") { onClick() }
            .clearAndSetSemantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(top = if (category == Category.IMPORTANT || category == Category.BOOKS) 6.dp else 0.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArtIcon(categoryIcon(category), size = 28.dp)
                Spacer(Modifier.width(8.dp))
                Text(category.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).semantics { heading() })
            }
            Spacer(Modifier.height(4.dp))
            if (progress.isComplete) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape).background(PackPalColors.Yellow).border(1.5.dp, PackPalColors.Navy, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { UiIcon(R.drawable.ic_ui_check, null, size = 16.dp) }
                    Spacer(Modifier.width(6.dp))
                    Text("All packed", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Text(
                    if (items.isEmpty()) "No items" else "${progress.done} of ${progress.total} packed",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (items.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items.take(3).forEach { item ->
                        val packed = item.id in packedIds
                        Box(
                            Modifier.size(34.dp).clip(CircleShape)
                                .background(if (packed) PackPalColors.White else PackPalColors.Cream)
                                .border(1.5.dp, PackPalColors.Navy, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { ArtIcon(ItemIcons.res(item.iconKey), size = 22.dp) }
                    }
                    if (items.size > 3) {
                        Text("+${items.size - 3}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
            }
        }
    }
}
