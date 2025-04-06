package com.yourstore.app.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.firebase.auth.FirebaseAuth
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Cart
import com.yourstore.app.data.model.CartItem
import com.yourstore.app.data.model.CartTotal
import com.yourstore.app.data.model.OrderItem
import com.yourstore.app.data.model.Product
import com.yourstore.app.data.source.local.CartCache
import com.yourstore.app.data.source.remote.FirestoreSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Репозиторий для управления корзиной покупок.
 * Обеспечивает синхронизацию между локальным кэшем и Firestore.
 */
class CartRepository(
    private val firestoreSource: FirestoreSource,
    private val cartCache: CartCache
) {
    private val TAG = "CartRepository"

    // Константы для работы с Firestore
    private val CARTS_COLLECTION = "carts"
    private val PRODUCTS_COLLECTION = "products"

    /**
     * Получает содержимое корзины
     */
    suspend fun getCartItems(): Resource<List<CartItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                // Если пользователь авторизован, загружаем корзину из Firestore
                getCartFromFirestore(userId)
            } else {
                // Если пользователь не авторизован, используем локальный кэш
                val cachedCart = cartCache.getCart()
                Resource.Success(cachedCart.items)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении корзины: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении корзины")
        }
    }

    /**
     * Добавляет товар в корзину
     */
    suspend fun addToCart(product: Product, quantity: Int = 1): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            // Проверяем наличие достаточного количества товара
            if (product.availableQuantity < quantity) {
                return@withContext Resource.Error("Недостаточное количество товара на складе")
            }

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
                // Если пользователь авторизован, сохраняем в Firestore
                val cartDoc = firestoreSource.getDocument("$CARTS_COLLECTION", userId)

                if (cartDoc.exists()) {
                    // Корзина уже существует
                    val cart = cartDoc.toObject(Cart::class.java)
                        ?: return@withContext Resource.Error("Ошибка при получении корзины")

                    // Проверяем, есть ли такой товар уже в корзине
                    val existingItemIndex = cart.items.indexOfFirst { it.productId == product.id }

                    if (existingItemIndex != -1) {
                        // Товар уже в корзине, обновляем количество
                        val existingItem = cart.items[existingItemIndex]
                        val newQuantity = existingItem.quantity + quantity

                        // Проверяем, не превышает ли новое количество наличие
                        if (newQuantity > product.availableQuantity) {
                            return@withContext Resource.Error("Недостаточное количество товара на складе")
                        }

                        val updatedItem = existingItem.copy(quantity = newQuantity)
                        val updatedItems = cart.items.toMutableList().apply {
                            this[existingItemIndex] = updatedItem
                        }

                        firestoreSource.updateDocument(
                            "$CARTS_COLLECTION",
                            userId,
                            cart.copy(
                                items = updatedItems,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        // Добавляем новый товар в корзину
                        val updatedItems = cart.items + cartItem

                        firestoreSource.updateDocument(
                            "$CARTS_COLLECTION",
                            userId,
                            cart.copy(
                                items = updatedItems,
                                updatedAt = System.currentTimeMillis()
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

                    firestoreSource.setDocument(
                        "$CARTS_COLLECTION",
                        userId,
                        newCart
                    )
                }

                // Также обновляем локальный кэш для оффлайн-доступа
                updateLocalCart(cartItem, true)
            } else {
                // Если пользователь не авторизован, сохраняем только в локальный кэш
                updateLocalCart(cartItem, true)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении товара в корзину: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при добавлении товара в корзину")
        }
    }

    /**
     * Обновляет количество товара в корзине
     */
    suspend fun updateCartItemQuantity(
        cartItemId: String,
        quantity: Int
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (quantity <= 0) {
                return@withContext Resource.Error("Количество должно быть положительным числом")
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                // Если пользователь авторизован, обновляем в Firestore
                val cartDoc = firestoreSource.getDocument("$CARTS_COLLECTION", userId)

                if (!cartDoc.exists()) {
                    return@withContext Resource.Error("Корзина не найдена")
                }

                val cart = cartDoc.toObject(Cart::class.java)
                    ?: return@withContext Resource.Error("Ошибка при получении корзины")

                // Находим товар в корзине
                val itemIndex = cart.items.indexOfFirst { it.id == cartItemId }

                if (itemIndex == -1) {
                    return@withContext Resource.Error("Товар не найден в корзине")
                }

                val item = cart.items[itemIndex]

                // Проверяем, не превышает ли новое количество наличие
                if (quantity > item.availableQuantity) {
                    return@withContext Resource.Error("Недостаточное количество товара на складе")
                }

                // Обновляем количество
                val updatedItem = item.copy(quantity = quantity)
                val updatedItems = cart.items.toMutableList().apply {
                    this[itemIndex] = updatedItem
                }

                firestoreSource.updateDocument(
                    "$CARTS_COLLECTION",
                    userId,
                    cart.copy(
                        items = updatedItems,
                        updatedAt = System.currentTimeMillis()
                    )
                )

                // Также обновляем локальный кэш
                updateLocalCartItemQuantity(cartItemId, quantity)
            } else {
                // Если пользователь не авторизован, обновляем только в локальном кэше
                updateLocalCartItemQuantity(cartItemId, quantity)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении количества товара: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при обновлении количества товара")
        }
    }

    /**
     * Удаляет товар из корзины
     */
    suspend fun removeCartItem(cartItemId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                // Если пользователь авторизован, удаляем из Firestore
                val cartDoc = firestoreSource.getDocument("$CARTS_COLLECTION", userId)

                if (!cartDoc.exists()) {
                    return@withContext Resource.Error("Корзина не найдена")
                }

                val cart = cartDoc.toObject(Cart::class.java)
                    ?: return@withContext Resource.Error("Ошибка при получении корзины")

                // Находим товар в корзине
                val updatedItems = cart.items.filter { it.id != cartItemId }

                if (updatedItems.size == cart.items.size) {
                    return@withContext Resource.Error("Товар не найден в корзине")
                }

                firestoreSource.updateDocument(
                    "$CARTS_COLLECTION",
                    userId,
                    cart.copy(
                        items = updatedItems,
                        updatedAt = System.currentTimeMillis()
                    )
                )

                // Также удаляем из локального кэша
                removeLocalCartItem(cartItemId)
            } else {
                // Если пользователь не авторизован, удаляем только из локального кэша
                removeLocalCartItem(cartItemId)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении товара из корзины: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при удалении товара из корзины")
        }
    }

    /**
     * Очищает корзину
     */
    suspend fun clearCart(): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                // Если пользователь авторизован, очищаем в Firestore
                val cartDoc = firestoreSource.getDocument("$CARTS_COLLECTION", userId)

                if (cartDoc.exists()) {
                    val cart = cartDoc.toObject(Cart::class.java)
                        ?: return@withContext Resource.Error("Ошибка при получении корзины")

                    firestoreSource.updateDocument(
                        "$CARTS_COLLECTION",
                        userId,
                        cart.copy(
                            items = emptyList(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            // Очищаем локальный кэш
            cartCache.clearCart()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при очистке корзины: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при очистке корзины")
        }
    }

    /**
     * Рассчитывает стоимость доставки
     */
    suspend fun calculateDeliveryFee(addressId: String): Resource<Double> = withContext(Dispatchers.IO) {
        return@withContext try {
            // В реальном приложении здесь был бы запрос к сервису расчета доставки
            // или учет зон доставки из Firebase
            // Для примера используем фиксированную стоимость
            Resource.Success(1500.0) // 1500 тенге за доставку
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при расчете стоимости доставки: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при расчете стоимости доставки")
        }
    }

    /**
     * Синхронизирует локальную корзину с Firestore после авторизации
     */
    suspend fun syncCartAfterLogin(userId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val localCart = cartCache.getCart()

            if (localCart.items.isEmpty()) {
                return@withContext Resource.Success(Unit)
            }

            val cartDoc = firestoreSource.getDocument("$CARTS_COLLECTION", userId)

            if (cartDoc.exists()) {
                // Корзина уже существует в Firestore
                val remoteCart = cartDoc.toObject(Cart::class.java)
                    ?: return@withContext Resource.Error("Ошибка при получении корзины из Firestore")

                // Объединяем корзины
                val mergedItems = mergeCartItems(remoteCart.items, localCart.items)

                firestoreSource.updateDocument(
                    "$CARTS_COLLECTION",
                    userId,
                    remoteCart.copy(
                        items = mergedItems,
                        updatedAt = System.currentTimeMillis()
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

                firestoreSource.setDocument(
                    "$CARTS_COLLECTION",
                    userId,
                    newCart
                )
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при синхронизации корзины: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при синхронизации корзины")
        }
    }

    /**
     * Получает количество товаров в корзине
     */
    fun getCartItemCount(): LiveData<Int> {
        return cartCache.getCartItemCountLiveData()
    }

    /**
     * Получает корзину из Firestore
     */
    private suspend fun getCartFromFirestore(userId: String): Resource<List<CartItem>> {
        return try {
            val cartDoc = firestoreSource.getDocument("$CARTS_COLLECTION", userId)

            if (!cartDoc.exists()) {
                // Если корзины нет, возвращаем пустой список
                val emptyCart = Cart(
                    id = userId,
                    items = emptyList(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                // Обновляем локальный кэш
                cartCache.saveCart(emptyCart)

                return Resource.Success(emptyList())
            }

            val cart = cartDoc.toObject(Cart::class.java)
                ?: return Resource.Error("Ошибка при получении корзины")

            // Проверяем актуальность данных о наличии товаров
            val updatedItems = mutableListOf<CartItem>()

            for (item in cart.items) {
                // Получаем актуальные данные о товаре
                val productDoc = firestoreSource.getDocument(PRODUCTS_COLLECTION, item.productId)

                if (productDoc.exists()) {
                    val product = productDoc.toObject(Product::class.java)

                    if (product != null) {
                        // Обновляем данные о товаре
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
                        // Если товар не найден, добавляем исходный элемент
                        updatedItems.add(item)
                    }
                } else {
                    // Если товар удален, пропускаем его
                    continue
                }
            }

            // Если были изменения, обновляем корзину в Firestore
            if (updatedItems.size != cart.items.size || updatedItems.any { updated ->
                    cart.items.find { it.id == updated.id }?.let {
                        it.quantity != updated.quantity ||
                                it.price != updated.price ||
                                it.discountPrice != updated.discountPrice
                    } ?: false
                }) {

                firestoreSource.updateDocument(
                    "$CARTS_COLLECTION",
                    userId,
                    cart.copy(
                        items = updatedItems,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            // Обновляем локальный кэш
            cartCache.saveCart(Cart(
                id = userId,
                items = updatedItems,
                updatedAt = System.currentTimeMillis()
            ))

            Resource.Success(updatedItems)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении корзины из Firestore: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении корзины")
        }
    }

    /**
     * Обновляет локальную корзину
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
     */
    suspend fun reorderItem(orderItem: OrderItem): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Получаем актуальную информацию о товаре
            val productDoc = firestoreSource.getDocument(PRODUCTS_COLLECTION, orderItem.productId)

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
            Log.e(TAG, "Ошибка при повторном заказе товара: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при повторном заказе товара")
        }
    }
}