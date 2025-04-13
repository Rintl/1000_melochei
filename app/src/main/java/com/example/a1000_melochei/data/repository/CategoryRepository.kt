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
    private val TAG = "CategoryRepository"

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

                Resource.Success(categories)
            } else {
                Resource.Error((result as Resource.Error).message ?: "Ошибка при получении категорий")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении категорий: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении категорий")
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
                    ?: return@withContext Resource.Error("Категория не найдена")

                Resource.Success(category)
            } else {
                Resource.Error((result as Resource.Error).message ?: "Ошибка при получении категории")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении категории: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении категории")
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
    ): Resource<String> = withContext(Dispatchers.IO) {
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
            val result = firestoreSource.setDocument(CATEGORIES_COLLECTION, categoryId, newCategory)

            if (result is Resource.Success) {
                Resource.Success(categoryId)
            } else {
                Resource.Error((result as Resource.Error).message ?: "Ошибка при добавлении категории")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении категории: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при добавлении категории")
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
                return@withContext categoryResult
            }

            val currentCategory = (categoryResult as Resource.Success).data

            // Обрабатываем изображение
            var imageUrl = currentCategory.imageUrl

            if (imageFile != null) {
                // Если текущее изображение существует, удаляем его
                if (imageUrl.isNotEmpty()) {
                    val deleteResult = storageSource.deleteFile(imageUrl)
                    if (deleteResult is Resource.Error) {
                        Log.w(TAG, "Ошибка при удалении старого изображения: ${deleteResult.message}")
                    }
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
            val updateResult = firestoreSource.updateDocument(
                CATEGORIES_COLLECTION,
                categoryId,
                updatedCategory
            )

            if (updateResult is Resource.Success) {
                Resource.Success(Unit)
            } else {
                Resource.Error((updateResult as Resource.Error).message ?: "Ошибка при обновлении категории")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении категории: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при обновлении категории")
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
                return@withContext categoryResult
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
                    return@withContext Resource.Error("Невозможно удалить категорию, содержащую товары")
                }
            }

            // Удаляем изображение категории, если оно есть
            if (category.imageUrl.isNotEmpty()) {
                val deleteImageResult = storageSource.deleteFile(category.imageUrl)
                if (deleteImageResult is Resource.Error) {
                    Log.w(TAG, "Ошибка при удалении изображения: ${deleteImageResult.message}")
                }
            }

            // Удаляем категорию из Firestore
            val deleteResult = firestoreSource.deleteDocument(CATEGORIES_COLLECTION, categoryId)

            if (deleteResult is Resource.Success) {
                Resource.Success(Unit)
            } else {
                Resource.Error((deleteResult as Resource.Error).message ?: "Ошибка при удалении категории")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении категории: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при удалении категории")
        }
    }

    /**
     * Получает количество категорий
     */
    suspend fun getCategoriesCount(): Resource<Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            val categoriesResult = getCategories()

            if (categoriesResult is Resource.Success) {
                Resource.Success(categoriesResult.data.size)
            } else {
                Resource.Error("Не удалось получить количество категорий")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении количества категорий: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении количества категорий")
        }
    }

    /**
     * Обновляет счетчик товаров в категории
     */
    suspend fun updateCategoryProductCount(categoryId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Получаем количество товаров в категории
            val productsResult = firestoreSource.getCollectionWithFilter(
                PRODUCTS_COLLECTION,
                "categoryId",
                categoryId
            )

            if (productsResult is Resource.Success) {
                val productsCount = productsResult.data?.documents?.size ?: 0

                // Обновляем счетчик товаров в категории
                val updateResult = firestoreSource.updateField(
                    CATEGORIES_COLLECTION,
                    categoryId,
                    "productCount",
                    productsCount
                )

                if (updateResult is Resource.Success) {
                    Resource.Success(Unit)
                } else {
                    Resource.Error((updateResult as Resource.Error).message ?:
                    "Ошибка при обновлении счетчика товаров в категории")
                }
            } else {
                Resource.Error((productsResult as Resource.Error).message ?:
                "Ошибка при получении товаров категории")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении счетчика товаров в категории: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при обновлении счетчика товаров в категории")
        }
    }

    /**
     * Переключает видимость категории
     */
    suspend fun toggleCategoryStatus(categoryId: String, isActive: Boolean): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val result = firestoreSource.updateField(
                CATEGORIES_COLLECTION,
                categoryId,
                "isActive",
                isActive
            )

            if (result is Resource.Success) {
                Resource.Success(Unit)
            } else {
                Resource.Error((result as Resource.Error).message ?: "Ошибка при изменении статуса категории")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при изменении статуса категории: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при изменении статуса категории")
        }
    }

    /**
     * Изменяет порядок категорий
     */
    suspend fun reorderCategories(categoryIds: List<String>): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Используем транзакцию Firestore для обновления порядка нескольких категорий
            val batch = firestoreSource.getBatch()

            categoryIds.forEachIndexed { index, categoryId ->
                val result = firestoreSource.updateField(
                    CATEGORIES_COLLECTION,
                    categoryId,
                    "sortOrder",
                    index
                )

                if (result is Resource.Error) {
                    return@withContext result
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при изменении порядка категорий: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при изменении порядка категорий")
        }
    }

    /**
     * Получает поток категорий как Flow
     */
    fun getCategoriesAsFlow(): Flow<Resource<List<Category>>> = flow {
        emit(Resource.Loading())
        try {
            // Первоначальная загрузка данных
            val initialData = getCategories()
            emit(initialData)

            // В реальном приложении здесь можно было бы использовать
            // Firestore's snapshotListener для обновления в реальном времени
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в потоке категорий: ${e.message}")
            emit(Resource.Error(e.message ?: "Ошибка при получении категорий"))
        }
    }
}