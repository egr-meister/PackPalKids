package com.packpal.kids.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.packpal.kids.R
import com.packpal.kids.ui.theme.PackPalColors

@Composable
fun UiIcon(@DrawableRes res: Int, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = PackPalColors.Navy, size: Dp = 24.dp) {
    Icon(painterResource(res), contentDescription = contentDescription, modifier = modifier.size(size), tint = tint)
}

/** Full-colour bundled illustration icon (item, category, template). */
@Composable
fun ArtIcon(@DrawableRes res: Int, modifier: Modifier = Modifier, size: Dp = 40.dp, contentDescription: String? = null) {
    Image(painterResource(res), contentDescription = contentDescription, modifier = modifier.size(size))
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 60.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PackPalColors.Yellow,
            contentColor = PackPalColors.Navy,
            disabledContainerColor = PackPalColors.CreamDeep,
            disabledContentColor = PackPalColors.NavySoft,
        ),
        border = BorderStroke(2.dp, if (enabled) PackPalColors.Navy else PackPalColors.NavySoft),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            UiIcon(icon, null, size = 26.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = androidx.compose.material3.MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    accent: Color = PackPalColors.Navy,
    container: Color = PackPalColors.White,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, if (enabled) accent else PackPalColors.NavySoft),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = container, contentColor = PackPalColors.Navy),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (icon != null) {
            UiIcon(icon, null, size = 22.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = androidx.compose.material3.MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancel",
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PackPalColors.Cream,
        title = { Text(title) },
        text = { Text(text, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                Text(confirmLabel, color = if (destructive) Color(0xFFB3261E) else PackPalColors.Navy)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                Text(dismissLabel, color = PackPalColors.Navy)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackPalScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    backLabel: String = "Back",
    @DrawableRes backIcon: Int = R.drawable.ic_ui_back,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = PackPalColors.Cream,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
        bottomBar = bottomBar,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                            UiIcon(backIcon, backLabel, size = 28.dp)
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PackPalColors.Cream,
                    titleContentColor = PackPalColors.Navy,
                ),
            )
        },
        content = content,
    )
}

/** Dashed stitching drawn just inside a rounded shape. */
fun Modifier.stitched(
    color: Color = PackPalColors.Stitch,
    inset: Dp = 6.dp,
    corner: Dp = 16.dp,
    strokeWidth: Dp = 2.dp,
): Modifier = this.drawBehind {
    val i = inset.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(i, i),
        size = Size(size.width - 2 * i, size.height - 2 * i),
        cornerRadius = CornerRadius((corner.toPx() - i).coerceAtLeast(2f)),
        style = Stroke(width = strokeWidth.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
    )
}

@Composable
fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(text, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = PackPalColors.NavySoft)
    }
}

@Composable
fun rememberSnackbar(): SnackbarHostState = remember { SnackbarHostState() }
