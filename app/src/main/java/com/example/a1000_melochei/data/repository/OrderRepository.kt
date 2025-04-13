package com.yourstore.app.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.CartTotal
import com.yourstore.app.data.model.Order
import com.yourstore.app.data.model.OrderItem
import com.yourstore.app.data.model.OrderStatus
import com.yourstore.app.data.model.User
import com.yourstore.app.data.source.local.CartCache
import com.yourstore.app.data.source.remote.FirestoreSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Репозиторий для управления заказами.
 * Обеспечивает создание, получение и обновление заказов в Firebase Firestore.
 */
class OrderRepository(
    private val firestoreSource: FirestoreSource,
    private val cartRepository: CartRepository,
    private val cartCache: CartCache
) {
    private val TAG = "OrderRepository"

    // Константы для работы с Firestore
    private val ORDERS_COLLECTION = "orders"
    private val USERS_COLLECTION = "users"
    private val PRODUCTS_COLLECTION = "products"

    // Счетчик новых заказов для администратора
    private val _newOrdersCount = MutableLiveData<Int>()

    /**
     * Оформляет новый заказ
     */
    suspend fun placeOrder(
        addressId: String?,
        deliveryMethod: String,
        pickupPoint: Int?,
        deliveryDate: Date?,
        paymentMethod: String,
        comment: String,
        cartTotal: CartTotal
    ): Resource<String> = withContext(Dispatchers.IO) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext Resource.Error("Пользователь не авторизован")

        return@withContext try {
            // Получаем содержимое корзины
            val cartItemsResult = cartRepository.getCartItems()

            if (cartItemsResult is Resource.Error) {
                return@withContext Resource.Error(cartItemsResult.message ?: "Ошибка при получении корзины")
            }

            val cartItems = (cartItemsResult as Resource.Success).data!!

            if (cartItems.isEmpty()) {
                return@withContext Resource.Error("Корзина пуста")
            }

            // Получаем информацию о пользователе
            val userResult = firestoreSource.getDocument(USERS_COLLECTION, userId)
            if (userResult is Resource.Error) {
                return@withContext Resource.Error(userResult.message ?: "Ошибка при получении данных пользователя")
            }

            val userDoc = (userResult as Resource.Success).data
            val user = userDoc.toObject(User::class.java)
                ?: return@withContext Resource.Error("Профиль пользователя не найден")

            // Получаем адрес доставки, если он используется
            val address = if (deliveryMethod == "delivery" && addressId != null) {
                user.addresses.find { it.id == addressId }
                    ?: return@withContext Resource.Error("Адрес доставки не найден")
            } else {
                null
            }

            // Создаем элементы заказа из элементов корзины
            val orderItems = cartItems.map { cartItem ->
                OrderItem(
                    id = UUID.randomUUID().toString(),
                    productId = cartItem.productId,
                    name = cartItem.name,
                    imageUrl = cartItem.imageUrl,
                    price = cartItem.price,
                    discountPrice = cartItem.discountPrice,
                    quantity = cartItem.quantity,
                    subtotal = (cartItem.discountPrice ?: cartItem.price) * cartItem.quantity,
                    sku = "", // В данном примере не используется
                    categoryId = "", // В данном примере не используется
                    categoryName = "" // В данном примере не используется
                )
            }

            // Генерируем номер заказа (например, текущий год + порядковый номер)
            val orderNumber = generateOrderNumber()

            // Создаем объект заказа
            val order = Order(
                id = UUID.randomUUID().toString(),
                userId = userId,
                number = orderNumber,
                items = orderItems,
                status = OrderStatus.PENDING,
                subtotal = cartTotal.subtotal,
                deliveryFee = cartTotal.deliveryFee,
                total = cartTotal.total,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                deliveryMethod = deliveryMethod,
                address = address,
                pickupPoint = pickupPoint,
                deliveryDate = deliveryDate?.time,
                paymentMethod = paymentMethod,
                comment = comment,
                userName = user.name,
                userPhone = user.phone,
                userEmail = user.email
            )

            // Сохраняем заказ в Firestore
            val result = firestoreSource.setDocument(ORDERS_COLLECTION, order.id, order)
            if (result is Resource.Error) {
                return@withContext Resource.Error(result.message ?: "Ошибка при сохранении заказа")
            }

            // Обновляем статистику продаж для товаров
            updateProductSalesStats(orderItems)

            // Очищаем корзину после оформления заказа
            cartRepository.clearCart()

            // Возвращаем ID созданного заказа
            Resource.Success(order.id)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при оформлении заказа: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при оформлении заказа")
        }
    }

    /**
     * Получает заказ по его ID
     */
    suspend fun getOrderById(orderId: String): Resource<Order> = withContext(Dispatchers.IO) {
        return@withContext try {
            val orderResult = firestoreSource.getDocument(ORDERS_COLLECTION, orderId)
            if (orderResult is Resource.Error) {
                return@withContext Resource.Error(orderResult.message ?: "Ошибка при получении заказа")
            }

            val orderDoc = (orderResult as Resource.Success).data
            val order = orderDoc.toObject(Order::class.java)?.copy(id = orderId)
                ?: return@withContext Resource.Error("Заказ не найден")

            Resource.Success(order)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении заказа: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении заказа")
        }
    }

    /**
     * Получает список заказов текущего пользователя
     */
    suspend fun getUserOrders(): Resource<List<Order>> = withContext(Dispatchers.IO) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext Resource.Error("Пользователь не авторизован")

        return@withContext try {
            // Так как метода getCollectionWithFilterOrderBy нет в FirestoreSource,
            // используем более простой подход с getCollection и фильтрацией результатов
            val ordersResult = firestoreSource.getCollection(ORDERS_COLLECTION)
            if (ordersResult is Resource.Error) {
                return@withContext Resource.Error(ordersResult.message ?: "Ошибка при получении заказов")
            }

            val ordersSnapshot = (ordersResult as Resource.Success).data
            val orders = ordersSnapshot.documents
                .mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }
                .filter { it.userId == userId }
                .sortedByDescending { it.createdAt }

            Resource.Success(orders)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении заказов пользователя: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении заказов")
        }
    }

    /**
     * Получает список всех заказов (для администратора)
     */
    suspend fun getAllOrders(): Resource<List<Order>> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Используем getCollection вместо getCollectionOrderBy
            val ordersResult = firestoreSource.getCollection(ORDERS_COLLECTION)
            if (ordersResult is Resource.Error) {
                return@withContext Resource.Error(ordersResult.message ?: "Ошибка при получении заказов")
            }

            val ordersSnapshot = (ordersResult as Resource.Success).data
            val orders = ordersSnapshot.documents
                .mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }
                .sortedByDescending { it.createdAt }

            Resource.Success(orders)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении всех заказов: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении заказов")
        }
    }

    /**
     * Получает список последних заказов (для дашборда администратора)
     */
    suspend fun getLatestOrders(limit: Int): Resource<List<Order>> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Используем getCollection вместо getCollectionOrderBy с лимитом
            val ordersResult = firestoreSource.getCollection(ORDERS_COLLECTION)
            if (ordersResult is Resource.Error) {
                return@withContext Resource.Error(ordersResult.message ?: "Ошибка при получении заказов")
            }

            val ordersSnapshot = (ordersResult as Resource.Success).data
            val orders = ordersSnapshot.documents
                .mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }
                .sortedByDescending { it.createdAt }
                .take(limit)

            Resource.Success(orders)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении последних заказов: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении заказов")
        }
    }

    /**
     * Обновляет статус заказа
     */
    suspend fun updateOrderStatus(
        orderId: String,
        status: OrderStatus
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Так как метода updateField нет в FirestoreSource, используем updateDocument
            val updates = mapOf(
                "status" to status,
                "updatedAt" to System.currentTimeMillis()
            )

            val result = firestoreSource.updateDocument(ORDERS_COLLECTION, orderId, updates)
            if (result is Resource.Error) {
                return@withContext Resource.Error(result.message ?: "Ошибка при обновлении статуса заказа")
            }

            // Если статус завершен, обновляем статистику продаж
            if (status == OrderStatus.COMPLETED) {
                val orderResult = getOrderById(orderId)

                if (orderResult is Resource.Success) {
                    val order = orderResult.data!!
                    updateProductSalesOnComplete(order.items)
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении статуса заказа: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при обновлении статуса заказа")
        }
    }

    /**
     * Отменяет заказ
     */
    suspend fun cancelOrder(orderId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val orderResult = getOrderById(orderId)

            if (orderResult is Resource.Error) {
                return@withContext orderResult
            }

            val order = (orderResult as Resource.Success).data!!

            // Проверяем, можно ли отменить заказ
            if (order.status != OrderStatus.PENDING && order.status != OrderStatus.PROCESSING) {
                return@withContext Resource.Error("Заказ невозможно отменить на текущем этапе")
            }

            // Обновляем статус заказа
            val updates = mapOf(
                "status" to OrderStatus.CANCELLED,
                "updatedAt" to System.currentTimeMillis()
            )

            val result = firestoreSource.updateDocument(ORDERS_COLLECTION, orderId, updates)
            if (result is Resource.Error) {
                return@withContext Resource.Error(result.message ?: "Ошибка при отмене заказа")
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при отмене заказа: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при отмене заказа")
        }
    }

    /**
     * Получает статистику заказов
     */
    suspend fun getOrdersStats(): Resource<OrdersStats> = withContext(Dispatchers.IO) {
        return@withContext try {
            val ordersResult = getAllOrders()

            if (ordersResult is Resource.Success) {
                val orders = ordersResult.data ?: emptyList()

                val totalOrders = orders.size
                val pendingOrders = orders.count { it.status == OrderStatus.PENDING }
                val processingOrders = orders.count { it.status == OrderStatus.PROCESSING }
                val shippingOrders = orders.count { it.status == OrderStatus.SHIPPING }
                val deliveredOrders = orders.count { it.status == OrderStatus.DELIVERED }
                val completedOrders = orders.count { it.status == OrderStatus.COMPLETED }
                val cancelledOrders = orders.count { it.status == OrderStatus.CANCELLED }

                val stats = OrdersStats(
                    totalOrders = totalOrders,
                    pendingOrders = pendingOrders,
                    processingOrders = processingOrders,
                    shippingOrders = shippingOrders,
                    deliveredOrders = deliveredOrders,
                    completedOrders = completedOrders,
                    cancelledOrders = cancelledOrders
                )

                Resource.Success(stats)
            } else {
                Resource.Error("Не удалось получить статистику заказов")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении статистики заказов: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении статистики заказов")
        }
    }

    /**
     * Получает статистику продаж
     */
    suspend fun getSalesStats(): Resource<SalesStats> = withContext(Dispatchers.IO) {
        return@withContext try {
            val ordersResult = getAllOrders()

            if (ordersResult is Resource.Success) {
                val orders = ordersResult.data ?: emptyList()

                val completedOrders = orders.filter {
                    it.status == OrderStatus.COMPLETED || it.status == OrderStatus.DELIVERED
                }

                val totalSales = completedOrders.sumOf { it.total }
                val averageOrderValue = if (completedOrders.isNotEmpty()) {
                    totalSales / completedOrders.size
                } else {
                    0.0
                }

                // Рассчитываем продажи за день
                val currentTime = System.currentTimeMillis()
                val dayInMillis = 24 * 60 * 60 * 1000L

                val todaySales = completedOrders
                    .filter { it.createdAt > (currentTime - dayInMillis) }
                    .sumOf { it.total }

                // Рассчитываем продажи за неделю
                val weekInMillis = 7 * dayInMillis
                val weekSales = completedOrders
                    .filter { it.createdAt > (currentTime - weekInMillis) }
                    .sumOf { it.total }

                // Рассчитываем продажи за месяц
                val monthInMillis = 30L * dayInMillis
                val monthSales = completedOrders
                    .filter { it.createdAt > (currentTime - monthInMillis) }
                    .sumOf { it.total }

                val stats = SalesStats(
                    totalSales = totalSales,
                    averageOrderValue = averageOrderValue,
                    todaySales = todaySales,
                    weekSales = weekSales,
                    monthSales = monthSales
                )

                Resource.Success(stats)
            } else {
                Resource.Error("Не удалось получить статистику продаж")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении статистики продаж: ${e.message}")
            Resource.Error(e.message ?: "Ошибка при получении статистики продаж")
        }
    }

    /**
     * Получает количество новых заказов как LiveData
     */
    fun getNewOrdersCountAsFlow(): LiveData<Int> {
        // Обновляем счетчик новых заказов при создании
        updateNewOrdersCount()
        return _newOrdersCount
    }

    /**
     * Обновляет счетчик новых заказов
     */
    fun updateNewOrdersCount() {
        FirebaseFirestore.getInstance()
            .collection(ORDERS_COLLECTION)
            .whereEqualTo("status", OrderStatus.PENDING.name)
            .get()
            .addOnSuccessListener { snapshot ->
                _newOrdersCount.value = snapshot.documents.size
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Ошибка при обновлении счетчика новых заказов: ${e.message}")
                _newOrdersCount.value = 0
            }
    }

    /**
     * Генерирует номер заказа
     */
    private fun generateOrderNumber(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        // Получаем текущее время в миллисекундах и берем последние 5 цифр
        val timeCode = (System.currentTimeMillis() % 100000).toString().padStart(5, '0')
        return "$year-$timeCode"
    }

    /**
     * Обновляет статистику продаж для товаров при создании заказа
     */
    private suspend fun updateProductSalesStats(orderItems: List<OrderItem>) {
        for (item in orderItems) {
            try {
                // Получаем текущие данные о товаре
                val productResult = firestoreSource.getDocument(PRODUCTS_COLLECTION, item.productId)
                if (productResult is Resource.Error) {
                    Log.e(TAG, "Ошибка при получении данных товара: ${productResult.message}")
                    continue
                }

                val productDoc = (productResult as Resource.Success).data
                val product = productDoc.toObject(com.yourstore.app.data.model.Product::class.java)
                    ?: continue

                // Обновляем количество проданных товаров и доступное количество
                val updates = mapOf(
                    "soldCount" to (product.soldCount + item.quantity),
                    "availableQuantity" to (product.availableQuantity - item.quantity),
                    "updatedAt" to System.currentTimeMillis()
                )

                firestoreSource.updateDocument(PRODUCTS_COLLECTION, item.productId, updates)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении статистики товара: ${e.message}")
                // Продолжаем с другими товарами
            }
        }
    }

    /**
     * Обновляет статистику продаж при завершении заказа
     */
    private suspend fun updateProductSalesOnComplete(orderItems: List<OrderItem>) {
        for (item in orderItems) {
            try {
                // Получаем текущие данные о товаре
                val productResult = firestoreSource.getDocument(PRODUCTS_COLLECTION, item.productId)
                if (productResult is Resource.Error) {
                    Log.e(TAG, "Ошибка при получении данных товара: ${productResult.message}")
                    continue
                }

                val productDoc = (productResult as Resource.Success).data
                val product = productDoc.toObject(com.yourstore.app.data.model.Product::class.java)
                    ?: continue

                // Обновляем счетчик завершенных продаж
                val completedSalesCount = product.completedSalesCount ?: 0
                val updates = mapOf(
                    "completedSalesCount" to (completedSalesCount + item.quantity),
                    "updatedAt" to System.currentTimeMillis()
                )

                firestoreSource.updateDocument(PRODUCTS_COLLECTION, item.productId, updates)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении статистики завершения продаж: ${e.message}")
                // Продолжаем с другими товарами
            }
        }
    }

    /**
     * Класс для хранения статистики заказов
     */
    data class OrdersStats(
        val totalOrders: Int,
        val pendingOrders: Int,
        val processingOrders: Int,
        val shippingOrders: Int,
        val deliveredOrders: Int,
        val completedOrders: Int,
        val cancelledOrders: Int
    )

    /**
     * Класс для хранения статистики продаж
     */
    data class SalesStats(
        val totalSales: Double,
        val averageOrderValue: Double,
        val todaySales: Double,
        val weekSales: Double,
        val monthSales: Double
    )
}