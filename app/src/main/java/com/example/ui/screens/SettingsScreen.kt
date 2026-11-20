package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ItemStatus
import com.example.data.model.PantryItem
import com.example.data.util.NotificationHelper
import com.example.data.util.UnitConverter
import com.example.ui.theme.LocalPantryColors
import com.example.viewmodel.PantryUiState

private enum class HistoryFilter {
    ALL, USED, DISCARDED
}

private val QuickReminderTimes = listOf(
    7 to 0,
    8 to 0,
    11 to 30,
    17 to 30,
    20 to 0
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: PantryUiState,
    onSetReminderDaysBefore: (Int) -> Unit,
    onSetReminderTime: (Int, Int) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit,
    onSetDarkMode: (Boolean) -> Unit,
    onRestoreItem: (PantryItem) -> Unit,
    onResetSampleData: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pantryColors = LocalPantryColors.current

    var showResetDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var historyFilter by rememberSaveable { mutableStateOf(HistoryFilter.ALL) }
    var permissionDeniedNotice by remember { mutableStateOf(false) }
    var pendingTestNotification by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNotice = false
            onSetNotificationsEnabled(true)
            if (pendingTestNotification) {
                pendingTestNotification = false
                onSendTestNotification()
            }
        } else {
            permissionDeniedNotice = true
            pendingTestNotification = false
            onSetNotificationsEnabled(false)
        }
    }

    fun requestNotificationAccess(triggerTestAfterGrant: Boolean) {
        if (NotificationHelper.hasNotificationPermission(context)) {
            permissionDeniedNotice = false
            if (!uiState.settings.notificationsEnabled) {
                onSetNotificationsEnabled(true)
            }
            if (triggerTestAfterGrant) {
                onSendTestNotification()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingTestNotification = triggerTestAfterGrant
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onSetNotificationsEnabled(true)
            if (triggerTestAfterGrant) onSendTestNotification()
        }
    }

    val filteredHistory = remember(uiState.historyItems, historyFilter) {
        when (historyFilter) {
            HistoryFilter.ALL -> uiState.historyItems
            HistoryFilter.USED -> uiState.historyItems.filter { it.status == ItemStatus.USED }
            HistoryFilter.DISCARDED -> uiState.historyItems.filter { it.status == ItemStatus.DISCARDED }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "settings_header") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.settings_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Stats Overview Card
            item(key = "stats_overview") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Thống kê sử dụng thực phẩm",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatTile(
                                value = uiState.allActiveItems.size.toString(),
                                label = "Đang trong tủ",
                                bgColor = MaterialTheme.colorScheme.surfaceVariant,
                                fgColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                value = uiState.usedCount.toString(),
                                label = "Đã dùng hết",
                                bgColor = pantryColors.freshBg,
                                fgColor = pantryColors.freshFg,
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                value = uiState.discardedCount.toString(),
                                label = "Đã bỏ đi",
                                bgColor = pantryColors.urgentBg,
                                fgColor = pantryColors.urgentFg,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Expiry Reminder System Configuration Card
            item(key = "reminder_system_card") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Reminder days before (1 ngày, 2 ngày, 3 ngày)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Nhắc trước khi hết hạn",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Hiện banner cảnh báo trên Tủ đồ và gửi thông báo cho các món còn ≤ ${uiState.settings.reminderDaysBefore} ngày.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1, 2, 3).forEach { days ->
                                    val selected = uiState.settings.reminderDaysBefore == days
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSetReminderDaysBefore(days) },
                                        label = {
                                            Text(
                                                text = "$days ngày",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("reminder_days_chip_$days")
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 2. Daily reminder time slot
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Khung giờ nhắc nhở trong ngày",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Đang đặt: ${uiState.settings.formattedReminderTime} hằng ngày",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = { showTimePickerDialog = true },
                                    modifier = Modifier.testTag("pick_custom_reminder_time")
                                ) {
                                    Text("Chọn giờ")
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QuickReminderTimes.forEach { (hour, minute) ->
                                    val label = String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
                                    val selected = uiState.settings.reminderHour == hour &&
                                        uiState.settings.reminderMinute == minute
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSetReminderTime(hour, minute) },
                                        label = { Text(label) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("reminder_time_chip_$label")
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 3. Notifications toggle + "Gửi thử thông báo ngay" button
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.settings.notificationsEnabled) {
                                            Icons.Filled.NotificationsActive
                                        } else {
                                            Icons.Filled.NotificationsNone
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Bật thông báo nhắc nhở",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Gửi thông báo trực tiếp lên thanh trạng thái thiết bị",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = uiState.settings.notificationsEnabled,
                                    onCheckedChange = { enable ->
                                        if (enable) {
                                            requestNotificationAccess(triggerTestAfterGrant = false)
                                        } else {
                                            onSetNotificationsEnabled(false)
                                        }
                                    },
                                    modifier = Modifier.testTag("switch_notifications_enabled")
                                )
                            }

                            if (permissionDeniedNotice) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = pantryColors.warningBg
                                ) {
                                    Text(
                                        text = "Bạn đã từ chối quyền Thông báo. Hãy cấp quyền thông báo cho PantryFlow trong Cài đặt ứng dụng để nhận nhắc nhở.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = pantryColors.warningFg,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = { requestNotificationAccess(triggerTestAfterGrant = true) },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_test_notification_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gửi thử thông báo ngay",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Appearance & Local Data Controls
            item(key = "appearance_and_data_card") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DarkMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Giao diện tối",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Dịu mắt hơn khi xem tủ đồ buổi tối",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.settings.darkModeEnabled,
                                onCheckedChange = onSetDarkMode,
                                modifier = Modifier.testTag("switch_dark_mode")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Dữ liệu lưu cục bộ trên máy",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { showResetDialog = true },
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                    modifier = Modifier.testTag("reset_sample_data_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RestartAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Nạp lại 6 món mẫu",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (uiState.historyItems.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = onClearHistory,
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = pantryColors.urgentFg
                                        ),
                                        modifier = Modifier.testTag("clear_history_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteSweep,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Xóa lịch sử",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Usage History Section ("Lịch sử sử dụng")
            item(key = "history_title") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Lịch sử sử dụng (${uiState.historyItems.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            HistoryFilter.ALL to "Tất cả (${uiState.historyItems.size})",
                            HistoryFilter.USED to "Đã dùng hết (${uiState.usedCount})",
                            HistoryFilter.DISCARDED to "Đã bỏ đi (${uiState.discardedCount})"
                        ).forEach { (filter, label) ->
                            val selected = historyFilter == filter
                            FilterChip(
                                selected = selected,
                                onClick = { historyFilter = filter },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(14.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("history_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            if (filteredHistory.isEmpty()) {
                item(key = "history_empty") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "Chưa có thực phẩm nào trong mục lịch sử này.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            } else {
                items(
                    items = filteredHistory,
                    key = { it.id }
                ) { historyItem ->
                    val isUsed = historyItem.status == ItemStatus.USED
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = historyItem.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isUsed) pantryColors.freshBg else pantryColors.urgentBg
                                    ) {
                                        Text(
                                            text = if (isUsed) "Đã dùng hết" else "Đã bỏ đi",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = if (isUsed) pantryColors.freshFg else pantryColors.urgentFg,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${UnitConverter.formatPreciseQuantity(historyItem.quantity)} ${historyItem.unit} • ${historyItem.storageLocation.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = { onRestoreItem(historyItem) },
                                modifier = Modifier.testTag("restore_item_${historyItem.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Restore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.action_restore))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.settings.reminderHour,
            initialMinute = uiState.settings.reminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text("Chọn giờ nhắc nhở") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSetReminderTime(timePickerState.hour, timePickerState.minute)
                        showTimePickerDialog = false
                    }
                ) {
                    Text("Lưu giờ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Khôi phục dữ liệu mẫu?") },
            text = {
                Text("Danh sách hiện tại sẽ được thay bằng 6 thực phẩm mẫu ban đầu (Ức gà phi lê, Rau cải ngọt, Sữa tươi, Trứng gà ta, Tôm sú, Gạo ST25).")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetSampleData()
                    }
                ) {
                    Text("Khôi phục")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    bgColor: androidx.compose.ui.graphics.Color,
    fgColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = fgColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fgColor
            )
        }
    }
}
