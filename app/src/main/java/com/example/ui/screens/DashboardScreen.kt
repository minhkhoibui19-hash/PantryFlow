package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ExpiryUrgency
import com.example.data.model.ItemStatus
import com.example.data.model.PantryItem
import com.example.data.model.StorageLocation
import com.example.data.util.DateUtils
import com.example.data.util.NotificationHelper
import com.example.ui.components.ItemActionBottomSheet
import com.example.ui.components.PantryItemCard
import com.example.ui.components.locationIcon
import com.example.ui.theme.LocalPantryColors
import com.example.viewmodel.PantryUiState

@Composable
fun DashboardScreen(
    uiState: PantryUiState,
    onSelectLocation: (StorageLocation?) -> Unit,
    onToggleUrgency: (ExpiryUrgency) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onUsePartialQuantity: (PantryItem, Double, String) -> Unit,
    onMarkItemStatus: (PantryItem, ItemStatus) -> Unit,
    onQuickUpdateLocation: (PantryItem, StorageLocation) -> Unit,
    onQuickUpdateExpiryDate: (PantryItem, String) -> Unit,
    onEditItem: (PantryItem) -> Unit,
    onOpenAddScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedItemIdForSheet by rememberSaveable { mutableStateOf<String?>(null) }

    // Resolve live item from StateFlow so inline quick-edits update inside the open sheet immediately
    val activeSheetItem = uiState.allActiveItems.firstOrNull { it.id == selectedItemIdForSheet }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Brand Header
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Eco,
                                contentDescription = stringResource(R.string.app_name),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Always-Visible Quick Search Bar
            item(key = "search_bar") {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Tìm kiếm thực phẩm",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Xóa từ khóa"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pantry_search_input")
                )
            }

            // Compact Rescued Food Statistics Card ("Đã giải cứu X món không bị lãng phí")
            item(key = "rescued_stat_card") {
                RescuedWasteStatCard(
                    usedCount = uiState.usedCount,
                    discardedCount = uiState.discardedCount
                )
            }

            // Prominent Expiry Reminder Alert Banner respecting user's reminderDaysBefore setting
            if (uiState.reminderAlertItems.isNotEmpty()) {
                item(key = "expiry_reminder_banner") {
                    ExpiryReminderAlertBanner(
                        items = uiState.reminderAlertItems,
                        reminderDaysBefore = uiState.settings.reminderDaysBefore,
                        reminderTimeText = uiState.settings.formattedReminderTime,
                        onClickItem = { item -> selectedItemIdForSheet = item.id }
                    )
                }
            }

            // Freshness Summary Hero Card with generated illustration
            item(key = "hero_summary") {
                FreshnessOverviewCard(
                    totalActive = uiState.allActiveItems.size,
                    urgentCount = uiState.urgentCount,
                    soonCount = uiState.soonCount,
                    freshCount = uiState.freshCount,
                    selectedUrgency = uiState.selectedUrgencyFilter,
                    onToggleUrgency = onToggleUrgency
                )
            }

            // Storage Location Quick Filter Chips
            item(key = "location_chips") {
                LocationFilterRow(
                    selectedLocation = uiState.selectedLocation,
                    totalActive = uiState.allActiveItems.size,
                    locationCounts = uiState.locationCounts,
                    onSelectLocation = onSelectLocation
                )
            }

            // Section Title + Active Filter Clear Action
            item(key = "list_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sort,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Thực phẩm (${uiState.filteredItems.size}) • Chạm thẻ để cập nhật",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.selectedLocation != null ||
                        uiState.selectedUrgencyFilter != null ||
                        uiState.searchQuery.isNotBlank()
                    ) {
                        TextButton(
                            onClick = {
                                onSelectLocation(null)
                                uiState.selectedUrgencyFilter?.let { onToggleUrgency(it) }
                                onSearchQueryChange("")
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("clear_filters_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FilterListOff,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Xóa lọc",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Empty State or Sorted Pantry Item Cards
            if (uiState.filteredItems.isEmpty() && !uiState.isLoading) {
                item(key = "empty_state") {
                    PantryEmptyStateCard(
                        isFiltered = uiState.allActiveItems.isNotEmpty(),
                        onPrimaryAction = {
                            if (uiState.allActiveItems.isNotEmpty()) {
                                onSelectLocation(null)
                                uiState.selectedUrgencyFilter?.let { onToggleUrgency(it) }
                                onSearchQueryChange("")
                            } else {
                                onOpenAddScreen()
                            }
                        }
                    )
                }
            } else {
                items(
                    items = uiState.filteredItems,
                    key = { it.id }
                ) { item ->
                    PantryItemCard(
                        item = item,
                        soonThresholdDays = uiState.settings.warningDaysThreshold,
                        onClick = { selectedItemIdForSheet = item.id },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    // Item Detail & Quick Status Update Modal Bottom Sheet
    if (activeSheetItem != null) {
        ItemActionBottomSheet(
            item = activeSheetItem,
            soonThresholdDays = uiState.settings.warningDaysThreshold,
            onDismiss = { selectedItemIdForSheet = null },
            onUsePartial = { usedAmount, usedUnit ->
                onUsePartialQuantity(activeSheetItem, usedAmount, usedUnit)
                selectedItemIdForSheet = null
            },
            onMarkUsedAll = {
                onMarkItemStatus(activeSheetItem, ItemStatus.USED)
                selectedItemIdForSheet = null
            },
            onMarkDiscarded = {
                onMarkItemStatus(activeSheetItem, ItemStatus.DISCARDED)
                selectedItemIdForSheet = null
            },
            onQuickUpdateLocation = { newLoc ->
                onQuickUpdateLocation(activeSheetItem, newLoc)
            },
            onQuickUpdateExpiryDate = { newDate ->
                onQuickUpdateExpiryDate(activeSheetItem, newDate)
            },
            onOpenFullEdit = {
                val target = activeSheetItem
                selectedItemIdForSheet = null
                onEditItem(target)
            }
        )
    }
}

@Composable
private fun RescuedWasteStatCard(
    usedCount: Int,
    discardedCount: Int
) {
    val pantryColors = LocalPantryColors.current
    val totalHistory = usedCount + discardedCount
    val savedRate = if (totalHistory > 0) ((usedCount * 100) / totalHistory) else null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rescued_waste_stat_card"),
        shape = RoundedCornerShape(16.dp),
        color = pantryColors.freshBg.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, pantryColors.freshAccent.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(pantryColors.freshAccent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.VolunteerActivism,
                    contentDescription = "Thống kê giải cứu thực phẩm",
                    tint = pantryColors.freshFg,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "Đã giải cứu $usedCount món không bị lãng phí",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = pantryColors.freshFg,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (usedCount > 0) {
                        "Tuyệt vời! Bạn đang giúp căn bếp tiết kiệm và xanh hơn mỗi ngày."
                    } else {
                        "Đánh dấu \"Đã dùng hết\" sau khi nấu để tích lũy thành tích."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = pantryColors.freshFg.copy(alpha = 0.82f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (savedRate != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = "$savedRate%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = pantryColors.freshFg,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpiryReminderAlertBanner(
    items: List<PantryItem>,
    reminderDaysBefore: Int,
    reminderTimeText: String,
    onClickItem: (PantryItem) -> Unit
) {
    val pantryColors = LocalPantryColors.current
    val hasCriticalToday = items.any { DateUtils.daysUntil(it.expiryDate) <= 0 }
    val bannerBg = if (hasCriticalToday) pantryColors.urgentBg else pantryColors.warningBg
    val bannerFg = if (hasCriticalToday) pantryColors.urgentFg else pantryColors.warningFg
    val bannerAccent = if (hasCriticalToday) pantryColors.urgentAccent else pantryColors.warningAccent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expiry_reminder_banner"),
        shape = RoundedCornerShape(20.dp),
        color = bannerBg,
        border = BorderStroke(1.3.dp, bannerAccent.copy(alpha = 0.45f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(bannerAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Cảnh báo sắp hết hạn",
                            tint = bannerFg,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Text(
                        text = "Nhắc nhở sắp hết hạn (${items.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = bannerFg
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                ) {
                    Text(
                        text = "≤ $reminderDaysBefore ngày • $reminderTimeText",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = bannerFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            items.take(3).forEach { alertItem ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onClickItem(alertItem) }
                        .testTag("reminder_banner_item_${alertItem.id}"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (DateUtils.daysUntil(alertItem.expiryDate) <= 1) {
                                            pantryColors.urgentAccent
                                        } else {
                                            pantryColors.warningAccent
                                        }
                                    )
                            )
                            Text(
                                text = NotificationHelper.buildReminderSentence(alertItem),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Cập nhật nhanh",
                            tint = bannerFg,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (items.size > 3) {
                Text(
                    text = "+ ${items.size - 3} thực phẩm khác sắp đến hạn bên dưới",
                    style = MaterialTheme.typography.labelSmall,
                    color = bannerFg.copy(alpha = 0.85f),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun FreshnessOverviewCard(
    totalActive: Int,
    urgentCount: Int,
    soonCount: Int,
    freshCount: Int,
    selectedUrgency: ExpiryUrgency?,
    onToggleUrgency: (ExpiryUrgency) -> Unit
) {
    val pantryColors = LocalPantryColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_pantry_hero),
                    contentDescription = "Hình ảnh tủ thực phẩm tươi mát PantryFlow",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0E4D25).copy(alpha = 0.88f),
                                    Color(0xFF157F3D).copy(alpha = 0.60f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = "$totalActive món đang bảo quản",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (urgentCount > 0) {
                            "Có $urgentCount món cần dùng ngay hôm nay"
                        } else if (soonCount > 0) {
                            "$soonCount món sắp hết hạn trong vài ngày tới"
                        } else {
                            "Tủ thực phẩm nhà bạn đang rất tươi!"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 2
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UrgencySummaryPill(
                    count = urgentCount,
                    label = stringResource(R.string.summary_urgent),
                    subLabel = "≤ 1 ngày",
                    bgColor = pantryColors.urgentBg,
                    fgColor = pantryColors.urgentFg,
                    accentColor = pantryColors.urgentAccent,
                    selected = selectedUrgency == ExpiryUrgency.URGENT,
                    testTag = "summary_pill_urgent",
                    onClick = { onToggleUrgency(ExpiryUrgency.URGENT) },
                    modifier = Modifier.weight(1f)
                )
                UrgencySummaryPill(
                    count = soonCount,
                    label = stringResource(R.string.summary_soon),
                    subLabel = "2–3 ngày",
                    bgColor = pantryColors.warningBg,
                    fgColor = pantryColors.warningFg,
                    accentColor = pantryColors.warningAccent,
                    selected = selectedUrgency == ExpiryUrgency.SOON,
                    testTag = "summary_pill_soon",
                    onClick = { onToggleUrgency(ExpiryUrgency.SOON) },
                    modifier = Modifier.weight(1f)
                )
                UrgencySummaryPill(
                    count = freshCount,
                    label = stringResource(R.string.summary_fresh),
                    subLabel = "> 3 ngày",
                    bgColor = pantryColors.freshBg,
                    fgColor = pantryColors.freshFg,
                    accentColor = pantryColors.freshAccent,
                    selected = selectedUrgency == ExpiryUrgency.FRESH,
                    testTag = "summary_pill_fresh",
                    onClick = { onToggleUrgency(ExpiryUrgency.FRESH) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun UrgencySummaryPill(
    count: Int,
    label: String,
    subLabel: String,
    bgColor: Color,
    fgColor: Color,
    accentColor: Color,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = if (selected) {
            BorderStroke(2.dp, accentColor)
        } else {
            BorderStroke(1.dp, accentColor.copy(alpha = 0.16f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = fgColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = fgColor,
                maxLines = 1
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = fgColor.copy(alpha = 0.78f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LocationFilterRow(
    selectedLocation: StorageLocation?,
    totalActive: Int,
    locationCounts: Map<StorageLocation, Int>,
    onSelectLocation: (StorageLocation?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        item(key = "chip_all") {
            FilterChip(
                selected = selectedLocation == null,
                onClick = { onSelectLocation(null) },
                label = {
                    Text(
                        text = "${stringResource(R.string.location_all)} ($totalActive)",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedLocation == null,
                    borderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.testTag("filter_chip_all")
            )
        }

        items(
            items = StorageLocation.entries,
            key = { it.name }
        ) { location ->
            val count = locationCounts[location] ?: 0
            val isSelected = selectedLocation == location
            FilterChip(
                selected = isSelected,
                onClick = { onSelectLocation(location) },
                leadingIcon = {
                    Icon(
                        imageVector = locationIcon(location),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = "${location.label} ($count)",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.testTag("filter_chip_${location.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun PantryEmptyStateCard(
    isFiltered: Boolean,
    onPrimaryAction: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_pantry),
                contentDescription = "Hình minh họa tủ đồ trống",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 180.dp, height = 135.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(
                    if (isFiltered) R.string.empty_filter_title else R.string.empty_pantry_title
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    if (isFiltered) R.string.empty_filter_subtitle else R.string.empty_pantry_subtitle
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onPrimaryAction,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("empty_state_action_button")
            ) {
                Icon(
                    imageVector = if (isFiltered) Icons.Filled.FilterListOff else Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isFiltered) "Xem tất cả thực phẩm" else "Thêm thực phẩm đầu tiên"
                )
            }
        }
    }
}
