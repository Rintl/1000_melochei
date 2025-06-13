package com.example.a1000_melochei.data.repository

import android.net.Uri
import android.util.Log
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Category
import com.example.a1000_melochei.data.source.remote.FirestoreSource
import com.example.a1000_melochei.data.source.remote.StorageSource
import com.example.a1000_melochei.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * Репозиторий для управления категориями товаров.
 * Обеспечивает взаимодействие с Firestore и Firebase Storage для категорий.
 */
class CategoryRepository(
    private val firestoreSource: FirestoreSource,
    private val storageSource: StorageSource
) {
    private val TAG = "CategoryRepository"

    /**
     * Получает список всех категорий
     */
    suspend fun getCategories(): Resource<List<Category>> {
        return try {
            val result = firestoreSource.getCategories()
            Log.d(TAG, "Загружено категорий: ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке категорий: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает список активных категорий
     */
    suspend fun getActiveCategories(): Resource<List<Category>> {
        return try {
            val result = firestoreSource.getCategoriesByField("isActive", true)
            Log.d(TAG, "Загружено активных категорий: ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке активных категорий: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает список родительских категорий (без родителя)
     */
    suspend fun getParentCategories(): Resource<List<Category>> {
        return try {
            val result = firestoreSource.getCategoriesByField("parentId", "")
            Log.d(TAG, "Загружено родительских категорий: ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке родительских категорий: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает подкатегории для указанной родительской категории
     */
    suspend fun getSubcategories(parentId: String): Resource<List<Category>> {
        return try {
            val result = firestoreSource.getCategoriesByField("parentId", parentId)
            Log.d(TAG, "Загружено подкатегорий для $parentId: ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке подкатегорий: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает категорию по ID
     */
    suspend fun getCategoryById(categoryId: String): Resource<Category> {
        return try {
            val result = firestoreSource.getCategoryById(categoryId)
            Log.d(TAG, "Загружена категория: ${result.getDataOrNull()?.name}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке категории: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Добавляет новую категорию
     */
    suspend fun addCategory(category: Category): Resource<String> {
        return try {
            if (!category.isValid()) {
                return Resource.Error("Данные категории некорректны")
            }

            val result = firestoreSource.addCategory(category)
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Категория добавлена: ${category.name}")
                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка при добавлении категории: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении категории: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Обновляет категорию
     */
    suspend fun updateCategory(category: Category): Resource<Unit> {
        return try {
            if (!category.isValid()) {
                return Resource.Error("Данные категории некорректны")
            }

            val result = firestoreSource.updateCategory(category.withUpdatedTime())
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Категория обновлена: ${category.name}")
                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка при обновлении категории: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении категории: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Удаляет категорию
     */
    suspend fun deleteCategory(categoryId: String): Resource<Unit> {
        return try {
            // Сначала проверяем, есть ли товары в категории
            val categoryResult = getCategoryById(categoryId)
            if (categoryResult is Resource.Error) {
                return Resource.Error("Категория не найдена")
            }

            val category = categoryResult.getDataOrNull()!!

            if (category.productCount > 0) {
                return Resource.Error("Нельзя удалить категорию с товарами")
            }

            // Проверяем, есть ли подкатегории
            val subcategoriesResult = getSubcategories(categoryId)
            if (subcategoriesResult is Resource.Success && subcategoriesResult.data.isNotEmpty()) {
                return Resource.Error("Нельзя удалить категорию с подкатегориями")
            }

            val result = firestoreSource.deleteCategory(categoryId)
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Категория удалена: ${category.name}")

                    // Удаляем изображение категории
                    if (category.imageUrl.isNotEmpty()) {
                        storageSource.deleteImage(category.imageUrl)
                    }

                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка при удалении категории: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении категории: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Загружает изображение категории
     */
    suspend fun uploadCategoryImage(imageUri: Uri): Resource<String> {
        return try {
            val result = storageSource.uploadImage(imageUri, Constants.STORAGE_CATEGORIES_FOLDER)
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Изображение категории загружено: ${result.data}")
                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка при загрузке изображения категории: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке изображения категории: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Загружает изображение категории из файла
     */
    suspend fun uploadCategoryImageFromFile(imageFile: File): Resource<String> {
        return try {
            val result = storageSource.uploadImageFromFile(imageFile, Constants.STORAGE_CATEGORIES_FOLDER)
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Изображение категории загружено из файла: ${result.data}")
                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка при загрузке изображения категории из файла: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке изображения категории из файла: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Удаляет изображение
     */
    suspend fun deleteImage(imageUrl: String): Resource<Unit> {
        return try {
            val result = storageSource.deleteImage(imageUrl)
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Изображение удалено: $imageUrl")
                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка при удалении изображения: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении изображения: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Поиск категорий по названию
     */
    suspend fun searchCategories(query: String): Resource<List<Category>> {
        return try {
            val result = firestoreSource.searchCategories(query)
            Log.d(TAG, "Найдено категорий по запросу '$query': ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при поиске категорий: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Изменяет статус активности категории
     */
    suspend fun updateCategoryStatus(categoryId: String, isActive: Boolean): Resource<Unit> {
        return try {
            val categoryResult = getCategoryById(categoryId)
            when (categoryResult) {
                is Resource.Success -> {
                    val updatedCategory = categoryResult.data.withActiveStatus(isActive)
                    updateCategory(updatedCategory)
                }
                is Resource.Error -> categoryResult
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении статуса категории: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Обновляет счетчик товаров в категории
     */
    suspend fun updateProductCount(categoryId: String, change: Int): Resource<Unit> {
        return try {
            val categoryResult = getCategoryById(categoryId)
            when (categoryResult) {
                is Resource.Success -> {
                    val currentCount = categoryResult.data.productCount
                    val newCount = (currentCount + change).coerceAtLeast(0)
                    val updatedCategory = categoryResult.data.withProductCount(newCount)
                    updateCategory(updatedCategory)
                }
                is Resource.Error -> categoryResult
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении счетчика товаров: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Обновляет порядок сортировки категорий
     */
    suspend fun updateCategoriesOrder(categories: List<Category>): Resource<Unit> {
        return try {
            var successCount = 0
            var errorCount = 0

            categories.forEachIndexed { index, category ->
                val updatedCategory = category.copy(
                    sortOrder = index,
                    updatedAt = System.currentTimeMillis()
                )
                val updateResult = updateCategory(updatedCategory)
                if (updateResult is Resource.Success) {
                    successCount++
                } else {
                    errorCount++
                }
            }

            Log.d(TAG, "Обновление порядка категорий: успешно $successCount, ошибок $errorCount")

            if (errorCount == 0) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Обновлено $successCount из ${categories.size} категорий")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении порядка категорий: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает категории с товарами (только те, в которых есть товары)
     */
    suspend fun getCategoriesWithProducts(): Resource<List<Category>> {
        return try {
            val allCategoriesResult = getActiveCategories()
            when (allCategoriesResult) {
                is Resource.Success -> {
                    val categoriesWithProducts = allCategoriesResult.data.filter { it.productCount > 0 }
                    Log.d(TAG, "Категорий с товарами: ${categoriesWithProducts.size}")
                    Resource.Success(categoriesWithProducts)
                }
                is Resource.Error -> allCategoriesResult
                is Resource.Loading -> allCategoriesResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке категорий с товарами: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает поток категорий в реальном времени
     */
    fun getCategoriesFlow(): Flow<Resource<List<Category>>> = flow {
        try {
            firestoreSource.getCategoriesFlow().collect { resource ->
                emit(resource)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в потоке категорий: ${e.message}", e)
            emit(Resource.Error(e.message ?: "Неизвестная ошибка"))
        }
    }

    /**
     * Получает статистику по категориям
     */
    suspend fun getCategoriesStats(): Resource<Map<String, Any>> {
        return try {
            val categoriesResult = getCategories()
            when (categoriesResult) {
                is Resource.Success -> {
                    val categories = categoriesResult.data
                    val stats = mapOf(
                        "totalCategories" to categories.size,
                        "activeCategories" to categories.count { it.isActive },
                        "parentCategories" to categories.count { it.isRootCategory() },
                        "subcategories" to categories.count { it.isSubcategory() },
                        "categoriesWithProducts" to categories.count { it.productCount > 0 },
                        "emptyCate­gories" to categories.count { it.productCount == 0 }
                    )
                    Log.d(TAG, "Статистика категорий: $stats")
                    Resource.Success(stats)
                }
                is Resource.Error -> Resource.Error(categoriesResult.message ?: "Ошибка загрузки категорий")
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении статистики категорий: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Проверяет уникальность названия категории
     */
    suspend fun isCategoryNameUnique(name: String, excludeCategoryId: String? = null): Boolean {
        return try {
            val categoriesResult = getCategories()
            when (categoriesResult) {
                is Resource.Success -> {
                    val categories = categoriesResult.data
                    val duplicateCategory = categories.find {
                        it.name.equals(name, ignoreCase = true) && it.id != excludeCategoryId
                    }
                    duplicateCategory == null
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке уникальности названия: ${e.message}", e)
            false
        }
    }
}