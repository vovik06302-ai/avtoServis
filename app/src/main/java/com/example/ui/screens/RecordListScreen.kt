package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.RepairRecord
import com.example.data.RepairStatus
import com.example.ui.DateFilterTarget
import com.example.ui.DateRangeFilter
import com.example.ui.components.LocalPhotoThumbnail
import com.example.ui.components.RepairTypeBadge
import com.example.ui.components.StatusBadgeSelector
import com.example.ui.components.UpdateAvailableBanner
import com.example.ui.components.getStatusAccentColor
import com.example.ui.components.getStatusBarColor
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.update.APP_VERSION
import com.example.update.ReleaseInfo
import com.example.util.ImageStorageHelper
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordListScreen(
    allRecords: List<RepairRecord>,
    records: List<RepairRecord>,
    photosByRecordId: Map<Long, List<String>>,
    selectedStatusFilter: String?,
    onSelectStatusFilter: (String?) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    dateRangeFilter: DateRangeFilter,
    onApplyDateRangeFilter: (DateFilterTarget, Long?, Long?) -> Unit,
    onClearDateRangeFilter: () -> Unit,
    onNewRecordClick: () -> Unit,
    onRecordClick: (RepairRecord) -> Unit,
    onStatusChangeClick: (RepairRecord, String) -> Unit,
    onExportCsvClick: () -> Unit,
    onAboutClick: () -> Unit,
    availableRelease: ReleaseInfo?,
    isDownloadingApk: Boolean,
    downloadProgress: Float,
    onDownloadUpdateClick: (ReleaseInfo) -> Unit,
    onDismissUpdateBanner: () -> Unit
) {
    val totalCount = allRecords.size
    val receptionCount = allRecords.count { RepairStatus.fromLabel(it.status) == RepairStatus.RECEPTION }
    val inProgressCount = allRecords.count { RepairStatus.fromLabel(it.status) == RepairStatus.IN_PROGRESS }
    val readyCount = allRecords.count { RepairStatus.fromLabel(it.status) == RepairStatus.READY }

    var showCalendarPickerDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "АвтоСервис Учёт",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Всего записей: $totalCount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onExportCsvClick,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("export_csv_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Экспорт в CSV",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Экспорт",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onAboutClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("about_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "О приложении и настройки",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewRecordClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .height(64.dp)
                    .testTag("new_record_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Новая запись",
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Новая запись",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Версия $APP_VERSION",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier
                            .clickable { onAboutClick() }
                            .padding(vertical = 2.dp, horizontal = 8.dp)
                            .testTag("main_version_footer")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (availableRelease != null) {
                Spacer(modifier = Modifier.height(8.dp))
                UpdateAvailableBanner(
                    release = availableRelease,
                    isDownloading = isDownloadingApk,
                    downloadProgress = downloadProgress,
                    onDownloadAndInstall = { onDownloadUpdateClick(availableRelease) },
                    onDismiss = onDismissUpdateBanner
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Фильтр-вкладки по статусам: «Все», «Приёмка», «В работе», «Готова» с количеством записей
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("status_filter_tabs"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusFilterTabChip(
                    title = "Все",
                    count = totalCount,
                    selected = selectedStatusFilter == null,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = { onSelectStatusFilter(null) },
                    testTag = "filter_tab_all"
                )
                StatusFilterTabChip(
                    title = RepairStatus.RECEPTION.label,
                    count = receptionCount,
                    selected = selectedStatusFilter == RepairStatus.RECEPTION.label,
                    accentColor = getStatusBarColor(RepairStatus.RECEPTION.label),
                    onClick = { onSelectStatusFilter(RepairStatus.RECEPTION.label) },
                    testTag = "filter_tab_reception"
                )
                StatusFilterTabChip(
                    title = RepairStatus.IN_PROGRESS.label,
                    count = inProgressCount,
                    selected = selectedStatusFilter == RepairStatus.IN_PROGRESS.label,
                    accentColor = getStatusBarColor(RepairStatus.IN_PROGRESS.label),
                    onClick = { onSelectStatusFilter(RepairStatus.IN_PROGRESS.label) },
                    testTag = "filter_tab_in_progress"
                )
                StatusFilterTabChip(
                    title = RepairStatus.READY.label,
                    count = readyCount,
                    selected = selectedStatusFilter == RepairStatus.READY.label,
                    accentColor = getStatusBarColor(RepairStatus.READY.label),
                    onClick = { onSelectStatusFilter(RepairStatus.READY.label) },
                    testTag = "filter_tab_ready"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Поле поиска + кнопка фильтра по дате через CalendarPicker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_input"),
                    placeholder = {
                        Text(
                            text = "Деталь, ремонт, Ф.И.О. или телефон…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Очистить поиск"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Кнопка открытия выбора диапазона дат (CalendarPicker)
                Surface(
                    onClick = { showCalendarPickerDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    color = if (dateRangeFilter.isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (dateRangeFilter.isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("date_range_filter_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Фильтр по дате",
                            tint = if (dateRangeFilter.isActive) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Полоса активного фильтра по диапазону дат
            if (dateRangeFilter.isActive) {
                val startText = dateRangeFilter.startUtcMillis?.let {
                    ImageStorageHelper.formatUtcMillisToRussianDate(it)
                } ?: ""
                val endText = dateRangeFilter.endUtcMillis?.let {
                    ImageStorageHelper.formatUtcMillisToRussianDate(it)
                } ?: startText
                val rangeLabel = if (startText == endText) startText else "$startText – $endText"

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCalendarPickerDialog = true }
                        .testTag("active_date_filter_chip")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${dateRangeFilter.target.shortLabel}: $rangeLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(
                            onClick = onClearDateRangeFilter,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("clear_date_filter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Сбросить фильтр по дате",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Handyman,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        val hasAnyFilter = searchQuery.isNotBlank() ||
                            selectedStatusFilter != null ||
                            dateRangeFilter.isActive
                        Text(
                            text = if (!hasAnyFilter) {
                                "Записей пока нет"
                            } else {
                                "Ничего не найдено"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (!hasAnyFilter) {
                                "Сфотографируйте деталь, укажите клиента и вид ремонта — нажмите кнопку «Новая запись»."
                            } else {
                                "Нет записей, подходящих под выбранный фильтр статуса, даты или поисковый запрос."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("records_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(
                        items = records,
                        key = { it.id }
                    ) { record ->
                        val recordPhotos = photosByRecordId[record.id]
                            ?: listOfNotNull(record.photoPath?.takeIf { it.isNotBlank() })
                        RepairRecordCard(
                            record = record,
                            photoPaths = recordPhotos,
                            onClick = { onRecordClick(record) },
                            onStatusChange = { newStatus ->
                                onStatusChangeClick(record, newStatus)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCalendarPickerDialog) {
        CalendarRangeFilterDialog(
            currentFilter = dateRangeFilter,
            onApply = { target, startMillis, endMillis ->
                onApplyDateRangeFilter(target, startMillis, endMillis)
                showCalendarPickerDialog = false
            },
            onReset = {
                onClearDateRangeFilter()
                showCalendarPickerDialog = false
            },
            onDismiss = { showCalendarPickerDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarRangeFilterDialog(
    currentFilter: DateRangeFilter,
    onApply: (DateFilterTarget, Long?, Long?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTarget by remember(currentFilter.target) {
        mutableStateOf(currentFilter.target)
    }

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = currentFilter.startUtcMillis,
        initialSelectedEndDateMillis = currentFilter.endUtcMillis
    )

    fun setQuickPresetDays(daysBack: Int) {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endMillis = utcCal.timeInMillis
        utcCal.add(Calendar.DAY_OF_YEAR, -daysBack)
        val startMillis = utcCal.timeInMillis
        dateRangePickerState.setSelection(startMillis, endMillis)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .testTag("calendar_range_picker_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Шапка диалога с выбором поля фильтрации (создание или смена статуса)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Фильтр по диапазону дат",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть календарь"
                            )
                        }
                    }

                    // Переключатель: по дате создания записи или по дате изменения статуса
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateFilterTarget.entries.forEach { targetOption ->
                            val isSelected = selectedTarget == targetOption
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTarget = targetOption },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("date_target_${targetOption.name.lowercase()}"),
                                label = {
                                    Text(
                                        text = targetOption.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Быстрые кнопки выбора диапазона
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { setQuickPresetDays(0) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("preset_today_button")
                        ) {
                            Text("Сегодня", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { setQuickPresetDays(6) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("preset_7days_button")
                        ) {
                            Text("За 7 дней", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { setQuickPresetDays(29) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("preset_30days_button")
                        ) {
                            Text("За 30 дней", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Календарь выбора диапазона дат (Material 3 DateRangePicker)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    DateRangePicker(
                        state = dateRangePickerState,
                        title = null,
                        headline = {
                            val start = dateRangePickerState.selectedStartDateMillis?.let {
                                ImageStorageHelper.formatUtcMillisToRussianDate(it)
                            } ?: "Начало"
                            val end = dateRangePickerState.selectedEndDateMillis?.let {
                                ImageStorageHelper.formatUtcMillisToRussianDate(it)
                            } ?: "Конец"
                            Text(
                                text = "$start — $end",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        },
                        showModeToggle = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("calendar_date_range_picker")
                    )
                }

                // Нижняя панель кнопок «Сбросить» и «Применить»
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onReset,
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("reset_date_range_button")
                        ) {
                            Text(
                                text = "Сбросить",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Button(
                            onClick = {
                                val start = dateRangePickerState.selectedStartDateMillis
                                val end = dateRangePickerState.selectedEndDateMillis ?: start
                                onApply(selectedTarget, start, end)
                            },
                            enabled = dateRangePickerState.selectedStartDateMillis != null,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("apply_date_range_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Применить диапазон",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusFilterTabChip(
    title: String,
    count: Int,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .height(48.dp)
            .testTag(testTag),
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!selected && title != "Все") {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }
                Text(
                    text = "$title ($count)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor,
            selectedLabelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = accentColor.copy(alpha = 0.55f),
            selectedBorderColor = accentColor,
            borderWidth = 1.5.dp
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun RepairRecordCard(
    record: RepairRecord,
    photoPaths: List<String>,
    onClick: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    val statusBarColor = getStatusBarColor(record.status)
    val statusAccentColor = getStatusAccentColor(record.status)
    val coverPhotoPath = photoPaths.firstOrNull() ?: record.photoPath
    val photoCount = photoPaths.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("record_card_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, statusBarColor.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Цветная вертикальная полоса статуса слева
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(statusBarColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusAccentColor.copy(alpha = 0.04f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Миниатюра первого фото (обложка) + значок «📷 N», если фото больше одного
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    LocalPhotoThumbnail(
                        photoPath = coverPhotoPath,
                        contentDescription = "Фото детали ${record.partNumber}",
                        modifier = Modifier.fillMaxSize()
                    )

                    if (photoCount > 1) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .testTag("record_photo_count_${record.id}")
                        ) {
                            Text(
                                text = "📷 $photoCount",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = record.partNumber,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        )

                        // Кнопка/бейдж смены статуса в один тап
                        StatusBadgeSelector(
                            currentStatus = record.status,
                            onStatusChange = onStatusChange
                        )
                    }

                    // Ф.И.О. клиента на карточке
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = record.clientName.ifBlank { "Клиент не указан" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RepairTypeBadge(repairType = record.repairType)

                        Text(
                            text = record.dateTimeStr,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
