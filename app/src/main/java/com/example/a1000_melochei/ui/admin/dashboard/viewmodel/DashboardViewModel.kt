package com.example.a1000_melochei.ui.admin.dashboard.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Order
import com.example.a1000_melochei.data.model.OrderStatus
import com.example.a1000_melochei.data.model.Product
import com.example.a1000_melochei.data.repository.OrderRepository
import com.example.a1000_melochei.data.repository.ProductRepository
import com.example.a1000_melochei.data.repository.CategoryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * ViewModel для панели управления администратора
 */
class DashboardViewModel(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val TAG = "DashboardViewModel"

    // Данные о заказах
    data class OrdersStats(
        val totalOrders: Int,
        val pendingOrders: Int,
        val processingOrders: Int,
        val shippingOrders: Int,
        val deliveredOrders: Int,
        val completedOrders: Int,
        val cancelledOrders: Int,
        val todayOrders: Int,
        val newOrdersCount: Int // Заказы за последние 24 часа
    )

    // Данные о продажах
    data class SalesStats(
        val totalSales: Double,
        val averageOrderValue: Double,
        val todaySales: Double,
        val weekSales: Double,
        val monthSales: Double,
        val salesGrowth: Double // Рост продаж в процентах
    )

    // Данные о товарах
    data class ProductsStats(
        val totalProducts: Int,
        val activeProducts: Int,
        val outOfStockProducts: Int,
        val lowStockProducts: Int,
        val categoriesCount: Int,
        val recentlyAddedProducts: Int
    )

    // Последние заказы для быстрого просмотра
    data class RecentOrder(
        val id: String,
        val orderNumber: String,
        val customerName: String,
        val total: Double,
        val status: OrderStatus,
        val createdAt: Long
    )

    // Популярные товары
    data class PopularProduct(
        val product: Product,
        val salesCount: Int,
        val revenue: Double
    )

    // LiveData для статистики заказов
    private val _ordersStats = MutableLiveData<Resource<OrdersStats>>()
    val ordersStats: LiveData<Resource<OrdersStats>> = _ordersStats

    // LiveData для статистики продаж
    private val _salesStats = MutableLiveData<Resource<SalesStats>>()
    val salesStats: LiveData<Resource<SalesStats>> = _salesStats

    // LiveData для статистики товаров
    private val _productsStats = MutableLiveData<Resource<ProductsStats>>()
    val productsStats: LiveData<Resource<ProductsStats>> = _productsStats

    // LiveData для последних заказов
    private val _recentOrders = MutableLiveData<Resource<List<RecentOrder>>>()
    val recentOrders: LiveData<Resource<List<RecentOrder>>> = _recentOrders

    // LiveData для популярных товаров
    private val _popularProducts = MutableLiveData<Resource<List<PopularProduct>>>()
    val popularProducts: LiveData<Resource<List<PopularProduct>>> = _popularProducts

    // LiveData для уведомлений
    private val _notifications = MutableLiveData<Resource<List<String>>>()
    val notifications: LiveData<Resource<List<String>>> = _notifications

    /**
     * Загружает все данные для дашборда
     */
    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                // Загружаем все данные параллельно
                val ordersStatsDeferred = async { loadOrdersStats() }
                val salesStatsDeferred = async { loadSalesStats() }
                val productsStatsDeferred = async { loadProductsStats() }
                val recentOrdersDeferred = async { loadRecentOrders() }
                val popularProductsDeferred = async { loadPopularProducts() }
                val notificationsDeferred = async { loadNotifications() }

                // Ждем завершения всех операций
                ordersStatsDeferred.await()
                salesStatsDeferred.await()
                productsStatsDeferred.await()
                recentOrdersDeferred.await()
                popularProductsDeferred.await()
                notificationsDeferred.await()

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке данных дашборда: ${e.message}", e)
            }
        }
    }

    /**
     * Загружает статистику заказов
     */
    private suspend fun loadOrdersStats() {
        _ordersStats.value = Resource.Loading()

        try {
            val result = orderRepository.getAllOrders()
            when (result) {
                is Resource.Success -> {
                    val orders = result.data
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val last24Hours = System.currentTimeMillis() - 24 * 60 * 60 * 1000

                    val stats = OrdersStats(
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

                    _ordersStats.value = Resource.Success(stats)
                }
                is Resource.Error -> {
                    _ordersStats.value = Resource.Error(result.message ?: "Ошибка загрузки статистики заказов")
                }
                is Resource.Loading -> {
                    // Уже установлено выше
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке статистики заказов: ${e.message}", e)
            _ordersStats.value = Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Загружает статистику продаж
     */
    private suspend fun loadSalesStats() {
        _salesStats.value = Resource.Loading()

        try {
            val result = orderRepository.getAllOrders()
            when (result) {
                is Resource.Success -> {
                    val orders = result.data.filter {
                        it.status in listOf(OrderStatus.DELIVERED, OrderStatus.COMPLETED)
                    }

                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val weekAgo = today - 7 * 24 * 60 * 60 * 1000
                    val monthAgo = today - 30 * 24 * 60 * 60 * 1000
                    val lastMonthStart = monthAgo - 30 * 24 * 60 * 60 * 1000

                    val totalSales = orders.sumOf { it.total }
                    val todaySales = orders.filter { it.createdAt >= today }.sumOf { it.total }
                    val weekSales = orders.filter { it.createdAt >= weekAgo }.sumOf { it.total }
                    val monthSales = orders.filter { it.createdAt >= monthAgo }.sumOf { it.total }
                    val lastMonthSales = orders.filter {
                        it.createdAt >= lastMonthStart && it.createdAt < monthAgo
                    }.sumOf { it.total }

                    val averageOrderValue = if (orders.isNotEmpty()) totalSales / orders.size else 0.0
                    val salesGrowth = if (lastMonthSales > 0) {
                        ((monthSales - lastMonthSales) / lastMonthSales * 100)
                    } else {
                        0.0
                    }

                    val stats = SalesStats(
                        totalSales = totalSales,
                        averageOrderValue = averageOrderValue,
                        todaySales = todaySales,
                        weekSales = weekSales,
                        monthSales = monthSales,
                        salesGrowth = salesGrowth
                    )

                    _salesStats.value = Resource.Success(stats)
                }
                is Resource.Error -> {
                    _salesStats.value = Resource.Error(result.message ?: "Ошибка загрузки статистики продаж")
                }
                is Resource.Loading -> {
                    // Уже установлено выше
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке статистики продаж: ${e.message}", e)
            _salesStats.value = Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Загружает статистику товаров
     */
    private suspend fun loadProductsStats() {
        _productsStats.value = Resource.Loading()

        try {
            val productsResult = productRepository.getAllProducts()
            val categoriesResult = categoryRepository.getCategories()

            when {
                productsResult is Resource.Success && categoriesResult is Resource.Success -> {
                    val products = productsResult.data
                    val categories = categoriesResult.data

                    val last7Days = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000

                    val stats = ProductsStats(
                        totalProducts = products.size,
                        activeProducts = products.count { it.isActive },
                        outOfStockProducts = products.count { it.quantity <= 0 },
                        lowStockProducts = products.count { it.quantity in 1..5 },
                        categoriesCount = categories.size,
                        recentlyAddedProducts = products.count { it.createdAt >= last7Days }
                    )

                    _productsStats.value = Resource.Success(stats)
                }
                productsResult is Resource.Error -> {
                    _productsStats.value = Resource.Error(productsResult.message ?: "Ошибка загрузки товаров")
                }
                categoriesResult is Resource.Error -> {
                    _productsStats.value = Resource.Error(categoriesResult.message ?: "Ошибка загрузки категорий")
                }
                else -> {
                    // Состояние загрузки уже установлено
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке статистики товаров: ${e.message}", e)
            _productsStats.value = Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Загружает последние заказы
     */
    private suspend fun loadRecentOrders() {
        _recentOrders.value = Resource.Loading()

        try {
            val result = orderRepository.getRecentOrders(10) // Последние 10 заказов
            when (result) {
                is Resource.Success -> {
                    val recentOrdersList = result.data.map { order ->
                        RecentOrder(
                            id = order.id,
                            orderNumber = order.orderNumber,
                            customerName = order.customerInfo.name,
                            total = order.total,
                            status = order.status,
                            createdAt = order.createdAt
                        )
                    }

                    _recentOrders.value = Resource.Success(recentOrdersList)
                }
                is Resource.Error -> {
                    _recentOrders.value = Resource.Error(result.message ?: "Ошибка загрузки последних заказов")
                }
                is Resource.Loading -> {
                    // Уже установлено выше
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке последних заказов: ${e.message}", e)
            _recentOrders.value = Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Загружает популярные товары
     */
    private suspend fun loadPopularProducts() {
        _popularProducts.value = Resource.Loading()

        try {
            val result = orderRepository.getPopularProducts(5) // Топ 5 товаров
            when (result) {
                is Resource.Success -> {
                    _popularProducts.value = Resource.Success(result.data)
                }
                is Resource.Error -> {
                    _popularProducts.value = Resource.Error(result.message ?: "Ошибка загрузки популярных товаров")
                }
                is Resource.Loading -> {
                    // Уже установлено выше
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке популярных товаров: ${e.message}", e)
            _popularProducts.value = Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Загружает уведомления для администратора
     */
    private suspend fun loadNotifications() {
        _notifications.value = Resource.Loading()

        try {
            val notifications = mutableListOf<String>()

            // Проверяем товары с низким остатком
            val productsResult = productRepository.getAllProducts()
            if (productsResult is Resource.Success) {
                val lowStockProducts = productsResult.data.filter { it.quantity in 1..5 }
                if (lowStockProducts.isNotEmpty()) {
                    notifications.add("${lowStockProducts.size} товаров заканчивается")
                }

                val outOfStockProducts = productsResult.data.filter { it.quantity <= 0 }
                if (outOfStockProducts.isNotEmpty()) {
                    notifications.add("${outOfStockProducts.size} товаров нет в наличии")
                }
            }

            // Проверяем новые заказы
            val ordersResult = orderRepository.getAllOrders()
            if (ordersResult is Resource.Success) {
                val pendingOrders = ordersResult.data.filter { it.status == OrderStatus.PENDING }
                if (pendingOrders.isNotEmpty()) {
                    notifications.add("${pendingOrders.size} новых заказов требуют обработки")
                }
            }

            _notifications.value = Resource.Success(notifications)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке уведомлений: ${e.message}", e)
            _notifications.value = Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Обновляет все данные дашборда
     */
    fun refreshData() {
        loadDashboardData()
    }

    /**
     * Получает краткую сводку для быстрого просмотра
     */
    fun getQuickSummary(): String {
        val ordersStats = _ordersStats.value?.getDataOrNull()
        val salesStats = _salesStats.value?.getDataOrNull()

        return when {
            ordersStats != null && salesStats != null -> {
                "Заказы: ${ordersStats.newOrdersCount} новых, " +
                        "Продажи сегодня: ${salesStats.todaySales.toInt()} ₸"
            }
            else -> "Загрузка данных..."
        }
    }
}