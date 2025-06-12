package com.yourstore.app.data.repository

import android.util.Log
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Category
import com.yourstore.app.data.source.remote.FirestoreSource
import com.yourstore.app.data.source.remote.StorageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

/**
 * Репозиторий для управления категориями товаров.
 * Предоставляет методы для работы с категориями в Firebase Firestore и Storage.
 */
class CategoryRepository(
    private val firestoreSource: FirestoreSource,
    private val storageSource: StorageSource
) {
    private val tag = "CategoryRepository"

    // Константы для работы с Firestore
    private val CATEGORIES_COLLECTION = "categories"
    private val PRODUCTS_COLLECTION = "products"

    /**
     * Получает список всех категорий
     */
    suspend fun getCategories(): Resource<List<Category>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val result = firestoreSource.getCollection(CATEGORIES_COLLECTION)

            if (result is Resource.Success) {
                val categories = result.data?.documents?.mapNotNull { doc ->
                    doc.toObject(Category::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                Resource.Success<List<Category>>(categories)
            } else {
                result as Resource.Error
                Resource.Error<List<Category>>(result.message ?: "Ошибка при получении категорий")
            }
        } catch (e: Exception) {
            Log.e(tag, "Ошибка при получении категорий: ${e.message}")
            Resource.Error<List<Category>>(e.message ?: "Ошибка при получении категорий")
        }
    }

    /**
     * Получает категорию по её ID
     */
    suspend fun getCategoryById(categoryId: String): Resource<Category> = withContext(Dispatchers.IO) {
        return@withContext try {
            val result = firestoreSource.getDocument(CATEGORIES_COLLECTION, categoryId)

            if (result is Resource.Success) {
                val category = result.data?.toObject(Category::class.java)?.copy(id = categoryId)
                    ?: return@withContext Resource.Error<Category>("Категория не найдена")

                Resource.Success<Category>(category)
            } else {
                result as Resource.Error
                Resource.Error<Category>(result.message ?: "Ошибка при получении категории")
            }
        } catch (e: Exception) {
            Log.e(tag, "Ошибка при получении категории: ${e.message}")
            Resource.Error<Category>(e.message ?: "Ошибка при получении категории")
        }
    }

    /**
     * Добавляет новую категорию
     */
    suspend fun addCategory(
        name: String,
        description: String,
        imageFile: File? = null,
        isActive: Boolean = true
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Загружаем изображение категории, если оно предоставлено
            var imageUrl = ""
            if (imageFile != null) {
                val fileName = "categories/${UUID.randomUUID()}"
                val uploadResult = storageSource.uploadFile(fileName, imageFile)

                if (uploadResult is Resource.Success) {
                    imageUrl = uploadResult.data ?: ""
                }
            }

            // Создаем новую категорию
            val categoryId = UUID.randomUUID().toString()
            val newCategory = Category(
                id = categoryId,
                name = name,
                description = description,
                imageUrl = imageUrl,
                isActive = isActive,
                sortOrder = 0,
                productCount = 0
            )

            // Добавляем категорию в Firestore
            firestoreSource.setDocument(CATEGORIES_COLLECTION, categoryId, newCategory)

            Resource.Success<Unit>(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Ошибка при добавлении категории: ${e.message}")
            Resource.Error<Unit>(e.message ?: "Ошибка при добавлении категории")
        }
    }

    /**
     * Обновляет существующую категорию
     */
    suspend fun updateCategory(
        categoryId: String,
        name: String,
        description: String,
        imageFile: File? = null,
        isActive: Boolean
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Получаем текущую категорию
            val categoryResult = getCategoryById(categoryId)

            if (categoryResult is Resource.Error) {
                return@withContext Resource.Error<Unit>(categoryResult.message ?: "Ошибка при получении категории")
            }

            val currentCategory = (categoryResult as Resource.Success).data

            // Обрабатываем изображение
            var imageUrl = currentCategory.imageUrl

            if (imageFile != null) {
                // Если текущее изображение существует, удаляем его
                if (imageUrl.isNotEmpty()) {
                    storageSource.deleteFile(imageUrl)
                }

                // Загружаем новое изображение
                val fileName = "categories/${UUID.randomUUID()}"
                val uploadResult = storageSource.uploadFile(fileName, imageFile)

                if (uploadResult is Resource.Success) {
                    imageUrl = uploadResult.data ?: ""
                }
            }

            // Создаем обновленную категорию
            val updatedCategory = currentCategory.copy(
                name = name,
                description = description,
                imageUrl = imageUrl,
                isActive = isActive
            )

            // Обновляем категорию в Firestore
            firestoreSource.updateDocument(CATEGORIES_COLLECTION, categoryId, updatedCategory)

            Resource.Success<Unit>(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Ошибка при обновлении категории: ${e.message}")
            Resource.Error<Unit>(e.message ?: "Ошибка при обновлении категории")
        }
    }

    /**
     * Удаляет категорию
     */
    suspend fun deleteCategory(categoryId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Получаем текущую категорию
            val categoryResult = getCategoryById(categoryId)

            if (categoryResult is Resource.Error) {
                return@withContext Resource.Error<Unit>(categoryResult.message ?: "Ошибка при получении категории")
            }

            val category = (categoryResult as Resource.Success).data

            // Проверяем, есть ли товары, связанные с этой категорией
            val productsResult = firestoreSource.getCollectionWithFilter(
                PRODUCTS_COLLECTION,
                "categoryId",
                categoryId
            )

            if (productsResult is Resource.Success) {
                val productsCount = productsResult.data?.documents?.size ?: 0

                if (productsCount > 0) {
                    return@withContext Resource.Error<Unit>("Невозможно удалить категорию, содержащую товары")
                }
            }

            // Удаляем изображение категории, если оно есть
            if (category.imageUrl.isNotEmpty()) {
                storageSource.deleteFile(category.imageUrl)
            }

            // Удаляем категорию из Firestore
            firestoreSource.deleteDocument(CATEGORIES_COLLECTION, categoryId)

            Resource.Success<Unit>(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Ошибка при удалении категории: ${e.message}")
            Resource.Error<Unit>(e.message ?: "Ошибка при удалении категории")
        }
    }

    /**
     * Получает количество категорий
     */
    suspend fun getCategoriesCount(): Resource<Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            val categoriesResult = getCategories()

            if (categoriesResult is Resource.Success) {
                Resource.Success<Int>(categoriesResult.data.size)
            } else {
                Resource.Error<Int>("Не удалось получить количество категорий")
            }
        } catch (e: Exception) {
            Log.e(tag, "Ошибка при получении количества категорий: ${e.message}")
            Resource.Error<Int>(e.message ?: "Ошибка при получении количества категорий")
        }
    }

    /**
     * Переключает видимость категории
     */
    suspend fun toggleCategoryStatus(categoryId: String, isActive: Boolean): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestoreSource.updateField(
                CATEGORIES_COLLECTION,
                categoryId,
                "isActive",
                isActive
            )
            Resource.Success<Unit>(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Ошибка при изменении статуса категории: ${e.message}")
            Resource.Error<Unit>(e.message ?: "Ошибка при изменении статуса категории")
        }
    }

    /**
     * Изменяет порядок категорий
     */
    suspend fun reorderCategories(categoryIds: List<String>): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            categoryIds.forEachIndexed { index, categoryId ->
                firestoreSource.updateField(
                    CATEGORIES_COLLECTION,
                    categoryId,
                    "sortOrder",
                    index
                )
            }
            Resource.Success<Unit>(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Ошибка при изменении порядка категорий: ${e.message}")
            Resource.Error<Unit>(e.message ?: "Ошибка при изменении порядка категорий")
        }
    }
}