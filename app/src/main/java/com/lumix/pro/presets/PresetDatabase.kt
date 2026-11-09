package com.lumix.pro.presets

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CameraPreset::class], version = 1, exportSchema = false)
abstract class PresetDatabase : RoomDatabase() {

    abstract fun presetDao(): PresetDao

    companion object {
        @Volatile
        private var INSTANCE: PresetDatabase? = null

        fun getInstance(context: Context): PresetDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PresetDatabase::class.java,
                    "lumix_presets.db"
                ).build().also { INSTANCE = it }
            }
    }
}
