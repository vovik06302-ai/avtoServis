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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.data.RepairRecord
import com.example.data.RepairStatus
import com.example.ui.components.LocalPhotoThumbnail
import com.example.ui.components.RepairTypeBadge
import com.example.ui.components.StatusBadgeSelector
import com.example.ui.components.UpdateAvailableBanner
import com.example.ui.components.getStatusAccentColor
import com.example.ui.components.getStatusBarColor
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.update.APP_VERSION
import com.example.update.ReleaseInfo

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

            // Поле поиска по номеру детали, виду ремонта, Ф.И.О. клиента и телефону
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
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
                        Text(
                            text = if (searchQuery.isBlank() && selectedStatusFilter == null) {
                                "Записей пока нет"
                            } else {
                                "Ничего не найдено"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank() && selectedStatusFilter == null) {
                                "Сфотографируйте деталь, укажите клиента и вид ремонта — нажмите кнопку «Новая запись»."
                            } else {
                                "Нет записей, подходящих под выбранный фильтр или поисковый запрос."
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
