package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.StorageLocation
import com.example.data.util.DateUtils
import com.example.ui.components.locationIcon
import kotlin.math.abs
import kotlin.math.round

private val FlashcardQuickFoods = listOf(
    "Thịt heo",
    "Ức gà",
    "Rau cải",
    "Trứng",
    "Sữa tươi",
    "Cá hồi",
    "Cà chua",
    "Tôm sú",
    "Đậu hũ",
    "Gạo"
)

private val FlashcardQuickUnits = listOf(
    "g", "kg", "quả", "hộp", "bó", "bịch", "ml"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardAddScreen(
    onSaveItem: (
        name: String,
        quantity: Double,
        unit: String,
        storageLocation: StorageLocation,
        purchaseDate: String,
        expiryDate: String,
        isExpiryEstimated: Boolean,
        keepAdding: Boolean
    ) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current.density

    var isFlipped by rememberSaveable { mutableStateOf(false) }
    var addedSessionCount by rememberSaveable { mutableIntStateOf(0) }

    var name by rememberSaveable { mutableStateOf("") }
    var lastSuggestedName by rememberSaveable { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var unit by rememberSaveable { mutableStateOf("g") }
    var quantityText by rememberSaveable { mutableStateOf("500") }
    var storageLocation by rememberSaveable { mutableStateOf(StorageLocation.FRIDGE) }
    var purchaseDate by rememberSaveable { mutableStateOf(DateUtils.todayIso()) }
    var expiryDate by rememberSaveable {
        mutableStateOf(
            DateUtils.addDaysToIso(
                DateUtils.todayIso(),
                DateUtils.estimateShelfLifeDays("", StorageLocation.FRIDGE)
            )
        )
    }
    var isExpiryEstimated by rememberSaveable { mutableStateOf(true) }
    var showExpiryDatePicker by remember { mutableStateOf(false) }

    // System back button: flip back to front if on back face, else exit to Pantry
    BackHandler {
        if (isFlipped) {
            isFlipped = false
        } else {
            onClose()
        }
    }

    fun applySmartSuggestionsForName(foodName: String) {
        val suggestedLocation = DateUtils.suggestStorageLocation(foodName)
        val (suggestedUnit, suggestedQty) = DateUtils.suggestUnitAndQuantity(foodName)
        val estimatedDays = DateUtils.estimateShelfLifeDays(foodName, suggestedLocation)
        storageLocation = suggestedLocation
        unit = suggestedUnit
        quantityText = DateUtils.formatQuantity(suggestedQty)
        expiryDate = DateUtils.addDaysToIso(purchaseDate, estimatedDays)
        isExpiryEstimated = true
        lastSuggestedName = foodName
    }

    fun proceedToBackFace() {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            nameError = true
            return
        }
        focusManager.clearFocus()
        if (!trimmed.equals(lastSuggestedName, ignoreCase = true)) {
            applySmartSuggestionsForName(trimmed)
        }
        isFlipped = true
    }

    fun resetCardForNextItem() {
        name = ""
        lastSuggestedName = ""
        nameError = false
        unit = "g"
        quantityText = "500"
        storageLocation = StorageLocation.FRIDGE
        purchaseDate = DateUtils.todayIso()
        expiryDate = DateUtils.addDaysToIso(
            purchaseDate,
            DateUtils.estimateShelfLifeDays("", StorageLocation.FRIDGE)
        )
        isExpiryEstimated = true
        isFlipped = false
    }

    fun commitSave(keepAdding: Boolean) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            isFlipped = false
            nameError = true
            return
        }
        val qty = quantityText.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
        focusManager.clearFocus()
        onSaveItem(
            trimmed,
            qty,
            unit,
            storageLocation,
            purchaseDate,
            expiryDate,
            isExpiryEstimated,
            keepAdding
        )
        if (keepAdding) {
            addedSessionCount += 1
            resetCardForNextItem()
        }
    }

    // 3D Flip Animation: 0f (Front) <-> 180f (Back)
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "flashcard_3d_rotation"
    )
    // Subtle physical depth dip at 90 degrees midpoint so 3D edges never clip on compact mobile viewports
    val midpointDistance = abs(rotation - 90f) / 90f
    val flipScale = 0.90f + (0.10f * midpointDistance)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top bar with step indicator & session counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (isFlipped) isFlipped = false else onClose()
                        },
                        modifier = Modifier.testTag("flashcard_close_button")
                    ) {
                        Icon(
                            imageVector = if (isFlipped) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Close,
                            contentDescription = if (isFlipped) "Quay lại mặt trước" else "Đóng"
                        )
                    }
                    Column {
                        Text(
                            text = "Nhập thủ công",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (!isFlipped) "Bước 1/2 • Chọn hoặc gõ tên thực phẩm"
                            else "Bước 2/2 • Số lượng & nơi bảo quản",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(visible = addedSessionCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Đã thêm $addedSessionCount",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Progress dots indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (!isFlipped) 32.dp else 12.dp)
                        .clip(CircleShape)
                        .background(
                            if (!isFlipped) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (isFlipped) 32.dp else 12.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFlipped) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                )
            }

            // 3D Flipping Flashcard Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        rotationY = rotation
                        scaleX = flipScale
                        scaleY = flipScale
                        cameraDistance = 16f * density
                    }
                    .testTag("flashcard_3d_container")
            ) {
                if (rotation <= 90f) {
                    // FRONT FACE (0° -> 90°)
                    FlashcardFrontFace(
                        name = name,
                        nameError = nameError,
                        onNameChange = {
                            name = it
                            if (it.isNotBlank()) nameError = false
                        },
                        onQuickFoodSelected = { quickName ->
                            name = quickName
                            nameError = false
                        },
                        onContinue = { proceedToBackFace() }
                    )
                } else {
                    // BACK FACE (90° -> 180°), counter-rotated 180° around Y so text is never mirrored!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                rotationY = 180f
                            }
                    ) {
                        FlashcardBackFace(
                            foodName = name.trim(),
                            unit = unit,
                            quantityText = quantityText,
                            storageLocation = storageLocation,
                            expiryDate = expiryDate,
                            isExpiryEstimated = isExpiryEstimated,
                            onSelectUnit = { selectedUnit ->
                                unit = selectedUnit
                                // Adjust default sensible quantity when switching between mass/volume and count
                                val currentQty = quantityText.replace(",", ".").toDoubleOrNull() ?: 1.0
                                if ((selectedUnit == "g" || selectedUnit == "ml") && currentQty <= 20.0) {
                                    quantityText = "500"
                                } else if (selectedUnit != "g" && selectedUnit != "ml" && currentQty >= 50.0) {
                                    quantityText = "1"
                                }
                            },
                            onQuantityChange = { input ->
                                val cleaned = input.replace(",", ".")
                                if (cleaned.isEmpty() || cleaned.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    quantityText = cleaned
                                }
                            },
                            onStepQuantity = { delta ->
                                val current = quantityText.replace(",", ".").toDoubleOrNull() ?: 0.0
                                val step = when (unit) {
                                    "g", "ml" -> 100.0
                                    "kg" -> 0.5
                                    else -> 1.0
                                }
                                val minVal = when (unit) {
                                    "g", "ml" -> 50.0
                                    "kg" -> 0.5
                                    else -> 0.5
                                }
                                val next = (current + delta * step).coerceAtLeast(minVal)
                                val rounded = round(next * 10.0) / 10.0
                                quantityText = DateUtils.formatQuantity(rounded)
                            },
                            onSelectLocation = { newLoc ->
                                storageLocation = newLoc
                                if (isExpiryEstimated) {
                                    val days = DateUtils.estimateShelfLifeDays(name, newLoc)
                                    expiryDate = DateUtils.addDaysToIso(purchaseDate, days)
                                }
                            },
                            onOpenExpiryPicker = { showExpiryDatePicker = true },
                            onResetEstimate = {
                                val days = DateUtils.estimateShelfLifeDays(name, storageLocation)
                                expiryDate = DateUtils.addDaysToIso(purchaseDate, days)
                                isExpiryEstimated = true
                            },
                            onFlipBack = { isFlipped = false },
                            onSaveAndAddMore = { commitSave(keepAdding = true) },
                            onFinish = { commitSave(keepAdding = false) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showExpiryDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.isoToUtcMillis(expiryDate)
        )
        DatePickerDialog(
            onDismissRequest = { showExpiryDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            expiryDate = DateUtils.utcMillisToIso(millis)
                            isExpiryEstimated = false
                        }
                        showExpiryDatePicker = false
                    }
                ) {
                    Text("Chọn ngày")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpiryDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlashcardFrontFace(
    name: String,
    nameError: Boolean,
    onNameChange: (String) -> Unit,
    onQuickFoodSelected: (String) -> Unit,
    onContinue: () -> Unit
) {
    val suggestedLocation = remember(name) { DateUtils.suggestStorageLocation(name) }
    val suggestedDays = remember(name, suggestedLocation) {
        DateUtils.estimateShelfLifeDays(name, suggestedLocation)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flashcard_front_face"),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Subtle botanical accent header strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MẶT TRƯỚC • TÊN THỰC PHẨM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Icon(
                        imageVector = Icons.Filled.FlipCameraAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Bạn vừa mua món gì?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        placeholder = {
                            Text(
                                text = "VD: Thịt heo, Ức gà, Rau cải...",
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        isError = nameError,
                        supportingText = if (nameError) {
                            { Text("Hãy nhập hoặc chọn nhanh một thực phẩm nhé") }
                        } else null,
                        trailingIcon = {
                            if (name.isNotEmpty()) {
                                IconButton(onClick = { onNameChange("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Xóa tên"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { onContinue() }
                        ),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("flashcard_name_input")
                    )

                    // Live smart preview pill when name is non-empty
                    AnimatedVisibility(visible = name.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                    text = "Tự động gợi ý: ${suggestedLocation.label} • HSD khoảng +$suggestedDays ngày",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // Quick suggestion buttons
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Gợi ý nhanh",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlashcardQuickFoods.forEach { food ->
                            val isSelected = name.trim().equals(food, ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onQuickFoodSelected(food) }
                                    .testTag("quick_food_$food"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = food,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Continue -> Flip Button
                Button(
                    onClick = onContinue,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flashcard_next_button")
                ) {
                    Text(
                        text = "Tiếp tục",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlashcardBackFace(
    foodName: String,
    unit: String,
    quantityText: String,
    storageLocation: StorageLocation,
    expiryDate: String,
    isExpiryEstimated: Boolean,
    onSelectUnit: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onStepQuantity: (Int) -> Unit,
    onSelectLocation: (StorageLocation) -> Unit,
    onOpenExpiryPicker: () -> Unit,
    onResetEstimate: () -> Unit,
    onFlipBack: () -> Unit,
    onSaveAndAddMore: () -> Unit,
    onFinish: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flashcard_back_face"),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Back Face Header showing the selected food name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "MẶT SAU • CHI TIẾT LƯU TRỮ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = foodName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onFlipBack)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Đổi tên",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Unit quick buttons (g, kg, quả, hộp, bó, bịch, ml)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Đơn vị",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlashcardQuickUnits.forEach { candidate ->
                            val selected = unit == candidate
                            Surface(
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectUnit(candidate) }
                                    .testTag("flashcard_unit_$candidate"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
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
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = candidate,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Quantity selector: large one-handed - / direct decimal input / +
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Số lượng",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hỗ trợ số thập phân (VD: 0.5)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onStepQuantity(-1) }
                                .testTag("flashcard_qty_minus")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Remove,
                                    contentDescription = "Giảm số lượng",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = onQuantityChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            suffix = {
                                Text(
                                    text = unit,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("flashcard_qty_input")
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onStepQuantity(1) }
                                .testTag("flashcard_qty_plus")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Tăng số lượng",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Storage Location quick selector (2x2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Nơi bảo quản",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StorageLocation.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { loc ->
                                val selected = storageLocation == loc
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 50.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onSelectLocation(loc) }
                                        .testTag("flashcard_loc_${loc.name.lowercase()}"),
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
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
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
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = loc.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Auto-estimated Expiry Date (clickable to edit)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hạn sử dụng",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!isExpiryEstimated) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onResetEstimate)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Dùng ngày gợi ý",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    val daysRemaining = DateUtils.daysUntil(expiryDate)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenExpiryPicker)
                            .testTag("flashcard_expiry_date"),
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
                                            text = DateUtils.formatDisplayDate(expiryDate),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isExpiryEstimated) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Text(
                                                        text = "Gợi ý",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${DateUtils.formatDaysRemainingLabel(daysRemaining)} • Bấm để chỉnh ngày",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Action Buttons: ← Quay lại | Lưu & Thêm tiếp | Hoàn tất (adaptive for compact phones)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isCompact = maxWidth < 310.dp
                        if (isCompact) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = onSaveAndAddMore,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 13.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp)
                                        .testTag("flashcard_save_and_next_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Lưu & Thêm tiếp",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = onFlipBack,
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 13.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("flashcard_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Quay lại",
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onFlipBack,
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 13.dp),
                                    modifier = Modifier
                                        .weight(0.95f)
                                        .heightIn(min = 50.dp)
                                        .testTag("flashcard_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Quay lại",
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                FilledTonalButton(
                                    onClick = onSaveAndAddMore,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 13.dp),
                                    modifier = Modifier
                                        .weight(1.35f)
                                        .heightIn(min = 50.dp)
                                        .testTag("flashcard_save_and_next_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Lưu & Thêm tiếp",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onFinish,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 15.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("flashcard_finish_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hoàn tất",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
