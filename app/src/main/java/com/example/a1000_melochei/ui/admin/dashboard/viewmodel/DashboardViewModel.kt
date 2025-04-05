package com..app.ui.admin.dashboard.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melochei1000.app.data.common.Resource
import com.melochei1000.app.data.model.Order
import com.melochei1000.app.data.model.Product
import com.melochei1000.app.data.repository.OrderRepository
import com.melochei1000.app.data.repository.ProductRepository
import com.melochei1000.app.data.repository.CategoryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

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
        val cancelledOrders: Int
    )

    // Данные о продажах
    data class SalesStats(
        val totalSales: Double,
        val averageOrderValue: Double,
        val todaySales: Double,
        val weekSales: Double,
        val monthSales: Double
    )

    // Данные о товарах
    data class ProductsStats(
        val totalProducts: Int,
        val outOfStockProducts: Int,
        val lowStockProducts: Int,
        val categoriesCount: Int
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
    private val _latestOrders = MutableLiveData<Resource<List<Order>>>()
    val latestOrders: LiveData<Resource<List<Order>>> = _latestOrders

    // LiveData для популярных товаров
    private val _popularProducts = MutableLiveData<Resource<List<Product>>>()
    val popularProducts: LiveData<Resource<List<Product>>> = _popularProducts

    /**
     * Загружает все данные для панели управления
     */
    fun loadDashboardData() {
        loadOrdersStats()
        loadSalesStats()
        loadProductsStats()
        loadLatestOrders()
        loadPopularProducts()
    }

    /**
     * Загружает статистику заказов
     */
    private fun loadOrdersStats() {
        _ordersStats.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = orderRepository.getOrdersStats()
                _ordersStats.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке статистики заказов: ${e.message}")
                _ordersStats.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает статистику продаж
     */
    private fun loadSalesStats() {
        _salesStats.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = orderRepository.getSalesStats()
                _salesStats.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке статистики продаж: ${e.message}")
                _salesStats.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает статистику товаров
     */
    private fun loadProductsStats() {
        _productsStats.value = Resource.Loading()

        viewModelScope.launch {
            try {
                // Создаем задачи для параллельного выполнения
                val productsStatsDeferred = async { productRepository.getProductsStats() }
                val categoriesCountDeferred = async { categoryRepository.getCategoriesCount() }

                // Ожидаем результатов
                val productsStatsResult = productsStatsDeferred.await()
                val categoriesCountResult = categoriesCountDeferred.await()

                // Проверяем результаты
                if (productsStatsResult is Resource.Success && categoriesCountResult is Resource.Success) {
                    val productsStats = productsStatsResult.data
                    val categoriesCount = categoriesCountResult.data

                    if (productsStats != null && categoriesCount != null) {
                        // Создаем объект статистики товаров
                        val stats = ProductsStats(
                            totalProducts = productsStats.totalProducts,
                            outOfStockProducts = productsStats.outOfStockProducts,
                            lowStockProducts = productsStats.lowStockProducts,
                            categoriesCount = categoriesCount
                        )
                        _productsStats.value = Resource.Success(stats)
                    } else {
                        _productsStats.value = Resource.Error("Не удалось получить статистику товаров")
                    }
                } else {
                    val errorMessage = productsStatsResult.message ?: categoriesCountResult.message
                    _productsStats.value = Resource.Error(errorMessage ?: "Не удалось получить статистику товаров")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке статистики товаров: ${e.message}")
                _productsStats.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает последние заказы
     */
    private fun loadLatestOrders() {
        _latestOrders.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = orderRepository.getLatestOrders(3) // Получаем последние 3 заказа
                _latestOrders.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке последних заказов: ${e.message}")
                _latestOrders.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает популярные товары
     */
    private fun loadPopularProducts() {
        _popularProducts.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = productRepository.getPopularProducts(3) // Получаем 3 популярных товара
                _popularProducts.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке популярных товаров: ${e.message}")
                _popularProducts.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }
}