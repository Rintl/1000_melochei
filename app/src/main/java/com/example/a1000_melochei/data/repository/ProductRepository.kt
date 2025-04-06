package com.yourstore.app.data.repository

import android.content.Context
import android.net.Uri
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
            val productDoc = firestoreSource.getDocument(PRODUCTS_COLLECTION, productId)
            val product = productDoc.toObject(Product::class.java)?.copy(id = productId)
                ?: return@withContext Resource.Error("Товар не найден")

            // Проверяем, находится ли товар в избранном у текущего пользователя
            val isFavorite = isProductInFavorites(productId)

            Resource.Success(product.copy(isFavorite = isFavorite))
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
            val productsSnapshot = firestoreSource.getCollection(PRODUCTS_COLLECTION)
            val products = productsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)?.copy(id = doc.id)
            }

            // Получаем избранные товары для текущего пользователя
            val favoriteIds = getFavoriteProductIds()

            // Обновляем флаг isFavorite для каждого товара
            val productsWithFavorites = products.map { product ->
                product.copy(isFavorite = favoriteIds.contains(product.id))
            }

            Resource.Success(productsWithFavorites)
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
            val productsSnapshot = firestoreSource.getCollectionWithFilter(
                PRODUCTS_COLLECTION,
                "categoryId",
                categoryId
            )

            val products = productsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)?.copy(id = doc.id)
            }

            // Получаем избранные товары для текущего пользователя
            val favoriteIds = getFavoriteProductIds()

            // Обновляем флаг isFavorite для каждого товара
            val productsWithFavorites = products.map { product ->
                product.copy(isFavorite = favoriteIds.contains(product.id))
            }

            Resource.Success(productsWithFavorites)
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
            val productsSnapshot = firestoreSource.getCollectionWithFilter(
                PRODUCTS_COLLECTION,
                "categoryId",
                categoryId
            )

            // Исключаем текущий товар из результатов
            val products = productsSnapshot.documents
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
            val productsSnapshot = firestoreSource.getCollectionOrderBy(
                PRODUCTS_COLLECTION,
                "soldCount",
                true,
                limit
            )

            val products = productsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)?.copy(id = doc.id)
            }

            // Получаем избранные товары для текущего пользователя
            val favoriteIds = getFavoriteProductIds()

            // Обновляем флаг isFavorite для каждого товара
            val productsWithFavorites = products.map { product ->
                product.copy(isFavorite = favoriteIds.contains(product.id))
            }

            Resource.Success(productsWithFavorites)
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

                if (uploadResult is Resource.Success) {
                    uploadResult.data?.let { imageUrls.add(it) }
                }
            }

            // Создаем документ товара в Firestore
            val newProduct = product.copy(
                id = UUID.randomUUID().toString(),
                images = imageUrls,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val documentId = firestoreSource.addDocument(PRODUCTS_COLLECTION, newProduct.id, newProduct)
            Resource.Success(documentId)
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
            imagesToDelete.forEach { imageUrl ->
                storageSource.deleteFile(imageUrl)
            }

            // Загружаем новые изображения
            val newImageUrls = mutableListOf<String>()

            for (imageFile in newImageFiles) {
                val fileName = "products/${UUID.randomUUID()}_${imageFile.name}"
                val uploadResult = storageSource.uploadFile(fileName, imageFile)

                if (uploadResult is Resource.Success) {
                    uploadResult.data?.let { newImageUrls.add(it) }
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

            firestoreSource.updateDocument(PRODUCTS_COLLECTION, product.id, updatedProduct)
            Resource.Success(Unit)
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
            currentProduct.images.forEach { imageUrl ->
                storageSource.deleteFile(imageUrl)
            }

            // Удаляем товар из Firestore
            firestoreSource.deleteDocument(PRODUCTS_COLLECTION, productId)
            Resource.Success(Unit)
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
            firestoreSource.updateField(
                PRODUCTS_COLLECTION,
                productId,
                "availableQuantity",
                availableQuantity
            )

            firestoreSource.updateField(
                PRODUCTS_COLLECTION,
                productId,
                "updatedAt",
                System.currentTimeMillis()
            )

            Resource.Success(Unit)
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
            firestoreSource.updateField(
                PRODUCTS_COLLECTION,
                productId,
                "price",
                price
            )

            if (discountPrice != null) {
                firestoreSource.updateField(
                    PRODUCTS_COLLECTION,
                    productId,
                    "discountPrice",
                    discountPrice
                )
            } else {
                firestoreSource.updateField(
                    PRODUCTS_COLLECTION,
                    productId,
                    "discountPrice",
                    null
                )
            }

            firestoreSource.updateField(
                PRODUCTS_COLLECTION,
                productId,
                "updatedAt",
                System.currentTimeMillis()
            )

            Resource.Success(Unit)
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
            firestoreSource.updateField(
                PRODUCTS_COLLECTION,
                productId,
                "isActive",
                isActive
            )

            firestoreSource.updateField(
                PRODUCTS_COLLECTION,
                productId,
                "updatedAt",
                System.currentTimeMillis()
            )

            Resource.Success(Unit)
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
            if (isFavorite) {
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

            Resource.Success(Unit)
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
            val favoritesSnapshot = firestoreSource.getCollection(favoritePath)
            val favoriteIds = favoritesSnapshot.documents.map { it.id }

            if (favoriteIds.isEmpty()) {
                return@withContext Resource.Success(emptyList<Product>())
            }

            // Получаем товары по их ID
            val products = favoriteIds.mapNotNull { productId ->
                val productResult = getProductById(productId)
                if (productResult is Resource.Success) {
                    productResult.data
                } else {
                    null
                }
            }

            Resource.Success(products.map { it.copy(isFavorite = true) })
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
            val doc = firestoreSource.getDocument(favoritePath, productId)
            doc.exists()
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
            val favoritesSnapshot = firestoreSource.getCollection(favoritePath)
            favoritesSnapshot.documents.map { it.id }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Увеличивает счетчик просмотров товара
     */
    suspend fun incrementProductViewCount(productId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestoreSource.incrementField(
                PRODUCTS_COLLECTION,
                productId,
                "viewCount",
                1
            )
            Resource.Success(Unit)
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