package com.example.a1000_melochei.data.repository

import android.util.Log
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Cart
import com.example.a1000_melochei.data.model.CartItem
import com.example.a1000_melochei.data.source.local.CartCache
import com.example.a1000_melochei.data.source.remote.FirestoreSource
import com.example.a1000_melochei.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Репозиторий для управления корзиной покупок.
 * Обеспечивает синхронизацию между локальным кэшем и удаленным хранилищем.
 */
class CartRepository(
    private val firestoreSource: FirestoreSource,
    private val cartCache: CartCache,
    private val firebaseAuth: FirebaseAuth
) {
    private val TAG = "CartRepository"
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    /**
     * Получает корзину пользователя
     */
    suspend fun getCart(): Resource<Cart> {
        return try {
            val userId = firebaseAuth.currentUser?.uid

            if (userId != null) {
                // Пытаемся загрузить корзину из Firestore
                val result = firestoreSource.getCart(userId)

                when (result) {
                    is Resource.Success -> {
                        // Сохраняем в локальный кэш
                        cartCache.saveCart(result.data)
                        result
                    }
                    is Resource.Error -> {
                        // Если ошибка загрузки, возвращаем из локального кэша
                        val cachedCart = cartCache.getCart()
                        if (cachedCart != null) {
                            Resource.Success(cachedCart)
                        } else {
                            // Создаем пустую корзину
                            Resource.Success(Cart(id = userId))
                        }
                    }
                    is Resource.Loading -> result
                }
            } else {
                // Для неавторизованных пользователей используем только локальный кэш
                val cachedCart = cartCache.getCart()
                Resource.Success(cachedCart ?: Cart())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении корзины: ${e.message}", e)

            // Пытаемся вернуть данные из локального кэша
            val cachedCart = cartCache.getCart()
            if (cachedCart != null) {
                Resource.Success(cachedCart)
            } else {
                Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Добавляет товар в корзину
     */
    suspend fun addToCart(cartItem: CartItem): Resource<Unit> {
        return try {
            val userId = firebaseAuth.currentUser?.uid

            if (userId != null) {
                // Добавляем в Firestore с повторными попытками
                var success = false
                var lastError: Exception? = null
                var retryCount = 0

                while (!success && retryCount < MAX_RETRY_ATTEMPTS) {
                    try {
                        firestore.runTransaction { transaction ->
                            val cartDocRef = firestore.collection(Constants.COLLECTION_CARTS).document(userId)
                            val cartSnapshot = transaction.get(cartDocRef)

                            if (cartSnapshot.exists()) {
                                val cart = cartSnapshot.toObject(Cart::class.java) ?: Cart(id = userId)

                                // Ищем существующий товар в корзине
                                val existingItemIndex = cart.items.indexOfFirst {
                                    it.productId == cartItem.productId
                                }

                                if (existingItemIndex != -1) {
                                    // Обновляем количество существующего товара
                                    val updatedItems = cart.items.toMutableList()
                                    val existingItem = updatedItems[existingItemIndex]
                                    val newQuantity = (existingItem.quantity + cartItem.quantity)
                                        .coerceAtMost(Constants.MAX_CART_ITEMS)

                                    updatedItems[existingItemIndex] = existingItem.copy(
                                        quantity = newQuantity,
                                        subtotal = newQuantity * existingItem.price
                                    )

                                    transaction.update(
                                        cartDocRef,
                                        mapOf(
                                            "items" to updatedItems,
                                            "updatedAt" to System.currentTimeMillis()
                                        )
                                    )
                                } else {
                                    // Добавляем новый товар в корзину
                                    val updatedItems = cart.items + cartItem.copy(
                                        subtotal = cartItem.quantity * cartItem.price
                                    )

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
                                    items = listOf(cartItem.copy(
                                        subtotal = cartItem.quantity * cartItem.price
                                    )),
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

                // Также обновляем локальный кэш
                updateLocalCart(cartItem, true)
            } else {
                // Если пользователь не авторизован, сохраняем только в локальный кэш
                updateLocalCart(cartItem, true)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении товара в корзину: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Удаляет товар из корзины
     */
    suspend fun removeFromCart(productId: String): Resource<Unit> {
        return try {
            val userId = firebaseAuth.currentUser?.uid

            if (userId != null) {
                firestore.runTransaction { transaction ->
                    val cartDocRef = firestore.collection(Constants.COLLECTION_CARTS).document(userId)
                    val cartSnapshot = transaction.get(cartDocRef)

                    if (cartSnapshot.exists()) {
                        val cart = cartSnapshot.toObject(Cart::class.java) ?: Cart(id = userId)
                        val updatedItems = cart.items.filter { it.productId != productId }

                        transaction.update(
                            cartDocRef,
                            mapOf(
                                "items" to updatedItems,
                                "updatedAt" to System.currentTimeMillis()
                            )
                        )
                    }
                }.await()
            }

            // Обновляем локальный кэш
            updateLocalCart(null, false, productId)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении товара из корзины: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Обновляет количество товара в корзине
     */
    suspend fun updateCartItemQuantity(productId: String, quantity: Int): Resource<Unit> {
        return try {
            val userId = firebaseAuth.currentUser?.uid

            if (userId != null) {
                firestore.runTransaction { transaction ->
                    val cartDocRef = firestore.collection(Constants.COLLECTION_CARTS).document(userId)
                    val cartSnapshot = transaction.get(cartDocRef)

                    if (cartSnapshot.exists()) {
                        val cart = cartSnapshot.toObject(Cart::class.java) ?: Cart(id = userId)

                        val updatedItems = cart.items.map { item ->
                            if (item.productId == productId) {
                                item.copy(
                                    quantity = quantity.coerceIn(1, Constants.MAX_CART_ITEMS),
                                    subtotal = quantity * item.price
                                )
                            } else {
                                item
                            }
                        }

                        transaction.update(
                            cartDocRef,
                            mapOf(
                                "items" to updatedItems,
                                "updatedAt" to System.currentTimeMillis()
                            )
                        )
                    }
                }.await()
            }

            // Обновляем локальный кэш
            val cachedCart = cartCache.getCart()
            if (cachedCart != null) {
                val updatedItems = cachedCart.items.map { item ->
                    if (item.productId == productId) {
                        item.copy(
                            quantity = quantity.coerceIn(1, Constants.MAX_CART_ITEMS),
                            subtotal = quantity * item.price
                        )
                    } else {
                        item
                    }
                }
                val updatedCart = cachedCart.copy(
                    items = updatedItems,
                    updatedAt = System.currentTimeMillis()
                )
                cartCache.saveCart(updatedCart)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении количества товара: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Очищает корзину
     */
    suspend fun clearCart(): Resource<Unit> {
        return try {
            val userId = firebaseAuth.currentUser?.uid

            if (userId != null) {
                val cartDocRef = firestore.collection(Constants.COLLECTION_CARTS).document(userId)
                cartDocRef.update(
                    mapOf(
                        "items" to emptyList<CartItem>(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            }

            // Очищаем локальный кэш
            cartCache.clearCart()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при очистке корзины: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает поток изменений корзины
     */
    fun getCartFlow(): Flow<Resource<Cart>> = flow {
        try {
            val userId = firebaseAuth.currentUser?.uid

            if (userId != null) {
                // Слушаем изменения в Firestore
                firestoreSource.getCartFlow(userId).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            cartCache.saveCart(resource.data)
                            emit(resource)
                        }
                        is Resource.Error -> {
                            // При ошибке возвращаем кэшированные данные
                            val cachedCart = cartCache.getCart()
                            if (cachedCart != null) {
                                emit(Resource.Success(cachedCart))
                            } else {
                                emit(resource)
                            }
                        }
                        is Resource.Loading -> emit(resource)
                    }
                }
            } else {
                // Для неавторизованных пользователей возвращаем кэшированную корзину
                val cachedCart = cartCache.getCart() ?: Cart()
                emit(Resource.Success(cachedCart))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в потоке корзины: ${e.message}", e)
            emit(Resource.Error(e.message ?: "Неизвестная ошибка"))
        }
    }

    /**
     * Обновляет локальный кэш корзины
     */
    private fun updateLocalCart(cartItem: CartItem?, isAdd: Boolean, productIdToRemove: String? = null) {
        try {
            val cachedCart = cartCache.getCart() ?: Cart()

            val updatedItems = when {
                isAdd && cartItem != null -> {
                    val existingItemIndex = cachedCart.items.indexOfFirst {
                        it.productId == cartItem.productId
                    }

                    if (existingItemIndex != -1) {
                        // Обновляем существующий товар
                        cachedCart.items.toMutableList().apply {
                            val existingItem = this[existingItemIndex]
                            val newQuantity = (existingItem.quantity + cartItem.quantity)
                                .coerceAtMost(Constants.MAX_CART_ITEMS)

                            this[existingItemIndex] = existingItem.copy(
                                quantity = newQuantity,
                                subtotal = newQuantity * existingItem.price
                            )
                        }
                    } else {
                        // Добавляем новый товар
                        cachedCart.items + cartItem.copy(
                            subtotal = cartItem.quantity * cartItem.price
                        )
                    }
                }
                !isAdd && productIdToRemove != null -> {
                    cachedCart.items.filter { it.productId != productIdToRemove }
                }
                else -> cachedCart.items
            }

            val updatedCart = cachedCart.copy(
                items = updatedItems,
                updatedAt = System.currentTimeMillis()
            )

            cartCache.saveCart(updatedCart)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении локального кэша: ${e.message}", e)
        }
    }

    /**
     * Синхронизирует локальную корзину с сервером
     */
    suspend fun syncCart(): Resource<Unit> {
        return try {
            val userId = firebaseAuth.currentUser?.uid ?: return Resource.Success(Unit)
            val cachedCart = cartCache.getCart() ?: return Resource.Success(Unit)

            val cartDocRef = firestore.collection(Constants.COLLECTION_CARTS).document(userId)
            cartDocRef.set(cachedCart).await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при синхронизации корзины: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }
}