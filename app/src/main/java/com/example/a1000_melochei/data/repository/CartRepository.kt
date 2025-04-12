package com.yourstore.app.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Cart
import com.yourstore.app.data.model.CartItem
import com.yourstore.app.data.model.CartTotal
import com.yourstore.app.data.model.DeliveryZone
import com.yourstore.app.data.model.OrderItem
import com.yourstore.app.data.model.Product
import com.yourstore.app.data.source.local.CartCache
import com.yourstore.app.data.source.remote.FirestoreSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Репозиторий для управления корзиной покупок.
 * Обеспечивает синхронизацию между локальным кэшем и Firestore, а также
 * предоставляет методы для добавления, обновления и удаления товаров из корзины.
 */
class CartRepository(
    private val firestoreSource: FirestoreSource,
    private val cartCache: CartCache,
    private val firebaseAuth: FirebaseAuth
) {
    private val TAG = "CartRepository"

    // Константы для работы с Firestore
    companion object {
        private const val CARTS_COLLECTION = "carts"
        private const val PRODUCTS_COLLECTION = "products"
        private const val DELIVERY_ZONES_COLLECTION = "delivery_zones"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    /**
     * Получает содержимое корзины пользователя
     * @return Resource с списком товаров в корзине или сообщение об ошибке
     */
    suspend fun getCartItems(): Resource<List<CartItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = firebaseAuth.currentUser?.uid

            if (userId != null) {
                // Если пользователь авторизован, загружаем корзину из Firestore
                getCartFromFirestore(userId)
            } else {
                // Если пользователь не авторизован, используем локальный кэш
                val cachedCart = cartCache.getCart()
                Resource.Success(cachedCart.items)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении корзины: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при получении корзины")
        }
    }

    /**
     * Добавляет товар в корзину
     * @param product Товар для добавления в корзину
     * @param quantity Количество товара (должно быть положительным)
     * @return Resource с результатом операции или сообщение об ошибке
     */
    suspend fun addToCart(product: Product, quantity: Int = 1): Resource<Unit> = withContext(Dispatchers.IO) {
        // Валидация входных параметров
        if (quantity <= 0) {
            return@withContext Resource.Error("Количество должно быть положительным числом")
        }

        // Проверяем наличие достаточного количества товара
        if (product.availableQuantity < quantity) {
            return@withContext Resource.Error("Недостаточное количество товара на складе. Доступно: ${product.availableQuantity}")
        }

        return@withContext try {
            val userId = firebaseAuth.currentUser?.uid

            // Создаем объект элемента корзины
            val cartItem = CartItem(
                id = UUID.randomUUID().toString(),
                productId = product.id,
                name = product.name,
                imageUrl = if (product.images.isNotEmpty()) product.images[0] else "",
                price = product.price,
                discountPrice = product.discountPrice,
                quantity = quantity,
                availableQuantity = product.availableQuantity,
                addedAt = System.currentTimeMillis()
            )

            if (userId != null) {
                // Если пользователь авторизован, сохраняем в Firestore с использованием транзакции
                val db = FirebaseFirestore.getInstance()

                var retryCount = 0
                var success = false
                var lastError: Exception? = null

                while (retryCount < MAX_RETRY_ATTEMPTS && !success) {
                    try {
                        db.runTransaction { transaction ->
                            val cartDocRef = db.collection(CARTS_COLLECTION).document(userId)
                            val cartDoc = transaction.get(cartDocRef)

                            if (cartDoc.exists()) {
                                // Корзина уже существует
                                val cart = cartDoc.toObject(Cart::class.java)
                                    ?: throw Exception("Ошибка при получении корзины")

                                // Проверяем, есть ли такой товар уже в корзине
                                val existingItemIndex = cart.items.indexOfFirst { it.productId == product.id }

                                if (existingItemIndex != -1) {
                                    // Товар уже в корзине, обновляем количество
                                    val existingItem = cart.items[existingItemIndex]
                                    val newQuantity = existingItem.quantity + quantity

                                    // Проверяем, не превышает ли новое количество наличие
                                    if (newQuantity > product.availableQuantity) {
                                        throw Exception("Недостаточное количество товара на складе")
                                    }

                                    val updatedItem = existingItem.copy(quantity = newQuantity)
                                    val updatedItems = cart.items.toMutableList().apply {
                                        this[existingItemIndex] = updatedItem
                                    }

                                    transaction.update(
                                        cartDocRef,
                                        mapOf(
                                            "items" to updatedItems,
                                            "updatedAt" to System.currentTimeMillis()
                                        )
                                    )
                                } else {
                                    // Добавляем новый товар в корзину
                                    val updatedItems = cart.items + cartItem

                                    transaction.update(
                                        cartDocRef,
                                        mapOf(
                                            "items" to updatedItems,
                                            "updatedAt" to System.currentTimeMillis()
                                        )
                                    )
                                }
                            } else {
                                // Создаем новую корзину
                                val newCart = Cart(
                                    id = userId,
                                    items = listOf(cartItem),
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )

                                transaction.set(cartDocRef, newCart)
                            }
                        }.await()

                        success = true
                    } catch (e: Exception) {
                        lastError = e
                        retryCount++
                        if (retryCount < MAX_RETRY_ATTEMPTS) {
                            // Добавляем задержку перед повторной попыткой
                            kotlinx.coroutines.delay(RETRY_DELAY_MS * retryCount)
                        }
                    }
                }

                if (!success && lastError != null) {
                    throw lastError
                }

                // Также обновляем локальный кэш для оффлайн-доступа
                updateLocalCart(cartItem, true)
            } else {
                // Если пользователь не авторизован, сохраняем только в локальный кэш
                updateLocalCart(cartItem, true)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении товара в корзину: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при добавлении товара в корзину")
        }
    }

    /**
     * Обновляет количество товара в корзине
     * @param cartItemId ID элемента корзины
     * @param quantity Новое количество (должно быть положительным)
     * @return Resource с результатом операции или сообщение об ошибке
     */
    suspend fun updateCartItemQuantity(
        cartItemId: String,
        quantity: Int
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        // Валидация входных параметров
        if (quantity <= 0) {
            return@withContext Resource.Error("Количество должно быть положительным числом")
        }

        return@withContext try {
            val userId = firebaseAuth.currentUser?.uid

            if (userId != null) {
                // Если пользователь авторизован, обновляем в Firestore с использованием транзакции
                val db = FirebaseFirestore.getInstance()

                var retryCount = 0
                var success = false
                var lastError: Exception? = null

                while (retryCount < MAX_RETRY_ATTEMPTS && !success) {
                    try {
                        db.runTransaction { transaction ->
                            val cartDocRef = db.collection(CARTS_COLLECTION).document(userId)
                            val cartDoc = transaction.get(cartDocRef)

                            if (!cartDoc.exists()) {
                                throw Exception("Корзина не найдена")
                            }

                            val cart = cartDoc.toObject(Cart::class.java)
                                ?: throw Exception("Ошибка при получении корзины")

                            // Находим товар в корзине
                            val itemIndex = cart.items.indexOfFirst { it.id == cartItemId }

                            if (itemIndex == -1) {
                                throw Exception("Товар не найден в корзине")
                            }

                            val item = cart.items[itemIndex]

                            // Проверяем, не превышает ли новое количество наличие
                            if (quantity > item.availableQuantity) {
                                throw Exception("Недостаточное количество товара на складе. Доступно: ${item.availableQuantity}")
                            }

                            // Обновляем количество
                            val updatedItem = item.copy(quantity = quantity)
                            val updatedItems = cart.items.toMutableList().apply {
                                this[itemIndex] = updatedItem
                            }

                            transaction.update(
                                cartDocRef,
                                mapOf(
                                    "items" to updatedItems,
                                    "updatedAt" to System.currentTimeMillis()
                                )
                            )
                        }.await()

                        success = true
                    } catch (e: Exception) {
                        lastError = e
                        retryCount++
                        if (retryCount < MAX_RETRY_ATTEMPTS) {
                            kotlinx.coroutines.delay(RETRY_DELAY_MS * retryCount)
                        }
                    }
                }

                if (!success && lastError != null) {
                    throw lastError
                }

                // Также обновляем локальный кэш
                updateLocalCartItemQuantity(cartItemId, quantity)
            } else {
                // Если пользователь не авторизован, обновляем только в локальном кэше
                updateLocalCartItemQuantity(cartItemId, quantity)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении количества товара: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при обновлении количества товара")
        }
    }

    /**
     * Удаляет товар из корзины
     * @param cartItemId ID элемента корзины для удаления
     * @return Resource с результатом операции или сообщение об ошибке
     */
    suspend fun removeCartItem(cartItemId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = firebaseAuth.currentUser?.uid

            if (userId != null) {
                // Если пользователь авторизован, удаляем из Firestore с использованием транзакции
                val db = FirebaseFirestore.getInstance()

                var retryCount = 0
                var success = false
                var lastError: Exception? = null

                while (retryCount < MAX_RETRY_ATTEMPTS && !success) {
                    try {
                        db.runTransaction { transaction ->
                            val cartDocRef = db.collection(CARTS_COLLECTION).document(userId)
                            val cartDoc = transaction.get(cartDocRef)

                            if (!cartDoc.exists()) {
                                throw Exception("Корзина не найдена")
                            }

                            val cart = cartDoc.toObject(Cart::class.java)
                                ?: throw Exception("Ошибка при получении корзины")

                            // Находим товар в корзине
                            val updatedItems = cart.items.filter { it.id != cartItemId }

                            if (updatedItems.size == cart.items.size) {
                                throw Exception("Товар не найден в корзине")
                            }

                            transaction.update(
                                cartDocRef,
                                mapOf(
                                    "items" to updatedItems,
                                    "updatedAt" to System.currentTimeMillis()
                                )
                            )
                        }.await()

                        success = true
                    } catch (e: Exception) {
                        lastError = e
                        retryCount++
                        if (retryCount < MAX_RETRY_ATTEMPTS) {
                            kotlinx.coroutines.delay(RETRY_DELAY_MS * retryCount)
                        }
                    }
                }

                if (!success && lastError != null) {
                    throw lastError
                }

                // Также удаляем из локального кэша
                removeLocalCartItem(cartItemId)
            } else {
                // Если пользователь не авторизован, удаляем только из локального кэша
                removeLocalCartItem(cartItemId)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении товара из корзины: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при удалении товара из корзины")
        }
    }

    /**
     * Очищает корзину
     * @return Resource с результатом операции или сообщение об ошибке
     */
    suspend fun clearCart(): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = firebaseAuth.currentUser?.uid

            if (userId != null) {
                // Если пользователь авторизован, очищаем в Firestore
                val db = FirebaseFirestore.getInstance()

                var retryCount = 0
                var success = false
                var lastError: Exception? = null

                while (retryCount < MAX_RETRY_ATTEMPTS && !success) {
                    try {
                        db.runTransaction { transaction ->
                            val cartDocRef = db.collection(CARTS_COLLECTION).document(userId)
                            val cartDoc = transaction.get(cartDocRef)

                            if (cartDoc.exists()) {
                                transaction.update(
                                    cartDocRef,
                                    mapOf(
                                        "items" to emptyList<CartItem>(),
                                        "updatedAt" to System.currentTimeMillis()
                                    )
                                )
                            }
                        }.await()

                        success = true
                    } catch (e: Exception) {
                        lastError = e
                        retryCount++
                        if (retryCount < MAX_RETRY_ATTEMPTS) {
                            kotlinx.coroutines.delay(RETRY_DELAY_MS * retryCount)
                        }
                    }
                }

                if (!success && lastError != null) {
                    throw lastError
                }
            }

            // Очищаем локальный кэш
            cartCache.clearCart()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при очистке корзины: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при очистке корзины")
        }
    }

    /**
     * Рассчитывает стоимость доставки на основе адреса
     * @param addressId ID адреса доставки
     * @return Resource с стоимостью доставки или сообщение об ошибке
     */
    suspend fun calculateDeliveryFee(addressId: String): Resource<Double> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = firebaseAuth.currentUser?.uid
                ?: return@withContext Resource.Error("Пользователь не авторизован")

            // Получаем данные пользователя для получения адреса
            val userDoc = firestoreSource.getDocument("users", userId)
            val user = userDoc.toObject(com.yourstore.app.data.model.User::class.java)
                ?: return@withContext Resource.Error("Профиль пользователя не найден")

            // Ищем выбранный адрес
            val address = user.addresses.find { it.id == addressId }
                ?: return@withContext Resource.Error("Адрес не найден")

            // Получаем зоны доставки из Firestore
            val deliveryZonesSnapshot = firestoreSource.getCollection(DELIVERY_ZONES_COLLECTION)
            val deliveryZones = deliveryZonesSnapshot.documents.mapNotNull { doc ->
                doc.toObject(DeliveryZone::class.java)
            }

            // Пытаемся найти подходящую зону доставки для адреса
            // В реальном приложении здесь может быть более сложная логика с геокодированием
            // и расчетом расстояния
            var deliveryFee = 2000.0 // Базовая стоимость доставки по умолчанию

            // Простая проверка по ключевым словам в адресе
            for (zone in deliveryZones) {
                if (zone.keywords.any { keyword ->
                        address.address.contains(keyword, ignoreCase = true)
                    }) {
                    deliveryFee = zone.baseFee
                    break
                }
            }

            // Получаем общий вес корзины для расчета дополнительной платы
            val cartItemsResult = getCartItems()
            if (cartItemsResult is Resource.Success) {
                val cartItems = cartItemsResult.data ?: emptyList()

                // Если в корзине много тяжелых товаров, увеличиваем стоимость доставки
                val totalQuantity = cartItems.sumOf { it.quantity }
                if (totalQuantity > 10) {
                    deliveryFee += 500.0 // Доплата за большое количество товаров
                }
            }

            Resource.Success(deliveryFee)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при расчете стоимости доставки: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при расчете стоимости доставки")
        }
    }

    /**
     * Синхронизирует локальную корзину с Firestore после авторизации
     * @param userId ID пользователя, который авторизовался
     * @return Resource с результатом операции или сообщение об ошибке
     */
    suspend fun syncCartAfterLogin(userId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val localCart = cartCache.getCart()

            if (localCart.items.isEmpty()) {
                return@withContext Resource.Success(Unit)
            }

            val db = FirebaseFirestore.getInstance()
            var retryCount = 0
            var success = false
            var lastError: Exception? = null

            while (retryCount < MAX_RETRY_ATTEMPTS && !success) {
                try {
                    db.runTransaction { transaction ->
                        val cartDocRef = db.collection(CARTS_COLLECTION).document(userId)
                        val cartDoc = transaction.get(cartDocRef)

                        if (cartDoc.exists()) {
                            // Корзина уже существует в Firestore
                            val remoteCart = cartDoc.toObject(Cart::class.java)
                                ?: throw Exception("Ошибка при получении корзины из Firestore")

                            // Объединяем корзины
                            val mergedItems = mergeCartItems(remoteCart.items, localCart.items)

                            transaction.update(
                                cartDocRef,
                                mapOf(
                                    "items" to mergedItems,
                                    "updatedAt" to System.currentTimeMillis()
                                )
                            )

                            // Обновляем локальный кэш объединенной корзиной
                            cartCache.saveCart(Cart(
                                id = userId,
                                items = mergedItems,
                                updatedAt = System.currentTimeMillis()
                            ))
                        } else {
                            // Создаем новую корзину в Firestore
                            val newCart = Cart(
                                id = userId,
                                items = localCart.items,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )

                            transaction.set(cartDocRef, newCart)
                        }
                    }.await()

                    success = true
                } catch (e: Exception) {
                    lastError = e
                    retryCount++
                    if (retryCount < MAX_RETRY_ATTEMPTS) {
                        kotlinx.coroutines.delay(RETRY_DELAY_MS * retryCount)
                    }
                }
            }

            if (!success && lastError != null) {
                throw lastError
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при синхронизации корзины: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при синхронизации корзины")
        }
    }

    /**
     * Получает количество товаров в корзине как LiveData
     * @return LiveData с количеством товаров
     */
    fun getCartItemCount(): LiveData<Int> {
        return cartCache.getCartItemCountLiveData()
    }

    /**
     * Получает общую стоимость корзины
     * @return Resource с общей стоимостью или сообщение об ошибке
     */
    suspend fun getCartTotal(): Resource<CartTotal> = withContext(Dispatchers.IO) {
        return@withContext try {
            val cartItemsResult = getCartItems()

            if (cartItemsResult is Resource.Error) {
                return@withContext cartItemsResult
            }

            val cartItems = (cartItemsResult as Resource.Success).data ?: emptyList()

            // Расчет стоимости товаров
            var subtotal = 0.0
            for (item in cartItems) {
                val itemPrice = item.discountPrice ?: item.price
                subtotal += itemPrice * item.quantity
            }

            // Базовая стоимость доставки по умолчанию
            val deliveryFee = 0.0

            val total = subtotal + deliveryFee

            Resource.Success(
                CartTotal(
                    subtotal = subtotal,
                    deliveryFee = deliveryFee,
                    total = total
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при расчете стоимости корзины: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при расчете стоимости корзины")
        }
    }

    /**
     * Получает корзину из Firestore с обновлением информации о товарах
     * @param userId ID пользователя
     * @param pageSize Размер страницы для пагинации (по умолчанию 0 - без пагинации)
     * @param lastItemId ID последнего загруженного элемента (для пагинации)
     * @return Resource с списком товаров в корзине или сообщение об ошибке
     */
    private suspend fun getCartFromFirestore(
        userId: String,
        pageSize: Int = 0,
        lastItemId: String? = null
    ): Resource<List<CartItem>> {
        return try {
            val db = FirebaseFirestore.getInstance()

            // Получаем документ корзины
            val cartDocRef = db.collection(CARTS_COLLECTION).document(userId)
            val cartDoc = cartDocRef.get().await()

            if (!cartDoc.exists()) {
                // Если корзины нет, создаем пустую
                val emptyCart = Cart(
                    id = userId,
                    items = emptyList(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                // Сохраняем пустую корзину в Firestore
                cartDocRef.set(emptyCart).await()

                // Обновляем локальный кэш
                cartCache.saveCart(emptyCart)

                return Resource.Success(emptyList())
            }

            val cart = cartDoc.toObject(Cart::class.java)
                ?: return Resource.Error("Ошибка при получении корзины")

            // Применяем пагинацию если требуется
            val itemsToProcess = if (pageSize > 0 && lastItemId != null) {
                val lastItemIndex = cart.items.indexOfFirst { it.id == lastItemId }
                if (lastItemIndex == -1) {
                    return Resource.Error("Некорректный ID последнего элемента для пагинации")
                }
                cart.items.subList(lastItemIndex + 1, minOf(lastItemIndex + 1 + pageSize, cart.items.size))
            } else {
                cart.items
            }

            // Проверяем актуальность данных о наличии товаров и собираем обновленные элементы
            val db = FirebaseFirestore.getInstance()
            val batch = db.batch()
            val updatedItems = mutableListOf<CartItem>()
            var cartNeedsUpdate = false

            for (item in itemsToProcess) {
                try {
                    // Получаем актуальные данные о товаре
                    val productDoc = db.collection(PRODUCTS_COLLECTION).document(item.productId).get().await()

                    if (productDoc.exists()) {
                        val product = productDoc.toObject(Product::class.java)

                        if (product != null) {
                            // Проверяем, нужно ли обновить элемент корзины
                            val needsUpdate = product.name != item.name ||
                                    (product.images.isNotEmpty() && product.images[0] != item.imageUrl) ||
                                    product.price != item.price ||
                                    product.discountPrice != item.discountPrice ||
                                    product.availableQuantity != item.availableQuantity ||
                                    (item.quantity > product.availableQuantity)

                            if (needsUpdate) {
                                cartNeedsUpdate = true
                            }

                            // Создаем обновленный элемент
                            val updatedItem = item.copy(
                                name = product.name,
                                imageUrl = if (product.images.isNotEmpty()) product.images[0] else "",
                                price = product.price,
                                discountPrice = product.discountPrice,
                                availableQuantity = product.availableQuantity,
                                // Корректируем количество, если оно превышает наличие
                                quantity = minOf(item.quantity, product.availableQuantity)
                            )

                            updatedItems.add(updatedItem)
                        } else {
                            // Если товар не преобразовался, добавляем исходный элемент
                            updatedItems.add(item)
                        }
                    } else {
                        // Если товар удален, пропускаем его
                        cartNeedsUpdate = true
                        continue
                    }
                } catch (e: Exception) {
                    // При ошибке получения товара добавляем исходный элемент
                    Log.w(TAG, "Ошибка при получении информации о товаре ${item.productId}: ${e.message}")
                    updatedItems.add(item)
                }
            }

            // Если были изменения, обновляем корзину в Firestore
            if (cartNeedsUpdate) {
                // Находим индексы обновленных элементов в исходном списке
                val allUpdatedItems = cart.items.toMutableList()
                for (i in itemsToProcess.indices) {
                    val originalIndex = cart.items.indexOfFirst { it.id == itemsToProcess[i].id }
                    if (originalIndex != -1 && i < updatedItems.size) {
                        allUpdatedItems[originalIndex] = updatedItems[i]
                    }
                }

                // Удаляем элементы, которые были пропущены (товар удален)
                allUpdatedItems.removeAll { item ->
                    itemsToProcess.any { it.id == item.id } && updatedItems.none { it.id == item.id }
                }

                // Обновляем корзину в Firestore
                try {
                    cartDocRef.update(
                        mapOf(
                            "items" to allUpdatedItems,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    ).await()

                    // Обновляем локальный кэш
                    cartCache.saveCart(Cart(
                        id = userId,
                        items = allUpdatedItems,
                        createdAt = cart.createdAt,
                        updatedAt = System.currentTimeMillis()
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при обновлении корзины в Firestore: ${e.message}", e)
                    // Продолжаем выполнение, так как основная задача - получить актуальные данные
                }
            } else {
                // Если изменений не было, просто обновляем кэш
                cartCache.saveCart(cart)
            }

            Resource.Success(updatedItems)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении корзины из Firestore: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при получении корзины")
        }
    }

    /**
     * Обновляет локальную корзину
     * @param cartItem Элемент корзины для обновления
     * @param isAdd True - добавить товар, False - удалить товар
     */
    private suspend fun updateLocalCart(cartItem: CartItem, isAdd: Boolean) {
        val cart = cartCache.getCart()

        if (isAdd) {
            // Проверяем, есть ли такой товар уже в корзине
            val existingItemIndex = cart.items.indexOfFirst { it.productId == cartItem.productId }

            if (existingItemIndex != -1) {
                // Товар уже в корзине, обновляем количество
                val existingItem = cart.items[existingItemIndex]
                val newQuantity = existingItem.quantity + cartItem.quantity

                val updatedItem = existingItem.copy(quantity = newQuantity)
                val updatedItems = cart.items.toMutableList().apply {
                    this[existingItemIndex] = updatedItem
                }

                cartCache.saveCart(cart.copy(
                    items = updatedItems,
                    updatedAt = System.currentTimeMillis()
                ))
            } else {
                // Добавляем новый товар в корзину
                val updatedItems = cart.items + cartItem

                cartCache.saveCart(cart.copy(
                    items = updatedItems,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        } else {
            // Удаляем товар из корзины
            val updatedItems = cart.items.filter { it.id != cartItem.id }

            cartCache.saveCart(cart.copy(
                items = updatedItems,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Обновляет количество товара в локальной корзине
     * @param cartItemId ID элемента корзины
     * @param quantity Новое количество
     */
    private suspend fun updateLocalCartItemQuantity(cartItemId: String, quantity: Int) {
        val cart = cartCache.getCart()

        // Находим товар в корзине
        val itemIndex = cart.items.indexOfFirst { it.id == cartItemId }

        if (itemIndex != -1) {
            val item = cart.items[itemIndex]

            // Обновляем количество
            val updatedItem = item.copy(quantity = quantity)
            val updatedItems = cart.items.toMutableList().apply {
                this[itemIndex] = updatedItem
            }

            cartCache.saveCart(cart.copy(
                items = updatedItems,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Удаляет товар из локальной корзины
     * @param cartItemId ID элемента корзины для удаления
     */
    private suspend fun removeLocalCartItem(cartItemId: String) {
        val cart = cartCache.getCart()

        // Удаляем товар из корзины
        val updatedItems = cart.items.filter { it.id != cartItemId }

        cartCache.saveCart(cart.copy(
            items = updatedItems,
            updatedAt = System.currentTimeMillis()
        ))
    }

    /**
     * Объединяет элементы двух корзин
     * @param remoteItems Элементы удаленной корзины
     * @param localItems Элементы локальной корзины
     * @return Объединенный список элементов корзины
     */
    private fun mergeCartItems(
        remoteItems: List<CartItem>,
        localItems: List<CartItem>
    ): List<CartItem> {
        val resultItems = remoteItems.toMutableList()

        // Добавляем локальные элементы, объединяя с существующими
        for (localItem in localItems) {
            val existingItemIndex = resultItems.indexOfFirst { it.productId == localItem.productId }

            if (existingItemIndex != -1) {
                // Товар уже есть в удаленной корзине, объединяем количество
                val existingItem = resultItems[existingItemIndex]
                val newQuantity = existingItem.quantity + localItem.quantity

                resultItems[existingItemIndex] = existingItem.copy(
                    quantity = minOf(newQuantity, existingItem.availableQuantity)
                )
            } else {
                // Добавляем новый товар
                resultItems.add(localItem)
            }
        }

        return resultItems
    }

    /**
     * Добавляет товар из заказа в корзину (для повторного заказа)
     * @param orderItem Элемент заказа для добавления в корзину
     * @return Resource с результатом операции или сообщение об ошибке
     */
    suspend fun reorderItem(orderItem: OrderItem): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Получаем актуальную информацию о товаре
            val productDocRef = FirebaseFirestore.getInstance()
                .collection(PRODUCTS_COLLECTION)
                .document(orderItem.productId)

            val productDoc = productDocRef.get().await()

            if (!productDoc.exists()) {
                return@withContext Resource.Error("Товар не найден или был удален")
            }

            val product = productDoc.toObject(Product::class.java)?.copy(id = orderItem.productId)
                ?: return@withContext Resource.Error("Ошибка при получении информации о товаре")

            // Проверяем наличие товара
            if (product.availableQuantity <= 0) {
                return@withContext Resource.Error("Товар временно отсутствует на складе")
            }

            // Добавляем товар в корзину
            addToCart(product, minOf(orderItem.quantity, product.availableQuantity))
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при повторном заказе товара: ${e.message}", e)
            Resource.Error(e.message ?: "Ошибка при повторном заказе товара")
        }
    }
}