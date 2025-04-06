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
            val categoriesSnapshot = firestoreSource.getCollectionOrderBy(
                CATEGORIES_COLLECTION,
                "order",
                false
            )

            val categories = categoriesSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.copy(id = doc.id)
            }

            Resource.Success(categories)
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
            val categoryDoc = firestoreSource.getDocument(CATEGORIES_COLLECTION, categoryId)
            val category = categoryDoc.toObject(Category::class.java)?.copy(id = categoryId)
                ?: return@withContext Resource.Error("Категория не найдена")

            Resource.Success(category)
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
        parentCategoryId: String? = null,
        order: Int = 0
    ): Resource<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Загружаем изображение категории, если оно предоставлено
            var imageUrl = ""
            if (imageFile != null) {
                val fileName = "categories/${UUID.randomUUID()}_${imageFile.name}"
                val uploadResult = storageSource.uploadFile(fileName, imageFile)

                if (uploadResult is Resource.Success) {
                    imageUrl = uploadResult.data ?: ""
                }
            }

            // Создаем новую категорию
            val newCategory = Category(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                imageUrl = imageUrl,
                parentId = parentCategoryId,
                order = order,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Добавляем категорию в Firestore
            val documentId = firestoreSource.addDocument(
                CATEGORIES_COLLECTION,
                newCategory.id,
                newCategory
            )

            Resource.Success(documentId)
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
        deleteImage: Boolean = false,
        parentCategoryId: String? = null,
        order: Int? = null
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Получаем текущую категорию
            val categoryResult = getCategoryById(categoryId)

            if (categoryResult is Resource.Error) {
                return@withContext categoryResult
            }

            val currentCategory = (categoryResult as Resource.Success).data!!

            // Обрабатываем изображение
            var imageUrl = currentCategory.imageUrl

            if (deleteImage && imageUrl.isNotEmpty()) {
                // Удаляем текущее изображение
                storageSource.deleteFile(imageUrl)
                imageUrl = ""
            }

            if (imageFile != null) {
                // Если текущее изображение существует, удаляем его
                if (imageUrl.isNotEmpty()) {
                    storageSource.deleteFile(imageUrl)
                }

                // Загружаем новое изображение
                val fileName = "categories/${UUID.randomUUID()}_${imageFile.name}"
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
                parentId = parentCategoryId ?: currentCategory.parentId,
                order = order ?: currentCategory.order,
                updatedAt = System.currentTimeMillis()
            )

            // Обновляем категорию в Firestore
            firestoreSource.updateDocument(
                CATEGORIES_COLLECTION,
                categoryId,
                updatedCategory
            )

            Resource.Success(Unit)
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

            val category = (categoryResult as Resource.Success).data!!

            // Проверяем, есть ли товары, связанные с этой категорией
            val productsInCategory = firestoreSource.getCollectionWithFilter(
                PRODUCTS_COLLECTION,
                "categoryId",
                categoryId
            ).documents

            if (productsInCategory.isNotEmpty()) {
                return@withContext Resource.Error("Невозможно удалить категорию, содержащую товары")
            }

            // Проверяем, есть ли подкатегории
            val subcategories = firestoreSource.getCollectionWithFilter(
                CATEGORIES_COLLECTION,
                "parentId",
                categoryId
            ).documents

            if (subcategories.isNotEmpty()) {
                return@withContext Resource.Error("Невозможно удалить категорию, содержащую подкатегории")
            }

            // Удаляем изображение категории, если оно есть
            if (category.imageUrl.isNotEmpty()) {
                storageSource.deleteFile(category.imageUrl)
            }

            // Удаляем категорию из Firestore
            firestoreSource.deleteDocument(CATEGORIES_COLLECTION, categoryId)

            Resource.Success(Unit)
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
                Resource.Success(categoriesResult.data?.size ?: 0)
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
            val productsInCategory = firestoreSource.getCollectionWithFilter(
                PRODUCTS_COLLECTION,
                "categoryId",
                categoryId
            ).documents

            // Обновляем счетчик товаров в категории
            firestoreSource.updateField(
                CATEGORIES_COLLECTION,
                categoryId,
                "productCount",
                productsInCategory.size
            )

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении счетчика товаров в категории: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при обновлении счетчика товаров в категории")
        }
    }

    /**
     * Получает список подкатегорий
     */
    suspend fun getSubcategories(parentCategoryId: String): Resource<List<Category>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val subcategoriesSnapshot = firestoreSource.getCollectionWithFilter(
                CATEGORIES_COLLECTION,
                "parentId",
                parentCategoryId
            )

            val subcategories = subcategoriesSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.copy(id = doc.id)
            }

            Resource.Success(subcategories)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении подкатегорий: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении подкатегорий")
        }
    }

    /**
     * Получает список корневых категорий (без родительской категории)
     */
    suspend fun getRootCategories(): Resource<List<Category>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val categoriesSnapshot = firestoreSource.getCollectionWithNullField(
                CATEGORIES_COLLECTION,
                "parentId"
            )

            val rootCategories = categoriesSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.copy(id = doc.id)
            }

            Resource.Success(rootCategories)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении корневых категорий: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении корневых категорий")
        }
    }

    /**
     * Проверяет, является ли категория корневой
     */
    suspend fun isRootCategory(categoryId: String): Resource<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val categoryResult = getCategoryById(categoryId)

            if (categoryResult is Resource.Success) {
                Resource.Success(categoryResult.data?.parentId == null)
            } else {
                categoryResult as Resource.Error
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке категории: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при проверке категории")
        }
    }

    /**
     * Получает иерархию категорий
     */
    suspend fun getCategoryHierarchy(categoryId: String): Resource<List<Category>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val hierarchy = mutableListOf<Category>()
            var currentCategoryId: String? = categoryId

            while (currentCategoryId != null) {
                val categoryResult = getCategoryById(currentCategoryId)

                if (categoryResult is Resource.Success) {
                    val category = categoryResult.data!!
                    hierarchy.add(0, category)
                    currentCategoryId = category.parentId
                } else {
                    return@withContext categoryResult
                }
            }

            Resource.Success(hierarchy)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении иерархии категорий: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении иерархии категорий")
        }
    }

    /**
     * Получает поток категорий как Flow
     */
    fun getCategoriesAsFlow(): Flow<Resource<List<Category>>> = flow {
        emit(Resource.Loading())
        emit(getCategories())
    }
}