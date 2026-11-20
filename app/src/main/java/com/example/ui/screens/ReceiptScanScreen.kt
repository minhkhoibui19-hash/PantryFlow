package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.ai.ReceiptDraftItem
import com.example.ai.ReceiptScannerService
import com.example.data.model.CommonUnits
import com.example.data.model.StorageLocation
import com.example.data.util.DateUtils
import com.example.ui.components.ReceiptAnalyzingSkeletonCard
import com.example.ui.components.locationIcon
import com.example.ui.theme.LocalPantryColors
import com.example.viewmodel.ReceiptScanUiState
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanScreen(
    scanState: ReceiptScanUiState,
    onAnalyzeBitmap: (Bitmap) -> Unit,
    onUseSampleReceipt: () -> Unit,
    onUpdateDraftRow: (ReceiptDraftItem) -> Unit,
    onDeleteDraftRow: (String) -> Unit,
    onAddEmptyDraftRow: () -> Unit,
    onSaveAllDrafts: () -> Unit,
    onRetryCapture: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraPermissionDenied by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editingDateRowId by remember { mutableStateOf<String?>(null) }

    val isReviewStep = scanState is ReceiptScanUiState.ReviewTable

    BackHandler {
        if (isReviewStep) {
            onRetryCapture()
        } else {
            onClose()
        }
    }

    fun processImageUri(uri: Uri) {
        scope.launch {
            val bitmap = ReceiptScannerService.decodeBitmapFromUri(context, uri)
            if (bitmap != null) {
                previewBitmap = bitmap
                onAnalyzeBitmap(bitmap)
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            processImageUri(uri)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            processImageUri(uri)
        }
    }

    fun launchCameraCapture() {
        val uri = createTempReceiptImageUri(context)
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraPermissionDenied = false
            launchCameraCapture()
        } else {
            cameraPermissionDenied = true
        }
    }

    fun requestOrLaunchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            cameraPermissionDenied = false
            launchCameraCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            if (isReviewStep) onRetryCapture() else onClose()
                        },
                        modifier = Modifier.testTag("receipt_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isReviewStep) "Bảng kiểm tra hóa đơn" else "Chụp hóa đơn",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isReviewStep) {
                                "Bước 2/2 • Chỉnh sửa trực tiếp từng ô rồi lưu"
                            } else {
                                "Bước 1/2 • Chụp ảnh hoặc tải ảnh hóa đơn siêu thị"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isReviewStep) {
                    TextButton(
                        onClick = onRetryCapture,
                        modifier = Modifier.testTag("receipt_rescan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chụp lại")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
                targetState = scanState,
                label = "receipt_scan_step_transition",
                modifier = Modifier.weight(1f)
            ) { state ->
                when (state) {
                    is ReceiptScanUiState.CaptureStep,
                    is ReceiptScanUiState.Analyzing,
                    is ReceiptScanUiState.Error -> {
                        ReceiptCaptureStepContent(
                            scanState = state,
                            previewBitmap = previewBitmap,
                            cameraPermissionDenied = cameraPermissionDenied,
                            onTakePhoto = { requestOrLaunchCamera() },
                            onPickFromGallery = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onUseSampleReceipt = onUseSampleReceipt,
                            onRetryWithLastBitmap = {
                                previewBitmap?.let { onAnalyzeBitmap(it) }
                            }
                        )
                    }

                    is ReceiptScanUiState.ReviewTable -> {
                        ReceiptExcelReviewStepContent(
                            items = state.items,
                            isSampleData = state.isSampleData,
                            onUpdateRow = onUpdateDraftRow,
                            onDeleteRow = onDeleteDraftRow,
                            onOpenDatePicker = { rowId -> editingDateRowId = rowId },
                            onAddEmptyRow = onAddEmptyDraftRow,
                            onConfirmSaveAll = onSaveAllDrafts
                        )
                    }
                }
            }
        }
    }

    // Material 3 DatePickerDialog for editing expiry date directly from Step 2 table
    val currentReviewState = scanState as? ReceiptScanUiState.ReviewTable
    val rowForDate = currentReviewState?.items?.firstOrNull { it.draftId == editingDateRowId }
    if (rowForDate != null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.isoToUtcMillis(rowForDate.expiryDate)
        )
        DatePickerDialog(
            onDismissRequest = { editingDateRowId = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onUpdateDraftRow(
                                rowForDate.copy(
                                    expiryDate = DateUtils.utcMillisToIso(millis),
                                    isExpiryEstimated = false
                                )
                            )
                        }
                        editingDateRowId = null
                    }
                ) {
                    Text("Chọn ngày")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingDateRowId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ReceiptCaptureStepContent(
    scanState: ReceiptScanUiState,
    previewBitmap: Bitmap?,
    cameraPermissionDenied: Boolean,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onUseSampleReceipt: () -> Unit,
    onRetryWithLastBitmap: () -> Unit
) {
    val pantryColors = LocalPantryColors.current
    val isAnalyzing = scanState is ReceiptScanUiState.Analyzing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Feature Highlight Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "PantryFlow tự động đọc hóa đơn Việt Nam",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "• Tự động lọc bỏ túi nilon, xà phòng, tiền thừa...\n• Dịch tên viết tắt không dấu (WinMart, BHX, Co.opmart...) sang tiếng Việt có dấu\n• Tự gợi ý nơi bảo quản và hạn sử dụng",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
            }
        }

        // Loading State with Receipt Preview + Shimmer Skeleton Table
        if (isAnalyzing) {
            ReceiptAnalyzingSkeletonCard(previewBitmap = previewBitmap)
        } else {
            // Error State Banner (Authentic Error Surface)
            if (scanState is ReceiptScanUiState.Error) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = pantryColors.urgentBg,
                    border = BorderStroke(1.dp, pantryColors.urgentAccent.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = pantryColors.urgentFg,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Không thể phân tích hóa đơn",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = pantryColors.urgentFg
                            )
                        }
                        Text(
                            text = scanState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = pantryColors.urgentFg
                        )
                        if (previewBitmap != null) {
                            TextButton(
                                onClick = onRetryWithLastBitmap,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Thử phân tích lại ảnh vừa chọn")
                            }
                        }
                    }
                }
            }

            if (cameraPermissionDenied) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = pantryColors.warningBg,
                    border = BorderStroke(1.dp, pantryColors.warningAccent.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Ứng dụng cần quyền Camera để chụp hóa đơn. Bạn có thể cấp quyền hoặc chọn ảnh có sẵn từ thư viện bên dưới.",
                        style = MaterialTheme.typography.bodySmall,
                        color = pantryColors.warningFg,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Capture Action Cards
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Text(
                        text = "Tải ảnh hoặc chụp hóa đơn đi chợ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onTakePhoto,
                            shape = RoundedCornerShape(15.dp),
                            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("receipt_take_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Chụp ảnh",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FilledTonalButton(
                            onClick = onPickFromGallery,
                            shape = RoundedCornerShape(15.dp),
                            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("receipt_pick_gallery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Thư viện",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Quick Test Mode Button ("Dùng hóa đơn mẫu (Test ngay)")
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Muốn xem thử bảng chỉnh sửa ngay?",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Tải ngay 5 món mẫu mô phỏng hóa đơn siêu thị (Ức gà phi lê, Trứng gà, Rau cải ngọt, Sữa tươi, Tôm sú đông lạnh) để kiểm tra giao diện bảng giống Excel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onUseSampleReceipt,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 13.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("use_sample_receipt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dùng hóa đơn mẫu (Test ngay)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptExcelReviewStepContent(
    items: List<ReceiptDraftItem>,
    isSampleData: Boolean,
    onUpdateRow: (ReceiptDraftItem) -> Unit,
    onDeleteRow: (String) -> Unit,
    onOpenDatePicker: (String) -> Unit,
    onAddEmptyRow: () -> Unit,
    onConfirmSaveAll: () -> Unit
) {
    val horizontalScrollState = rememberScrollState()
    val validCount = items.count { it.name.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status & Horizontal Scroll Tip
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Swipe,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isSampleData) {
                            "Dữ liệu hóa đơn mẫu (${items.size} món) • Vuốt ngang bảng để sửa từng ô"
                        } else {
                            "PantryFlow đã nhận diện ${items.size} thực phẩm • Vuốt ngang bảng để chỉnh sửa"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Excel-like Horizontally Scrollable Editable Table
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("receipt_excel_table"),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
            ) {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableHeaderCell("#", width = 32.dp, alignCenter = true)
                    TableHeaderCell("Tên thực phẩm", width = 186.dp)
                    TableHeaderCell("Số lượng", width = 160.dp, alignCenter = true)
                    TableHeaderCell("Đơn vị", width = 104.dp, alignCenter = true)
                    TableHeaderCell("Nơi bảo quản", width = 140.dp)
                    TableHeaderCell("Hạn dùng (Gợi ý)", width = 156.dp)
                    TableHeaderCell("Xóa", width = 54.dp, alignCenter = true)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(832.dp)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có dòng thực phẩm nào. Nhấn \"+ Thêm dòng mới\" bên dưới để bổ sung.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items.forEachIndexed { index, row ->
                        ExcelEditableRow(
                            rowIndex = index + 1,
                            item = row,
                            isEvenRow = index % 2 == 1,
                            onUpdate = onUpdateRow,
                            onDelete = { onDeleteRow(row.draftId) },
                            onPickDate = { onOpenDatePicker(row.draftId) }
                        )
                        if (index < items.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }

        // "+ Thêm dòng mới" button
        OutlinedButton(
            onClick = onAddEmptyRow,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("receipt_add_row_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Thêm dòng mới",
                style = MaterialTheme.typography.labelLarge
            )
        }

        // "Xác nhận & Lưu tất cả vào tủ đồ" primary CTA
        Button(
            onClick = onConfirmSaveAll,
            enabled = validCount > 0,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 15.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("receipt_confirm_save_all_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Xác nhận & Lưu tất cả vào tủ đồ ($validCount món)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    alignCenter: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = if (alignCenter) TextAlign.Center else TextAlign.Start,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 6.dp)
    )
}

@Composable
private fun ExcelEditableRow(
    rowIndex: Int,
    item: ReceiptDraftItem,
    isEvenRow: Boolean,
    onUpdate: (ReceiptDraftItem) -> Unit,
    onDelete: () -> Unit,
    onPickDate: () -> Unit
) {
    val pantryColors = LocalPantryColors.current
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var locationMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .background(
                if (isEvenRow) MaterialTheme.colorScheme.surfaceContainerLow
                else MaterialTheme.colorScheme.surface
            )
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Row number
        Text(
            text = "$rowIndex",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(32.dp)
        )

        // Column 1: Tên thực phẩm (direct editable text cell)
        Column(
            modifier = Modifier
                .width(186.dp)
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (item.name.isEmpty()) {
                    Text(
                        text = "Nhập tên món...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("excel_name_${item.draftId}")
                )
            }
            if (item.rawLabel.isNotBlank()) {
                Text(
                    text = "Gốc: ${item.rawLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // Column 2: Số lượng (- / direct input / +) with comfortable thumb targets
        Row(
            modifier = Modifier
                .width(160.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(42.dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        val current = item.quantityText.replace(",", ".").toDoubleOrNull() ?: 1.0
                        val step = when (item.unit.lowercase()) {
                            "g", "ml" -> 50.0
                            "kg", "lít" -> 0.5
                            else -> 1.0
                        }
                        val next = (current - step).coerceAtLeast(if (item.unit == "g" || item.unit == "ml") 50.0 else 0.5)
                        val rounded = round(next * 10.0) / 10.0
                        onUpdate(item.copy(quantityText = DateUtils.formatQuantity(rounded)))
                    }
                    .testTag("excel_qty_minus_${item.draftId}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Remove, contentDescription = "Giảm", modifier = Modifier.size(18.dp))
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 4.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = item.quantityText,
                    onValueChange = { input ->
                        val cleaned = input.replace(",", ".")
                        if (cleaned.isEmpty() || cleaned.matches(Regex("^\\d*\\.?\\d*$"))) {
                            onUpdate(item.copy(quantityText = cleaned))
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("excel_qty_input_${item.draftId}")
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(42.dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        val current = item.quantityText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val step = when (item.unit.lowercase()) {
                            "g", "ml" -> 50.0
                            "kg", "lít" -> 0.5
                            else -> 1.0
                        }
                        val rounded = round((current + step) * 10.0) / 10.0
                        onUpdate(item.copy(quantityText = DateUtils.formatQuantity(rounded)))
                    }
                    .testTag("excel_qty_plus_${item.draftId}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Tăng",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Column 3: Đơn vị (Dropdown)
        Box(
            modifier = Modifier
                .width(104.dp)
                .padding(horizontal = 4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { unitMenuExpanded = true }
                    .testTag("excel_unit_${item.draftId}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.unit,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Chọn đơn vị",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = unitMenuExpanded,
                onDismissRequest = { unitMenuExpanded = false }
            ) {
                CommonUnits.forEach { u ->
                    DropdownMenuItem(
                        text = { Text(u) },
                        onClick = {
                            onUpdate(item.copy(unit = u))
                            unitMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Column 4: Nơi bảo quản (Dropdown)
        Box(
            modifier = Modifier
                .width(140.dp)
                .padding(horizontal = 4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { locationMenuExpanded = true }
                    .testTag("excel_loc_${item.draftId}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = locationIcon(item.storageLocation),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = item.storageLocation.label,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Chọn nơi bảo quản",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = locationMenuExpanded,
                onDismissRequest = { locationMenuExpanded = false }
            ) {
                StorageLocation.entries.forEach { loc ->
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = locationIcon(loc),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        text = { Text(loc.label) },
                        onClick = {
                            val updatedExpiry = if (item.isExpiryEstimated) {
                                DateUtils.addDaysToIso(
                                    DateUtils.todayIso(),
                                    DateUtils.estimateShelfLifeDays(item.name, loc)
                                )
                            } else {
                                item.expiryDate
                            }
                            onUpdate(item.copy(storageLocation = loc, expiryDate = updatedExpiry))
                            locationMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Column 5: Hạn dùng (Gợi ý) -> clickable date cell
        Box(
            modifier = Modifier
                .width(156.dp)
                .padding(horizontal = 4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onPickDate)
                    .testTag("excel_date_${item.draftId}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = DateUtils.formatDisplayDate(item.expiryDate),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        if (item.isExpiryEstimated) {
                            Text(
                                text = "Gợi ý",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // Column 6: Nút xóa dòng
        Box(
            modifier = Modifier.width(54.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("excel_delete_${item.draftId}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Xóa dòng",
                    tint = pantryColors.urgentFg,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

private fun createTempReceiptImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
