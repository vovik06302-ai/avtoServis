package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.RepairRecord
import com.example.data.RepairRepository
import com.example.ui.components.RepairTypeBadge
import com.example.ui.components.StatusBadgeSelector
import com.example.ui.components.getStatusBarColor
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.WorkshopGreen
import com.example.ui.theme.WorkshopRed
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    record: RepairRecord?,
    photoPaths: List<String>,
    onAddPhotos: (RepairRecord, List<String>) -> Unit,
    onStatusChange: (RepairRecord, String) -> Unit,
    onEditClick: (RepairRecord) -> Unit,
    onDeleteConfirm: (RepairRecord) -> Unit,
    onNavigateBack: () -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isFullScreenZoom by remember { mutableStateOf(false) }
    var fullScreenInitialPage by remember { mutableIntStateOf(0) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAddPhotoDialog by remember { mutableStateOf(false) }
    var isCompressingPhoto by remember { mutableStateOf(false) }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    val effectivePhotos = remember(photoPaths, record?.photoPath) {
        if (photoPaths.isNotEmpty()) {
            photoPaths
        } else {
            listOfNotNull(record?.photoPath?.takeIf { it.isNotBlank() })
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { effectivePhotos.size.coerceAtLeast(1) }
    )

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val capturedUri = tempCameraUri
        val currentRec = record
        if (success && capturedUri != null && currentRec != null) {
            coroutineScope.launch {
                isCompressingPhoto = true
                val compressedPath = ImageStorageHelper.compressAndSavePhoto(context, capturedUri)
                tempCameraFile?.delete()
                isCompressingPhoto = false
                if (compressedPath != null) {
                    onAddPhotos(currentRec, listOf(compressedPath))
                } else {
                    onMessage("Не удалось обработать снимок с камеры")
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val (file, uri) = ImageStorageHelper.createTempCameraFileUri(context)
                tempCameraFile = file
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                onMessage("Камера недоступна: используйте выбор из галереи")
            }
        } else {
            onMessage("Разрешение на камеру отклонено")
        }
    }

    val pickMultiplePhotosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(RepairRepository.MAX_PHOTOS_PER_RECORD)
    ) { uris: List<Uri> ->
        val currentRec = record
        if (uris.isNotEmpty() && currentRec != null) {
            coroutineScope.launch {
                val remaining = RepairRepository.MAX_PHOTOS_PER_RECORD - effectivePhotos.size
                if (remaining <= 0) {
                    onMessage("В одной записи может быть максимум ${RepairRepository.MAX_PHOTOS_PER_RECORD} фото")
                    return@launch
                }
                val toProcess = uris.take(remaining)
                val skipped = uris.size - toProcess.size

                isCompressingPhoto = true
                val newPaths = mutableListOf<String>()
                for (uri in toProcess) {
                    val path = ImageStorageHelper.compressAndSavePhoto(context, uri)
                    if (path != null) {
                        newPaths.add(path)
                    }
                }
                isCompressingPhoto = false

                if (newPaths.isNotEmpty()) {
                    onAddPhotos(currentRec, newPaths)
                }
                if (skipped > 0) {
                    onMessage("Добавлено ${newPaths.size} фото. Достигнут максимум (${RepairRepository.MAX_PHOTOS_PER_RECORD} фото)")
                }
            }
        }
    }

    fun launchCameraFromDetail() {
        if (effectivePhotos.size >= RepairRepository.MAX_PHOTOS_PER_RECORD) {
            onMessage("Можно добавить максимум ${RepairRepository.MAX_PHOTOS_PER_RECORD} фото в одну запись")
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            try {
                val (file, uri) = ImageStorageHelper.createTempCameraFileUri(context)
                tempCameraFile = file
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                onMessage("Не удалось открыть камеру устройства")
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BackHandler {
        if (isFullScreenZoom) {
            isFullScreenZoom = false
        } else {
            onNavigateBack()
        }
    }

    if (record == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = onNavigateBack) {
                Text("Вернуться к списку")
            }
        }
        return
    }

    if (isFullScreenZoom && effectivePhotos.isNotEmpty()) {
        FullScreenZoomPhotoViewer(
            photoPaths = effectivePhotos,
            initialPage = fullScreenInitialPage.coerceIn(0, effectivePhotos.lastIndex),
            partNumber = record.partNumber,
            onClose = { isFullScreenZoom = false }
        )
        return
    }

    val statusColor = getStatusBarColor(record.status)

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Запись №${record.id}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад к списку",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    StatusBadgeSelector(
                        currentStatus = record.status,
                        onStatusChange = { newStatus -> onStatusChange(record, newStatus) },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onEditClick(record) },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("edit_record_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Редактировать",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("delete_record_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WorkshopRed,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Удалить",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ГАЛЕРЕЯ ФОТОГРАФИЙ СО СВАЙПОМ, ИНДИКАТОРОМ И КНОПКОЙ «ДОБАВИТЬ ЕЩЁ ФОТО»
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(2.dp, statusColor.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("detail_photo_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (effectivePhotos.isNotEmpty()) {
                            val currentPage = pagerState.currentPage.coerceIn(0, effectivePhotos.lastIndex)

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("detail_photo_pager")
                            ) { pageIndex ->
                                val path = effectivePhotos[pageIndex]
                                var pageBitmap by remember(path) { mutableStateOf<Bitmap?>(null) }

                                LaunchedEffect(path) {
                                    pageBitmap = ImageStorageHelper.loadBitmapFromPath(path, maxDimension = 1280)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            fullScreenInitialPage = pageIndex
                                            isFullScreenZoom = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val bmp = pageBitmap
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Фото детали ${record.partNumber} (${pageIndex + 1} из ${effectivePhotos.size})",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }

                            // Индикатор «2 из 5» в верхнем левом углу
                            Surface(
                                color = Color.Black.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                                    .testTag("detail_photo_counter_badge")
                            ) {
                                Text(
                                    text = "${currentPage + 1} из ${effectivePhotos.size}",
                                    color = Color.White,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            // Кнопка «На весь экран / Увеличить» в нижнем правом углу
                            Surface(
                                color = Color.Black.copy(alpha = 0.68f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                                    .clickable {
                                        fullScreenInitialPage = currentPage
                                        isFullScreenZoom = true
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomOutMap,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "На весь экран",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = "Фото отсутствует",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Фотографии не прикреплены",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Индикатор-точки (если фото больше одного)
                    if (effectivePhotos.size > 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("detail_photo_dots")
                        ) {
                            val currentPage = pagerState.currentPage.coerceIn(0, effectivePhotos.lastIndex)
                            effectivePhotos.indices.forEach { idx ->
                                val isSelected = idx == currentPage
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 10.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                            }
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isCompressingPhoto) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Сжатие и сохранение фото…",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Кнопка «Добавить ещё фото» прямо на экране просмотра
                    FilledTonalButton(
                        onClick = {
                            if (effectivePhotos.size >= RepairRepository.MAX_PHOTOS_PER_RECORD) {
                                onMessage("В одной записи может быть максимум ${RepairRepository.MAX_PHOTOS_PER_RECORD} фото")
                            } else {
                                showAddPhotoDialog = true
                            }
                        },
                        enabled = !isCompressingPhoto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("detail_add_more_photo_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Добавить ещё фото (${effectivePhotos.size}/${RepairRepository.MAX_PHOTOS_PER_RECORD})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ДАННЫЕ КЛИЕНТА И КНОПКА «ПОЗВОНИТЬ»
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Ф.И.О. КЛИЕНТА",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = record.clientName.ifBlank { "Не указано" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("detail_client_name")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "НОМЕР ТЕЛЕФОНА",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = record.phone.ifBlank { "Не указан" },
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("detail_client_phone")
                            )
                        }

                        if (record.phone.isNotBlank()) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    ImageStorageHelper.openPhoneDialer(context, record.phone)
                                },
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("call_client_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WorkshopGreen,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Позвонить клиенту",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Позвонить",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            // ДАННЫЕ ДЕТАЛИ, СТАТУСА И ДАТ
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "СТАТУС РЕМОНТА",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            StatusBadgeSelector(
                                currentStatus = record.status,
                                onStatusChange = { newStatus -> onStatusChange(record, newStatus) }
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ВИД РЕМОНТА",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            RepairTypeBadge(repairType = record.repairType)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Column {
                        Text(
                            text = "НОМЕР ДЕТАЛИ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = record.partNumber,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("detail_part_number")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ДАТА СОЗДАНИЯ",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = record.dateTimeStr,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "СМЕНА СТАТУСА",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = record.statusChangedAt.ifBlank { record.dateTimeStr },
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("detail_status_changed_at")
                            )
                        }
                    }
                }
            }
        }
    }

    // Диалог выбора способа добавления фото прямо с экрана просмотра
    if (showAddPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showAddPhotoDialog = false },
            title = {
                Text(
                    text = "Добавить ещё фото",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Доступно слотов: ${RepairRepository.MAX_PHOTOS_PER_RECORD - effectivePhotos.size} из ${RepairRepository.MAX_PHOTOS_PER_RECORD}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            showAddPhotoDialog = false
                            launchCameraFromDetail()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("detail_dialog_camera_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("+ Сфотографировать", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showAddPhotoDialog = false
                            pickMultiplePhotosLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("detail_dialog_gallery_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("+ Из галереи", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showAddPhotoDialog = false
                            coroutineScope.launch {
                                isCompressingPhoto = true
                                val demoPath = ImageStorageHelper.createWorkshopPartSnapshot(
                                    context = context,
                                    partCodeLabel = record.partNumber,
                                    photoIndexLabel = effectivePhotos.size + 1
                                )
                                isCompressingPhoto = false
                                if (demoPath != null) {
                                    onAddPhotos(record, listOf(demoPath))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("detail_dialog_demo_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ Снимок стенда (демо)", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddPhotoDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Удалить запись?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Деталь «${record.partNumber}» (${record.repairType}) и все прикреплённые фотографии (${effectivePhotos.size} шт.) будут удалены с телефона безвозвратно.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteConfirm(record)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WorkshopRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("confirm_delete_button")
                ) {
                    Text("Да, удалить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun FullScreenZoomPhotoViewer(
    photoPaths: List<String>,
    initialPage: Int,
    partNumber: String,
    onClose: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, photoPaths.lastIndex.coerceAtLeast(0)),
        pageCount = { photoPaths.size }
    )
    var currentPageScale by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("fullscreen_zoom_viewer")
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = currentPageScale <= 1.05f,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val path = photoPaths[pageIndex]
            var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
            var scale by remember(pageIndex, pagerState.currentPage) { mutableFloatStateOf(1f) }
            var offset by remember(pageIndex, pagerState.currentPage) { mutableStateOf(Offset.Zero) }

            LaunchedEffect(path) {
                bitmap = ImageStorageHelper.loadBitmapFromPath(path, maxDimension = 1280)
            }

            LaunchedEffect(scale, pagerState.currentPage) {
                if (pageIndex == pagerState.currentPage) {
                    currentPageScale = scale
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pageIndex) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    }
                    .pointerInput(pageIndex) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            if (newScale > 1f) {
                                offset = Offset(
                                    x = offset.x + pan.x,
                                    y = offset.y + pan.y
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val bmp = bitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Полноэкранное фото детали $partNumber (${pageIndex + 1} из ${photoPaths.size})",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                } else {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }

        // Верхняя панель с индикатором «2 из 5», масштабом и кнопкой закрытия
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} из ${photoPaths.size} • $partNumber • ${(currentPageScale * 100).roundToInt()}%",
                    color = Color.White,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("close_fullscreen_photo_button")
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Закрыть")
            }
        }

        // Нижние точки-индикаторы в полноэкранном режиме
        if (photoPaths.size > 1) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    photoPaths.indices.forEach { idx ->
                        val isSelected = idx == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.White.copy(alpha = 0.4f)
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}
