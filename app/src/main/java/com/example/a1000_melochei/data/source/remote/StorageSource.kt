package com.yourstore.app.data.source.remote

import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.yourstore.app.data.common.Resource
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import java.util.concurrent.CancellationException

/**
 * Источник данных для работы с Firebase Storage.
 * Позволяет загружать, скачивать и управлять файлами в облачном хранилище.
 */
class StorageSource(private val firebaseStorage: FirebaseStorage) {

    companion object {
        private const val PRODUCTS_PATH = "products"
        private const val CATEGORIES_PATH = "categories"
        private const val USERS_PATH = "users"
        private const val MAX_DOWNLOAD_SIZE: Long = 10 * 1024 * 1024 // 10MB
    }

    /**
     * Загрузка изображения товара
     * @param file Файл изображения
     * @param productId ID товара
     * @return Resource с URL загруженного изображения
     */
    suspend fun uploadProductImage(file: File, productId: String): Resource<String> {
        return uploadFile(file, "$PRODUCTS_PATH/$productId/${UUID.randomUUID()}")
    }

    /**
     * Загрузка изображения категории
     * @param file Файл изображения
     * @param categoryId ID категории
     * @return Resource с URL загруженного изображения
     */
    suspend fun uploadCategoryImage(file: File, categoryId: String): Resource<String> {
        return uploadFile(file, "$CATEGORIES_PATH/$categoryId")
    }

    /**
     * Загрузка изображения пользователя (аватара)
     * @param file Файл изображения
     * @param userId ID пользователя
     * @return Resource с URL загруженного изображения
     */
    suspend fun uploadUserAvatar(file: File, userId: String): Resource<String> {
        return uploadFile(file, "$USERS_PATH/$userId/avatar")
    }

    /**
     * Общий метод для загрузки файла
     * @param file Файл для загрузки
     * @param path Путь в хранилище
     * @return Resource с URL загруженного файла
     */
    private suspend fun uploadFile(file: File, path: String): Resource<String> {
        if (!file.exists()) {
            return Resource.Error("Файл не существует")
        }

        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString())
        val fullPath = if (fileExtension.isNullOrEmpty()) path else "$path.$fileExtension"
        val storageRef = firebaseStorage.reference.child(fullPath)

        return try {
            val uploadTask = storageRef.putFile(Uri.fromFile(file)).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Resource.Success(downloadUrl)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Ошибка загрузки файла: ${e.message}")
        }
    }

    /**
     * Удаление файла по URL
     * @param fileUrl URL файла для удаления
     * @return Resource с результатом операции
     */
    suspend fun deleteFile(fileUrl: String): Resource<Unit> {
        if (fileUrl.isEmpty()) {
            return Resource.Error("URL файла пуст")
        }

        return try {
            val ref = firebaseStorage.getReferenceFromUrl(fileUrl)
            ref.delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Ошибка удаления файла: ${e.message}")
        }
    }

    /**
     * Получение ссылки на скачивание файла по пути
     * @param path Путь к файлу в хранилище
     * @return Resource с URL для скачивания
     */
    suspend fun getDownloadUrl(path: String): Resource<String> {
        return try {
            val url = firebaseStorage.reference.child(path).downloadUrl.await().toString()
            Resource.Success(url)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Ошибка получения URL: ${e.message}")
        }
    }

    /**
     * Скачивание файла во временный файл
     * @param path Путь к файлу в хранилище
     * @return Resource с файлом
     */
    suspend fun downloadFile(path: String): Resource<File> {
        val tempFile = File.createTempFile("download", null)

        return try {
            val ref = firebaseStorage.reference.child(path)
            ref.getFile(tempFile).await()
            Resource.Success(tempFile)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            tempFile.delete()
            Resource.Error("Ошибка скачивания файла: ${e.message}")
        }
    }

    /**
     * Проверка существования файла по пути
     * @param path Путь к файлу в хранилище
     * @return Resource с результатом проверки (true - существует)
     */
    suspend fun fileExists(path: String): Resource<Boolean> {
        return try {
            val metadata = firebaseStorage.reference.child(path).metadata.await()
            Resource.Success(true)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (e.message?.contains("Object does not exist") == true) {
                Resource.Success(false)
            } else {
                Resource.Error("Ошибка проверки файла: ${e.message}")
            }
        }
    }

    /**
     * Получение списка файлов в директории
     * @param directoryPath Путь к директории
     * @return Resource со списком StorageReference
     */
    suspend fun listFiles(directoryPath: String): Resource<List<StorageReference>> {
        return try {
            val result = firebaseStorage.reference.child(directoryPath).listAll().await()
            Resource.Success(result.items)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Ошибка получения списка файлов: ${e.message}")
        }
    }
}