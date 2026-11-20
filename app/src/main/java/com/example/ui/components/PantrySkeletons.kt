package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "pantry_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    return Brush.linearGradient(
        colors = listOf(
            base.copy(alpha = 0.85f),
            highlight.copy(alpha = 0.95f),
            base.copy(alpha = 0.85f)
        ),
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 240f)
    )
}

@Composable
private fun ShimmerBar(
    brush: Brush,
    height: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

@Composable
fun ReceiptAnalyzingSkeletonCard(
    previewBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    val shimmer = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("receipt_loading_skeleton"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                previewBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Ảnh hóa đơn đang đọc",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 148.dp, height = 176.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                1.5.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                RoundedCornerShape(14.dp)
                            )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "PantryFlow đang đọc hóa đơn...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Đang chuẩn hóa tên thực phẩm, tách số lượng và ước tính hạn sử dụng phù hợp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Shimmer Skeleton Preview of extracted rows
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShimmerBar(shimmer, 14.dp, Modifier.weight(1.8f))
                    ShimmerBar(shimmer, 14.dp, Modifier.weight(0.9f))
                    ShimmerBar(shimmer, 14.dp, Modifier.weight(1.1f))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                repeat(4) { idx ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(shimmer)
                        )
                        Column(
                            modifier = Modifier.weight(1.7f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ShimmerBar(shimmer, 14.dp, Modifier.fillMaxWidth(0.88f))
                            ShimmerBar(shimmer, 10.dp, Modifier.fillMaxWidth(0.55f))
                        }
                        ShimmerBar(shimmer, 28.dp, Modifier.weight(0.8f), cornerRadius = 10.dp)
                        ShimmerBar(shimmer, 28.dp, Modifier.weight(1f), cornerRadius = 10.dp)
                    }
                    if (idx < 3) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeThinkingSkeletonSection(
    cookTimeLabel: String,
    modifier: Modifier = Modifier
) {
    val shimmer = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recipes_loading_skeleton"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    text = "PantryFlow đang suy nghĩ món ngon cho bạn...",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Đang ưu tiên các nguyên liệu sắp hết hạn theo thời gian nấu $cookTimeLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 2 Shimmer Skeleton Recipe Cards
        repeat(2) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShimmerBar(shimmer, 20.dp, Modifier.fillMaxWidth(0.62f))
                        Spacer(modifier = Modifier.width(12.dp))
                        ShimmerBar(shimmer, 24.dp, Modifier.width(68.dp), cornerRadius = 12.dp)
                    }
                    ShimmerBar(shimmer, 13.dp, Modifier.fillMaxWidth(0.92f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerBar(shimmer, 24.dp, Modifier.width(110.dp), cornerRadius = 9.dp)
                        ShimmerBar(shimmer, 24.dp, Modifier.width(96.dp), cornerRadius = 9.dp)
                    }
                    ShimmerBar(shimmer, 12.dp, Modifier.fillMaxWidth(0.5f))
                }
            }
        }
    }
}
