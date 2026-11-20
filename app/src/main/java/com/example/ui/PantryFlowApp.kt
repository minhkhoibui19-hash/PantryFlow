package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ItemStatus
import com.example.data.util.NotificationHelper
import com.example.ui.components.AddMethodBottomSheet
import com.example.ui.components.PantryBottomBar
import com.example.ui.components.PantryNavigationRail
import com.example.ui.components.PantryToastOverlay
import com.example.ui.screens.AddEditItemScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FlashcardAddScreen
import com.example.ui.screens.ReceiptScanScreen
import com.example.ui.screens.RecipesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.PantryFlowTheme
import com.example.viewmodel.AddFlowMode
import com.example.viewmodel.PantryTab
import com.example.viewmodel.PantryUiState
import com.example.viewmodel.PantryViewModel
import com.example.viewmodel.ReceiptScanUiState
import com.example.viewmodel.RecipeTabUiState
import com.example.viewmodel.SnackbarEvent
import kotlinx.coroutines.delay

@Composable
fun PantryFlowApp(
    viewModel: PantryViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val receiptScanState by viewModel.receiptScanState.collectAsStateWithLifecycle()
    val recipeTabState by viewModel.recipeTabState.collectAsStateWithLifecycle()
    var activeToast by remember { mutableStateOf<SnackbarEvent?>(null) }
    var toastSeq by remember { mutableStateOf(0) }
    val undoLabel = stringResource(R.string.action_undo)

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { event ->
            activeToast = event
            toastSeq += 1
        }
    }

    LaunchedEffect(toastSeq) {
        val current = activeToast
        if (current != null) {
            val timeoutMs = if (current.undoItemId != null) 4200L else 3200L
            delay(timeoutMs)
            activeToast = null
        }
    }

    PantryFlowTheme(darkTheme = uiState.settings.darkModeEnabled) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 700.dp

            Box(modifier = Modifier.fillMaxSize()) {
                if (useNavigationRail) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .windowInsetsPadding(WindowInsets.statusBars)
                        ) {
                            PantryNavigationRail(
                                selectedTab = uiState.selectedTab,
                                urgentCount = uiState.urgentCount,
                                onSelectTab = viewModel::selectTab
                            )
                            PantryTabContent(
                                uiState = uiState,
                                receiptScanState = receiptScanState,
                                recipeTabState = recipeTabState,
                                viewModel = viewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            PantryBottomBar(
                                selectedTab = uiState.selectedTab,
                                urgentCount = uiState.urgentCount,
                                onSelectTab = viewModel::selectTab
                            )
                        }
                    ) { innerPadding ->
                        PantryTabContent(
                            uiState = uiState,
                            receiptScanState = receiptScanState,
                            recipeTabState = recipeTabState,
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .windowInsetsPadding(WindowInsets.statusBars)
                        )
                    }
                }

                // Floating Branded Toast Notification
                PantryToastOverlay(
                    event = activeToast,
                    undoLabel = undoLabel,
                    onUndo = {
                        val evt = activeToast
                        if (evt?.undoItemId != null && evt.undoPreviousStatus != null) {
                            viewModel.undoStatusChange(evt.undoItemId, evt.undoPreviousStatus)
                        }
                        activeToast = null
                    },
                    onDismiss = { activeToast = null },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                )
            }

            if (uiState.showAddMethodSheet) {
                AddMethodBottomSheet(
                    onDismiss = viewModel::dismissAddMethodSheet,
                    onSelectManualEntry = viewModel::startManualFlashcardAdd,
                    onSelectReceiptScan = viewModel::startReceiptScanFlow
                )
            }
        }
    }
}

@Composable
private fun PantryTabContent(
    uiState: PantryUiState,
    receiptScanState: ReceiptScanUiState,
    recipeTabState: RecipeTabUiState,
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = Pair(uiState.selectedTab, uiState.addFlowMode),
        label = "pantry_tab_crossfade",
        modifier = modifier
    ) { (tab, addMode) ->
        when (tab) {
            PantryTab.PANTRY -> DashboardScreen(
                uiState = uiState,
                onSelectLocation = viewModel::selectLocation,
                onToggleUrgency = viewModel::toggleUrgencyFilter,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onUsePartialQuantity = viewModel::consumePartialQuantity,
                onMarkItemStatus = viewModel::markItemStatus,
                onQuickUpdateLocation = viewModel::quickUpdateLocation,
                onQuickUpdateExpiryDate = viewModel::quickUpdateExpiryDate,
                onEditItem = viewModel::startEditing,
                onOpenAddScreen = viewModel::openAddMethodSheet
            )

            PantryTab.ADD -> {
                when (addMode) {
                    AddFlowMode.EDIT_ITEM -> AddEditItemScreen(
                        editingItem = uiState.editingItem,
                        onSave = { id, name, qty, unit, loc, purchase, expiry, estimated ->
                            viewModel.savePantryItem(
                                id = id,
                                name = name,
                                quantity = qty,
                                unit = unit,
                                storageLocation = loc,
                                purchaseDate = purchase,
                                expiryDate = expiry,
                                isExpiryEstimated = estimated,
                                keepAdding = false
                            )
                        },
                        onCancel = viewModel::cancelEditing
                    )

                    AddFlowMode.MANUAL_FLASHCARD -> FlashcardAddScreen(
                        onSaveItem = { name, qty, unit, loc, purchase, expiry, estimated, keepAdding ->
                            viewModel.savePantryItem(
                                id = null,
                                name = name,
                                quantity = qty,
                                unit = unit,
                                storageLocation = loc,
                                purchaseDate = purchase,
                                expiryDate = expiry,
                                isExpiryEstimated = estimated,
                                keepAdding = keepAdding
                            )
                        },
                        onClose = viewModel::cancelEditing
                    )

                    AddFlowMode.RECEIPT_SCAN -> ReceiptScanScreen(
                        scanState = receiptScanState,
                        onAnalyzeBitmap = viewModel::analyzeReceiptBitmap,
                        onUseSampleReceipt = viewModel::loadSampleReceipt,
                        onUpdateDraftRow = viewModel::updateReceiptDraftRow,
                        onDeleteDraftRow = viewModel::deleteReceiptDraftRow,
                        onAddEmptyDraftRow = viewModel::addEmptyReceiptDraftRow,
                        onSaveAllDrafts = viewModel::saveAllReceiptDrafts,
                        onRetryCapture = viewModel::resetReceiptScanToCapture,
                        onClose = viewModel::cancelEditing
                    )
                }
            }

            PantryTab.RECIPES -> {
                BackHandler {
                    viewModel.selectTab(PantryTab.PANTRY)
                }
                RecipesScreen(
                    uiState = uiState,
                    recipeTabState = recipeTabState,
                    onToggleIngredient = viewModel::toggleRecipeIngredient,
                    onSelectOnlyExpiring = viewModel::selectOnlyExpiringIngredients,
                    onSelectAllIngredients = viewModel::selectAllRecipeIngredients,
                    onSetCookTimeFilter = viewModel::setCookTimeFilter,
                    onSetMealTypeFilter = viewModel::setMealTypeFilter,
                    onGenerateAiRecipes = viewModel::generateAiRecipes,
                    onLoadSampleRecipes = viewModel::loadSampleRecipes,
                    onConfirmCookedDeductions = viewModel::applyCookedRecipeDeductions
                )
            }

            PantryTab.SETTINGS -> {
                val context = LocalContext.current
                BackHandler {
                    viewModel.selectTab(PantryTab.PANTRY)
                }
                SettingsScreen(
                    uiState = uiState,
                    onSetReminderDaysBefore = viewModel::setReminderDaysBefore,
                    onSetReminderTime = viewModel::setReminderTime,
                    onSetNotificationsEnabled = viewModel::setNotificationsEnabled,
                    onSendTestNotification = {
                        val sent = NotificationHelper.sendExpiryReminderNotification(
                            context = context,
                            expiringItems = uiState.reminderAlertItems,
                            reminderDaysBefore = uiState.settings.reminderDaysBefore,
                            reminderTimeText = uiState.settings.formattedReminderTime
                        )
                        viewModel.notifyTestNotificationSent(sent)
                    },
                    onSetDarkMode = viewModel::setDarkModeEnabled,
                    onRestoreItem = { item -> viewModel.markItemStatus(item, ItemStatus.ACTIVE) },
                    onResetSampleData = viewModel::resetSampleData,
                    onClearHistory = viewModel::clearHistory
                )
            }
        }
    }
}
