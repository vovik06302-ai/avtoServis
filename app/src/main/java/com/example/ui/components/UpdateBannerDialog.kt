package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RepairStatus
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SafetyOrange
import com.example.ui.theme.WorkshopAmber
import com.example.ui.theme.WorkshopBlue
import com.example.ui.theme.WorkshopGreen
import com.example.update.ReleaseInfo
import com.example.util.ImageStorageHelper
import kotlin.math.roundToInt

/**
 * Возвращает контрастный цвет статуса для светлой и тёмной темы:
 * - «Приёмка» — синий
 * - «В работе» — оранжевый
 * - «Готова» — зелёный
 */
@Composable
fun getStatusAccentColor(statusLabel: String): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (RepairStatus.fromLabel(statusLabel)) {
        RepairStatus.RECEPTION -> if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)
        RepairStatus.IN_PROGRESS -> if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
        RepairStatus.READY -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    }
}

/**
 * Возвращает цвет вертикальной полосы для статуса.
 */
@Composable
fun getStatusBarColor(statusLabel: String): Color {
    return when (RepairStatus.fromLabel(statusLabel)) {
        RepairStatus.RECEPTION -> Color(0xFF2563EB)
        RepairStatus.IN_PROGRESS -> Color(0xFFF97316)
        RepairStatus.READY -> Color(0xFF10B981)
    }
}

/**
 * Интерактивный цветной бейдж статуса на карточке:
 * по одному нажатию открывает выбор из трёх статусов («Приёмка», «В работе», «Готова»).
 */
@Composable
fun StatusBadgeSelector(
    currentStatus: String,
    onStatusChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val normalizedStatus = RepairStatus.fromLabel(currentStatus).label
    val textColor = getStatusAccentColor(normalizedStatus)
    val barColor = getStatusBarColor(normalizedStatus)

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .testTag("status_badge_button"),
            shape = RoundedCornerShape(10.dp),
            color = barColor.copy(alpha = 0.16f),
            border = BorderStroke(1.5.dp, barColor.copy(alpha = 0.75f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(barColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = normalizedStatus,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Изменить статус",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            RepairStatus.entries.forEach { statusOption ->
                val optionColor = getStatusBarColor(statusOption.label)
                val isSelected = statusOption.label == normalizedStatus
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(optionColor)
                            )
                            Text(
                                text = statusOption.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Выбрано",
                                tint = optionColor
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        if (statusOption.label != normalizedStatus) {
                            onStatusChange(statusOption.label)
                        }
                    },
                    modifier = Modifier.height(48.dp)
                )
            }
        }
    }
}

/**
 * Компонент ProgressBar с плавным анимированным заполнением и движущимся световым бликом
 * для отображения прогресса скачивания APK в диалоге и баннере обновления.
 */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String = "Загрузка APK…"
) {
    val targetProgress = progress.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(
            durationMillis = 350,
            easing = FastOutSlowInEasing
        ),
        label = "apk_progress_fill"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "apk_progress_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val percent = (animatedProgress * 100f).roundToInt().coerceIn(0, 100)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (percent >= 100) "Загрузка завершена, запуск установки…" else label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$percent%",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (percent >= 100) WorkshopGreen else MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("apk_download_percent_text")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(trackColor)
                .border(1.dp, outlineColor, RoundedCornerShape(10.dp))
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(animatedProgress, 0f..1f)
                }
                .testTag("apk_download_progress_bar")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                val fillWidth = (size.width * animatedProgress).coerceAtLeast(0f)
                if (fillWidth > 0f) {
                    val gradientBrush = Brush.horizontalGradient(
                        colors = listOf(
                            SafetyOrange,
                            WorkshopAmber,
                            WorkshopGreen
                        ),
                        startX = 0f,
                        endX = size.width.coerceAtLeast(1f)
                    )

                    drawRoundRect(
                        brush = gradientBrush,
                        topLeft = Offset.Zero,
                        size = Size(width = fillWidth, height = size.height),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )

                    if (animatedProgress < 1f) {
                        val bandWidth = size.width * 0.28f
                        val startX = shimmerOffset * fillWidth
                        val shimmerBrush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.38f),
                                Color.Transparent
                            ),
                            start = Offset(startX - bandWidth, 0f),
                            end = Offset(startX + bandWidth, size.height)
                        )
                        drawRoundRect(
                            brush = shimmerBrush,
                            topLeft = Offset.Zero,
                            size = Size(width = fillWidth, height = size.height),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateAvailableDialog(
    release: ReleaseInfo,
    isDownloading: Boolean,
    downloadProgress: Float,
    onDownloadAndInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) {
                onDismiss()
            }
        },
        modifier = Modifier.testTag("update_available_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = "Доступно обновление",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Доступно обновление: ${release.version}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!release.apkName.isNullOrBlank()) {
                    Text(
                        text = "Пакет: ${release.apkName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (release.body.isNotBlank()) {
                    Text(
                        text = release.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ProgressBar(
                        progress = downloadProgress,
                        label = "Скачивание обновления…"
                    )
                }
            }
        },
        confirmButton = {
            if (!isDownloading) {
                Button(
                    onClick = onDownloadAndInstall,
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("dialog_download_install_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Скачать и установить",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("dialog_dismiss_update_button")
                ) {
                    Text("Позже")
                }
            }
        }
    )
}

@Composable
fun UpdateAvailableBanner(
    release: ReleaseInfo,
    isDownloading: Boolean,
    downloadProgress: Float,
    onDownloadAndInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("update_available_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = "Доступно обновление",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Доступно обновление: ${release.version}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    if (!release.apkName.isNullOrBlank()) {
                        Text(
                            text = "Файл: ${release.apkName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    enabled = !isDownloading,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dismiss_update_banner_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть баннер обновления",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (release.body.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = release.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isDownloading) {
                ProgressBar(
                    progress = downloadProgress,
                    label = "Загрузка APK…"
                )
            } else {
                Button(
                    onClick = onDownloadAndInstall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("download_install_apk_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Скачать и установить",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun LocalPhotoThumbnail(
    photoPath: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    maxDimension: Int = 512
) {
    var bitmap by remember(photoPath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(photoPath) {
        bitmap = ImageStorageHelper.loadBitmapFromPath(photoPath, maxDimension = maxDimension)
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun RepairTypeBadge(
    repairType: String,
    modifier: Modifier = Modifier
) {
    val lower = repairType.lowercase()
    val badgeColor = when {
        lower.contains("замен") -> SafetyOrange
        lower.contains("ремонт") -> WorkshopBlue
        lower.contains("диагност") -> WorkshopAmber
        lower.contains("покраск") -> Color(0xFF8B5CF6)
        lower.contains("сварк") -> Color(0xFFEC4899)
        lower.contains("регулир") -> WorkshopGreen
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = badgeColor.copy(alpha = 0.16f)
    ) {
        Text(
            text = repairType.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = badgeColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
