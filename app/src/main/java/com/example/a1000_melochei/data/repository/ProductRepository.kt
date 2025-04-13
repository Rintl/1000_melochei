package com.yourstore.app.data.repository

import android.util.Log
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Product
import com.yourstore.app.data.source.remote.FirestoreSource
import com.yourstore.app.data.source.remote.StorageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

/**
 * Репозиторий для управления данными товаров.
 * Предоставляет методы для работы с товарами в Firebase Firestore и Storage.
 */
class ProductRepository(
    private val firestoreSource: FirestoreSource,
    private val storageSource: StorageSource
) {
    private val TAG = "ProductRepository"

    // Константы для работы с Firestore
    private val PRODUCTS_COLLECTION = "products"
    private val FAVORITES_COLLECTION = "favorites"

    /**
     * Получает товар по его ID
     */
    suspend fun getProductById(productId: String): Resource<Product> = withContext(Dispatchers.IO) {
        return@withContext try {
            val documentResult = firestoreSource.getDocument(PRODUCTS_COLLECTION, productId)

            if (documentResult is Resource.Success) {
                val document = documentResult.data
                if (document != null && document.exists()) {
                    val product = document.toObject(Product::class.java)?.copy(id = productId)
                    if (product != null) {
                        // Проверяем, находится ли товар в избранном у текущего пользователя
                        val isFavorite = isProductInFavorites(productId)
                        Resource.Success(product.copy(isFavorite = isFavorite))
                    } else {
                        Resource.Error("Ошибка при преобразовании данных товара")
                    }
                } else {
                    Resource.Error("Товар не найден")
                }
            } else {
                Resource.Error((documentResult as Resource.Error).message ?: "Ошибка при получении товара")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении товара: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении товара")
        }
    }

    /**
     * Получает список всех товаров
     */
    suspend fun getAllProducts(): Resource<List<Product>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val collectionResult = firestoreSource.getCollection(PRODUCTS_COLLECTION)

            if (collectionResult is Resource.Success) {
                val snapshot = collectionResult.data
                if (snapshot != null) {
                    val products = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    }

                    // Получаем избранные товары для текущего пользователя
                    val favoriteIds = getFavoriteProductIds()

                    // Обновляем флаг isFavorite для каждого товара
                    val productsWithFavorites = products.map { product ->
                        product.copy(isFavorite = favoriteIds.contains(product.id))
                    }

                    Resource.Success(productsWithFavorites)
                } else {
                    Resource.Success(emptyList())
                }
            } else {
                Resource.Error((collectionResult as Resource.Error).message ?: "Ошибка при получении списка товаров")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении списка товаров: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении списка товаров")
        }
    }

    /**
     * Получает список товаров по категории
     */
    suspend fun getProductsByCategory(categoryId: String): Resource<List<Product>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val queryResult = firestoreSource.getCollectionWithFilter(
                PRODUCTS_COLLECTION,
                "categoryId",
                categoryId
            )

            if (queryResult is Resource.Success) {
                val snapshot = queryResult.data
                if (snapshot != null) {
                    val products = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    }

                    // Получаем избранные товары для текущего пользователя
                    val favoriteIds = getFavoriteProductIds()

                    // Обновляем флаг isFavorite для каждого товара
                    val productsWithFavorites = products.map { product ->
                        product.copy(isFavorite = favoriteIds.contains(product.id))
                    }

                    Resource.Success(productsWithFavorites)
                } else {
                    Resource.Success(emptyList())
                }
            } else {
                Resource.Error((queryResult as Resource.Error).message ?: "Ошибка при получении товаров по категории")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении товаров по категории: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении товаров по категории")
        }
    }

    /**
     * Получает список похожих товаров
     */
    suspend fun getSimilarProducts(categoryId: String, currentProductId: String, limit: Int): Resource<List<Product>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val queryResult = firestoreSource.getCollectionWithFilter(
                PRODUCTS_COLLECTION,
                "categoryId",
                categoryId
            )

            if (queryResult is Resource.Success) {
                val snapshot = queryResult.data
                if (snapshot != null) {
                    // Исключаем текущий товар из результатов
                    val products = snapshot.documents
                        .mapNotNull { doc ->
                            if (doc.id != currentProductId) {
                                doc.toObject(Product::class.java)?.copy(id = doc.id)
                            } else {
                                null
                            }
                        }
                        .take(limit)

                    // Получаем избранные товары для текущего пользователя
                    val favoriteIds = getFavoriteProductIds()

                    // Обновляем флаг isFavorite для каждого товара
                    val productsWithFavorites = products.map { product ->
                        product.copy(isFavorite = favoriteIds.contains(product.id))
                    }

                    Resource.Success(productsWithFavorites)
                } else {
                    Resource.Success(emptyList())
                }
            } else {
                Resource.Error((queryResult as Resource.Error).message ?: "Ошибка при получении похожих товаров")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении похожих товаров: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении похожих товаров")
        }
    }

    /**
     * Получает список популярных товаров
     */
    suspend fun getPopularProducts(limit: Int = 10): Resource<List<Product>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val queryResult = firestoreSource.getCollectionOrderBy(
                PRODUCTS_COLLECTION,
                "soldCount",
                true,
                limit
            )

            if (queryResult is Resource.Success) {
                val snapshot = queryResult.data
                if (snapshot != null) {
                    val products = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    }

                    // Получаем избранные товары для текущего пользователя
                    val favoriteIds = getFavoriteProductIds()

                    // Обновляем флаг isFavorite для каждого товара
                    val productsWithFavorites = products.map { product ->
                        product.copy(isFavorite = favoriteIds.contains(product.id))
                    }

                    Resource.Success(productsWithFavorites)
                } else {
                    Resource.Success(emptyList())
                }
            } else {
                Resource.Error((queryResult as Resource.Error).message ?: "Ошибка при получении популярных товаров")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении популярных товаров: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении популярных товаров")
        }
    }

    /**
     * Поиск товаров по заданным критериям
     */
    suspend fun searchProducts(
        query: String = "",
        categoryId: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        inStockOnly: Boolean = false,
        onSaleOnly: Boolean = false
    ): Resource<List<Product>> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Получаем все товары и фильтруем локально
            // В реальном приложении лучше использовать Firestore Query для более эффективного поиска
            val allProductsResult = getAllProducts()

            if (allProductsResult is Resource.Success) {
                val allProducts = allProductsResult.data ?: emptyList()

                // Применяем фильтры
                val filteredProducts = allProducts.filter { product ->
                    var matches = true

                    // Фильтр по поисковому запросу
                    if (query.isNotEmpty()) {
                        matches = matches && (
                                product.name.contains(query, ignoreCase = true) ||
                                        product.description.contains(query, ignoreCase = true) ||
                                        product.sku.contains(query, ignoreCase = true)
                                )
                    }

                    // Фильтр по категории
                    if (categoryId != null) {
                        matches = matches && (product.categoryId == categoryId)
                    }

                    // Фильтр по цене
                    val effectivePrice = product.discountPrice ?: product.price
                    if (minPrice != null) {
                        matches = matches && (effectivePrice >= minPrice)
                    }
                    if (maxPrice != null) {
                        matches = matches && (effectivePrice <= maxPrice)
                    }

                    // Фильтр по наличию
                    if (inStockOnly) {
                        matches = matches && (product.availableQuantity > 0)
                    }

                    // Фильтр по акциям
                    if (onSaleOnly) {
                        matches = matches && (product.discountPrice != null && product.discountPrice < product.price)
                    }

                    matches
                }

                Resource.Success(filteredProducts)
            } else {
                allProductsResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при поиске товаров: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при поиске товаров")
        }
    }

    /**
     * Добавляет новый товар
     */
    suspend fun addProduct(
        product: Product,
        imageFiles: List<File>
    ): Resource<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Загружаем изображения в Storage
            val imageUrls = mutableListOf<String>()

            for (imageFile in imageFiles) {
                val fileName = "products/${UUID.randomUUID()}_${imageFile.name}"
                val uploadResult = storageSource.uploadFile(fileName, imageFile)

                if (uploadResult is Resource.Success && uploadResult.data != null) {
                    imageUrls.add(uploadResult.data)
                }
            }

            // Создаем документ товара в Firestore
            val productId = UUID.randomUUID().toString()
            val newProduct = product.copy(
                id = productId,
                images = imageUrls,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val result = firestoreSource.setDocument(PRODUCTS_COLLECTION, productId, newProduct)

            if (result is Resource.Success) {
                Resource.Success(productId)
            } else {
                Resource.Error((result as Resource.Error).message ?: "Ошибка при добавлении товара")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении товара: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при добавлении товара")
        }
    }

    /**
     * Обновляет существующий товар
     */
    suspend fun updateProduct(
        product: Product,
        newImageFiles: List<File> = emptyList(),
        imagesToDelete: List<String> = emptyList()
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Получаем текущий товар
            val currentProductResult = getProductById(product.id)

            if (currentProductResult is Resource.Error) {
                return@withContext currentProductResult
            }

            val currentProduct = (currentProductResult as Resource.Success).data!!

            // Удаляем указанные изображения
            for (imageUrl in imagesToDelete) {
                storageSource.deleteFile(imageUrl)
            }

            // Загружаем новые изображения
            val newImageUrls = mutableListOf<String>()

            for (imageFile in newImageFiles) {
                val fileName = "products/${UUID.randomUUID()}_${imageFile.name}"
                val uploadResult = storageSource.uploadFile(fileName, imageFile)

                if (uploadResult is Resource.Success && uploadResult.data != null) {
                    newImageUrls.add(uploadResult.data)
                }
            }

            // Формируем обновленный список изображений
            val updatedImages = currentProduct.images
                .filter { it !in imagesToDelete }
                .toMutableList()
                .apply { addAll(newImageUrls) }

            // Обновляем товар в Firestore
            val updatedProduct = product.copy(
                images = updatedImages,
                updatedAt = System.currentTimeMillis()
            )

            val result = firestoreSource.updateDocument(PRODUCTS_COLLECTION, product.id, updatedProduct)

            if (result is Resource.Success) {
                Resource.Success(Unit)
            } else {
                Resource.Error((result as Resource.Error).message ?: "Ошибка при обновлении товара")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении товара: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при обновлении товара")
        }
    }

    /**
     * Удаляет товар
     */
    suspend fun deleteProduct(productId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Получаем текущий товар
            val currentProductResult = getProductById(productId)

            if (currentProductResult is Resource.Error) {
                return@withContext currentProductResult
            }

            val currentProduct = (currentProductResult as Resource.Success).data!!

            // Удаляем все изображения товара
            for (imageUrl in currentProduct.images) {
                storageSource.deleteFile(imageUrl)
            }

            // Удаляем товар из Firestore
            val result = firestoreSource.deleteDocument(PRODUCTS_COLLECTION, productId)

            if (result is Resource.Success) {
                Resource.Success(Unit)
            } else {
                Resource.Error((result as Resource.Error).message ?: "Ошибка при удалении товара")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении товара: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при удалении товара")
        }
    }

    /**
     * Обновляет наличие товара
     */
    suspend fun updateProductAvailability(
        productId: String,
        availableQuantity: Int
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val updates = mapOf(
                "availableQuantity" to availableQuantity,
                "updatedAt" to System.currentTimeMillis()
            )

            val result = firestoreSource.updateDocument(PRODUCTS_COLLECTION, productId, updates)

            if (result is Resource.Success) {
                Resource.Success(Unit)
            } else {
                Resource.Error((result as Resource.Error).message ?: "Ошибка при обновлении наличия товара")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении наличия товара: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при обновлении наличия товара")
        }
    }

    /**
     * Обновляет цену товара
     */
    suspend fun updateProductPrice(
        productId: String,
        price: Double,
        discountPrice: Double? = null
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val updates = mutableMapOf<String, Any?>(
                "price" to price,
                "updatedAt" to System.currentTimeMillis()
            )

            // Добавляем или удаляем скидочную цену
            updates["discountPrice"] = discountPrice

            val result = firestoreSource.updateDocument(PRODUCTS_COLLECTION, productId, updates)

            if (result is Resource.Success) {
                Resource.Success(Unit)
            } else {
                Resource.Error((result as Resource.Error).message ?: "Ошибка при обновлении цены товара")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении цены товара: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при обновлении цены товара")
        }
    }

    /**
     * Устанавливает статус активности товара
     */
    suspend fun setProductActive(
        productId: String,
        isActive: Boolean
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val updates = mapOf(
                "isActive" to isActive,
                "updatedAt" to System.currentTimeMillis()
            )

            val result = firestoreSource.updateDocument(PRODUCTS_COLLECTION, productId, updates)

            if (result is Resource.Success) {
                Resource.Success(Unit)
            } else {
                Resource.Error((result as Resource.Error).message ?: "Ошибка при обновлении статуса товара")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении статуса товара: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при обновлении статуса товара")
        }
    }

    /**
     * Добавляет/удаляет товар из избранного
     */
    suspend fun setProductFavorite(
        productId: String,
        isFavorite: Boolean
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        val userId = firestoreSource.getCurrentUserId()
            ?: return@withContext Resource.Error("Пользователь не авторизован")

        val favoritePath = "$FAVORITES_COLLECTION/$userId/products"

        return@withContext try {
            val result = if (isFavorite) {
                // Добавляем товар в избранное
                firestoreSource.setDocument(
                    favoritePath,
                    productId,
                    mapOf(
                        "productId" to productId,
                        "addedAt" to System.currentTimeMillis()
                    )
                )
            } else {
                // Удаляем товар из избранного
                firestoreSource.deleteDocument(favoritePath, productId)
            }

            if (result is Resource.Success) {
                Resource.Success(Unit)
            } else {
                Resource.Error((result as Resource.Error).message ?: "Ошибка при обновлении избранного")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении избранного: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при обновлении избранного")
        }
    }

    /**
     * Получает список избранных товаров
     */
    suspend fun getFavoriteProducts(): Resource<List<Product>> = withContext(Dispatchers.IO) {
        val userId = firestoreSource.getCurrentUserId()
            ?: return@withContext Resource.Error("Пользователь не авторизован")

        val favoritePath = "$FAVORITES_COLLECTION/$userId/products"

        return@withContext try {
            // Получаем список ID избранных товаров
            val favoritesResult = firestoreSource.getCollection(favoritePath)

            if (favoritesResult is Resource.Success) {
                val snapshot = favoritesResult.data
                if (snapshot != null) {
                    val favoriteIds = snapshot.documents.map { it.id }

                    if (favoriteIds.isEmpty()) {
                        return@withContext Resource.Success(emptyList<Product>())
                    }

                    // Получаем товары по их ID
                    val products = mutableListOf<Product>()
                    for (productId in favoriteIds) {
                        val productResult = getProductById(productId)
                        if (productResult is Resource.Success && productResult.data != null) {
                            products.add(productResult.data)
                        }
                    }

                    Resource.Success(products.map { it.copy(isFavorite = true) })
                } else {
                    Resource.Success(emptyList())
                }
            } else {
                Resource.Error((favoritesResult as Resource.Error).message ?: "Ошибка при получении избранных товаров")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении избранных товаров: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении избранных товаров")
        }
    }

    /**
     * Проверяет, находится ли товар в избранном
     */
    private suspend fun isProductInFavorites(productId: String): Boolean {
        val userId = firestoreSource.getCurrentUserId() ?: return false
        val favoritePath = "$FAVORITES_COLLECTION/$userId/products"

        return try {
            val docResult = firestoreSource.getDocument(favoritePath, productId)
            if (docResult is Resource.Success && docResult.data != null) {
                docResult.data.exists()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Получает список ID избранных товаров
     */
    private suspend fun getFavoriteProductIds(): Set<String> {
        val userId = firestoreSource.getCurrentUserId() ?: return emptySet()
        val favoritePath = "$FAVORITES_COLLECTION/$userId/products"

        return try {
            val favoritesResult = firestoreSource.getCollection(favoritePath)
            if (favoritesResult is Resource.Success && favoritesResult.data != null) {
                favoritesResult.data.documents.map { it.id }.toSet()
            } else {
                emptySet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Увеличивает счетчик просмотров товара
     */
    suspend fun incrementProductViewCount(productId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Получаем текущее значение счетчика
            val productResult = getProductById(productId)

            if (productResult is Resource.Success && productResult.data != null) {
                val product = productResult.data
                val viewCount = product.viewCount + 1

                // Обновляем счетчик
                val updates = mapOf("viewCount" to viewCount)
                val result = firestoreSource.updateDocument(PRODUCTS_COLLECTION, productId, updates)

                if (result is Resource.Success) {
                    Resource.Success(Unit)
                } else {
                    Resource.Error((result as Resource.Error).message ?: "Ошибка при увеличении счетчика просмотров")
                }
            } else {
                Resource.Error("Товар не найден")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при увеличении счетчика просмотров: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при увеличении счетчика просмотров")
        }
    }

    /**
     * Получает статистику товаров для административной панели
     */
    suspend fun getProductsStats(): Resource<ProductsStats> = withContext(Dispatchers.IO) {
        return@withContext try {
            val allProductsResult = getAllProducts()

            if (allProductsResult is Resource.Success) {
                val products = allProductsResult.data ?: emptyList()

                val totalProducts = products.size
                val outOfStockProducts = products.count { it.availableQuantity <= 0 }
                val lowStockProducts = products.count { it.availableQuantity in 1..5 }

                Resource.Success(
                    ProductsStats(
                        totalProducts = totalProducts,
                        outOfStockProducts = outOfStockProducts,
                        lowStockProducts = lowStockProducts
                    )
                )
            } else {
                Resource.Error("Не удалось получить статистику товаров")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении статистики товаров: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении статистики товаров")
        }
    }

    /**
     * Класс для хранения статистики товаров
     */
    data class ProductsStats(
        val totalProducts: Int,
        val outOfStockProducts: Int,
        val lowStockProducts: Int
    )

    /**
     * Получает поток товаров как Flow
     */
    fun getProductsAsFlow(): Flow<Resource<List<Product>>> = flow {
        emit(Resource.Loading())
        emit(getAllProducts())
    }
}