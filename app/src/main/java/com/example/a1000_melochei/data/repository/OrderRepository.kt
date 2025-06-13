package com.example.a1000_melochei.data.repository

import android.util.Log
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.*
import com.example.a1000_melochei.data.source.remote.FirestoreSource
import com.example.a1000_melochei.ui.admin.analytics.viewmodel.AnalyticsViewModel
import com.example.a1000_melochei.ui.admin.dashboard.viewmodel.DashboardViewModel
import com.example.a1000_melochei.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Calendar

/**
 * Репозиторий для управления заказами.
 * Обеспечивает взаимодействие с Firestore для заказов и интеграцию с корзиной.
 */
class OrderRepository(
    private val firestoreSource: FirestoreSource,
    private val cartRepository: CartRepository
) {
    private val TAG = "OrderRepository"

    /**
     * Получает список всех заказов
     */
    suspend fun getAllOrders(): Resource<List<Order>> {
        return try {
            val result = firestoreSource.getOrders()
            Log.d(TAG, "Загружено заказов: ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке заказов: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает заказы пользователя
     */
    suspend fun getUserOrders(userId: String): Resource<List<Order>> {
        return try {
            val result = firestoreSource.getOrdersByField("userId", userId)
            Log.d(TAG, "Загружено заказов пользователя $userId: ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке заказов пользователя: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает заказ по ID
     */
    suspend fun getOrderById(orderId: String): Resource<Order> {
        return try {
            val result = firestoreSource.getOrderById(orderId)
            Log.d(TAG, "Загружен заказ: ${result.getDataOrNull()?.orderNumber}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке заказа: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Создает новый заказ из корзины
     */
    suspend fun createOrderFromCart(
        userId: String,
        deliveryType: DeliveryType,
        deliveryAddress: Address?,
        customerInfo: CustomerInfo,
        paymentMethod: PaymentMethod,
        notes: String = ""
    ): Resource<String> {
        return try {
            // Получаем корзину пользователя
            val cartResult = cartRepository.getCart()
            when (cartResult) {
                is Resource.Success -> {
                    val cart = cartResult.data
                    if (cart.isEmpty()) {
                        return Resource.Error("Корзина пуста")
                    }

                    // Проверяем минимальную сумму заказа
                    val subtotal = cart.getTotalAmount()
                    if (subtotal < Constants.MIN_ORDER_AMOUNT) {
                        return Resource.Error("Минимальная сумма заказа ${Constants.MIN_ORDER_AMOUNT.toInt()} ₸")
                    }

                    // Рассчитываем стоимость доставки
                    val deliveryFee = calculateDeliveryFee(subtotal, deliveryType)
                    val total = subtotal + deliveryFee

                    // Создаем заказ
                    val order = Order(
                        userId = userId,
                        orderNumber = Order.generateOrderNumber(),
                        items = cart.items.map { cartItem ->
                            OrderItem(
                                productId = cartItem.productId,
                                productName = cartItem.productName,
                                productImage = cartItem.productImage,
                                price = cartItem.price,
                                originalPrice = cartItem.originalPrice,
                                quantity = cartItem.quantity,
                                subtotal = cartItem.subtotal
                            )
                        },
                        status = OrderStatus.PENDING,
                        deliveryType = deliveryType,
                        deliveryAddress = deliveryAddress,
                        customerInfo = customerInfo,
                        subtotal = subtotal,
                        deliveryFee = deliveryFee,
                        total = total,
                        paymentMethod = paymentMethod,
                        notes = notes,
                        estimatedDeliveryDate = calculateEstimatedDeliveryDate(deliveryType)
                    )

                    val result = firestoreSource.addOrder(order)
                    when (result) {
                        is Resource.Success -> {
                            Log.d(TAG, "Заказ создан: ${order.orderNumber}")

                            // Очищаем корзину после успешного создания заказа
                            cartRepository.clearCart()

                            result
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "Ошибка при создании заказа: ${result.message}")
                            result
                        }
                        is Resource.Loading -> result
                    }
                }
                is Resource.Error -> {
                    Resource.Error("Ошибка загрузки корзины: ${cartResult.message}")
                }
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при создании заказа: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Обновляет статус заказа
     */
    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus, comment: String = ""): Resource<Unit> {
        return try {
            val orderResult = getOrderById(orderId)
            when (orderResult) {
                is Resource.Success -> {
                    val updatedOrder = orderResult.data.withStatus(newStatus, comment)

                    // Устанавливаем фактическую дату доставки при доставке
                    val finalOrder = if (newStatus == OrderStatus.DELIVERED) {
                        updatedOrder.copy(actualDeliveryDate = System.currentTimeMillis())
                    } else {
                        updatedOrder
                    }

                    val result = firestoreSource.updateOrder(finalOrder)
                    when (result) {
                        is Resource.Success -> {
                            Log.d(TAG, "Статус заказа ${orderResult.data.orderNumber} обновлен: $newStatus")
                            result
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "Ошибка при обновлении статуса заказа: ${result.message}")
                            result
                        }
                        is Resource.Loading -> result
                    }
                }
                is Resource.Error -> orderResult
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении статуса заказа: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Отменяет заказ
     */
    suspend fun cancelOrder(orderId: String, reason: String = ""): Resource<Unit> {
        return try {
            val orderResult = getOrderById(orderId)
            when (orderResult) {
                is Resource.Success -> {
                    val order = orderResult.data
                    if (!order.canBeCancelled()) {
                        return Resource.Error("Заказ нельзя отменить в текущем статусе")
                    }

                    updateOrderStatus(orderId, OrderStatus.CANCELLED, "Отменен: $reason")
                }
                is Resource.Error -> orderResult
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при отмене заказа: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает заказы по статусу
     */
    suspend fun getOrdersByStatus(status: OrderStatus): Resource<List<Order>> {
        return try {
            val result = firestoreSource.getOrdersByField("status", status.name)
            Log.d(TAG, "Загружено заказов со статусом $status: ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке заказов по статусу: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает последние заказы
     */
    suspend fun getRecentOrders(limit: Int = 10): Resource<List<Order>> {
        return try {
            val result = firestoreSource.getRecentOrders(limit)
            Log.d(TAG, "Загружено последних заказов: ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке последних заказов: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает заказы за период
     */
    suspend fun getOrdersByDateRange(startDate: Long, endDate: Long): Resource<List<Order>> {
        return try {
            val result = firestoreSource.getOrdersByDateRange(startDate, endDate)
            Log.d(TAG, "Загружено заказов за период: ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке заказов за период: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Поиск заказов по номеру или имени клиента
     */
    suspend fun searchOrders(query: String): Resource<List<Order>> {
        return try {
            val result = firestoreSource.searchOrders(query)
            Log.d(TAG, "Найдено заказов по запросу '$query': ${result.getDataOrNull()?.size ?: 0}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при поиске заказов: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает статистику заказов для дашборда
     */
    suspend fun getOrdersStatistics(): Resource<DashboardViewModel.OrdersStats> {
        return try {
            val ordersResult = getAllOrders()
            when (ordersResult) {
                is Resource.Success -> {
                    val orders = ordersResult.data
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val last24Hours = System.currentTimeMillis() - 24 * 60 * 60 * 1000

                    val stats = DashboardViewModel.OrdersStats(
                        totalOrders = orders.size,
                        pendingOrders = orders.count { it.status == OrderStatus.PENDING },
                        processingOrders = orders.count { it.status == OrderStatus.PROCESSING },
                        shippingOrders = orders.count { it.status == OrderStatus.IN_DELIVERY },
                        deliveredOrders = orders.count { it.status == OrderStatus.DELIVERED },
                        completedOrders = orders.count { it.status == OrderStatus.COMPLETED },
                        cancelledOrders = orders.count { it.status == OrderStatus.CANCELLED },
                        todayOrders = orders.count { it.createdAt >= today },
                        newOrdersCount = orders.count { it.createdAt >= last24Hours }
                    )

                    Resource.Success(stats)
                }
                is Resource.Error -> Resource.Error(ordersResult.message ?: "Ошибка загрузки заказов")
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении статистики заказов: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает данные о продажах по дням для аналитики
     */
    suspend fun getDailySalesData(startDate: Long, endDate: Long): Resource<List<Order>> {
        return try {
            val result = getOrdersByDateRange(startDate, endDate)
            when (result) {
                is Resource.Success -> {
                    val completedOrders = result.data.filter {
                        it.status in listOf(OrderStatus.DELIVERED, OrderStatus.COMPLETED)
                    }
                    Resource.Success(completedOrders)
                }
                is Resource.Error -> result
                is Resource.Loading -> result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении данных продаж по дням: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает данные о продажах по категориям
     */
    suspend fun getCategorySalesData(startDate: Long, endDate: Long): Resource<List<AnalyticsViewModel.CategorySalesData>> {
        return try {
            val ordersResult = getDailySalesData(startDate, endDate)
            when (ordersResult) {
                is Resource.Success -> {
                    val categoryStats = mutableMapOf<String, MutableMap<String, Any>>()

                    ordersResult.data.forEach { order ->
                        order.items.forEach { item ->
                            val categoryId = item.productId // Здесь нужно получить categoryId из товара
                            val categoryName = "Категория" // Здесь нужно получить имя категории

                            val stats = categoryStats.getOrPut(categoryId) {
                                mutableMapOf(
                                    "categoryId" to categoryId,
                                    "categoryName" to categoryName,
                                    "salesAmount" to 0.0,
                                    "itemsSold" to 0
                                )
                            }

                            stats["salesAmount"] = (stats["salesAmount"] as Double) + item.subtotal
                            stats["itemsSold"] = (stats["itemsSold"] as Int) + item.quantity
                        }
                    }

                    val categoryDataList = categoryStats.values.map { stats ->
                        AnalyticsViewModel.CategorySalesData(
                            categoryId = stats["categoryId"] as String,
                            categoryName = stats["categoryName"] as String,
                            salesAmount = stats["salesAmount"] as Double,
                            salesPercentage = 0.0, // Будет рассчитано в ViewModel
                            itemsSold = stats["itemsSold"] as Int
                        )
                    }

                    Resource.Success(categoryDataList)
                }
                is Resource.Error -> Resource.Error(ordersResult.message ?: "Ошибка загрузки данных продаж")
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении данных продаж по категориям: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает топ товаров для аналитики
     */
    suspend fun getTopProductsData(startDate: Long, endDate: Long): Resource<List<AnalyticsViewModel.TopProductData>> {
        return try {
            val ordersResult = getDailySalesData(startDate, endDate)
            when (ordersResult) {
                is Resource.Success -> {
                    val productStats = mutableMapOf<String, MutableMap<String, Any>>()

                    ordersResult.data.forEach { order ->
                        order.items.forEach { item ->
                            val stats = productStats.getOrPut(item.productId) {
                                mutableMapOf(
                                    "productId" to item.productId,
                                    "productName" to item.productName,
                                    "salesCount" to 0,
                                    "salesAmount" to 0.0
                                )
                            }

                            stats["salesCount"] = (stats["salesCount"] as Int) + item.quantity
                            stats["salesAmount"] = (stats["salesAmount"] as Double) + item.subtotal
                        }
                    }

                    val topProductsList = productStats.values
                        .sortedByDescending { it["salesCount"] as Int }
                        .map { stats ->
                            AnalyticsViewModel.TopProductData(
                                product = Product(
                                    id = stats["productId"] as String,
                                    name = stats["productName"] as String
                                ),
                                salesCount = stats["salesCount"] as Int,
                                salesAmount = stats["salesAmount"] as Double
                            )
                        }

                    Resource.Success(topProductsList)
                }
                is Resource.Error -> Resource.Error(ordersResult.message ?: "Ошибка загрузки данных продаж")
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении топ товаров: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает общую сводку продаж
     */
    suspend fun getSalesSummary(startDate: Long, endDate: Long): Resource<AnalyticsViewModel.SalesSummary> {
        return try {
            val ordersResult = getDailySalesData(startDate, endDate)
            when (ordersResult) {
                is Resource.Success -> {
                    val orders = ordersResult.data
                    val totalSales = orders.sumOf { it.total }
                    val averageOrderValue = if (orders.isNotEmpty()) totalSales / orders.size else 0.0

                    val summary = AnalyticsViewModel.SalesSummary(
                        totalSales = totalSales,
                        totalOrders = orders.size,
                        averageOrderValue = averageOrderValue,
                        topCategory = "Общие товары", // Здесь нужно определить топ категорию
                        growth = 0.0 // Здесь нужно рассчитать рост
                    )

                    Resource.Success(summary)
                }
                is Resource.Error -> Resource.Error(ordersResult.message ?: "Ошибка загрузки данных продаж")
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении сводки продаж: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает популярные товары для дашборда
     */
    suspend fun getPopularProducts(limit: Int = 5): Resource<List<DashboardViewModel.PopularProduct>> {
        return try {
            val last30Days = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000
            val topProductsResult = getTopProductsData(last30Days, System.currentTimeMillis())

            when (topProductsResult) {
                is Resource.Success -> {
                    val popularProducts = topProductsResult.data.take(limit).map { topProduct ->
                        DashboardViewModel.PopularProduct(
                            product = topProduct.product,
                            salesCount = topProduct.salesCount,
                            revenue = topProduct.salesAmount
                        )
                    }
                    Resource.Success(popularProducts)
                }
                is Resource.Error -> Resource.Error(topProductsResult.message ?: "Ошибка загрузки популярных товаров")
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении популярных товаров: ${e.message}", e)
            Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Получает поток заказов в реальном времени
     */
    fun getOrdersFlow(): Flow<Resource<List<Order>>> = flow {
        try {
            firestoreSource.getOrdersFlow().collect { resource ->
                emit(resource)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в потоке заказов: ${e.message}", e)
            emit(Resource.Error(e.message ?: "Неизвестная ошибка"))
        }
    }

    /**
     * Рассчитывает стоимость доставки
     */
    private fun calculateDeliveryFee(subtotal: Double, deliveryType: DeliveryType): Double {
        return when (deliveryType) {
            DeliveryType.PICKUP -> 0.0
            DeliveryType.DELIVERY -> {
                if (subtotal >= Constants.FREE_DELIVERY_THRESHOLD) {
                    0.0
                } else {
                    Constants.DEFAULT_DELIVERY_COST
                }
            }
        }
    }

    /**
     * Рассчитывает предполагаемую дату доставки
     */
    private fun calculateEstimatedDeliveryDate(deliveryType: DeliveryType): Long {
        val calendar = Calendar.getInstance()
        return when (deliveryType) {
            DeliveryType.PICKUP -> {
                calendar.add(Calendar.HOUR, 2) // Через 2 часа для самовывоза
                calendar.timeInMillis
            }
            DeliveryType.DELIVERY -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1) // На следующий день для доставки
                calendar.timeInMillis
            }
        }
    }
}