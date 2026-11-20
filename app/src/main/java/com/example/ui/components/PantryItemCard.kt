package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ExpiryUrgency
import com.example.data.model.FoodVisualCategory
import com.example.data.model.PantryItem
import com.example.data.model.StorageLocation
import com.example.data.model.classifyFoodCategory
import com.example.data.util.DateUtils
import com.example.data.util.UnitConverter
import com.example.ui.theme.LocalPantryColors

@Composable
fun PantryItemCard(
    item: PantryItem,
    soonThresholdDays: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pantryColors = LocalPantryColors.current
    val daysRemaining = DateUtils.daysUntil(item.expiryDate)
    val urgency = DateUtils.urgencyOf(item.expiryDate, soonThresholdDays)
    val daysLabel = DateUtils.formatDaysRemainingLabel(daysRemaining)

    val (badgeBg, badgeFg, accentStrip) = when (urgency) {
        ExpiryUrgency.URGENT -> Triple(pantryColors.urgentBg, pantryColors.urgentFg, pantryColors.urgentAccent)
        ExpiryUrgency.SOON -> Triple(pantryColors.warningBg, pantryColors.warningFg, pantryColors.warningAccent)
        ExpiryUrgency.FRESH -> Triple(pantryColors.freshBg, pantryColors.freshFg, pantryColors.freshAccent)
    }

    val category = classifyFoodCategory(item.name, item.storageLocation)
    val categoryIcon = foodCategoryIcon(category)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pantry_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = if (urgency == ExpiryUrgency.URGENT) 2.dp else 0.5.dp,
        border = BorderStroke(
            width = if (urgency == ExpiryUrgency.URGENT) 1.2.dp else 1.dp,
            color = if (urgency == ExpiryUrgency.URGENT) {
                accentStrip.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clickable(onClick = onClick)
        ) {
            // Left semantic urgency bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentStrip)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 13.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = item.name,
                            tint = badgeFg,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ExpiryStatusBadge(
                                label = daysLabel,
                                bgColor = badgeBg,
                                fgColor = badgeFg,
                                dotColor = accentStrip
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${UnitConverter.formatPreciseQuantity(item.quantity)} ${item.unit}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = locationIcon(item.storageLocation),
                                    contentDescription = item.storageLocation.label,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = item.storageLocation.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "HSD: ${DateUtils.formatDisplayDate(item.expiryDate)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.isExpiryEstimated) {
                            Text(
                                text = stringResource(R.string.estimated_tag),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.suggested_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Mở chi tiết và cập nhật trạng thái",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpiryStatusBadge(
    label: String,
    bgColor: Color,
    fgColor: Color,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = fgColor,
                maxLines = 1
            )
        }
    }
}

fun locationIcon(location: StorageLocation): ImageVector {
    return when (location) {
        StorageLocation.FRIDGE -> Icons.Filled.Kitchen
        StorageLocation.FREEZER -> Icons.Filled.AcUnit
        StorageLocation.PANTRY -> Icons.Filled.Storefront
        StorageLocation.OTHER -> Icons.Filled.Inventory2
    }
}

fun foodCategoryIcon(category: FoodVisualCategory): ImageVector {
    return when (category) {
        FoodVisualCategory.POULTRY_MEAT -> Icons.Filled.Restaurant
        FoodVisualCategory.VEGETABLE -> Icons.Filled.Eco
        FoodVisualCategory.DAIRY -> Icons.Filled.LocalDrink
        FoodVisualCategory.EGGS -> Icons.Filled.Egg
        FoodVisualCategory.SEAFOOD -> Icons.Filled.SetMeal
        FoodVisualCategory.GRAIN_PANTRY -> Icons.Filled.Inventory2
        FoodVisualCategory.FRUIT -> Icons.Filled.Spa
        FoodVisualCategory.OTHER -> Icons.Filled.Kitchen
    }
}
