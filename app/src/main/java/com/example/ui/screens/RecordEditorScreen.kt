package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.RepairRecord
import com.example.ui.components.LocalPhotoThumbnail
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.WorkshopGreen
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordEditorScreen(
    editingRecord: RepairRecord?,
    availableRepairTypes: List<String>,
    onAddCustomRepairType: (String) -> Unit,
    onSaveRecord: (
        partNumber: String,
        repairType: String,
        dateTimeStr: String,
        photoPath: String?
    ) -> Unit,
    onNavigateBack: () -> Unit,
    onMessage: (String) -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var partNumber by rememberSaveable {
        mutableStateOf(editingRecord?.partNumber ?: "")
    }
    var selectedRepairType by rememberSaveable {
        mutableStateOf(
            editingRecord?.repairType
                ?: availableRepairTypes.firstOrNull()
                ?: "замена"
        )
    }
    // Дата и время ставятся АВТОМАТИЧЕСКИ в момент открытия экрана (ДД.ММ.ГГГГ ЧЧ:ММ)
    var dateTimeStr by rememberSaveable {
        mutableStateOf(
            editingRecord?.dateTimeStr ?: ImageStorageHelper.formatCurrentDateTime()
        )
    }
    var photoPath by rememberSaveable {
        mutableStateOf(editingRecord?.photoPath)
    }

    var isCompressingPhoto by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    var showRepairDropdown by remember { mutableStateOf(false) }
    var showAddCustomTypeDialog by remember { mutableStateOf(false) }
    var customRepairTypeInput by remember { mutableStateOf("") }

    var showEditDateTimeDialog by remember { mutableStateOf(false) }
    var manualDateTimeInput by remember { mutableStateOf(dateTimeStr) }

    // Обработчик результата съёмки с камеры
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val capturedUri = tempCameraUri
        if (success && capturedUri != null) {
            coroutineScope.launch {
                isCompressingPhoto = true
                val compressedPath = ImageStorageHelper.compressAndSavePhoto(context, capturedUri)
                tempCameraFile?.delete()
                isCompressingPhoto = false
                if (compressedPath != null) {
                    photoPath = compressedPath
                } else {
                    onMessage("Не удалось обработать снимок с камеры")
                }
            }
        }
    }

    // Запрос разрешения на камеру + запуск съёмки
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
                onMessage("Камера недоступна: используйте выбор файла или демо-фото")
            }
        } else {
            onMessage("Разрешение на камеру отклонено. Вы можете выбрать фото из памяти.")
        }
    }

    // Запасной вариант: выбор фото через системный Photo Picker (без разрешений на хранилище)
    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isCompressingPhoto = true
                val compressedPath = ImageStorageHelper.compressAndSavePhoto(context, uri)
                isCompressingPhoto = false
                if (compressedPath != null) {
                    photoPath = compressedPath
                } else {
                    onMessage("Не удалось сжать выбранное изображение")
                }
            }
        }
    }

    fun launchCameraCapture() {
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

    val isSaveEnabled = partNumber.trim().isNotEmpty() && !isCompressingPhoto

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
                        Text(
                            text = "Для сохранения обязательно укажите номер детали",
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
                                dateTimeStr,
                                photoPath
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. БЛОК ФОТОГРАФИИ ДЕТАЛИ
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isCompressingPhoto) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Сжатие фото (1280px, JPEG 0.8)…",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else if (!photoPath.isNullOrBlank()) {
                        LocalPhotoThumbnail(
                            photoPath = photoPath,
                            contentDescription = "Превью фото детали",
                            maxDimension = 1280,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Фото сжато (до 1280 px, JPEG ~0.8) и сохранено локально",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = WorkshopGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Крупная кнопка «Сфотографировать» / «Переснять»
                    Button(
                        onClick = { launchCameraCapture() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("take_photo_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (photoPath.isNullOrBlank()) Icons.Default.CameraAlt else Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (photoPath.isNullOrBlank()) "Сфотографировать" else "Переснять фото",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Запасные варианты (выбор из галереи или быстрый снимок стенда для эмулятора)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                pickPhotoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("pick_gallery_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Из файла",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isCompressingPhoto = true
                                    val generated = ImageStorageHelper.createWorkshopPartSnapshot(
                                        context = context,
                                        partCodeLabel = partNumber.trim()
                                    )
                                    isCompressingPhoto = false
                                    if (generated != null) {
                                        photoPath = generated
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("demo_snapshot_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Снимок стенда",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 2. ПОЛЕ «НОМЕР ДЕТАЛИ» (обязательное)
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

            // 3. ПОЛЕ «ВИД РЕМОНТА» (выпадающий список + крупные чипы + свой вариант)
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

                    // Выпадающий список (Dropdown)
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

                    // Крупные кнопки быстрого выбора одной рукой
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

            // 4. ДАТА И ВРЕМЯ (автоматически + возможность исправить вручную)
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
                            text = "Дата и время (авто)",
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

    // Диалог добавления своего вида ремонта
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

    // Диалог ручного изменения даты и времени (формат ДД.ММ.ГГГГ ЧЧ:ММ)
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
                        text = "Формат ДД.ММ.ГГГГ ЧЧ:ММ:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = manualDateTimeInput,
                        onValueChange = { manualDateTimeInput = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
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
                        Text("Поставить текущее время («Сейчас»)")
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
                    Text("Готово")
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
