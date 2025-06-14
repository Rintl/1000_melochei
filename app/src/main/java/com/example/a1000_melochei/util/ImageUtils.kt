package com.example.a1000_melochei.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * Класс-утилита для работы с изображениями в приложении.
 * Предоставляет методы для сжатия, поворота, сохранения и других операций с изображениями.
 */
object ImageUtils {

    private const val TAG = "ImageUtils"
    private const val DEFAULT_QUALITY = Constants.IMAGE_QUALITY
    private const val MAX_WIDTH = Constants.MAX_IMAGE_WIDTH
    private const val MAX_HEIGHT = Constants.MAX_IMAGE_HEIGHT

    /**
     * Создает временный файл для хранения изображения
     *
     * @param context контекст приложения
     * @param format расширение файла (по умолчанию ".jpg")
     * @return созданный файл или null в случае ошибки
     */
    fun createTempImageFile(context: Context, format: String = ".jpg"): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            File.createTempFile(
                imageFileName,
                format,
                storageDir
            )
        } catch (e: IOException) {
            Log.e(TAG, "Ошибка при создании временного файла: ${e.message}")
            null
        }
    }

    /**
     * Сжимает изображение для уменьшения размера файла и оптимизации хранения
     *
     * @param context контекст приложения
     * @param uri URI изображения для сжатия
     * @param quality качество сжатия (0-100)
     * @param maxWidth максимальная ширина результата
     * @param maxHeight максимальная высота результата
     * @return сжатый файл изображения или null в случае ошибки
     */
    fun compressImage(
        context: Context,
        uri: Uri,
        quality: Int = DEFAULT_QUALITY,
        maxWidth: Int = MAX_WIDTH,
        maxHeight: Int = MAX_HEIGHT
    ): File? {
        val contentResolver = context.contentResolver

        try {
            // Получаем входной поток для URI
            val inputStream = contentResolver.openInputStream(uri) ?: return null

            // Получаем опции и размеры изображения без загрузки в память
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Рассчитываем масштаб для уменьшения размеров
            val scale = calculateInSampleSize(options, maxWidth, maxHeight)

            // Загружаем изображение с учетом рассчитанного масштаба
            val newInputStream = contentResolver.openInputStream(uri) ?: return null
            val scaledOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
                inJustDecodeBounds = false
            }

            val bitmap = BitmapFactory.decodeStream(newInputStream, null, scaledOptions)
            newInputStream.close()

            if (bitmap == null) {
                Log.e(TAG, "Не удалось декодировать изображение")
                return null
            }

            // Корректируем ориентацию изображения
            val rotatedBitmap = correctImageOrientation(context, uri, bitmap)

            // Создаем файл для сжатого изображения
            val compressedFile = createTempImageFile(context)

            if (compressedFile != null) {
                val outputStream = FileOutputStream(compressedFile)
                // Сжимаем и сохраняем в файл
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                outputStream.flush()
                outputStream.close()

                // Освобождаем ресурсы
                if (rotatedBitmap != bitmap) {
                    rotatedBitmap.recycle()
                }
                bitmap.recycle()

                return compressedFile
            } else {
                Log.e(TAG, "Не удалось создать файл для сжатого изображения")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сжатии изображения: ${e.message}")
            return null
        }
    }

    /**
     * Рассчитывает оптимальный коэффициент масштабирования для загрузки изображения
     *
     * @param options опции декодирования изображения
     * @param reqWidth требуемая ширина
     * @param reqHeight требуемая высота
     * @return коэффициент масштабирования (степень двойки)
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Вычисляем наибольший inSampleSize, который кратен степени 2
            // и при этом итоговое изображение будет не меньше требуемого
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Корректирует ориентацию изображения на основе EXIF-данных
     *
     * @param context контекст приложения
     * @param uri URI изображения
     * @param bitmap исходное изображение
     * @return скорректированное изображение
     */
    private fun correctImageOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val contentResolver = context.contentResolver
        var inputStream: InputStream? = null
        var orientation = ExifInterface.ORIENTATION_NORMAL

        try {
            inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val exif = ExifInterface(inputStream)
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при чтении EXIF данных: ${e.message}")
        } finally {
            inputStream?.close()
        }

        // Определяем угол поворота на основе ориентации EXIF
        val rotationAngle = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        // Если поворот не требуется, возвращаем исходное изображение
        if (rotationAngle == 0f) {
            return bitmap
        }

        // Создаем матрицу поворота
        val matrix = Matrix().apply {
            postRotate(rotationAngle)
        }

        // Поворачиваем изображение
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    /**
     * Преобразует Bitmap в файл с указанным именем
     *
     * @param context контекст приложения
     * @param bitmap изображение для сохранения
     * @param fileName имя файла (по умолчанию генерируется временное имя)
     * @param quality качество сжатия (0-100)
     * @return сохраненный файл или null в случае ошибки
     */
    fun saveBitmapToFile(
        context: Context,
        bitmap: Bitmap,
        fileName: String? = null,
        quality: Int = DEFAULT_QUALITY
    ): File? {
        val file = if (fileName != null) {
            File(context.filesDir, fileName)
        } else {
            createTempImageFile(context) ?: return null
        }

        return try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сохранении Bitmap в файл: ${e.message}")
            null
        }
    }

    /**
     * Преобразует Bitmap в массив байтов
     *
     * @param bitmap изображение для преобразования
     * @param quality качество сжатия (0-100)
     * @return массив байтов или null в случае ошибки
     */
    fun bitmapToByteArray(bitmap: Bitmap, quality: Int = DEFAULT_QUALITY): ByteArray? {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при преобразовании Bitmap в ByteArray: ${e.message}")
            null
        }
    }

    /**
     * Загружает изображение из URI с проверкой размера
     *
     * @param contentResolver ContentResolver для доступа к контенту
     * @param uri URI изображения
     * @param maxSize максимальный размер файла в байтах
     * @return Bitmap или null, если файл слишком большой или произошла ошибка
     */
    fun loadImageFromUriWithSizeCheck(
        contentResolver: ContentResolver,
        uri: Uri,
        maxSize: Long = 10 * 1024 * 1024 // 10MB по умолчанию
    ): Bitmap? {
        try {
            // Проверяем размер файла
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex("_size")
                    if (sizeIndex != -1) {
                        val size = it.getLong(sizeIndex)
                        if (size > maxSize) {
                            Log.e(TAG, "Размер файла превышает максимально допустимый ($size > $maxSize)")
                            return null
                        }
                    }
                }
            }

            // Загружаем изображение
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке изображения из URI: ${e.message}")
            return null
        }
    }

    /**
     * Масштабирует Bitmap до указанных размеров
     *
     * @param bitmap исходное изображение
     * @param maxWidth максимальная ширина
     * @param maxHeight максимальная высота
     * @return масштабированное изображение
     */
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    /**
     * Преобразует URI в путь к файлу
     *
     * @param context контекст приложения
     * @param uri URI для преобразования
     * @return путь к файлу или null в случае ошибки
     */
    fun getRealPathFromUri(context: Context, uri: Uri): String? {
        val contentResolver = context.contentResolver
        val projection = arrayOf("_data")

        try {
            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow("_data")
                    return it.getString(columnIndex)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении пути к файлу: ${e.message}")
        }

        // Если не удалось получить путь через запрос, копируем содержимое во временный файл
        return try {
            val tempFile = createTempImageFile(context)
            if (tempFile != null) {
                val inputStream = contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(tempFile)

                inputStream?.use { input ->
                    outputStream.use { output ->
                        val buffer = ByteArray(4 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                    }
                }

                tempFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при копировании файла: ${e.message}")
            null
        }
    }

    /**
     * Проверяет, является ли URI изображением
     *
     * @param context контекст приложения
     * @param uri URI для проверки
     * @return true, если URI указывает на изображение
     */
    fun isImageUri(context: Context, uri: Uri): Boolean {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)

        return mimeType?.startsWith("image/") ?: false
    }
}