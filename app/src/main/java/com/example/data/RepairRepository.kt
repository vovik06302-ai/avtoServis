package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RepairRepository(private val dao: RepairDao) {

    companion object {
        val DEFAULT_REPAIR_TYPES = listOf(
            "замена",
            "ремонт",
            "диагностика",
            "покраска",
            "сварка",
            "регулировка"
        )
    }

    val allRecords: Flow<List<RepairRecord>> = dao.getAllRecords()

    val allRepairTypes: Flow<List<String>> = dao.getCustomRepairTypes().map { customList ->
        val merged = LinkedHashSet<String>()
        DEFAULT_REPAIR_TYPES.forEach { merged.add(it) }
        customList.forEach { item ->
            val trimmed = item.name.trim()
            if (trimmed.isNotEmpty() && DEFAULT_REPAIR_TYPES.none { it.equals(trimmed, ignoreCase = true) }) {
                merged.add(trimmed)
            }
        }
        merged.toList()
    }

    suspend fun insertRecord(record: RepairRecord): Long {
        ensureRepairTypeSaved(record.repairType)
        return dao.insertRecord(record)
    }

    suspend fun updateRecord(record: RepairRecord) {
        ensureRepairTypeSaved(record.repairType)
        dao.updateRecord(record)
    }

    suspend fun deleteRecordById(id: Long) {
        dao.deleteRecordById(id)
    }

    suspend fun addCustomRepairType(typeName: String) {
        ensureRepairTypeSaved(typeName)
    }

    private suspend fun ensureRepairTypeSaved(typeName: String) {
        val clean = typeName.trim()
        if (clean.isNotEmpty() && DEFAULT_REPAIR_TYPES.none { it.equals(clean, ignoreCase = true) }) {
            dao.insertCustomRepairType(CustomRepairType(name = clean))
        }
    }
}
