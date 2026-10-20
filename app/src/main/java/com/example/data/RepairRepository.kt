package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RepairRepository(private val dao: RepairDao) {

    companion object {
        const val MAX_PHOTOS_PER_RECORD = 10

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

    /**
     * Карта recordId -> упорядоченный список путей к фото (по полю position).
     */
    val photosByRecordId: Flow<Map<Long, List<String>>> = dao.getAllPhotos().map { photoEntities ->
        photoEntities
            .groupBy { it.recordId }
            .mapValues { (_, list) ->
                list.sortedWith(compareBy<RecordPhoto> { it.position }.thenBy { it.id })
                    .map { it.filePath }
            }
    }

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

    suspend fun getPhotosForRecord(recordId: Long): List<String> {
        return dao.getPhotosForRecord(recordId)
            .sortedWith(compareBy<RecordPhoto> { it.position }.thenBy { it.id })
            .map { it.filePath }
    }

    suspend fun insertRecordWithPhotos(record: RepairRecord, photoPaths: List<String>): Long {
        ensureRepairTypeSaved(record.repairType)
        val cleanPhotos = photoPaths.filter { it.isNotBlank() }.take(MAX_PHOTOS_PER_RECORD)
        val coverPath = cleanPhotos.firstOrNull()
        val recordId = dao.insertRecord(record.copy(photoPath = coverPath))
        replacePhotosForRecord(recordId, cleanPhotos)
        return recordId
    }

    suspend fun updateRecordWithPhotos(record: RepairRecord, photoPaths: List<String>) {
        ensureRepairTypeSaved(record.repairType)
        val cleanPhotos = photoPaths.filter { it.isNotBlank() }.take(MAX_PHOTOS_PER_RECORD)
        val coverPath = cleanPhotos.firstOrNull()
        dao.updateRecord(record.copy(photoPath = coverPath))
        replacePhotosForRecord(record.id, cleanPhotos)
    }

    suspend fun replacePhotosForRecord(recordId: Long, orderedPaths: List<String>) {
        val cleanPhotos = orderedPaths.filter { it.isNotBlank() }.take(MAX_PHOTOS_PER_RECORD)
        dao.deletePhotosForRecord(recordId)
        if (cleanPhotos.isNotEmpty()) {
            val entities = cleanPhotos.mapIndexed { index, path ->
                RecordPhoto(
                    recordId = recordId,
                    filePath = path,
                    position = index
                )
            }
            dao.insertPhotos(entities)
        }
        dao.updateRecordCoverPhoto(recordId, cleanPhotos.firstOrNull())
    }

    suspend fun appendPhotosToRecord(recordId: Long, newPaths: List<String>): List<String> {
        val existing = getPhotosForRecord(recordId)
        val combined = (existing + newPaths.filter { it.isNotBlank() })
            .distinct()
            .take(MAX_PHOTOS_PER_RECORD)
        replacePhotosForRecord(recordId, combined)
        return combined
    }

    suspend fun updateRecordStatus(id: Long, status: String, statusChangedAt: String) {
        dao.updateRecordStatus(id = id, status = status, statusChangedAt = statusChangedAt)
    }

    suspend fun deleteRecordById(id: Long) {
        dao.deletePhotosForRecord(id)
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
