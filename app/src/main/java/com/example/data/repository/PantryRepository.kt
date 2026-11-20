package com.example.data.repository

import com.example.data.local.PantryDao
import com.example.data.local.PantrySettings
import com.example.data.local.UserPreferences
import com.example.data.model.ItemStatus
import com.example.data.model.PantryItem
import com.example.data.model.StorageLocation
import com.example.data.util.DateUtils
import kotlinx.coroutines.flow.Flow

class PantryRepository(
    private val dao: PantryDao,
    private val preferences: UserPreferences
) {
    val activeItems: Flow<List<PantryItem>> = dao.observeActiveItems()
    val historyItems: Flow<List<PantryItem>> = dao.observeHistoryItems()
    val settings: Flow<PantrySettings> = preferences.settingsFlow

    suspend fun seedInitialSampleDataIfNeeded() {
        val alreadySeeded = preferences.hasSeededInitialData()
        val currentCount = dao.countAll()
        if (!alreadySeeded && currentCount == 0) {
            dao.insertItems(createDefaultSampleItems())
            preferences.markInitialDataSeeded()
        }
    }

    suspend fun resetToSampleData() {
        dao.clearAll()
        dao.insertItems(createDefaultSampleItems())
        preferences.markInitialDataSeeded()
    }

    suspend fun saveItem(item: PantryItem) {
        dao.insertItem(item)
    }

    suspend fun saveItems(items: List<PantryItem>) {
        dao.insertItems(items)
    }

    suspend fun updateItem(item: PantryItem) {
        dao.updateItem(item)
    }

    suspend fun updateStatus(id: String, status: ItemStatus) {
        dao.updateStatus(id, status)
    }

    suspend fun updateQuantity(id: String, newQuantity: Double) {
        if (newQuantity <= 0.0) {
            dao.updateStatus(id, ItemStatus.USED)
        } else {
            dao.updateQuantity(id, newQuantity)
        }
    }

    suspend fun deleteItem(id: String) {
        dao.deleteById(id)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun setWarningDaysThreshold(days: Int) {
        preferences.setWarningDaysThreshold(days)
    }

    suspend fun setReminderDaysBefore(days: Int) {
        preferences.setReminderDaysBefore(days)
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        preferences.setReminderTime(hour, minute)
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        preferences.setNotificationsEnabled(enabled)
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        preferences.setDarkModeEnabled(enabled)
    }

    companion object {
        fun createDefaultSampleItems(): List<PantryItem> {
            return listOf(
                PantryItem(
                    id = "sample-uc-ga",
                    name = "Ức gà phi lê",
                    quantity = 500.0,
                    unit = "g",
                    storageLocation = StorageLocation.FRIDGE,
                    purchaseDate = DateUtils.plusDaysIso(-2),
                    expiryDate = DateUtils.todayIso(),
                    isExpiryEstimated = true,
                    status = ItemStatus.ACTIVE
                ),
                PantryItem(
                    id = "sample-cai-ngot",
                    name = "Rau cải ngọt",
                    quantity = 1.0,
                    unit = "bó",
                    storageLocation = StorageLocation.FRIDGE,
                    purchaseDate = DateUtils.plusDaysIso(-1),
                    expiryDate = DateUtils.plusDaysIso(2),
                    isExpiryEstimated = true,
                    status = ItemStatus.ACTIVE
                ),
                PantryItem(
                    id = "sample-sua-tuoi",
                    name = "Sữa tươi không đường",
                    quantity = 1.0,
                    unit = "hộp",
                    storageLocation = StorageLocation.FRIDGE,
                    purchaseDate = DateUtils.plusDaysIso(-4),
                    expiryDate = DateUtils.plusDaysIso(3),
                    isExpiryEstimated = false,
                    status = ItemStatus.ACTIVE
                ),
                PantryItem(
                    id = "sample-trung-ga",
                    name = "Trứng gà ta",
                    quantity = 10.0,
                    unit = "quả",
                    storageLocation = StorageLocation.FRIDGE,
                    purchaseDate = DateUtils.plusDaysIso(-2),
                    expiryDate = DateUtils.plusDaysIso(10),
                    isExpiryEstimated = true,
                    status = ItemStatus.ACTIVE
                ),
                PantryItem(
                    id = "sample-tom-su",
                    name = "Tôm sú đông lạnh",
                    quantity = 400.0,
                    unit = "g",
                    storageLocation = StorageLocation.FREEZER,
                    purchaseDate = DateUtils.plusDaysIso(-5),
                    expiryDate = DateUtils.plusDaysIso(25),
                    isExpiryEstimated = false,
                    status = ItemStatus.ACTIVE
                ),
                PantryItem(
                    id = "sample-gao-st25",
                    name = "Gạo ST25",
                    quantity = 2.0,
                    unit = "kg",
                    storageLocation = StorageLocation.PANTRY,
                    purchaseDate = DateUtils.plusDaysIso(-10),
                    expiryDate = DateUtils.plusDaysIso(90),
                    isExpiryEstimated = true,
                    status = ItemStatus.ACTIVE
                )
            )
        }
    }
}
