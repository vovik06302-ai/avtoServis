package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RepairStatus(val label: String) {
    RECEPTION("Приёмка"),
    IN_PROGRESS("В работе"),
    READY("Готова");

    companion object {
        val ALL_LABELS: List<String> = entries.map { it.label }

        fun fromLabel(value: String?): RepairStatus {
            return entries.find { it.label.equals(value?.trim(), ignoreCase = true) } ?: RECEPTION
        }
    }
}

@Entity(tableName = "repair_records")
data class RepairRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val partNumber: String,
    val repairType: String,
    val dateTimeStr: String,
    val photoPath: String?,
    val createdAtMillis: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "Приёмка")
    val status: String = RepairStatus.RECEPTION.label,
    @ColumnInfo(defaultValue = "")
    val clientName: String = "",
    @ColumnInfo(defaultValue = "")
    val phone: String = "",
    @ColumnInfo(defaultValue = "")
    val statusChangedAt: String = ""
)

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = RepairRecord::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recordId"])]
)
data class RecordPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val recordId: Long,
    val filePath: String,
    val position: Int
)

@Entity(tableName = "custom_repair_types")
data class CustomRepairType(
    @PrimaryKey
    val name: String,
    val addedAtMillis: Long = System.currentTimeMillis()
)
