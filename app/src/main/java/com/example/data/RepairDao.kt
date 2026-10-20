package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RepairDao {

    @Query("SELECT * FROM repair_records ORDER BY createdAtMillis DESC, id DESC")
    fun getAllRecords(): Flow<List<RepairRecord>>

    @Query("SELECT * FROM repair_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): RepairRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RepairRecord): Long

    @Update
    suspend fun updateRecord(record: RepairRecord)

    @Query("DELETE FROM repair_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("SELECT * FROM custom_repair_types ORDER BY addedAtMillis ASC")
    fun getCustomRepairTypes(): Flow<List<CustomRepairType>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustomRepairType(type: CustomRepairType)
}
