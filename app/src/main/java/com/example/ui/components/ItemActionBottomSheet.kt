package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ExpiryUrgency
import com.example.data.model.PantryItem
import com.example.data.model.StorageLocation
import com.example.data.model.classifyFoodCategory
import com.example.data.util.DateUtils
import com.example.data.util.UnitConverter
import com.example.ui.theme.LocalPantryColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemActionBottomSheet(
    item: PantryItem,
    soonThresholdDays: Int,
    onDismiss: () -> Unit,
    onUsePartial: (usedAmount: Double, usedUnit: String) -> Unit,
    onMarkUsedAll: () -> Unit,
    onMarkDiscarded: () -> Unit,
    onQuickUpdateLocation: (StorageLocation) -> Unit,
    onQuickUpdateExpiryDate: (String) -> Unit,
    onOpenFullEdit: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pantryColors = LocalPantryColors.current

    val compatibleUnits = remember(item.unit) { UnitConverter.compatibleUnitsFor(item.unit) }
    var showPartialSection by remember(item.id) { mutableStateOf(true) }
    var usedAmountText by remember(item.id) { mutableStateOf("") }
    var selectedUsedUnit by remember(item.id) { mutableStateOf(item.unit) }
    var partialError by remember(item.id) { mutableStateOf<String?>(null) }
    var showExpiryPicker by remember { mutableStateOf(false) }

    val daysRemaining = DateUtils.daysUntil(item.expiryDate)
    val urgency = DateUtils.urgencyOf(item.expiryDate, soonThresholdDays)
    val (badgeBg, badgeFg, dotColor) = when (urgency) {
        ExpiryUrgency.URGENT -> Triple(pantryColors.urgentBg, pantryColors.urgentFg, pantryColors.urgentAccent)
        ExpiryUrgency.SOON -> Triple(pantryColors.warningBg, pantryColors.warningFg, pantryColors.warningAccent)
        ExpiryUrgency.FRESH -> Triple(pantryColors.freshBg, pantryColors.freshFg, pantryColors.freshAccent)
    }

    val parsedUsedAmount = usedAmountText.replace(",", ".").toDoubleOrNull()
    val previewRemaining = if (parsedUsedAmount != null && parsedUsedAmount > 0.0) {
        UnitConverter.calculateRemaining(
            currentQuantity = item.quantity,
            itemUnit = item.unit,
            usedAmount = parsedUsedAmount,
            usedUnit = selectedUsedUnit
        )
    } else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Item details + Urgency badge + Full edit link
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = foodCategoryIcon(classifyFoodCategory(item.name, item.storageLocation)),
                        contentDescription = item.name,
                        tint = badgeFg,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Hiện còn: ${UnitConverter.formatPreciseQuantity(item.quantity)} ${item.unit}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        ExpiryStatusBadge(
                            label = DateUtils.formatDaysRemainingLabel(daysRemaining),
                            bgColor = badgeBg,
                            fgColor = badgeFg,
                            dotColor = dotColor
                        )
                    }
                }

                TextButton(
                    onClick = onOpenFullEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("sheet_full_edit_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sửa", style = MaterialTheme.typography.labelMedium)
                }
            }

            // 3 Quick Actions Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Cập nhật trạng thái nhanh",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Action 1: Đã dùng một phần (with unit conversion & live deduction preview)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        1.dp,
                        if (showPartialSection) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPartialSection = !showPartialSection }
                                .testTag("action_partial_use_header"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PieChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Đã dùng một phần",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Nhập lượng đã dùng để tự động trừ vào tồn kho",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = showPartialSection) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Quick fraction buttons (1/4, 1/2, 3/4)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "1/4" to 0.25,
                                        "1/2" to 0.5,
                                        "3/4" to 0.75
                                    ).forEach { (label, factor) ->
                                        val amountInSelected = UnitConverter.convert(
                                            amount = item.quantity * factor,
                                            fromUnit = item.unit,
                                            toUnit = selectedUsedUnit
                                        )
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    usedAmountText = UnitConverter.formatPreciseQuantity(amountInSelected)
                                                    partialError = null
                                                }
                                                .testTag("partial_fraction_$label"),
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 10.dp)
                                            ) {
                                                Text(
                                                    text = "Dùng $label",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = usedAmountText,
                                        onValueChange = { input ->
                                            val cleaned = input.replace(",", ".")
                                            if (cleaned.isEmpty() || cleaned.matches(Regex("^\\d*\\.?\\d*$"))) {
                                                usedAmountText = cleaned
                                                partialError = null
                                            }
                                        },
                                        placeholder = { Text("VD: 200 hoặc 0.5") },
                                        label = { Text("Số lượng đã dùng") },
                                        singleLine = true,
                                        isError = partialError != null,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("partial_use_input")
                                    )

                                    // Compatible Unit Switcher (e.g. g <-> kg, ml <-> lít)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        compatibleUnits.forEach { u ->
                                            val isSelected = selectedUsedUnit == u
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { selectedUsedUnit = u },
                                                label = { Text(u, style = MaterialTheme.typography.labelMedium) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                modifier = Modifier.testTag("partial_unit_$u")
                                            )
                                        }
                                    }
                                }

                                if (partialError != null) {
                                    Text(
                                        text = partialError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else if (previewRemaining != null) {
                                    Text(
                                        text = if (previewRemaining <= 0.0) {
                                            "Sẽ trừ hết và chuyển sang trạng thái Đã dùng hết"
                                        } else {
                                            "Còn lại sau khi trừ: ${UnitConverter.formatPreciseQuantity(previewRemaining)} ${item.unit}"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = pantryColors.freshFg
                                    )
                                }

                                Button(
                                    onClick = {
                                        val amount = usedAmountText.replace(",", ".").toDoubleOrNull()
                                        if (amount == null || amount <= 0.0) {
                                            partialError = "Vui lòng nhập số lượng lớn hơn 0"
                                        } else {
                                            onUsePartial(amount, selectedUsedUnit)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 11.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_partial_use_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Xác nhận trừ số lượng")
                                }
                            }
                        }
                    }
                }

                // Action 2 & Action 3: Đã dùng hết | Bỏ đi (hỏng/quá hạn)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onMarkUsedAll,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = pantryColors.freshBg,
                            contentColor = pantryColors.freshFg
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 13.dp, horizontal = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .testTag("sheet_mark_used_all")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Đã dùng hết",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalButton(
                        onClick = onMarkDiscarded,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = pantryColors.urgentBg,
                            contentColor = pantryColors.urgentFg
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 13.dp, horizontal = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .testTag("sheet_mark_discarded")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bỏ đi (hỏng)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Quick Inline Edit: Storage Location & Expiry Date
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Chỉnh nhanh nơi bảo quản & hạn dùng",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StorageLocation.entries.forEach { loc ->
                        val selected = item.storageLocation == loc
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (!selected) onQuickUpdateLocation(loc)
                                }
                                .testTag("sheet_quick_loc_${loc.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = locationIcon(loc),
                                    contentDescription = loc.label,
                                    tint = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = loc.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Quick Expiry Date Selector
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showExpiryPicker = true }
                        .testTag("sheet_quick_expiry_date"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "HSD: ${DateUtils.formatDisplayDate(item.expiryDate)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (item.isExpiryEstimated) {
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
                                                    text = "Ước tính",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "Ngày mua: ${DateUtils.formatDisplayDate(item.purchaseDate)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "Đổi ngày",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (showExpiryPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.isoToUtcMillis(item.expiryDate)
        )
        DatePickerDialog(
            onDismissRequest = { showExpiryPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onQuickUpdateExpiryDate(DateUtils.utcMillisToIso(millis))
                        }
                        showExpiryPicker = false
                    }
                ) {
                    Text("Lưu ngày")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpiryPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
