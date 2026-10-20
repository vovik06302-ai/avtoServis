package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.RepairRecord
import com.example.data.RepairStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.roundToInt

object ImageStorageHelper {

    private const val MAX_LONG_SIDE_PX = 1280
    private const val JPEG_QUALITY = 80 // ~80%
    private val fileCounter = AtomicInteger(0)

    fun formatCurrentDateTime(date: Date = Date()): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru-RU"))
        return formatter.format(date)
    }

    /**
     * Форматирует вводимый номер телефона по маске +7 (XXX) XXX-XX-XX.
     */
    fun formatRussianPhoneMask(rawInput: String): String {
        val allDigits = rawInput.filter { it.isDigit() }
        if (allDigits.isEmpty()) return ""

        val subscriberDigits = when {
            allDigits.startsWith("7") || allDigits.startsWith("8") -> allDigits.drop(1)
            else -> allDigits
        }.take(10)

        if (subscriberDigits.isEmpty()) {
            return "+7 ("
        }

        val sb = StringBuilder("+7 (")
        for (i in subscriberDigits.indices) {
            when (i) {
                3 -> sb.append(") ")
                6 -> sb.append("-")
                8 -> sb.append("-")
            }
            sb.append(subscriberDigits[i])
        }
        return sb.toString()
    }

    /**
     * Подготавливает номер телефона для интента звонка (tel:+7XXXXXXXXXX).
     */
    fun normalizePhoneForDial(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        return when {
            digits.startsWith("7") && digits.length == 11 -> "+$digits"
            digits.startsWith("8") && digits.length == 11 -> "+7${digits.drop(1)}"
            digits.length == 10 -> "+7$digits"
            else -> "+$digits"
        }
    }

    /**
     * Открывает системную звонилку телефона с указанным номером.
     */
    fun openPhoneDialer(context: Context, phone: String) {
        val dialNumber = normalizePhoneForDial(phone)
        if (dialNumber.isBlank()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Создаёт временный файл для съёмки с камеры и возвращает его File и content:// Uri.
     */
    fun createTempCameraFileUri(context: Context): Pair<File, Uri> {
        val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val seq = fileCounter.incrementAndGet()
        val file = File(cameraDir, "capture_${System.currentTimeMillis()}_$seq.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return file to uri
    }

    /**
     * Сжимает изображение с камеры или из галереи:
     * длинная сторона <= 1280 px, формат JPEG с качеством 80 (~80%),
     * и сохраняет локально во внутреннем хранилище приложения.
     */
    suspend fun compressAndSavePhoto(
        context: Context,
        sourceUri: Uri
    ): String? = withContext(Dispatchers.IO) {
        try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val srcWidth = boundsOptions.outWidth
            val srcHeight = boundsOptions.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return@withContext null

            var inSampleSize = 1
            val longestSide = max(srcWidth, srcHeight)
            if (longestSide > MAX_LONG_SIDE_PX * 2) {
                val halfLong = longestSide / 2
                while ((halfLong / inSampleSize) >= MAX_LONG_SIDE_PX) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val decodedBitmap = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            val currentLongSide = max(decodedBitmap.width, decodedBitmap.height)
            val finalBitmap = if (currentLongSide > MAX_LONG_SIDE_PX) {
                val ratio = MAX_LONG_SIDE_PX.toFloat() / currentLongSide.toFloat()
                val targetW = (decodedBitmap.width * ratio).roundToInt().coerceAtLeast(1)
                val targetH = (decodedBitmap.height * ratio).roundToInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true)
                if (scaled != decodedBitmap) {
                    decodedBitmap.recycle()
                }
                scaled
            } else {
                decodedBitmap
            }

            val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
            val seq = fileCounter.incrementAndGet()
            val destFile = File(photosDir, "part_${System.currentTimeMillis()}_$seq.jpg")
            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.flush()
            }
            finalBitmap.recycle()
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Загружает Bitmap из локального пути для отображения превью или полноэкранного фото.
     */
    suspend fun loadBitmapFromPath(path: String?, maxDimension: Int = 1280): Bitmap? = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext null
        try {
            val file = File(path)
            if (!file.exists()) return@withContext null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sampleSize = 1
            val longSide = max(bounds.outWidth, bounds.outHeight)
            if (longSide > maxDimension) {
                sampleSize = (longSide.toFloat() / maxDimension.toFloat()).roundToInt().coerceAtLeast(1)
            }

            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Удаляет локальный файл фотографии при удалении фото или записи.
     */
    fun deletePhotoFileIfExists(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Удаляет список локальных файлов фотографий.
     */
    fun deletePhotoFiles(paths: Collection<String?>) {
        paths.forEach { deletePhotoFileIfExists(it) }
    }

    /**
     * Создаёт наглядное демо-фото автомобильной детали (1280x960, JPEG 80%),
     * если мастер или тестировщик проверяет работу в эмуляторе без физической камеры.
     */
    suspend fun createWorkshopPartSnapshot(
        context: Context,
        partCodeLabel: String,
        photoIndexLabel: Int = 1
    ): String? = withContext(Dispatchers.IO) {
        try {
            val width = 1280
            val height = 960
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            canvas.drawColor(Color.parseColor("#1E293B"))

            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                strokeWidth = 2f
            }
            for (x in 0..width step 80) {
                canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
            }
            for (y in 0..height step 80) {
                canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
            }

            val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(width / 2f, height / 2f - 40f, 290f, discPaint)

            val innerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                style = Paint.Style.STROKE
                strokeWidth = 28f
            }
            canvas.drawCircle(width / 2f, height / 2f - 40f, 210f, innerRingPaint)

            val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(width / 2f, height / 2f - 40f, 95f, hubPaint)

            val badgeRect = RectF(180f, height - 210f, width - 180f, height - 70f)
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F97316")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(badgeRect, 24f, 24f, badgePaint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 42f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            val codeToDraw = partCodeLabel.ifBlank { "ДЕТАЛЬ АВТОСЕРВИСА" }
            canvas.drawText(
                "ДЕТАЛЬ: $codeToDraw • ФОТО №$photoIndexLabel",
                width / 2f,
                height - 125f,
                textPaint
            )

            val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
            val seq = fileCounter.incrementAndGet()
            val destFile = File(photosDir, "part_${System.currentTimeMillis()}_$seq.jpg")
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.flush()
            }
            bitmap.recycle()
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Формирует CSV-строку (с UTF-8 BOM для корректного открытия русских букв в Excel).
     * Включает столбцы: ID, Статус, Номер детали, Вид ремонта, Ф.И.О. клиента, Телефон,
     * Дата и время, Дата смены статуса, Количество фото, Фото файлы.
     */
    fun buildCsvContent(
        records: List<RepairRecord>,
        photosByRecordId: Map<Long, List<String>> = emptyMap()
    ): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append("ID;Статус;Номер детали;Вид ремонта;Ф.И.О. клиента;Телефон;Дата и время;Дата смены статуса;Количество фото;Фото файлы\r\n")
        for (record in records) {
            val safeStatus = escapeCsvField(record.status.ifBlank { RepairStatus.RECEPTION.label })
            val safePart = escapeCsvField(record.partNumber)
            val safeRepair = escapeCsvField(record.repairType)
            val safeClient = escapeCsvField(record.clientName)
            val safePhone = escapeCsvField(record.phone)
            val safeDate = escapeCsvField(record.dateTimeStr)
            val safeStatusDate = escapeCsvField(record.statusChangedAt.ifBlank { record.dateTimeStr })

            val recordPhotos = photosByRecordId[record.id]
                ?: listOfNotNull(record.photoPath?.takeIf { it.isNotBlank() })
            val photoCount = recordPhotos.size
            val photoNames = if (recordPhotos.isEmpty()) {
                "Нет фото"
            } else {
                recordPhotos.joinToString(", ") { File(it).name }
            }

            sb.append(
                "${record.id};$safeStatus;$safePart;$safeRepair;$safeClient;$safePhone;$safeDate;$safeStatusDate;$photoCount;${escapeCsvField(photoNames)}\r\n"
            )
        }
        return sb.toString()
    }

    private fun escapeCsvField(value: String): String {
        val needsQuotes = value.contains(';') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Экспортирует записи в CSV-файл во временную папку и открывает системное меню «Поделиться / Сохранить».
     */
    suspend fun shareCsvFile(
        context: Context,
        records: List<RepairRecord>,
        photosByRecordId: Map<Long, List<String>> = emptyMap()
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val csvText = buildCsvContent(records, photosByRecordId)
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val csvFile = File(exportDir, "autoservice_records_$stamp.csv")
            csvFile.writeText(csvText, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )

            withContext(Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Экспорт записей автосервиса ($stamp)")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Экспорт списка записей в CSV")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            Result.success(csvFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
