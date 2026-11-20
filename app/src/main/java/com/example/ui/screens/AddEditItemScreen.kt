package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CommonUnits
import com.example.data.model.CommonVietnamesePresets
import com.example.data.model.PantryItem
import com.example.data.model.StorageLocation
import com.example.data.util.DateUtils
import com.example.ui.components.locationIcon

private enum class ActiveDatePicker {
    NONE, PURCHASE, EXPIRY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(
    editingItem: PantryItem?,
    onSave: (
        id: String?,
        name: String,
        quantity: Double,
        unit: String,
        storageLocation: StorageLocation,
        purchaseDate: String,
        expiryDate: String,
        isExpiryEstimated: Boolean
    ) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onCancel() }

    val isEditing = editingItem != null
    var name by rememberSaveable(editingItem?.id) { mutableStateOf(editingItem?.name ?: "") }
    var quantityText by rememberSaveable(editingItem?.id) {
        mutableStateOf(editingItem?.let { DateUtils.formatQuantity(it.quantity) } ?: "1")
    }
    var unit by rememberSaveable(editingItem?.id) { mutableStateOf(editingItem?.unit ?: "g") }
    var storageLocation by rememberSaveable(editingItem?.id) {
        mutableStateOf(editingItem?.storageLocation ?: StorageLocation.FRIDGE)
    }
    var purchaseDate by rememberSaveable(editingItem?.id) {
        mutableStateOf(editingItem?.purchaseDate ?: DateUtils.todayIso())
    }
    var isExpiryEstimated by rememberSaveable(editingItem?.id) {
        mutableStateOf(editingItem?.isExpiryEstimated ?: true)
    }
    var expiryDate by rememberSaveable(editingItem?.id) {
        mutableStateOf(
            editingItem?.expiryDate ?: DateUtils.addDaysToIso(
                DateUtils.todayIso(),
                DateUtils.estimateShelfLifeDays("", StorageLocation.FRIDGE)
            )
        )
    }
    var nameError by remember { mutableStateOf(false) }
    var activeDatePicker by remember { mutableStateOf(ActiveDatePicker.NONE) }

    val estimatedDays = remember(name, storageLocation) {
        DateUtils.estimateShelfLifeDays(name, storageLocation)
    }

    // Recalculate expiry date automatically when auto-estimation is ON and name/location/purchaseDate changes
    LaunchedEffect(name, storageLocation, purchaseDate, isExpiryEstimated) {
        if (isExpiryEstimated) {
            val days = DateUtils.estimateShelfLifeDays(name, storageLocation)
            expiryDate = DateUtils.addDaysToIso(purchaseDate, days)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("add_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại tủ đồ"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (isEditing) R.string.edit_screen_title else R.string.add_screen_title
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Điền thông tin để PantryFlow nhắc hạn giúp bạn",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick-fill presets when adding new item
            if (!isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Chọn nhanh thực phẩm phổ biến",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CommonVietnamesePresets) { preset ->
                            AssistChip(
                                onClick = {
                                    name = preset.name
                                    nameError = false
                                    quantityText = DateUtils.formatQuantity(preset.defaultQuantity)
                                    unit = preset.defaultUnit
                                    storageLocation = preset.defaultLocation
                                    isExpiryEstimated = true
                                    val days = DateUtils.estimateShelfLifeDays(
                                        preset.name,
                                        preset.defaultLocation
                                    )
                                    expiryDate = DateUtils.addDaysToIso(purchaseDate, days)
                                },
                                label = {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }

            // Section 1: Name & Quantity
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
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) nameError = false
                        },
                        label = { Text(stringResource(R.string.field_name_label)) },
                        placeholder = { Text(stringResource(R.string.field_name_placeholder)) },
                        isError = nameError,
                        supportingText = if (nameError) {
                            { Text("Vui lòng nhập tên thực phẩm") }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_food_name")
                    )

                    // Quantity Stepper
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.field_quantity_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                IconButton(
                                    onClick = {
                                        val current = quantityText.toDoubleOrNull() ?: 1.0
                                        val step = if (unit == "g" || unit == "ml") 50.0 else 1.0
                                        val next = (current - step).coerceAtLeast(if (unit == "g" || unit == "ml") 50.0 else 0.5)
                                        quantityText = DateUtils.formatQuantity(next)
                                    },
                                    modifier = Modifier.testTag("form_qty_minus")
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Giảm số lượng")
                                }
                            }

                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        quantityText = input
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_quantity")
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                IconButton(
                                    onClick = {
                                        val current = quantityText.toDoubleOrNull() ?: 0.0
                                        val step = if (unit == "g" || unit == "ml") 50.0 else 1.0
                                        quantityText = DateUtils.formatQuantity(current + step)
                                    },
                                    modifier = Modifier.testTag("form_qty_plus")
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Tăng số lượng",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Unit Selector Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.field_unit_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(CommonUnits) { candidate ->
                                val selected = unit == candidate
                                FilterChip(
                                    selected = selected,
                                    onClick = { unit = candidate },
                                    label = { Text(candidate) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("unit_chip_$candidate")
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Storage Location 2x2 Selector
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
                    Text(
                        text = stringResource(R.string.field_location_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val rows = StorageLocation.entries.chunked(2)
                    rows.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { loc ->
                                val selected = storageLocation == loc
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { storageLocation = loc }
                                        .testTag("location_select_${loc.name.lowercase()}"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    border = BorderStroke(
                                        width = if (selected) 1.6.dp else 1.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = locationIcon(loc),
                                            contentDescription = null,
                                            tint = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = loc.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Dates & Auto-Estimate
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
                    // Smart Expiry Auto-Estimate Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.auto_estimate_label),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Gợi ý +$estimatedDays ngày cho ${storageLocation.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isExpiryEstimated,
                            onCheckedChange = { isExpiryEstimated = it },
                            modifier = Modifier.testTag("switch_auto_estimate")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DateFieldCard(
                            label = stringResource(R.string.field_purchase_date_label),
                            isoDate = purchaseDate,
                            badgeText = null,
                            onClick = { activeDatePicker = ActiveDatePicker.PURCHASE },
                            testTag = "pick_purchase_date",
                            modifier = Modifier.weight(1f)
                        )
                        DateFieldCard(
                            label = stringResource(R.string.field_expiry_date_label),
                            isoDate = expiryDate,
                            badgeText = if (isExpiryEstimated) stringResource(R.string.suggested_badge) else null,
                            onClick = { activeDatePicker = ActiveDatePicker.EXPIRY },
                            testTag = "pick_expiry_date",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        nameError = true
                        return@Button
                    }
                    val parsedQty = quantityText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
                    onSave(
                        editingItem?.id,
                        trimmed,
                        parsedQty,
                        unit,
                        storageLocation,
                        purchaseDate,
                        expiryDate,
                        isExpiryEstimated
                    )
                },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 15.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_pantry_item_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(if (isEditing) R.string.action_update else R.string.action_save),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Material 3 Date Picker Dialog
    if (activeDatePicker != ActiveDatePicker.NONE) {
        val initialIso = if (activeDatePicker == ActiveDatePicker.PURCHASE) purchaseDate else expiryDate
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.isoToUtcMillis(initialIso)
        )
        DatePickerDialog(
            onDismissRequest = { activeDatePicker = ActiveDatePicker.NONE },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val chosenIso = DateUtils.utcMillisToIso(selectedMillis)
                            if (activeDatePicker == ActiveDatePicker.PURCHASE) {
                                purchaseDate = chosenIso
                            } else {
                                expiryDate = chosenIso
                                isExpiryEstimated = false
                            }
                        }
                        activeDatePicker = ActiveDatePicker.NONE
                    }
                ) {
                    Text("Chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDatePicker = ActiveDatePicker.NONE }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DateFieldCard(
    label: String,
    isoDate: String,
    badgeText: String?,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = DateUtils.formatDisplayDate(isoDate),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
