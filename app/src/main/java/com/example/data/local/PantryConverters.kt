package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.ItemStatus
import com.example.data.model.StorageLocation

class PantryConverters {
    @TypeConverter
    fun fromStorageLocation(value: StorageLocation): String = value.name

    @TypeConverter
    fun toStorageLocation(value: String): StorageLocation =
        StorageLocation.entries.firstOrNull { it.name == value } ?: StorageLocation.OTHER

    @TypeConverter
    fun fromItemStatus(value: ItemStatus): String = value.name

    @TypeConverter
    fun toItemStatus(value: String): ItemStatus =
        ItemStatus.entries.firstOrNull { it.name == value } ?: ItemStatus.ACTIVE
}
