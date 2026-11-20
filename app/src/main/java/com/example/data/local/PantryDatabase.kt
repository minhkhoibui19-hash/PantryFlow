package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.PantryItem

@Database(
    entities = [PantryItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(PantryConverters::class)
abstract class PantryDatabase : RoomDatabase() {
    abstract fun pantryDao(): PantryDao

    companion object {
        @Volatile
        private var INSTANCE: PantryDatabase? = null

        fun getInstance(context: Context): PantryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PantryDatabase::class.java,
                    "pantryflow_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
