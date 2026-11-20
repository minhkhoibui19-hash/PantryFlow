package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AiRecipeSuggestion
import com.example.ai.CookTimeFilter
import com.example.ai.MealTypeFilter
import com.example.ai.ReceiptDraftItem
import com.example.ai.ReceiptScannerService
import com.example.ai.RecipeSuggestionService
import com.example.data.local.PantrySettings
import com.example.data.model.ExpiryUrgency
import com.example.data.model.ItemStatus
import com.example.data.model.PantryItem
import com.example.data.model.StorageLocation
import com.example.data.repository.PantryRepository
import com.example.data.util.DateUtils
import com.example.data.util.UnitConverter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.util.Locale
import java.util.UUID

private const val TAG = "PantryViewModel"

enum class PantryTab {
    PANTRY,
    ADD,
    RECIPES,
    SETTINGS
}

enum class AddFlowMode {
    MANUAL_FLASHCARD,
    RECEIPT_SCAN,
    EDIT_ITEM
}

sealed interface ReceiptScanUiState {
    data object CaptureStep : ReceiptScanUiState
    data object Analyzing : ReceiptScanUiState
    data class ReviewTable(
        val items: List<ReceiptDraftItem>,
        val isSampleData: Boolean = false
    ) : ReceiptScanUiState
    data class Error(val message: String) : ReceiptScanUiState
}

sealed interface RecipeGenerationState {
    data object Idle : RecipeGenerationState
    data object Loading : RecipeGenerationState
    data class Success(
        val recipes: List<AiRecipeSuggestion>,
        val isSampleData: Boolean = false
    ) : RecipeGenerationState
    data class Error(val message: String) : RecipeGenerationState
}

data class CookedIngredientDeduction(
    val pantryItem: PantryItem,
    val usedAmount: Double,
    val usedUnit: String,
    val markFullyUsed: Boolean
)

data class RecipeTabUiState(
    val selectedIngredientIds: Set<String> = emptySet(),
    val cookTimeFilter: CookTimeFilter = CookTimeFilter.UNDER_30,
    val mealTypeFilter: MealTypeFilter = MealTypeFilter.DINNER,
    val generationState: RecipeGenerationState = RecipeGenerationState.Idle
)

enum class ToastType {
    SUCCESS,
    INFO,
    ERROR
}

data class SnackbarEvent(
    val message: String,
    val type: ToastType = ToastType.SUCCESS,
    val undoItemId: String? = null,
    val undoPreviousStatus: ItemStatus? = null
)

data class PantryUiState(
    val isLoading: Boolean = true,
    val selectedTab: PantryTab = PantryTab.PANTRY,
    val addFlowMode: AddFlowMode = AddFlowMode.MANUAL_FLASHCARD,
    val showAddMethodSheet: Boolean = false,
    val editingItem: PantryItem? = null,
    val allActiveItems: List<PantryItem> = emptyList(),
    val filteredItems: List<PantryItem> = emptyList(),
    val reminderAlertItems: List<PantryItem> = emptyList(),
    val historyItems: List<PantryItem> = emptyList(),
    val selectedLocation: StorageLocation? = null,
    val selectedUrgencyFilter: ExpiryUrgency? = null,
    val searchQuery: String = "",
    val locationCounts: Map<StorageLocation, Int> = emptyMap(),
    val urgentCount: Int = 0,
    val soonCount: Int = 0,
    val freshCount: Int = 0,
    val usedCount: Int = 0,
    val discardedCount: Int = 0,
    val settings: PantrySettings = PantrySettings()
)

class PantryViewModel(
    private val repository: PantryRepository,
    private val receiptScannerService: ReceiptScannerService,
    private val recipeSuggestionService: RecipeSuggestionService
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(PantryTab.PANTRY)
    private val _addFlowMode = MutableStateFlow(AddFlowMode.MANUAL_FLASHCARD)
    private val _showAddMethodSheet = MutableStateFlow(false)
    private val _editingItem = MutableStateFlow<PantryItem?>(null)
    private val _selectedLocation = MutableStateFlow<StorageLocation?>(null)
    private val _selectedUrgencyFilter = MutableStateFlow<ExpiryUrgency?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isInitialized = MutableStateFlow(false)

    private val _receiptScanState = MutableStateFlow<ReceiptScanUiState>(ReceiptScanUiState.CaptureStep)
    val receiptScanState: StateFlow<ReceiptScanUiState> = _receiptScanState.asStateFlow()

    // Recipe suggestion tab state
    private val _hasUserModifiedRecipeSelection = MutableStateFlow(false)
    private val _manualSelectedIngredientIds = MutableStateFlow<Set<String>>(emptySet())
    private val _cookTimeFilter = MutableStateFlow(CookTimeFilter.UNDER_30)
    private val _mealTypeFilter = MutableStateFlow(MealTypeFilter.DINNER)
    private val _recipeGenerationState = MutableStateFlow<RecipeGenerationState>(RecipeGenerationState.Idle)
    private var recipeGenerationJob: Job? = null

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 4)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialSampleDataIfNeeded()
            _isInitialized.value = true
        }
    }

    private data class NavigationFilterState(
        val tab: PantryTab,
        val addMode: AddFlowMode,
        val showAddSheet: Boolean,
        val editingItem: PantryItem?,
        val location: StorageLocation?,
        val urgency: ExpiryUrgency?,
        val query: String
    )

    private val navFilterFlow = combine(
        _selectedTab,
        _addFlowMode,
        _showAddMethodSheet,
        _editingItem
    ) { tab, addMode, showSheet, editing ->
        NavigationFilterState(tab, addMode, showSheet, editing, null, null, "")
    }.combine(
        combine(_selectedLocation, _selectedUrgencyFilter, _searchQuery) { loc, urg, q ->
            Triple(loc, urg, q)
        }
    ) { nav, (loc, urg, q) ->
        nav.copy(location = loc, urgency = urg, query = q)
    }

    val uiState: StateFlow<PantryUiState> = combine(
        repository.activeItems,
        repository.historyItems,
        repository.settings,
        navFilterFlow,
        _isInitialized
    ) { activeRaw, history, settings, filter, initialized ->
        val sortedActive = activeRaw.sortedWith(
            compareBy<PantryItem> { DateUtils.daysUntil(it.expiryDate) }
                .thenBy { it.name.lowercase() }
        )

        val soonThreshold = settings.warningDaysThreshold
        val urgentCount = sortedActive.count {
            DateUtils.urgencyOf(it.expiryDate, soonThreshold) == ExpiryUrgency.URGENT
        }
        val soonCount = sortedActive.count {
            DateUtils.urgencyOf(it.expiryDate, soonThreshold) == ExpiryUrgency.SOON
        }
        val freshCount = sortedActive.count {
            DateUtils.urgencyOf(it.expiryDate, soonThreshold) == ExpiryUrgency.FRESH
        }

        val locationCounts = StorageLocation.entries.associateWith { loc ->
            sortedActive.count { it.storageLocation == loc }
        }

        val trimmedQuery = filter.query.trim().lowercase()
        val filtered = sortedActive.filter { item ->
            val matchesLocation = filter.location == null || item.storageLocation == filter.location
            val matchesUrgency = filter.urgency == null ||
                DateUtils.urgencyOf(item.expiryDate, soonThreshold) == filter.urgency
            val matchesQuery = trimmedQuery.isEmpty() ||
                item.name.lowercase().contains(trimmedQuery) ||
                item.storageLocation.label.lowercase().contains(trimmedQuery)
            matchesLocation && matchesUrgency && matchesQuery
        }

        val reminderAlertItems = sortedActive.filter { item ->
            DateUtils.daysUntil(item.expiryDate) <= settings.reminderDaysBefore
        }

        PantryUiState(
            isLoading = !initialized && sortedActive.isEmpty(),
            selectedTab = filter.tab,
            addFlowMode = filter.addMode,
            showAddMethodSheet = filter.showAddSheet,
            editingItem = filter.editingItem,
            allActiveItems = sortedActive,
            filteredItems = filtered,
            reminderAlertItems = reminderAlertItems,
            historyItems = history,
            selectedLocation = filter.location,
            selectedUrgencyFilter = filter.urgency,
            searchQuery = filter.query,
            locationCounts = locationCounts,
            urgentCount = urgentCount,
            soonCount = soonCount,
            freshCount = freshCount,
            usedCount = history.count { it.status == ItemStatus.USED },
            discardedCount = history.count { it.status == ItemStatus.DISCARDED },
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PantryUiState()
    )

    val recipeTabState: StateFlow<RecipeTabUiState> = combine(
        repository.activeItems,
        _hasUserModifiedRecipeSelection,
        _manualSelectedIngredientIds,
        _cookTimeFilter,
        _mealTypeFilter
    ) { activeItems, hasManualEdit, manualIds, cookTime, mealType ->
        val activeIds = activeItems.map { it.id }.toSet()
        val autoExpiringIds = activeItems
            .filter { DateUtils.daysUntil(it.expiryDate) <= 3 }
            .map { it.id }
            .toSet()

        val resolvedSelection = if (!hasManualEdit) {
            if (autoExpiringIds.isNotEmpty()) autoExpiringIds else activeItems.take(3).map { it.id }.toSet()
        } else {
            manualIds.intersect(activeIds)
        }

        RecipeTabUiState(
            selectedIngredientIds = resolvedSelection,
            cookTimeFilter = cookTime,
            mealTypeFilter = mealType,
            generationState = RecipeGenerationState.Idle
        )
    }.combine(_recipeGenerationState) { base, genState ->
        base.copy(generationState = genState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecipeTabUiState()
    )

    // --- Navigation & Add Flow ---

    fun selectTab(tab: PantryTab) {
        if (tab == PantryTab.ADD) {
            _showAddMethodSheet.value = true
            return
        }
        _editingItem.value = null
        _selectedTab.value = tab
    }

    fun openAddMethodSheet() {
        _showAddMethodSheet.value = true
    }

    fun dismissAddMethodSheet() {
        _showAddMethodSheet.value = false
    }

    fun startManualFlashcardAdd() {
        _showAddMethodSheet.value = false
        _editingItem.value = null
        _addFlowMode.value = AddFlowMode.MANUAL_FLASHCARD
        _selectedTab.value = PantryTab.ADD
    }

    fun startReceiptScanFlow() {
        _showAddMethodSheet.value = false
        _editingItem.value = null
        _receiptScanState.value = ReceiptScanUiState.CaptureStep
        _addFlowMode.value = AddFlowMode.RECEIPT_SCAN
        _selectedTab.value = PantryTab.ADD
    }

    fun startEditing(item: PantryItem) {
        _showAddMethodSheet.value = false
        _editingItem.value = item
        _addFlowMode.value = AddFlowMode.EDIT_ITEM
        _selectedTab.value = PantryTab.ADD
    }

    fun cancelEditing() {
        _editingItem.value = null
        _selectedTab.value = PantryTab.PANTRY
    }

    // --- Tab Gợi ý món (AI Recipe Suggestions + Stock Deduction) ---

    fun toggleRecipeIngredient(itemId: String) {
        val activeIds = uiState.value.allActiveItems.map { it.id }.toSet()
        val currentSelection = if (_hasUserModifiedRecipeSelection.value) {
            _manualSelectedIngredientIds.value.intersect(activeIds)
        } else {
            recipeTabState.value.selectedIngredientIds
        }
        val updated = if (itemId in currentSelection) {
            currentSelection - itemId
        } else {
            currentSelection + itemId
        }
        _hasUserModifiedRecipeSelection.value = true
        _manualSelectedIngredientIds.value = updated
    }

    fun selectOnlyExpiringIngredients() {
        val expiringIds = uiState.value.allActiveItems
            .filter { DateUtils.daysUntil(it.expiryDate) <= 3 }
            .map { it.id }
            .toSet()
        _hasUserModifiedRecipeSelection.value = true
        _manualSelectedIngredientIds.value = expiringIds
    }

    fun selectAllRecipeIngredients() {
        _hasUserModifiedRecipeSelection.value = true
        _manualSelectedIngredientIds.value = uiState.value.allActiveItems.map { it.id }.toSet()
    }

    fun setCookTimeFilter(filter: CookTimeFilter) {
        _cookTimeFilter.value = filter
    }

    fun setMealTypeFilter(filter: MealTypeFilter) {
        _mealTypeFilter.value = filter
    }

    fun generateAiRecipes() {
        val allActive = uiState.value.allActiveItems
        val resolvedIds = if (_hasUserModifiedRecipeSelection.value) {
            _manualSelectedIngredientIds.value
        } else {
            val fromState = recipeTabState.value.selectedIngredientIds
            if (fromState.isNotEmpty()) {
                fromState
            } else {
                val expiring = allActive.filter { DateUtils.daysUntil(it.expiryDate) <= 3 }.map { it.id }.toSet()
                if (expiring.isNotEmpty()) expiring else allActive.take(3).map { it.id }.toSet()
            }
        }
        val selectedItems = allActive.filter { it.id in resolvedIds }
        if (selectedItems.isEmpty()) {
            _snackbarEvents.tryEmit(
                SnackbarEvent(
                    message = "Hãy tick chọn ít nhất 1 nguyên liệu trong tủ đồ nhé!",
                    type = ToastType.ERROR
                )
            )
            return
        }

        val cookTime = _cookTimeFilter.value
        val mealType = _mealTypeFilter.value

        recipeGenerationJob?.cancel()
        recipeGenerationJob = viewModelScope.launch {
            _recipeGenerationState.value = RecipeGenerationState.Loading
            try {
                val recipes = recipeSuggestionService.suggestRecipes(
                    selectedItems = selectedItems,
                    cookTime = cookTime,
                    mealType = mealType
                )
                if (recipes.isEmpty()) {
                    val msg = "PantryFlow chưa tìm được món phù hợp. Hãy thử chọn thêm nguyên liệu hoặc đổi bộ lọc."
                    _recipeGenerationState.value = RecipeGenerationState.Error(msg)
                    _snackbarEvents.tryEmit(SnackbarEvent(msg, ToastType.ERROR))
                } else {
                    _recipeGenerationState.value = RecipeGenerationState.Success(
                        recipes = recipes,
                        isSampleData = false
                    )
                    _snackbarEvents.tryEmit(
                        SnackbarEvent(
                            message = "PantryFlow đã gợi ý ${recipes.size} món ngon cho bạn!",
                            type = ToastType.SUCCESS
                        )
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Recipe generation timed out", e)
                val msg = "PantryFlow mất hơi nhiều thời gian để lên thực đơn. Bạn hãy thử bấm lại hoặc dùng \"Xem gợi ý mẫu\" nhé."
                _recipeGenerationState.value = RecipeGenerationState.Error(msg)
                _snackbarEvents.tryEmit(SnackbarEvent("Quá thời gian chờ gợi ý món ăn. Vui lòng thử lại.", ToastType.ERROR))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Recipe generation failed: ${e.javaClass.simpleName}: ${e.message}", e)
                val msg = mapRecipeErrorMessage(e)
                _recipeGenerationState.value = RecipeGenerationState.Error(msg)
                _snackbarEvents.tryEmit(SnackbarEvent(msg, ToastType.ERROR))
            }
        }
    }

    private fun mapRecipeErrorMessage(e: Throwable): String {
        var cur: Throwable? = e
        while (cur != null) {
            if (cur is UnknownHostException || cur is ConnectException || cur is NoRouteToHostException) {
                return "Không có kết nối mạng. Vui lòng kiểm tra Wi-Fi/4G và thử lại."
            }
            if (cur is JSONException) {
                return "Dữ liệu thực đơn chưa đầy đủ. Vui lòng bấm \"Thử lại\" để PantryFlow gợi ý lại nhé."
            }
            val lower = cur.message?.lowercase(Locale.ROOT).orEmpty()
            if (cur is IOException && (lower.contains("unable to resolve host") || lower.contains("network"))) {
                return "Không có kết nối mạng. Vui lòng kiểm tra Wi-Fi/4G và thử lại."
            }
            if (lower.contains("429") || lower.contains("quota") || lower.contains("resource_exhausted") || lower.contains("overloaded") || lower.contains("503")) {
                return "Hệ thống PantryFlow đang hơi bận. Vui lòng đợi vài giây rồi bấm \"Thử lại\" nhé."
            }
            cur = cur.cause
        }
        return "PantryFlow tạm thời chưa thể tạo gợi ý lúc này. Vui lòng thử lại hoặc bấm \"Xem gợi ý mẫu\"."
    }

    fun loadSampleRecipes() {
        recipeGenerationJob?.cancel()
        val sampleList = RecipeSuggestionService.createSampleSuggestions(
            allActiveItems = uiState.value.allActiveItems,
            mealType = _mealTypeFilter.value
        )
        _recipeGenerationState.value = RecipeGenerationState.Success(
            recipes = sampleList,
            isSampleData = true
        )
        _snackbarEvents.tryEmit(
            SnackbarEvent("Đã tải ${sampleList.size} gợi ý món mẫu theo tủ đồ của bạn", ToastType.INFO)
        )
    }

    fun applyCookedRecipeDeductions(recipeTitle: String, deductions: List<CookedIngredientDeduction>) {
        if (deductions.isEmpty()) return
        viewModelScope.launch {
            var updatedCount = 0
            deductions.forEach { d ->
                if (d.markFullyUsed) {
                    repository.updateStatus(d.pantryItem.id, ItemStatus.USED)
                    updatedCount += 1
                } else if (d.usedAmount > 0.0) {
                    val remaining = UnitConverter.calculateRemaining(
                        currentQuantity = d.pantryItem.quantity,
                        itemUnit = d.pantryItem.unit,
                        usedAmount = d.usedAmount,
                        usedUnit = d.usedUnit
                    )
                    if (remaining <= 0.0) {
                        repository.updateStatus(d.pantryItem.id, ItemStatus.USED)
                    } else {
                        repository.updateQuantity(d.pantryItem.id, remaining)
                    }
                    updatedCount += 1
                }
            }
            if (updatedCount > 0) {
                _snackbarEvents.tryEmit(
                    SnackbarEvent("Đã trừ kho $updatedCount nguyên liệu cho món \"$recipeTitle\"!")
                )
            } else {
                _snackbarEvents.tryEmit(
                    SnackbarEvent("Vui lòng nhập số lượng lớn hơn 0 để trừ kho.", ToastType.ERROR)
                )
            }
        }
    }

    // --- Receipt Scanning Flow ---

    fun analyzeReceiptBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _receiptScanState.value = ReceiptScanUiState.Analyzing
            try {
                val parsedItems = receiptScannerService.analyzeReceiptBitmap(bitmap)
                if (parsedItems.isEmpty()) {
                    val msg = "PantryFlow chưa tìm thấy thực phẩm nào trên hóa đơn này. Hãy chụp ảnh rõ nét hơn hoặc thử bằng hóa đơn mẫu."
                    _receiptScanState.value = ReceiptScanUiState.Error(msg)
                    _snackbarEvents.tryEmit(SnackbarEvent(msg, ToastType.ERROR))
                } else {
                    _receiptScanState.value = ReceiptScanUiState.ReviewTable(
                        items = parsedItems,
                        isSampleData = false
                    )
                    _snackbarEvents.tryEmit(
                        SnackbarEvent(
                            message = "PantryFlow đã nhận diện ${parsedItems.size} thực phẩm từ hóa đơn!",
                            type = ToastType.SUCCESS
                        )
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Receipt analysis timed out", e)
                val msg = "Quá thời gian xử lý. Vui lòng kiểm tra kết nối mạng và thử lại."
                _receiptScanState.value = ReceiptScanUiState.Error(msg)
                _snackbarEvents.tryEmit(SnackbarEvent("Lỗi kết nối mạng khi đọc hóa đơn.", ToastType.ERROR))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to analyze receipt", e)
                val msg = sanitizeUserFacingError(
                    raw = e.localizedMessage,
                    fallback = "PantryFlow không thể kết nối để đọc hóa đơn. Vui lòng kiểm tra mạng và thử lại."
                )
                _receiptScanState.value = ReceiptScanUiState.Error(msg)
                _snackbarEvents.tryEmit(SnackbarEvent(msg, ToastType.ERROR))
            }
        }
    }

    fun loadSampleReceipt() {
        _receiptScanState.value = ReceiptScanUiState.ReviewTable(
            items = ReceiptScannerService.createSampleReceiptDrafts(),
            isSampleData = true
        )
        _snackbarEvents.tryEmit(
            SnackbarEvent("Đã tải hóa đơn mẫu (5 thực phẩm) để kiểm tra bảng", ToastType.INFO)
        )
    }

    private fun sanitizeUserFacingError(raw: String?, fallback: String): String {
        if (raw.isNullOrBlank()) return fallback
        val lower = raw.lowercase()
        val containsTechnicalOrBrandLeak = lower.contains("gemini") ||
            lower.contains("firebase") ||
            lower.contains("google") ||
            lower.contains("vertex") ||
            lower.contains("exception") ||
            lower.contains("http") ||
            lower.contains("quota") ||
            lower.contains("app check") ||
            Regex("\\bai\\b").containsMatchIn(lower)
        return if (containsTechnicalOrBrandLeak) fallback else raw
    }

    fun resetReceiptScanToCapture() {
        _receiptScanState.value = ReceiptScanUiState.CaptureStep
    }

    fun updateReceiptDraftRow(updated: ReceiptDraftItem) {
        val current = _receiptScanState.value as? ReceiptScanUiState.ReviewTable ?: return
        val newList = current.items.map { row ->
            if (row.draftId == updated.draftId) updated else row
        }
        _receiptScanState.value = current.copy(items = newList)
    }

    fun deleteReceiptDraftRow(draftId: String) {
        val current = _receiptScanState.value as? ReceiptScanUiState.ReviewTable ?: return
        _receiptScanState.value = current.copy(
            items = current.items.filterNot { it.draftId == draftId }
        )
    }

    fun addEmptyReceiptDraftRow() {
        val current = _receiptScanState.value as? ReceiptScanUiState.ReviewTable ?: return
        val newRow = ReceiptDraftItem(
            name = "",
            quantityText = "1",
            unit = "g",
            storageLocation = StorageLocation.FRIDGE,
            expiryDate = DateUtils.plusDaysIso(5),
            isExpiryEstimated = true
        )
        _receiptScanState.value = current.copy(items = current.items + newRow)
    }

    fun saveAllReceiptDrafts() {
        val current = _receiptScanState.value as? ReceiptScanUiState.ReviewTable ?: return
        val today = DateUtils.todayIso()
        val pantryItems = current.items
            .filter { it.name.isNotBlank() }
            .map { draft ->
                val qty = draft.quantityText.replace(",", ".").toDoubleOrNull()
                    ?.takeIf { it > 0.0 } ?: 1.0
                PantryItem(
                    id = UUID.randomUUID().toString(),
                    name = draft.name.trim(),
                    quantity = qty,
                    unit = draft.unit.trim().ifEmpty { "cái" },
                    storageLocation = draft.storageLocation,
                    purchaseDate = today,
                    expiryDate = draft.expiryDate,
                    isExpiryEstimated = draft.isExpiryEstimated,
                    status = ItemStatus.ACTIVE
                )
            }

        if (pantryItems.isEmpty()) return

        viewModelScope.launch {
            repository.saveItems(pantryItems)
            _receiptScanState.value = ReceiptScanUiState.CaptureStep
            _selectedTab.value = PantryTab.PANTRY
            _snackbarEvents.tryEmit(
                SnackbarEvent("Đã lưu ${pantryItems.size} thực phẩm từ hóa đơn vào tủ đồ!")
            )
        }
    }

    // --- Dashboard & Settings Actions ---

    fun selectLocation(location: StorageLocation?) {
        _selectedLocation.value = location
    }

    fun toggleUrgencyFilter(urgency: ExpiryUrgency) {
        _selectedUrgencyFilter.value = if (_selectedUrgencyFilter.value == urgency) null else urgency
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun savePantryItem(
        id: String?,
        name: String,
        quantity: Double,
        unit: String,
        storageLocation: StorageLocation,
        purchaseDate: String,
        expiryDate: String,
        isExpiryEstimated: Boolean,
        keepAdding: Boolean = false
    ) {
        viewModelScope.launch {
            val isEdit = id != null
            val item = PantryItem(
                id = id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                quantity = quantity,
                unit = unit.trim().ifEmpty { "cái" },
                storageLocation = storageLocation,
                purchaseDate = purchaseDate,
                expiryDate = expiryDate,
                isExpiryEstimated = isExpiryEstimated,
                status = ItemStatus.ACTIVE
            )
            if (isEdit) {
                repository.updateItem(item)
                _snackbarEvents.tryEmit(SnackbarEvent("Đã cập nhật \"${item.name}\""))
            } else {
                repository.saveItem(item)
                val message = if (keepAdding) {
                    "Đã lưu \"${item.name}\" • Nhập món tiếp theo"
                } else {
                    "Đã thêm \"${item.name}\" vào ${storageLocation.label}"
                }
                _snackbarEvents.tryEmit(SnackbarEvent(message))
            }
            if (!keepAdding) {
                _editingItem.value = null
                _selectedTab.value = PantryTab.PANTRY
            }
        }
    }

    fun markItemStatus(item: PantryItem, newStatus: ItemStatus) {
        viewModelScope.launch {
            val previous = item.status
            repository.updateStatus(item.id, newStatus)
            val (msg, toastType) = when (newStatus) {
                ItemStatus.USED -> "Đã giải cứu & dùng hết \"${item.name}\"!" to ToastType.SUCCESS
                ItemStatus.DISCARDED -> "Đã bỏ \"${item.name}\" khỏi tủ" to ToastType.INFO
                ItemStatus.ACTIVE -> "Đã khôi phục \"${item.name}\" vào tủ" to ToastType.SUCCESS
            }
            _snackbarEvents.tryEmit(
                SnackbarEvent(
                    message = msg,
                    type = toastType,
                    undoItemId = item.id,
                    undoPreviousStatus = previous
                )
            )
        }
    }

    fun undoStatusChange(itemId: String, previousStatus: ItemStatus) {
        viewModelScope.launch {
            repository.updateStatus(itemId, previousStatus)
        }
    }

    fun consumePartialQuantity(item: PantryItem, usedAmount: Double, usedUnit: String) {
        viewModelScope.launch {
            val remaining = UnitConverter.calculateRemaining(
                currentQuantity = item.quantity,
                itemUnit = item.unit,
                usedAmount = usedAmount,
                usedUnit = usedUnit
            )
            if (remaining <= 0.0) {
                markItemStatus(item, ItemStatus.USED)
            } else {
                repository.updateQuantity(item.id, remaining)
                _snackbarEvents.tryEmit(
                    SnackbarEvent(
                        "Đã trừ ${UnitConverter.formatPreciseQuantity(usedAmount)} $usedUnit • Còn lại ${UnitConverter.formatPreciseQuantity(remaining)} ${item.unit}"
                    )
                )
            }
        }
    }

    fun quickUpdateLocation(item: PantryItem, newLocation: StorageLocation) {
        viewModelScope.launch {
            repository.updateItem(item.copy(storageLocation = newLocation))
            _snackbarEvents.tryEmit(
                SnackbarEvent("Đã chuyển \"${item.name}\" sang ${newLocation.label}")
            )
        }
    }

    fun quickUpdateExpiryDate(item: PantryItem, newExpiryDate: String) {
        viewModelScope.launch {
            repository.updateItem(
                item.copy(
                    expiryDate = newExpiryDate,
                    isExpiryEstimated = false
                )
            )
            _snackbarEvents.tryEmit(
                SnackbarEvent("Đã cập nhật HSD của \"${item.name}\" thành ${DateUtils.formatDisplayDate(newExpiryDate)}")
            )
        }
    }

    fun resetSampleData() {
        viewModelScope.launch {
            repository.resetToSampleData()
            _selectedLocation.value = null
            _selectedUrgencyFilter.value = null
            _searchQuery.value = ""
            _hasUserModifiedRecipeSelection.value = false
            _snackbarEvents.tryEmit(SnackbarEvent("Đã khôi phục 6 thực phẩm mẫu ban đầu"))
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _snackbarEvents.tryEmit(SnackbarEvent("Đã xóa lịch sử thực phẩm đã dùng/bỏ"))
        }
    }

    fun setReminderDaysBefore(days: Int) {
        viewModelScope.launch {
            repository.setReminderDaysBefore(days)
            _snackbarEvents.tryEmit(
                SnackbarEvent("Đã đặt nhắc nhở trước $days ngày")
            )
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.setReminderTime(hour, minute)
            val timeText = String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
            _snackbarEvents.tryEmit(
                SnackbarEvent("Đã lưu khung giờ nhắc nhở lúc $timeText")
            )
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotificationsEnabled(enabled)
        }
    }

    fun notifyTestNotificationSent(sent: Boolean) {
        val event = if (sent) {
            SnackbarEvent("Đã gửi thông báo nhắc nhở lên thanh trạng thái!", ToastType.SUCCESS)
        } else {
            SnackbarEvent("Chưa thể gửi thông báo — vui lòng cấp quyền Thông báo.", ToastType.ERROR)
        }
        _snackbarEvents.tryEmit(event)
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDarkModeEnabled(enabled)
        }
    }

    class Factory(
        private val repository: PantryRepository,
        private val receiptScannerService: ReceiptScannerService,
        private val recipeSuggestionService: RecipeSuggestionService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PantryViewModel(
                repository,
                receiptScannerService,
                recipeSuggestionService
            ) as T
        }
    }
}
