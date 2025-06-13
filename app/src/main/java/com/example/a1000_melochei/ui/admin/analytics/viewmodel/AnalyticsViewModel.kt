package com.example.a1000_melochei.ui.admin.analytics.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Product
import com.example.a1000_melochei.data.repository.OrderRepository
import com.example.a1000_melochei.data.repository.ProductRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ViewModel для работы с аналитическими данными магазина
 */
class AnalyticsViewModel(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val TAG = "AnalyticsViewModel"

    // Класс для хранения данных о продажах по дням
    data class DailySalesData(
        val date: String,
        val salesAmount: Double,
        val ordersCount: Int
    )

    // Класс для хранения данных о продажах по категориям
    data class CategorySalesData(
        val categoryId: String,
        val categoryName: String,
        val salesAmount: Double,
        val salesPercentage: Double,
        val itemsSold: Int
    )

    // Класс для хранения данных о самых продаваемых товарах
    data class TopProductData(
        val product: Product,
        val salesCount: Int,
        val salesAmount: Double
    )

    // Класс для хранения данных о общих показателях
    data class SalesSummary(
        val totalSales: Double,
        val totalOrders: Int,
        val averageOrderValue: Double,
        val topCategory: String,
        val growth: Double // Рост в процентах по сравнению с предыдущим периодом
    )

    // LiveData для данных о продажах по дням
    private val _dailySalesData = MutableLiveData<Resource<List<DailySalesData>>>()
    val dailySalesData: LiveData<Resource<List<DailySalesData>>> = _dailySalesData

    // LiveData для данных о продажах по категориям
    private val _categorySalesData = MutableLiveData<Resource<List<CategorySalesData>>>()
    val categorySalesData: LiveData<Resource<List<CategorySalesData>>> = _categorySalesData

    // LiveData для топ товаров
    private val _topProductsData = MutableLiveData<Resource<List<TopProductData>>>()
    val topProductsData: LiveData<Resource<List<TopProductData>>> = _topProductsData

    // LiveData для общих показателей
    private val _salesSummary = MutableLiveData<Resource<SalesSummary>>()
    val salesSummary: LiveData<Resource<SalesSummary>> = _salesSummary

    // Текущий выбранный период
    private var selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.LAST_30_DAYS

    /**
     * Загружает аналитические данные для выбранного периода
     */
    fun loadAnalyticsData(period: AnalyticsPeriod = selectedPeriod) {
        selectedPeriod = period

        viewModelScope.launch {
            try {
                // Определяем временные рамки для анализа
                val dateRange = getDateRange(period)

                // Загружаем данные параллельно
                val dailySalesDeferred = async { loadDailySalesData(dateRange) }
                val categorySalesDeferred = async { loadCategorySalesData(dateRange) }
                val topProductsDeferred = async { loadTopProductsData(dateRange) }
                val summaryDeferred = async { loadSalesSummary(dateRange) }

                // Ожидаем завершения всех операций
                dailySalesDeferred.await()
                categorySalesDeferred.await()
                topProductsDeferred.await()
                summaryDeferred.await()

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке аналитических данных: ${e.message}", e)
                _dailySalesData.value = Resource.Error(e.message ?: "Неизвестная ошибка")
                _categorySalesData.value = Resource.Error(e.message ?: "Неизвестная ошибка")
                _topProductsData.value = Resource.Error(e.message ?: "Неизвестная ошибка")
                _salesSummary.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает данные о продажах по дням
     */
    private suspend fun loadDailySalesData(dateRange: Pair<Long, Long>) {
        _dailySalesData.value = Resource.Loading()

        try {
            val result = orderRepository.getDailySalesData(dateRange.first, dateRange.second)
            when (result) {
                is Resource.Success -> {
                    val dailyData = result.data.map { order ->
                        val date = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(order.createdAt))
                        DailySalesData(
                            date = date,
                            salesAmount = order.total,
                            ordersCount = 1
                        )
                    }.groupBy { it.date }
                        .map { (date, orders) ->
                            DailySalesData(
                                date = date,
                                salesAmount = orders.sumOf { it.salesAmount },
                                ordersCount = orders.size
                            )
                        }
                        .sortedBy { it.date }

                    _dailySalesData.value = Resource.Success(dailyData)
                }
                is Resource.Error -> {
                    _dailySalesData.value = Resource.Error(result.message)
                }
                is Resource.Loading -> {
                    // Уже установлено выше
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке данных продаж по дням: ${e.message}", e)
            _dailySalesData.value = Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Загружает данные о продажах по категориям
     */
    private suspend fun loadCategorySalesData(dateRange: Pair<Long, Long>) {
        _categorySalesData.value = Resource.Loading()

        try {
            val result = orderRepository.getCategorySalesData(dateRange.first, dateRange.second)
            when (result) {
                is Resource.Success -> {
                    val totalSales = result.data.sumOf { it.salesAmount }

                    val categoryData = result.data.map { category ->
                        CategorySalesData(
                            categoryId = category.categoryId,
                            categoryName = category.categoryName,
                            salesAmount = category.salesAmount,
                            salesPercentage = if (totalSales > 0) (category.salesAmount / totalSales * 100) else 0.0,
                            itemsSold = category.itemsSold
                        )
                    }.sortedByDescending { it.salesAmount }

                    _categorySalesData.value = Resource.Success(categoryData)
                }
                is Resource.Error -> {
                    _categorySalesData.value = Resource.Error(result.message)
                }
                is Resource.Loading -> {
                    // Уже установлено выше
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке данных продаж по категориям: ${e.message}", e)
            _categorySalesData.value = Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Загружает данные о топ товарах
     */
    private suspend fun loadTopProductsData(dateRange: Pair<Long, Long>) {
        _topProductsData.value = Resource.Loading()

        try {
            val result = orderRepository.getTopProductsData(dateRange.first, dateRange.second)
            when (result) {
                is Resource.Success -> {
                    val topProducts = result.data.take(10) // Топ 10 товаров
                    _topProductsData.value = Resource.Success(topProducts)
                }
                is Resource.Error -> {
                    _topProductsData.value = Resource.Error(result.message)
                }
                is Resource.Loading -> {
                    // Уже установлено выше
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке данных топ товаров: ${e.message}", e)
            _topProductsData.value = Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Загружает общие показатели продаж
     */
    private suspend fun loadSalesSummary(dateRange: Pair<Long, Long>) {
        _salesSummary.value = Resource.Loading()

        try {
            val result = orderRepository.getSalesSummary(dateRange.first, dateRange.second)
            when (result) {
                is Resource.Success -> {
                    _salesSummary.value = Resource.Success(result.data)
                }
                is Resource.Error -> {
                    _salesSummary.value = Resource.Error(result.message)
                }
                is Resource.Loading -> {
                    // Уже установлено выше
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке общих показателей: ${e.message}", e)
            _salesSummary.value = Resource.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    /**
     * Возвращает временные рамки для выбранного периода
     */
    private fun getDateRange(period: AnalyticsPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        when (period) {
            AnalyticsPeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return Pair(calendar.timeInMillis, endTime)
            }
            AnalyticsPeriod.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                return Pair(calendar.timeInMillis, endTime)
            }
            AnalyticsPeriod.LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                return Pair(calendar.timeInMillis, endTime)
            }
            AnalyticsPeriod.LAST_YEAR -> {
                calendar.add(Calendar.YEAR, -1)
                return Pair(calendar.timeInMillis, endTime)
            }
            AnalyticsPeriod.CUSTOM -> {
                // Для пользовательского периода используем последние 30 дней по умолчанию
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                return Pair(calendar.timeInMillis, endTime)
            }
        }
    }

    /**
     * Обновляет данные аналитики
     */
    fun refreshData() {
        loadAnalyticsData(selectedPeriod)
    }

    /**
     * Экспортирует данные аналитики в CSV
     */
    fun exportAnalyticsData(): String {
        // Здесь будет логика экспорта данных в CSV формат
        return "Экспорт данных пока не реализован"
    }
}

/**
 * Перечисление периодов для аналитики
 */
enum class AnalyticsPeriod {
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_YEAR,
    CUSTOM
}