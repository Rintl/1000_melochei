package com.example.a1000_melochei.data.repository

import android.net.Uri
import android.util.Log
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Product
import com.example.a1000_melochei.data.source.remote.FirestoreSource
import com.example.a1000_melochei.data.source.remote.StorageSource
import com.example.a1000_melochei.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * Репозиторий для управления товарами.
 * Обеспечивает взаимодействие с Firestore и Firebase Storage для товаров.
 */
class ProductRepository(
    private val firestoreSource: FirestoreSource,
    private val storageSource: StorageSource
) {
    private val TAG = "ProductRepository"

    /**
     * Получает список всех товаров
     */
    suspend fun getAllProducts(): Resource<List<Product>> {
        return try {
            val result = firestoreSource.getProducts()
            Log.d(TAG, "Загружено товаров: ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке товаров: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает список активных товаров
     */
    suspend fun getActiveProducts(): Resource<List<Product>> {
        return try {
            val result = firestoreSource.getProductsByField("isActive", true)
            Log.d(TAG, "Загружено активных товаров: ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке активных товаров: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает товары по категории
     */
    suspend fun getProductsByCategory(categoryId: String): Resource<List<Product>> {
        return try {
            val result = firestoreSource.getProductsByField("categoryId", categoryId)
            Log.d(TAG, "Загружено товаров в категории $categoryId: ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке товаров категории: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает товар по ID
     */
    suspend fun getProductById(productId: String): Resource<Product> {
        return try {
            val result = firestoreSource.getProductById(productId)
            Log.d(TAG, "Загружен товар: ${result.getDataOrNull()?.name}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке товара: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Добавляет новый товар
     */
    suspend fun addProduct(product: Product): Resource<String> {
        return try {
            if (!product.isValid()) {
                return Resource.Error("Данные товара некорректны")
            }

            val result = firestoreSource.addProduct(product)
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Товар добавлен: ${product.name}")

                    // Увеличиваем счетчик товаров в категории
                    updateCategoryProductCount(product.categoryId, 1)

                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка при добавлении товара: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении товара: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Обновляет товар
     */
    suspend fun updateProduct(product: Product): Resource<Unit> {
        return try {
            if (!product.isValid()) {
                return Resource.Error("Данные товара некорректны")
            }

            val result = firestoreSource.updateProduct(product.withUpdatedTime())
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Товар обновлен: ${product.name}")
                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка при обновлении товара: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении товара: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Удаляет товар
     */
    suspend fun deleteProduct(productId: String): Resource<Unit> {
        return try {
            // Сначала получаем информацию о товаре
            val productResult = getProductById(productId)
            if (productResult is Resource.Error) {
                return Resource.Error("Товар не найден")
            }

            val product = productResult.getDataOrNull()!!

            val result = firestoreSource.deleteProduct(productId)
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Товар удален: ${product.name}")

                    // Удаляем изображения товара
                    product.images.forEach { imageUrl ->
                        storageSource.deleteImage(imageUrl)
                    }

                    // Уменьшаем счетчик товаров в категории
                    updateCategoryProductCount(product.categoryId, -1)

                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка при удалении товара: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении товара: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Загружает изображение товара
     */
    suspend fun uploadProductImage(imageUri: Uri): Resource<String> {
        return try {
            val result = storageSource.uploadImage(imageUri, Constants.STORAGE_PRODUCTS_FOLDER)
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Изображение товара загружено: ${result.data}")
                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка при загрузке изображения товара: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке изображения товара: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Загружает изображение товара из файла
     */
    suspend fun uploadProductImageFromFile(imageFile: File): Resource<String> {
        return try {
            val result = storageSource.uploadImageFromFile(imageFile, Constants.STORAGE_PRODUCTS_FOLDER)
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Изображение товара загружено из файла: ${result.data}")
                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка при загрузке изображения товара из файла: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке изображения товара из файла: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Удаляет изображение товара
     */
    suspend fun deleteProductImage(imageUrl: String): Resource<Unit> {
        return try {
            val result = storageSource.deleteImage(imageUrl)
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Изображение товара удалено: $imageUrl")
                    result
                }
                is Resource.Error -> {
                    Log.e(TAG, "Ошибка при удалении изображения товара: ${result.message}")
                    result
                }
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении изображения товара: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Поиск товаров по названию
     */
    suspend fun searchProducts(query: String): Resource<List<Product>> {
        return try {
            val result = firestoreSource.searchProducts(query)
            Log.d(TAG, "Найдено товаров по запросу '$query': ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при поиске товаров: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает рекомендуемые товары
     */
    suspend fun getFeaturedProducts(): Resource<List<Product>> {
        return try {
            val result = firestoreSource.getProductsByField("isFeatured", true)
            Log.d(TAG, "Загружено рекомендуемых товаров: ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке рекомендуемых товаров: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает товары с низким остатком
     */
    suspend fun getLowStockProducts(): Resource<List<Product>> {
        return try {
            val allProductsResult = getAllProducts()
            when (allProductsResult) {
                is Resource.Success -> {
                    val lowStockProducts = allProductsResult.data.filter { it.quantity in 1..5 }
                    Log.d(TAG, "Товаров с низким остатком: ${lowStockProducts.size}")
                    Resource.Success(lowStockProducts)
                }
                is Resource.Error -> allProductsResult
                is Resource.Loading -> allProductsResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке товаров с низким остатком: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает товары, которых нет в наличии
     */
    suspend fun getOutOfStockProducts(): Resource<List<Product>> {
        return try {
            val allProductsResult = getAllProducts()
            when (allProductsResult) {
                is Resource.Success -> {
                    val outOfStockProducts = allProductsResult.data.filter { it.quantity <= 0 }
                    Log.d(TAG, "Товаров нет в наличии: ${outOfStockProducts.size}")
                    Resource.Success(outOfStockProducts)
                }
                is Resource.Error -> allProductsResult
                is Resource.Loading -> allProductsResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке товаров без остатка: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Обновляет количество товара
     */
    suspend fun updateProductQuantity(productId: String, newQuantity: Int): Resource<Unit> {
        return try {
            val productResult = getProductById(productId)
            when (productResult) {
                is Resource.Success -> {
                    val updatedProduct = productResult.data.withQuantity(newQuantity)
                    updateProduct(updatedProduct)
                }
                is Resource.Error -> productResult
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении количества товара: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Изменяет статус активности товара
     */
    suspend fun updateProductStatus(productId: String, isActive: Boolean): Resource<Unit> {
        return try {
            val productResult = getProductById(productId)
            when (productResult) {
                is Resource.Success -> {
                    val updatedProduct = productResult.data.copy(
                        isActive = isActive,
                        updatedAt = System.currentTimeMillis()
                    )
                    updateProduct(updatedProduct)
                }
                is Resource.Error -> productResult
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении статуса товара: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Увеличивает счетчик просмотров товара
     */
    suspend fun incrementProductViews(productId: String): Resource<Unit> {
        return try {
            val productResult = getProductById(productId)
            when (productResult) {
                is Resource.Success -> {
                    val updatedProduct = productResult.data.withIncrementedViews()
                    updateProduct(updatedProduct)
                }
                is Resource.Error -> productResult
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при увеличении счетчика просмотров: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает поток товаров в реальном времени
     */
    fun getProductsFlow(): Flow<Resource<List<Product>>> = flow {
        try {
            firestoreSource.getProductsFlow().collect { resource ->
                emit(resource)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в потоке товаров: ${e.message}", e)
            emit(Resource.Error(e.message ?: "Неизвестная ошибка"))
        }
    }

    /**
     * Обновляет счетчик товаров в категории
     */
    private suspend fun updateCategoryProductCount(categoryId: String, change: Int) {
        try {
            firestoreSource.updateCategoryProductCount(categoryId, change)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении счетчика товаров в категории: ${e.message}", e)
        }
    }

    /**
     * Массовое обновление цен товаров
     */
    suspend fun bulkUpdatePrices(priceUpdates: Map<String, Double>): Resource<Unit> {
        return try {
            var successCount = 0
            var errorCount = 0

            priceUpdates.forEach { (productId, newPrice) ->
                val productResult = getProductById(productId)
                if (productResult is Resource.Success) {
                    val updatedProduct = productResult.data.copy(
                        price = newPrice,
                        updatedAt = System.currentTimeMillis()
                    )
                    val updateResult = updateProduct(updatedProduct)
                    if (updateResult is Resource.Success) {
                        successCount++
                    } else {
                        errorCount++
                    }
                } else {
                    errorCount++
                }
            }

            Log.d(TAG, "Массовое обновление цен: успешно $successCount, ошибок $errorCount")

            if (errorCount == 0) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Обновлено $successCount из ${priceUpdates.size} товаров")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при массовом обновлении цен: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }
}