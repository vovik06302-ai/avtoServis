package com.example.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.RepairRecord
import com.example.data.RepairRepository
import com.example.ui.theme.AppThemeMode
import com.example.update.APP_VERSION
import com.example.update.GITHUB_REPO
import com.example.update.ReleaseInfo
import com.example.update.UpdateCheckResult
import com.example.update.UpdateManager
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScreenState {
    data object RecordList : ScreenState
    data class RecordEditor(val editingRecord: RepairRecord? = null) : ScreenState
    data class RecordDetail(val recordId: Long) : ScreenState
    data object AboutAndSettings : ScreenState
}

class RepairViewModel(
    private val repository: RepairRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.RecordList)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _themeMode = MutableStateFlow(loadSavedThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _customGithubRepo = MutableStateFlow(
        prefs.getString(KEY_CUSTOM_REPO, GITHUB_REPO) ?: GITHUB_REPO
    )
    val customGithubRepo: StateFlow<String> = _customGithubRepo.asStateFlow()

    // Состояние проверки обновлений через GitHub API
    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _availableRelease = MutableStateFlow<ReleaseInfo?>(null)
    val availableRelease: StateFlow<ReleaseInfo?> = _availableRelease.asStateFlow()

    private val _updateMessage = MutableStateFlow<String?>(null)
    val updateMessage: StateFlow<String?> = _updateMessage.asStateFlow()

    private val _isDownloadingApk = MutableStateFlow(false)
    val isDownloadingApk: StateFlow<Boolean> = _isDownloadingApk.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val allRecords: StateFlow<List<RepairRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredRecords: StateFlow<List<RepairRecord>> = combine(
        allRecords,
        _searchQuery
    ) { records, query ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            records
        } else {
            records.filter { rec ->
                rec.partNumber.contains(trimmed, ignoreCase = true) ||
                    rec.repairType.contains(trimmed, ignoreCase = true) ||
                    rec.dateTimeStr.contains(trimmed, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val repairTypes: StateFlow<List<String>> = repository.allRepairTypes
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            RepairRepository.DEFAULT_REPAIR_TYPES
        )

    init {
        // Автоматическая проверка обновлений при запуске приложения
        checkForUpdates(isManualTrigger = false)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun navigateToNewRecord() {
        _screenState.value = ScreenState.RecordEditor(editingRecord = null)
    }

    fun navigateToEditRecord(record: RepairRecord) {
        _screenState.value = ScreenState.RecordEditor(editingRecord = record)
    }

    fun navigateToDetail(recordId: Long) {
        _screenState.value = ScreenState.RecordDetail(recordId = recordId)
    }

    fun navigateToAbout() {
        _screenState.value = ScreenState.AboutAndSettings
    }

    fun navigateBackToList() {
        _screenState.value = ScreenState.RecordList
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun updateCustomGithubRepo(repo: String) {
        val cleaned = repo.trim()
        _customGithubRepo.value = cleaned
        prefs.edit().putString(KEY_CUSTOM_REPO, cleaned).apply()
    }

    fun saveRecord(
        existingRecord: RepairRecord?,
        partNumber: String,
        repairType: String,
        dateTimeStr: String,
        photoPath: String?,
        onSaved: () -> Unit
    ) {
        val cleanPart = partNumber.trim()
        if (cleanPart.isEmpty()) {
            showSnackbar("Введите номер детали")
            return
        }
        val cleanRepair = repairType.trim().ifEmpty { "замена" }
        val cleanDate = dateTimeStr.trim().ifEmpty { ImageStorageHelper.formatCurrentDateTime() }

        viewModelScope.launch {
            if (existingRecord == null) {
                repository.insertRecord(
                    RepairRecord(
                        partNumber = cleanPart,
                        repairType = cleanRepair,
                        dateTimeStr = cleanDate,
                        photoPath = photoPath,
                        createdAtMillis = System.currentTimeMillis()
                    )
                )
                showSnackbar("Запись «$cleanPart» сохранена")
            } else {
                if (existingRecord.photoPath != null && existingRecord.photoPath != photoPath) {
                    ImageStorageHelper.deletePhotoFileIfExists(existingRecord.photoPath)
                }
                repository.updateRecord(
                    existingRecord.copy(
                        partNumber = cleanPart,
                        repairType = cleanRepair,
                        dateTimeStr = cleanDate,
                        photoPath = photoPath
                    )
                )
                showSnackbar("Запись «$cleanPart» обновлена")
            }
            _screenState.value = ScreenState.RecordList
            onSaved()
        }
    }

    fun addCustomRepairType(customType: String) {
        val cleaned = customType.trim()
        if (cleaned.isNotEmpty()) {
            viewModelScope.launch {
                repository.addCustomRepairType(cleaned)
                showSnackbar("Вид ремонта «$cleaned» добавлен в список")
            }
        }
    }

    fun deleteRecord(record: RepairRecord) {
        viewModelScope.launch {
            ImageStorageHelper.deletePhotoFileIfExists(record.photoPath)
            repository.deleteRecordById(record.id)
            showSnackbar("Запись «${record.partNumber}» удалена")
            _screenState.value = ScreenState.RecordList
        }
    }

    fun exportCsv(context: Context) {
        val list = allRecords.value
        if (list.isEmpty()) {
            showSnackbar("Нет записей для экспорта в CSV")
            return
        }
        viewModelScope.launch {
            val result = ImageStorageHelper.shareCsvFile(context, list)
            result.onFailure {
                showSnackbar("Не удалось выгрузить CSV: ${it.localizedMessage ?: "ошибка"}")
            }
        }
    }

    fun checkForUpdates(isManualTrigger: Boolean = true) {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            if (isManualTrigger) {
                _updateMessage.value = null
            }

            val repoToUse = _customGithubRepo.value.ifBlank { GITHUB_REPO }
            when (val result = UpdateManager.checkForUpdates(repoToUse)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _availableRelease.value = result.release
                    _updateMessage.value = "Доступно обновление: ${result.release.version}"
                }
                is UpdateCheckResult.UpToDate -> {
                    _availableRelease.value = null
                    _updateMessage.value = "У вас последняя версия (${result.currentVersion})"
                    if (isManualTrigger) {
                        showSnackbar("У вас последняя версия (${result.currentVersion})")
                    }
                }
                is UpdateCheckResult.Error -> {
                    _updateMessage.value = result.message
                    if (isManualTrigger) {
                        showSnackbar(result.message)
                    }
                }
            }
            _isCheckingUpdate.value = false
        }
    }

    fun dismissUpdateBanner() {
        _availableRelease.value = null
    }

    fun downloadAndInstallUpdate(context: Context, release: ReleaseInfo) {
        val url = release.apkDownloadUrl
        if (url.isNullOrBlank()) {
            showSnackbar("В релизе ${release.tagName} не найден прикреплённый файл .apk")
            return
        }
        if (_isDownloadingApk.value) return

        viewModelScope.launch {
            _isDownloadingApk.value = true
            _downloadProgress.value = 0.02f
            val res = UpdateManager.downloadAndInstallApk(
                context = context,
                apkUrl = url,
                apkFileName = release.apkName
            ) { progress ->
                _downloadProgress.value = progress
            }
            _isDownloadingApk.value = false
            res.onFailure { err ->
                showSnackbar("Ошибка при скачивании APK: ${err.localizedMessage ?: "неизвестная ошибка"}")
            }
        }
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun loadSavedThemeMode(): AppThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return try {
            AppThemeMode.valueOf(saved ?: AppThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_CUSTOM_REPO = "key_custom_github_repo"

        fun provideFactory(
            repository: RepairRepository,
            prefs: SharedPreferences
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RepairViewModel(repository, prefs) as T
            }
        }
    }
}
