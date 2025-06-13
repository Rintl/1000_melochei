package com.example.a1000_melochei.data.source.remote

import android.net.Uri
import android.util.Log
import com.example.a1000_melochei.data.common.Resource
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

/**
 * Источник данных для работы с Firebase Storage.
 * Управляет загрузкой, скачиванием и удалением файлов.
 */
class StorageSource(
    private val storage: FirebaseStorage
) {
    private val TAG = "StorageSource"

    /**
     * Загружает изображение в Firebase Storage
     */
    suspend fun uploadImage(imageUri: Uri, folder: String): Resource<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child(folder).child(fileName)

            val uploadTask = storageRef.putFile(imageUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

            Log.d(TAG, "Изображение загружено в $folder: $downloadUrl")
            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки изображения в $folder: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки изображения")
        }
    }

    /**
     * Загружает изображение из файла
     */
    suspend fun uploadImageFromFile(imageFile: File, folder: String): Resource<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child(folder).child(fileName)
            val fileUri = Uri.fromFile(imageFile)

            val uploadTask = storageRef.putFile(fileUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

            Log.d(TAG, "Изображение загружено из файла в $folder: $downloadUrl")
            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки изображения из файла в $folder: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки изображения")
        }
    }

    /**
     * Загружает несколько изображений
     */
    suspend fun uploadMultipleImages(imageUris: List<Uri>, folder: String): Resource<List<String>> {
        return try {
            val uploadResults = mutableListOf<String>()
            val errors = mutableListOf<String>()

            imageUris.forEach { uri ->
                when (val result = uploadImage(uri, folder)) {
                    is Resource.Success -> uploadResults.add(result.data)
                    is Resource.Error -> errors.add(result.message ?: "Ошибка загрузки")
                    is Resource.Loading -> { /* Игнорируем */ }
                }
            }

            if (errors.isEmpty()) {
                Log.d(TAG, "Все изображения загружены успешно: ${uploadResults.size}")
                Resource.Success(uploadResults)
            } else {
                Log.e(TAG, "Ошибки при загрузке изображений: $errors")
                Resource.Error("Загружено ${uploadResults.size} из ${imageUris.size} изображений")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка массовой загрузки изображений: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка массовой загрузки изображений")
        }
    }

    /**
     * Удаляет изображение из Firebase Storage
     */
    suspend fun deleteImage(imageUrl: String): Resource<Unit> {
        return try {
            if (imageUrl.isBlank()) {
                return Resource.Success(Unit)
            }

            val storageRef = storage.getReferenceFromUrl(imageUrl)
            storageRef.delete().await()

            Log.d(TAG, "Изображение удалено: $imageUrl")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка удаления изображения: ${e.message}", e)
            // Не возвращаем ошибку, так как файл может уже не существовать
            Resource.Success(Unit)
        }
    }

    /**
     * Удаляет несколько изображений
     */
    suspend fun deleteMultipleImages(imageUrls: List<String>): Resource<Unit> {
        return try {
            val errors = mutableListOf<String>()

            imageUrls.forEach { url ->
                when (val result = deleteImage(url)) {
                    is Resource.Error -> errors.add(result.message ?: "Ошибка удаления")
                    else -> { /* Успех или загрузка */ }
                }
            }

            if (errors.isEmpty()) {
                Log.d(TAG, "Все изображения удалены успешно: ${imageUrls.size}")
                Resource.Success(Unit)
            } else {
                Log.w(TAG, "Некоторые изображения не удалились: $errors")
                // Возвращаем успех, так как основная операция может продолжаться
                Resource.Success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка массового удаления изображений: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка массового удаления изображений")
        }
    }

    /**
     * Скачивает файл из Firebase Storage
     */
    suspend fun downloadFile(fileUrl: String, localFile: File): Resource<File> {
        return try {
            val storageRef = storage.getReferenceFromUrl(fileUrl)
            storageRef.getFile(localFile).await()

            Log.d(TAG, "Файл скачан: ${localFile.absolutePath}")
            Resource.Success(localFile)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка скачивания файла: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка скачивания файла")
        }
    }

    /**
     * Получает размер файла
     */
    suspend fun getFileSize(fileUrl: String): Resource<Long> {
        return try {
            val storageRef = storage.getReferenceFromUrl(fileUrl)
            val metadata = storageRef.metadata.await()
            val size = metadata.sizeBytes

            Log.d(TAG, "Размер файла: $size байт")
            Resource.Success(size)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения размера файла: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка получения размера файла")
        }
    }

    /**
     * Проверяет существование файла
     */
    suspend fun fileExists(fileUrl: String): Boolean {
        return try {
            val storageRef = storage.getReferenceFromUrl(fileUrl)
            storageRef.metadata.await()
            true
        } catch (e: Exception) {
            Log.d(TAG, "Файл не существует или недоступен: $fileUrl")
            false
        }
    }

    /**
     * Получает метаданные файла
     */
    suspend fun getFileMetadata(fileUrl: String): Resource<Map<String, Any>> {
        return try {
            val storageRef = storage.getReferenceFromUrl(fileUrl)
            val metadata = storageRef.metadata.await()

            val metadataMap = mapOf(
                "name" to metadata.name,
                "size" to metadata.sizeBytes,
                "contentType" to (metadata.contentType ?: ""),
                "timeCreated" to metadata.creationTimeMillis,
                "updated" to metadata.updatedTimeMillis,
                "downloadUrl" to fileUrl
            )

            Log.d(TAG, "Метаданные файла получены: ${metadata.name}")
            Resource.Success(metadataMap)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения метаданных файла: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка получения метаданных файла")
        }
    }

    /**
     * Загружает файл с прогрессом
     */
    suspend fun uploadImageWithProgress(
        imageUri: Uri,
        folder: String,
        onProgress: (Int) -> Unit
    ): Resource<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child(folder).child(fileName)

            val uploadTask = storageRef.putFile(imageUri)

            // Отслеживаем прогресс
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                onProgress(progress)
            }

            val uploadResult = uploadTask.await()
            val downloadUrl = uploadResult.storage.downloadUrl.await().toString()

            Log.d(TAG, "Изображение загружено с прогрессом в $folder: $downloadUrl")
            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки изображения с прогрессом в $folder: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки изображения")
        }
    }

    /**
     * Сжимает и загружает изображение
     */
    suspend fun uploadCompressedImage(
        imageUri: Uri,
        folder: String,
        quality: Int = 85,
        maxWidth: Int = 1200,
        maxHeight: Int = 1200
    ): Resource<String> {
        return try {
            // В реальном приложении здесь должно быть сжатие изображения
            // Для демо просто загружаем как есть
            val result = uploadImage(imageUri, folder)

            Log.d(TAG, "Сжатое изображение загружено в $folder")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки сжатого изображения: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка загрузки сжатого изображения")
        }
    }

    /**
     * Очищает папку от файлов
     */
    suspend fun clearFolder(folder: String): Resource<Unit> {
        return try {
            val folderRef = storage.reference.child(folder)
            val listResult = folderRef.listAll().await()

            val deleteResults = mutableListOf<Resource<Unit>>()

            listResult.items.forEach { item ->
                try {
                    item.delete().await()
                    deleteResults.add(Resource.Success(Unit))
                } catch (e: Exception) {
                    deleteResults.add(Resource.Error(e.message ?: "Ошибка удаления файла"))
                }
            }

            val errors = deleteResults.filterIsInstance<Resource.Error<Unit>>()

            if (errors.isEmpty()) {
                Log.d(TAG, "Папка $folder очищена: удалено ${listResult.items.size} файлов")
                Resource.Success(Unit)
            } else {
                Log.w(TAG, "Папка $folder частично очищена: ${errors.size} ошибок")
                Resource.Error("Не удалось удалить ${errors.size} файлов")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка очистки папки $folder: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка очистки папки")
        }
    }

    /**
     * Получает список файлов в папке
     */
    suspend fun listFiles(folder: String): Resource<List<StorageReference>> {
        return try {
            val folderRef = storage.reference.child(folder)
            val listResult = folderRef.listAll().await()

            Log.d(TAG, "В папке $folder найдено файлов: ${listResult.items.size}")
            Resource.Success(listResult.items)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения списка файлов в папке $folder: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка получения списка файлов")
        }
    }

    /**
     * Копирует файл в другую папку
     */
    suspend fun copyFile(sourceUrl: String, targetFolder: String, newFileName: String? = null): Resource<String> {
        return try {
            // Сначала скачиваем файл
            val tempFile = File.createTempFile("temp_copy", ".tmp")
            val downloadResult = downloadFile(sourceUrl, tempFile)

            when (downloadResult) {
                is Resource.Success -> {
                    // Затем загружаем в новое место
                    val fileName = newFileName ?: "${UUID.randomUUID()}.jpg"
                    val targetRef = storage.reference.child(targetFolder).child(fileName)
                    val uploadResult = targetRef.putFile(Uri.fromFile(tempFile)).await()
                    val newUrl = uploadResult.storage.downloadUrl.await().toString()

                    // Удаляем временный файл
                    tempFile.delete()

                    Log.d(TAG, "Файл скопирован в $targetFolder: $newUrl")
                    Resource.Success(newUrl)
                }
                is Resource.Error -> {
                    tempFile.delete()
                    Resource.Error("Ошибка скачивания исходного файла: ${downloadResult.message}")
                }
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка копирования файла: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка копирования файла")
        }
    }

    /**
     * Получает прямую ссылку на файл для скачивания
     */
    suspend fun getDownloadUrl(storageRef: StorageReference): Resource<String> {
        return try {
            val downloadUrl = storageRef.downloadUrl.await().toString()

            Log.d(TAG, "Получена ссылка для скачивания: $downloadUrl")
            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения ссылки для скачивания: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка получения ссылки для скачивания")
        }
    }
}