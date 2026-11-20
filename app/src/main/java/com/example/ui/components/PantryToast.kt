package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalPantryColors
import com.example.viewmodel.SnackbarEvent
import com.example.viewmodel.ToastType

@Composable
fun PantryToastOverlay(
    event: SnackbarEvent?,
    undoLabel: String,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pantryColors = LocalPantryColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = event != null,
            enter = slideInVertically(
                initialOffsetY = { -it - 40 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it - 40 }
            ) + fadeOut()
        ) {
            if (event != null) {
                val (containerColor, contentColor, iconBg, iconVector) = when (event.type) {
                    ToastType.SUCCESS -> ToastStyle(
                        container = pantryColors.freshBg,
                        content = pantryColors.freshFg,
                        accent = pantryColors.freshAccent,
                        icon = Icons.Filled.CheckCircle
                    )

                    ToastType.ERROR -> ToastStyle(
                        container = pantryColors.urgentBg,
                        content = pantryColors.urgentFg,
                        accent = pantryColors.urgentAccent,
                        icon = Icons.Filled.ErrorOutline
                    )

                    ToastType.INFO -> ToastStyle(
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                        accent = MaterialTheme.colorScheme.primary,
                        icon = Icons.Filled.Info
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onDismiss)
                        .testTag("pantry_toast_banner"),
                    shape = RoundedCornerShape(18.dp),
                    color = containerColor,
                    border = BorderStroke(1.2.dp, iconBg.copy(alpha = 0.45f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(iconBg.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = event.message,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = contentColor,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (event.undoItemId != null) {
                            TextButton(
                                onClick = onUndo,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("toast_undo_button")
                            ) {
                                Text(
                                    text = undoLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = iconBg
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("toast_dismiss_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Đóng thông báo",
                                    tint = contentColor.copy(alpha = 0.75f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ToastStyle(
    val container: Color,
    val content: Color,
    val accent: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
