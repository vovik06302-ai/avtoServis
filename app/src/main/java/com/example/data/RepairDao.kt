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

    @Query("UPDATE repair_records SET status = :status, statusChangedAt = :statusChangedAt WHERE id = :id")
    suspend fun updateRecordStatus(id: Long, status: String, statusChangedAt: String)

    @Query("UPDATE repair_records SET photoPath = :coverPhotoPath WHERE id = :id")
    suspend fun updateRecordCoverPhoto(id: Long, coverPhotoPath: String?)

    @Query("DELETE FROM repair_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("SELECT * FROM photos ORDER BY recordId ASC, position ASC, id ASC")
    fun getAllPhotos(): Flow<List<RecordPhoto>>

    @Query("SELECT * FROM photos WHERE recordId = :recordId ORDER BY position ASC, id ASC")
    suspend fun getPhotosForRecord(recordId: Long): List<RecordPhoto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<RecordPhoto>)

    @Query("DELETE FROM photos WHERE recordId = :recordId")
    suspend fun deletePhotosForRecord(recordId: Long)

    @Query("SELECT * FROM custom_repair_types ORDER BY addedAtMillis ASC")
    fun getCustomRepairTypes(): Flow<List<CustomRepairType>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustomRepairType(type: CustomRepairType)
}
