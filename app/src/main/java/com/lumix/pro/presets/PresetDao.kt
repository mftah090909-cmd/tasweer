package com.lumix.pro.presets

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM camera_presets ORDER BY createdMs DESC")
    fun getAll(): Flow<List<CameraPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: CameraPreset): Long

    @Delete
    suspend fun delete(preset: CameraPreset)
}
