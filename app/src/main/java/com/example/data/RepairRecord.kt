package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repair_records")
data class RepairRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val partNumber: String,
    val repairType: String,
    val dateTimeStr: String,
    val photoPath: String?,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_repair_types")
data class CustomRepairType(
    @PrimaryKey
    val name: String,
    val addedAtMillis: Long = System.currentTimeMillis()
)
