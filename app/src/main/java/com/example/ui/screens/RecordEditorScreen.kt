package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.example.data.RepairRecord
import com.example.data.RepairRepository
import com.example.data.RepairStatus
import com.example.ui.components.LocalPhotoThumbnail
import com.example.ui.components.getStatusBarColor
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.WorkshopGreen
import com.example.ui.theme.WorkshopRed
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordEditorScreen(
    editingRecord: RepairRecord?,
    initialPhotos: List<String>,
    availableRepairTypes: List<String>,
    onAddCustomRepairType: (String) -> Unit,
    onSaveRecord: (
        partNumber: String,
        repairType: String,
        status: String,
        clientName: String,
        phone: String,
        dateTimeStr: String,
        photoPaths: List<String>
    ) -> Unit,
    onNavigateBack: () -> Unit,
    onMessage: (String) -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var partNumber by rememberSaveable {
        mutableStateOf(editingRecord?.partNumber ?: "")
    }
    var selectedStatus by rememberSaveable {
        mutableStateOf(
            editingRecord?.status?.takeIf { it.isNotBlank() }
                ?: RepairStatus.RECEPTION.label
        )
    }
    var clientName by rememberSaveable {
        mutableStateOf(editingRecord?.clientName ?: "")
    }
    var phoneFieldValue by remember {
        val initialPhone = editingRecord?.phone ?: ""
        mutableStateOf(
            TextFieldValue(
                text = initialPhone,
                selection = TextRange(initialPhone.length)
            )
        )
    }
    var selectedRepairType by rememberSaveable {
        mutableStateOf(
            editingRecord?.repairType
                ?: availableRepairTypes.firstOrNull()
                ?: "замена"
        )
    }
    var dateTimeStr by rememberSaveable {
        mutableStateOf(
            editingRecord?.dateTimeStr ?: ImageStorageHelper.formatCurrentDateTime()
        )
    }

    // Список путей к фотографиям текущей записи (максимум 10)
    var photoPaths by rememberSaveable {
        val seed = if (initialPhotos.isNotEmpty()) {
            initialPhotos
        } else {
            listOfNotNull(editingRecord?.photoPath?.takeIf { it.isNotBlank() })
        }
        mutableStateOf(seed.take(RepairRepository.MAX_PHOTOS_PER_RECORD))
    }

    var isCompressingPhoto by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    // Подтверждение удаления отдельного фото
    var photoIndexPendingDelete by remember { mutableStateOf<Int?>(null) }

    // Состояние перетаскивания миниатюр (долгое нажатие + сдвиг)
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val itemStepPx = with(density) { 132.dp.toPx() }

    var showRepairDropdown by remember { mutableStateOf(false) }
    var showAddCustomTypeDialog by remember { mutableStateOf(false) }
    var customRepairTypeInput by remember { mutableStateOf("") }

    var showEditDateTimeDialog by remember { mutableStateOf(false) }
    var manualDateTimeInput by remember { mutableStateOf(dateTimeStr) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val capturedUri = tempCameraUri
        if (success && capturedUri != null) {
            coroutineScope.launch {
                if (photoPaths.size >= RepairRepository.MAX_PHOTOS_PER_RECORD) {
                    tempCameraFile?.delete()
                    onMessage("Достигнут лимит: максимум ${RepairRepository.MAX_PHOTOS_PER_RECORD} фото в одной записи")
                    return@launch
                }
                isCompressingPhoto = true
                val compressedPath = ImageStorageHelper.compressAndSavePhoto(context, capturedUri)
                tempCameraFile?.delete()
                isCompressingPhoto = false
                if (compressedPath != null) {
                    photoPaths = (photoPaths + compressedPath).take(RepairRepository.MAX_PHOTOS_PER_RECORD)
                    onMessage("Фото добавлено (${photoPaths.size} из ${RepairRepository.MAX_PHOTOS_PER_RECORD}). Можно сразу снять следующее.")
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
                onMessage("Камера недоступна: используйте «+ Из галереи» или демо-фото")
            }
        } else {
            onMessage("Разрешение на камеру отклонено. Вы можете выбрать фото из галереи.")
        }
    }

    val pickMultiplePhotosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(RepairRepository.MAX_PHOTOS_PER_RECORD)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                val remaining = RepairRepository.MAX_PHOTOS_PER_RECORD - photoPaths.size
                if (remaining <= 0) {
                    onMessage("Можно добавить максимум ${RepairRepository.MAX_PHOTOS_PER_RECORD} фото в одну запись")
                    return@launch
                }
                val urisToProcess = uris.take(remaining)
                val skippedCount = uris.size - urisToProcess.size

                isCompressingPhoto = true
                val addedPaths = mutableListOf<String>()
                for (uri in urisToProcess) {
                    val path = ImageStorageHelper.compressAndSavePhoto(context, uri)
                    if (path != null) {
                        addedPaths.add(path)
                    }
                }
                isCompressingPhoto = false

                if (addedPaths.isNotEmpty()) {
                    photoPaths = (photoPaths + addedPaths).take(RepairRepository.MAX_PHOTOS_PER_RECORD)
                }

                if (skippedCount > 0) {
                    onMessage(
                        "Добавлено ${addedPaths.size} фото. Лимит ${RepairRepository.MAX_PHOTOS_PER_RECORD} фото: лишние $skippedCount фото пропущены."
                    )
                } else if (addedPaths.isNotEmpty()) {
                    onMessage("Добавлено фото: ${addedPaths.size} (всего ${photoPaths.size} из ${RepairRepository.MAX_PHOTOS_PER_RECORD})")
                } else {
                    onMessage("Не удалось сжать выбранные изображения")
                }
            }
        }
    }

    fun launchCameraCapture() {
        if (photoPaths.size >= RepairRepository.MAX_PHOTOS_PER_RECORD) {
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

    fun movePhoto(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in photoPaths.indices || toIndex !in photoPaths.indices || fromIndex == toIndex) return
        val mutable = photoPaths.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        photoPaths = mutable
    }

    val isSaveEnabled = partNumber.trim().isNotEmpty() &&
        clientName.trim().isNotEmpty() &&
        !isCompressingPhoto

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editingRecord == null) "Новая запись" else "Редактирование",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.size(28.dp)
                        )
                    }
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (!isSaveEnabled && !isCompressingPhoto) {
                        val missingHint = when {
                            partNumber.trim().isEmpty() && clientName.trim().isEmpty() ->
                                "Укажите номер детали и Ф.И.О. клиента"
                            partNumber.trim().isEmpty() ->
                                "Для сохранения обязательно укажите номер детали"
                            else ->
                                "Для сохранения обязательно укажите Ф.И.О. клиента"
                        }
                        Text(
                            text = missingHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Button(
                        onClick = {
                            onSaveRecord(
                                partNumber.trim(),
                                selectedRepairType,
                                selectedStatus,
                                clientName.trim(),
                                phoneFieldValue.text.trim(),
                                dateTimeStr,
                                photoPaths
                            )
                        },
                        enabled = isSaveEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .testTag("save_record_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WorkshopGreen,
                            contentColor = Color.White,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Сохранить",
                            style = MaterialTheme.typography.titleLarge
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
            // 1. БЛОК СТАТУСА РЕМОНТА («Приёмка», «В работе», «Готова»)
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
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Статус ремонта",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RepairStatus.entries.forEach { statusOption ->
                            val isSelected = selectedStatus.equals(statusOption.label, ignoreCase = true)
                            val color = getStatusBarColor(statusOption.label)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStatus = statusOption.label },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("editor_status_${statusOption.name.lowercase()}"),
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (!isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = statusOption.label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color,
                                    selectedLabelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = color.copy(alpha = 0.6f),
                                    selectedBorderColor = color,
                                    borderWidth = 1.5.dp
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // 2. ДАННЫЕ КЛИЕНТА («Ф.И.О. клиента» и «Номер телефона» с маской +7 (XXX) XXX-XX-XX)
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Данные клиента",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Ф.И.О. клиента *") },
                        placeholder = { Text("Иванов Иван Иванович") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("client_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = phoneFieldValue,
                        onValueChange = { newValue ->
                            val formatted = ImageStorageHelper.formatRussianPhoneMask(newValue.text)
                            phoneFieldValue = TextFieldValue(
                                text = formatted,
                                selection = TextRange(formatted.length)
                            )
                        },
                        label = { Text("Номер телефона") },
                        placeholder = { Text("+7 (999) 123-45-67") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("client_phone_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            // 3. БЛОК «ФОТО» С ГОРИЗОНТАЛЬНОЙ ЛЕНТОЙ МИНИАТЮР (до 10 фото, удаление с подтверждением, перестановка)
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Фото",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = if (photoPaths.size >= RepairRepository.MAX_PHOTOS_PER_RECORD) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${photoPaths.size} из ${RepairRepository.MAX_PHOTOS_PER_RECORD}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (photoPaths.size >= RepairRepository.MAX_PHOTOS_PER_RECORD) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .testTag("editor_photo_counter")
                            )
                        }
                    }

                    if (photoPaths.isNotEmpty()) {
                        Text(
                            text = "Первое фото — обложка карточки. Удерживайте миниатюру и тяните (или нажимайте стрелки ◄ ►), чтобы поменять порядок.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(
                                    state = rememberScrollState(),
                                    enabled = draggedIndex == null
                                )
                                .padding(vertical = 4.dp)
                                .testTag("editor_photo_strip"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            photoPaths.forEachIndexed { index, path ->
                                val isDragged = draggedIndex == index
                                val isCover = index == 0

                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .zIndex(if (isDragged) 10f else 1f)
                                        .graphicsLayer {
                                            translationX = if (isDragged) dragOffsetX else 0f
                                            scaleX = if (isDragged) 1.07f else 1f
                                            scaleY = if (isDragged) 1.07f else 1f
                                        }
                                        .pointerInput(photoPaths.size) {
                                            var currentIdx = index
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    currentIdx = index
                                                    draggedIndex = currentIdx
                                                    dragOffsetX = 0f
                                                },
                                                onDragEnd = {
                                                    draggedIndex = null
                                                    dragOffsetX = 0f
                                                },
                                                onDragCancel = {
                                                    draggedIndex = null
                                                    dragOffsetX = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetX += dragAmount.x
                                                    val threshold = itemStepPx * 0.55f
                                                    if (dragOffsetX > threshold && currentIdx < photoPaths.lastIndex) {
                                                        movePhoto(currentIdx, currentIdx + 1)
                                                        currentIdx += 1
                                                        draggedIndex = currentIdx
                                                        dragOffsetX -= itemStepPx
                                                    } else if (dragOffsetX < -threshold && currentIdx > 0) {
                                                        movePhoto(currentIdx, currentIdx - 1)
                                                        currentIdx -= 1
                                                        draggedIndex = currentIdx
                                                        dragOffsetX += itemStepPx
                                                    }
                                                }
                                            )
                                        }
                                        .testTag("editor_photo_item_$index")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .border(
                                                    width = if (isCover || isDragged) 2.5.dp else 1.dp,
                                                    color = when {
                                                        isDragged -> MaterialTheme.colorScheme.primary
                                                        isCover -> WorkshopGreen
                                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                    },
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                        ) {
                                            LocalPhotoThumbnail(
                                                photoPath = path,
                                                contentDescription = "Фото №${index + 1}",
                                                maxDimension = 640,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            // Бейдж «Обложка» на первом фото или номер фото
                                            Surface(
                                                color = if (isCover) {
                                                    WorkshopGreen.copy(alpha = 0.92f)
                                                } else {
                                                    Color.Black.copy(alpha = 0.72f)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(6.dp)
                                            ) {
                                                Text(
                                                    text = if (isCover) "1 • Обложка" else "№${index + 1}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            // Маленький крестик для удаления фото (с подтверждением)
                                            Surface(
                                                color = WorkshopRed,
                                                shape = CircleShape,
                                                shadowElevation = 4.dp,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(30.dp)
                                                    .clickable { photoIndexPendingDelete = index }
                                                    .testTag("delete_photo_button_$index")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Удалить фото №${index + 1}",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (photoPaths.size > 1) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { movePhoto(index, index - 1) },
                                                    enabled = index > 0,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                                        contentDescription = "Сдвинуть влево",
                                                        tint = if (index > 0) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                                        }
                                                    )
                                                }
                                                Text(
                                                    text = "${index + 1}/${photoPaths.size}",
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                IconButton(
                                                    onClick = { movePhoto(index, index + 1) },
                                                    enabled = index < photoPaths.lastIndex,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                        contentDescription = "Сдвинуть вправо",
                                                        tint = if (index < photoPaths.lastIndex) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Пока нет фотографий (можно добавить до 10 шт.)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    if (isCompressingPhoto) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Сжатие фото (1280 px, JPEG 80%)…",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Кнопка «+ Сфотографировать»
                    Button(
                        onClick = { launchCameraCapture() },
                        enabled = !isCompressingPhoto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("take_photo_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "+ Сфотографировать",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Кнопка «+ Из галереи» (мультивыбор) + «Снимок стенда» для проверки в эмуляторе
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (photoPaths.size >= RepairRepository.MAX_PHOTOS_PER_RECORD) {
                                    onMessage("Можно добавить максимум ${RepairRepository.MAX_PHOTOS_PER_RECORD} фото в одну запись")
                                } else {
                                    pickMultiplePhotosLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            },
                            enabled = !isCompressingPhoto,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("pick_gallery_button"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ Из галереи",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (photoPaths.size >= RepairRepository.MAX_PHOTOS_PER_RECORD) {
                                    onMessage("Можно добавить максимум ${RepairRepository.MAX_PHOTOS_PER_RECORD} фото в одну запись")
                                    return@OutlinedButton
                                }
                                coroutineScope.launch {
                                    isCompressingPhoto = true
                                    val generated = ImageStorageHelper.createWorkshopPartSnapshot(
                                        context = context,
                                        partCodeLabel = partNumber.trim(),
                                        photoIndexLabel = photoPaths.size + 1
                                    )
                                    isCompressingPhoto = false
                                    if (generated != null) {
                                        photoPaths = (photoPaths + generated).take(RepairRepository.MAX_PHOTOS_PER_RECORD)
                                    }
                                }
                            },
                            enabled = !isCompressingPhoto,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("demo_snapshot_button"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "+ Снимок стенда",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 4. ПОЛЕ «НОМЕР ДЕТАЛИ» (обязательное)
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
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Номер детали *",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = partNumber,
                        onValueChange = { partNumber = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("part_number_input"),
                        placeholder = {
                            Text(
                                text = "Например: 4F0 615 301 G или Артикул-105",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            // 5. ПОЛЕ «ВИД РЕМОНТА»
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
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Вид ремонта",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .clickable { showRepairDropdown = true }
                                .testTag("repair_type_dropdown"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = selectedRepairType.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Открыть список видов ремонта",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showRepairDropdown,
                            onDismissRequest = { showRepairDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.88f)
                        ) {
                            availableRepairTypes.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    },
                                    onClick = {
                                        selectedRepairType = option
                                        showRepairDropdown = false
                                    },
                                    modifier = Modifier.height(52.dp)
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "+ Ввести свой вариант…",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    showRepairDropdown = false
                                    customRepairTypeInput = ""
                                    showAddCustomTypeDialog = true
                                },
                                modifier = Modifier.height(52.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableRepairTypes.forEach { typeOption ->
                            val selected = selectedRepairType.equals(typeOption, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = { selectedRepairType = typeOption },
                                label = {
                                    Text(
                                        text = typeOption.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        FilterChip(
                            selected = false,
                            onClick = {
                                customRepairTypeInput = ""
                                showAddCustomTypeDialog = true
                            },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Свой вариант",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_custom_repair_type_chip")
                        )
                    }
                }
            }

            // 6. ДАТА И ВРЕМЯ СОЗДАНИЯ
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Дата и время создания (авто)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dateTimeStr,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("date_time_display")
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            manualDateTimeInput = dateTimeStr
                            showEditDateTimeDialog = true
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("edit_date_time_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = "Исправить дату и время вручную",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Исправить",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Диалог подтверждения удаления фото из ленты
    val deleteIdx = photoIndexPendingDelete
    if (deleteIdx != null && deleteIdx in photoPaths.indices) {
        AlertDialog(
            onDismissRequest = { photoIndexPendingDelete = null },
            title = {
                Text(
                    text = "Удалить фото №${deleteIdx + 1}?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Вы уверены, что хотите убрать эту фотографию из записи?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val removedPath = photoPaths[deleteIdx]
                        // Если фото было создано только в текущей сессии и не принадлежит исходной записи, удаляем файл сразу
                        if (removedPath !in initialPhotos) {
                            ImageStorageHelper.deletePhotoFileIfExists(removedPath)
                        }
                        photoPaths = photoPaths.filterIndexed { i, _ -> i != deleteIdx }
                        photoIndexPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WorkshopRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("confirm_delete_photo_button")
                ) {
                    Text("Да, удалить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { photoIndexPendingDelete = null },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showAddCustomTypeDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomTypeDialog = false },
            title = {
                Text(
                    text = "Новый вид ремонта",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "Введите свой вариант — он сохранится в списке для следующих записей:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customRepairTypeInput,
                        onValueChange = { customRepairTypeInput = it },
                        placeholder = { Text("Например: расточка, балансировка") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_repair_type_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = customRepairTypeInput.trim()
                        if (cleaned.isNotEmpty()) {
                            onAddCustomRepairType(cleaned)
                            selectedRepairType = cleaned
                        }
                        showAddCustomTypeDialog = false
                    },
                    enabled = customRepairTypeInput.trim().isNotEmpty(),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Добавить и выбрать")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddCustomTypeDialog = false },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showEditDateTimeDialog) {
        AlertDialog(
            onDismissRequest = { showEditDateTimeDialog = false },
            title = {
                Text(
                    text = "Дата и время записи",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "Формат ДД.ММ.ГГГГ ЧЧ:ММ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualDateTimeInput,
                        onValueChange = { manualDateTimeInput = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_date_time_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            manualDateTimeInput = ImageStorageHelper.formatCurrentDateTime()
                        }
                    ) {
                        Text("Поставить текущее время")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = manualDateTimeInput.trim()
                        if (cleaned.isNotEmpty()) {
                            dateTimeStr = cleaned
                        }
                        showEditDateTimeDialog = false
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Применить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDateTimeDialog = false },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}
