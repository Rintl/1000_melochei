package com.yourstore.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Product
import com.yourstore.app.data.source.remote.FirestoreSource
import com.yourstore.app.data.source.remote.StorageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

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
    private companion object {
        const val PRODUCTS_COLLECTION = "products"
        const val FAVORITES_COLLECTION = "favorites"
        const val DEFAULT_PAGE_SIZE = 20
    }

    /**
     * Получает товар по его ID
     * @param productId ID товара
     * @return Resource с данными товара или сообщением об ошибке
     */
    suspend fun getProductById(productId: String): Resource<Product> = withContext(Dispatchers.IO) {
        return@withContext try {
            val document = firestoreSource.getDocument(PRODUCTS_COLLECTION, productId)

            if (document.exists()) {
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
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении товара: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при получении товара")
        }
    }

    /**
     * Получает список всех товаров с пагинацией
     * @param page Номер страницы (начиная с 0)
     * @param pageSize Размер страницы
     * @return Resource со списком товаров или сообщением об ошибке
     */
    suspend fun getAllProducts(
        page: Int = 0,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): Resource<List<Product>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val query = FirebaseFirestore.getInstance()
                .collection(PRODUCTS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageSize.toLong())

            // Применяем стартовую позицию только для страниц после первой
            val finalQuery = if (page > 0) {
                // Здесь должна быть более сложная логика с использованием startAfter,
                // но для упрощения мы пропустим это
                query
            } else {
                query
            }

            val snapshot = finalQuery.get().await()
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
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении списка товаров: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при получении списка товаров")
        }
    }

    /**
     * Получает список товаров по категории с пагинацией
     * @param categoryId ID категории
     * @param page Номер страницы (начиная с 0)
     * @param pageSize Размер страницы
     * @return Resource со списком товаров или сообщением об ошибке
     */
    suspend fun getProductsByCategory(
        categoryId: String,
        page: Int = 0,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): Resource<List<Product>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = firestoreSource.getCollectionWithFilter(
                PRODUCTS_COLLECTION,
                "categoryId",
                categoryId
            )

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
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении товаров по категории: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при получении товаров по категории")
        }
    }

    /**
     * Получает список похожих товаров
     * @param categoryId ID категории
     * @param currentProductId ID текущего товара (для исключения из результатов)
     * @param limit Максимальное количество товаров в результате
     * @return Resource со списком товаров или сообщением об ошибке
     */
    suspend fun getSimilarProducts(
        categoryId: String,
        currentProductId: String,
        limit: Int = 5
    ): Resource<List<Product>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = firestoreSource.getCollectionWithFilter(
                PRODUCTS_COLLECTION,
                "categoryId",
                categoryId
            )

            val allProducts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)?.copy(id = doc.id)
            }

            // Фильтруем текущий товар
            val similarProducts = allProducts.filter { it.id != currentProductId }
                .take(limit)

            // Получаем избранные товары для текущего пользователя
            val favoriteIds = getFavoriteProductIds()

            // Обновляем флаг isFavorite для каждого товара
            val productsWithFavorites = similarProducts.map { product ->
                product.copy(isFavorite = favoriteIds.contains(product.id))
            }

            Resource.Success(productsWithFavorites)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении похожих товаров: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при получении похожих товаров")
        }
    }

    /**
     * Получает список популярных товаров
     * @param limit Максимальное количество товаров в результате
     * @return Resource со списком товаров или сообщением об ошибке
     */
    suspend fun getPopularProducts(limit: Int = 10): Resource<List<Product>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = firestoreSource.getCollectionOrderBy(
                PRODUCTS_COLLECTION,
                "soldCount",
                true,
                limit
            )

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
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении популярных товаров: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при получении популярных товаров")
        }
    }

    /**
     * Поиск товаров по заданным критериям
     * @param query Поисковый запрос
     * @param categoryId ID категории (опционально)
     * @param minPrice Минимальная цена (опционально)
     * @param maxPrice Максимальная цена (опционально)
     * @param inStockOnly Только товары в наличии
     * @param onSaleOnly Только товары со скидкой
     * @return Resource со списком товаров или сообщением об ошибке
     */
    suspend fun searchProducts(
        query: String = "",
        categoryId: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        inStockOnly: Boolean = false,
        onSaleOnly: Boolean = false,
        limit: Int = 20
    ): Resource<List<Product>> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Получаем все товары и фильтруем локально
            val allProductsResult = getAllProducts(0, 100)  // Задаем больший лимит для поиска

            if (allProductsResult is Resource.Success) {
                val allProducts = allProductsResult.data ?: emptyList()

                // Применяем фильтры
                val filteredProducts = allProducts.filter { product ->
                    var matches = true

                    // Текстовый поиск
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

                    // Фильтр по скидке
                    if (onSaleOnly) {
                        matches = matches && (product.discountPrice != null)
                    }

                    matches
                }.take(limit)

                Resource.Success(filteredProducts)
            } else {
                Resource.Error("Ошибка при поиске товаров")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при поиске товаров: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при поиске товаров")
        }
    }

    /**
     * Добавляет новый товар
     * @param product Объект товара
     * @param imageFiles Список файлов изображений
     * @return Resource с ID нового товара или сообщением об ошибке
     */
    suspend fun addProduct(
        product: Product,
        imageFiles: List<File>
    ): Resource<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Проверяем входные данные
            if (product.name.isBlank()) {
                return@withContext Resource.Error("Название товара не может быть пустым")
            }

            if (product.price <= 0) {
                return@withContext Resource.Error("Цена товара должна быть положительной")
            }

            if (product.availableQuantity < 0) {
                return@withContext Resource.Error("Количество товара не может быть отрицательным")
            }

            // Загружаем изображения в Storage
            val imageUrls = mutableListOf<String>()
            val maxImageSize = 5 * 1024 * 1024 // 5 МБ

            for (imageFile in imageFiles) {
                // Проверяем размер файла
                if (imageFile.length() > maxImageSize) {
                    return@withContext Resource.Error("Размер изображения не должен превышать 5 МБ")
                }

                val fileName = "products/${UUID.randomUUID()}"
                val url = storageSource.uploadFile(fileName, imageFile)
                imageUrls.add(url)
            }

            // Создаем документ товара в Firestore
            val productId = UUID.randomUUID().toString()
            val newProduct = product.copy(
                id = productId,
                images = imageUrls,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                viewCount = 0,
                soldCount = 0
            )

            firestoreSource.addDocument(PRODUCTS_COLLECTION, productId, newProduct)
            Resource.Success(productId)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении товара: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при добавлении товара")
        }
    }

    /**
     * Обновляет существующий товар
     * @param product Объект товара
     * @param newImageFiles Список новых файлов изображений
     * @param imagesToDelete Список URL изображений для удаления
     * @return Resource с результатом операции или сообщением об ошибке
     */
    suspend fun updateProduct(
        product: Product,
        newImageFiles: List<File> = emptyList(),
        imagesToDelete: List<String> = emptyList()
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Проверяем входные данные
            if (product.name.isBlank()) {
                return@withContext Resource.Error("Название товара не может быть пустым")
            }

            if (product.price <= 0) {
                return@withContext Resource.Error("Цена товара должна быть положительной")
            }

            if (product.availableQuantity < 0) {
                return@withContext Resource.Error("Количество товара не может быть отрицательным")
            }

            // Получаем текущий товар
            val currentProductResult = getProductById(product.id)

            if (currentProductResult !is Resource.Success) {
                return@withContext Resource.Error("Товар не найден")
            }

            val currentProduct = currentProductResult.data

            // Удаляем указанные изображения
            for (imageUrl in imagesToDelete) {
                storageSource.deleteFile(imageUrl)
            }

            // Загружаем новые изображения
            val newImageUrls = mutableListOf<String>()
            val maxImageSize = 5 * 1024 * 1024 // 5 МБ

            for (imageFile in newImageFiles) {
                // Проверяем размер файла
                if (imageFile.length() > maxImageSize) {
                    return@withContext Resource.Error("Размер изображения не должен превышать 5 МБ")
                }

                val fileName = "products/${UUID.randomUUID()}"
                val url = storageSource.uploadFile(fileName, imageFile)
                newImageUrls.add(url)
            }

            // Формируем обновленный список изображений
            val updatedImages = currentProduct!!.images
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
            Log.e(TAG, "Ошибка при обновлении товара: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при обновлении товара")
        }
    }

    /**
     * Удаляет товар
     * @param productId ID товара
     * @return Resource с результатом операции или сообщением об ошибке
     */
    suspend fun deleteProduct(productId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Получаем текущий товар
            val currentProductResult = getProductById(productId)

            if (currentProductResult !is Resource.Success) {
                return@withContext Resource.Error("Товар не найден")
            }

            val currentProduct = currentProductResult.data

            // Удаляем товар из Firestore
            firestoreSource.deleteDocument(PRODUCTS_COLLECTION, productId)

            // Удаляем все изображения товара
            for (imageUrl in currentProduct!!.images) {
                storageSource.deleteFile(imageUrl)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении товара: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при удалении товара")
        }
    }

    /**
     * Обновляет наличие товара
     * @param productId ID товара
     * @param availableQuantity Доступное количество
     * @return Resource с результатом операции или сообщением об ошибке
     */
    suspend fun updateProductAvailability(
        productId: String,
        availableQuantity: Int
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Проверяем входные данные
            if (availableQuantity < 0) {
                return@withContext Resource.Error("Количество товара не может быть отрицательным")
            }

            val updates = mapOf(
                "availableQuantity" to availableQuantity,
                "updatedAt" to System.currentTimeMillis()
            )

            firestoreSource.updateField(PRODUCTS_COLLECTION, productId, "availableQuantity", availableQuantity)
            firestoreSource.updateField(PRODUCTS_COLLECTION, productId, "updatedAt", System.currentTimeMillis())

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении наличия товара: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при обновлении наличия товара")
        }
    }

    /**
     * Обновляет цену товара
     * @param productId ID товара
     * @param price Новая цена
     * @param discountPrice Новая цена со скидкой (опционально)
     * @return Resource с результатом операции или сообщением об ошибке
     */
    suspend fun updateProductPrice(
        productId: String,
        price: Double,
        discountPrice: Double? = null
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Проверяем входные данные
            if (price <= 0) {
                return@withContext Resource.Error("Цена товара должна быть положительной")
            }

            if (discountPrice != null && (discountPrice <= 0 || discountPrice >= price)) {
                return@withContext Resource.Error("Некорректная цена со скидкой")
            }

            firestoreSource.updateField(PRODUCTS_COLLECTION, productId, "price", price)
            firestoreSource.updateField(PRODUCTS_COLLECTION, productId, "discountPrice", discountPrice)
            firestoreSource.updateField(PRODUCTS_COLLECTION, productId, "updatedAt", System.currentTimeMillis())

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении цены товара: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при обновлении цены товара")
        }
    }

    /**
     * Устанавливает статус активности товара
     * @param productId ID товара
     * @param isActive Флаг активности
     * @return Resource с результатом операции или сообщением об ошибке
     */
    suspend fun setProductActive(
        productId: String,
        isActive: Boolean
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestoreSource.updateField(PRODUCTS_COLLECTION, productId, "isActive", isActive)
            firestoreSource.updateField(PRODUCTS_COLLECTION, productId, "updatedAt", System.currentTimeMillis())

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении статуса товара: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при обновлении статуса товара")
        }
    }

    /**
     * Добавляет/удаляет товар из избранного
     * @param productId ID товара
     * @param isFavorite true - добавить в избранное, false - удалить из избранного
     * @return Resource с результатом операции или сообщением об ошибке
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
                firestoreSource.addDocument(
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
            Log.e(TAG, "Ошибка при обновлении избранного: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при обновлении избранного")
        }
    }

    /**
     * Получает список избранных товаров
     * @return Resource со списком товаров или сообщением об ошибке
     */
    suspend fun getFavoriteProducts(): Resource<List<Product>> = withContext(Dispatchers.IO) {
        val userId = firestoreSource.getCurrentUserId()
            ?: return@withContext Resource.Error("Пользователь не авторизован")

        val favoritePath = "$FAVORITES_COLLECTION/$userId/products"

        return@withContext try {
            // Получаем список ID избранных товаров
            val snapshot = firestoreSource.getCollection(favoritePath)

            val favoriteIds = snapshot.documents.map { it.id }

            if (favoriteIds.isEmpty()) {
                return@withContext Resource.Success(emptyList<Product>())
            }

            // Получаем товары по их ID (по одному для простоты)
            val products = mutableListOf<Product>()
            for (productId in favoriteIds) {
                val productResult = getProductById(productId)
                if (productResult is Resource.Success && productResult.data != null) {
                    products.add(productResult.data.copy(isFavorite = true))
                }
            }

            Resource.Success(products)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении избранных товаров: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при получении избранных товаров")
        }
    }

    /**
     * Проверяет, находится ли товар в избранном
     * @param productId ID товара
     * @return true, если товар в избранном, иначе false
     */
    private suspend fun isProductInFavorites(productId: String): Boolean {
        val userId = firestoreSource.getCurrentUserId() ?: return false
        val favoritePath = "$FAVORITES_COLLECTION/$userId/products"

        return try {
            val document = firestoreSource.getDocument(favoritePath, productId)
            document.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке избранного: ${e.message}", e)
            false
        }
    }

    /**
     * Получает список ID избранных товаров
     * @return Множество ID избранных товаров
     */
    private suspend fun getFavoriteProductIds(): Set<String> {
        val userId = firestoreSource.getCurrentUserId() ?: return emptySet()
        val favoritePath = "$FAVORITES_COLLECTION/$userId/products"

        return try {
            val snapshot = firestoreSource.getCollection(favoritePath)
            snapshot.documents.map { it.id }.toSet()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении ID избранных товаров: ${e.message}", e)
            emptySet()
        }
    }

    /**
     * Увеличивает счетчик просмотров товара
     * @param productId ID товара
     * @return Resource с результатом операции или сообщением об ошибке
     */
    suspend fun incrementProductViewCount(productId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection(PRODUCTS_COLLECTION).document(productId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw Exception("Товар не найден")
                }

                val currentCount = snapshot.getLong("viewCount") ?: 0
                transaction.update(docRef, "viewCount", currentCount + 1)
            }.await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при увеличении счетчика просмотров: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при увеличении счетчика просмотров")
        }
    }

    /**
     * Получает статистику товаров для административной панели
     * @return Resource со статистикой товаров или сообщением об ошибке
     */
    suspend fun getProductsStats(): Resource<ProductsStats> = withContext(Dispatchers.IO) {
        return@withContext try {
            val db = FirebaseFirestore.getInstance()

            // Общее количество товаров
            val totalProducts = db.collection(PRODUCTS_COLLECTION)
                .get()
                .await()
                .size()

            // Товары не в наличии
            val outOfStockProducts = db.collection(PRODUCTS_COLLECTION)
                .whereEqualTo("availableQuantity", 0)
                .get()
                .await()
                .size()

            // Товары с малым количеством
            val lowStockProducts = db.collection(PRODUCTS_COLLECTION)
                .whereGreaterThan("availableQuantity", 0)
                .whereLessThanOrEqualTo("availableQuantity", 5)
                .get()
                .await()
                .size()

            Resource.Success(
                ProductsStats(
                    totalProducts = totalProducts,
                    outOfStockProducts = outOfStockProducts,
                    lowStockProducts = lowStockProducts
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении статистики товаров: ${e.message}", e)
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
     * Получает Flow с данными товаров
     * @return Flow с ресурсом списка товаров
     */
    fun getProductsAsFlow(): Flow<Resource<List<Product>>> = flow {
        emit(Resource.Loading())

        try {
            // Начальная загрузка
            val initialResult = getAllProducts()
            emit(initialResult)

            // Здесь можно было бы добавить слушатель изменений для обновления в реальном времени
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в потоке товаров: ${e.message}", e)
            emit(Resource.Error(e.message ?: "Ошибка при получении товаров"))
        }
    }
}