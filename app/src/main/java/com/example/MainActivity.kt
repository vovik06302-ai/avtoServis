package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.RepairRepository
import com.example.ui.RepairViewModel
import com.example.ui.ScreenState
import com.example.ui.components.UpdateAvailableDialog
import com.example.ui.screens.AboutSettingsScreen
import com.example.ui.screens.RecordDetailScreen
import com.example.ui.screens.RecordEditorScreen
import com.example.ui.screens.RecordListScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = RepairRepository(database.repairDao())
        val prefs = applicationContext.getSharedPreferences("autoservice_prefs", Context.MODE_PRIVATE)

        setContent {
            val viewModel: RepairViewModel = viewModel(
                factory = RepairViewModel.provideFactory(repository, prefs)
            )
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = themeMode) {
                AutoServiceApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AutoServiceApp(viewModel: RepairViewModel) {
    val context = LocalContext.current
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val filteredRecords by viewModel.filteredRecords.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val repairTypes by viewModel.repairTypes.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val customGithubRepo by viewModel.customGithubRepo.collectAsStateWithLifecycle()

    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()
    val availableRelease by viewModel.availableRelease.collectAsStateWithLifecycle()
    val updateMessage by viewModel.updateMessage.collectAsStateWithLifecycle()
    val isDownloadingApk by viewModel.isDownloadingApk.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { rootPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(rootPadding)
        ) {
            when (val state = screenState) {
                is ScreenState.RecordList -> {
                    RecordListScreen(
                        records = filteredRecords,
                        totalRecordsCount = allRecords.size,
                        searchQuery = searchQuery,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onNewRecordClick = viewModel::navigateToNewRecord,
                        onRecordClick = { record -> viewModel.navigateToDetail(record.id) },
                        onExportCsvClick = { viewModel.exportCsv(context) },
                        onAboutClick = viewModel::navigateToAbout,
                        availableRelease = availableRelease,
                        isDownloadingApk = isDownloadingApk,
                        downloadProgress = downloadProgress,
                        onDownloadUpdateClick = { rel ->
                            viewModel.downloadAndInstallUpdate(context, rel)
                        },
                        onDismissUpdateBanner = viewModel::dismissUpdateBanner
                    )
                }

                is ScreenState.RecordEditor -> {
                    RecordEditorScreen(
                        editingRecord = state.editingRecord,
                        availableRepairTypes = repairTypes,
                        onAddCustomRepairType = viewModel::addCustomRepairType,
                        onSaveRecord = { partNumber, repairType, dateTimeStr, photoPath ->
                            viewModel.saveRecord(
                                existingRecord = state.editingRecord,
                                partNumber = partNumber,
                                repairType = repairType,
                                dateTimeStr = dateTimeStr,
                                photoPath = photoPath,
                                onSaved = {}
                            )
                        },
                        onNavigateBack = viewModel::navigateBackToList,
                        onMessage = viewModel::showSnackbar
                    )
                }

                is ScreenState.RecordDetail -> {
                    val selectedRecord = allRecords.find { it.id == state.recordId }
                    RecordDetailScreen(
                        record = selectedRecord,
                        onEditClick = { rec -> viewModel.navigateToEditRecord(rec) },
                        onDeleteConfirm = { rec -> viewModel.deleteRecord(rec) },
                        onNavigateBack = viewModel::navigateBackToList
                    )
                }

                is ScreenState.AboutAndSettings -> {
                    AboutSettingsScreen(
                        themeMode = themeMode,
                        onThemeModeChange = viewModel::setThemeMode,
                        customGithubRepo = customGithubRepo,
                        onGithubRepoChange = viewModel::updateCustomGithubRepo,
                        isCheckingUpdate = isCheckingUpdate,
                        updateMessage = updateMessage,
                        availableRelease = availableRelease,
                        isDownloadingApk = isDownloadingApk,
                        downloadProgress = downloadProgress,
                        onCheckUpdateClick = { viewModel.checkForUpdates(isManualTrigger = true) },
                        onDownloadUpdateClick = { rel ->
                            viewModel.downloadAndInstallUpdate(context, rel)
                        },
                        onDismissUpdateBanner = viewModel::dismissUpdateBanner,
                        onNavigateBack = viewModel::navigateBackToList
                    )
                }
            }

            // Модальный диалог обновления с анимированным ProgressBar при наличии новой версии
            val release = availableRelease
            if (release != null) {
                UpdateAvailableDialog(
                    release = release,
                    isDownloading = isDownloadingApk,
                    downloadProgress = downloadProgress,
                    onDownloadAndInstall = { viewModel.downloadAndInstallUpdate(context, release) },
                    onDismiss = viewModel::dismissUpdateBanner
                )
            }
        }
    }
}
