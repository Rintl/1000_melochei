package com.yourstore.app.ui.admin.analytics.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Product
import com.yourstore.app.data.repository.OrderRepository
import com.yourstore.app.data.repository.ProductRepository
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
        val comparisonPercentage: Double // процент изменения по сравнению с предыдущим периодом
    )

    // LiveData для данных о продажах по дням
    private val _dailySalesData = MutableLiveData<Resource<List<DailySalesData>>>()
    val dailySalesData: LiveData<Resource<List<DailySalesData>>> = _dailySalesData

    // LiveData для данных о продажах по категориям
    private val _categorySalesData = MutableLiveData<Resource<List<CategorySalesData>>>()
    val categorySalesData: LiveData<Resource<List<CategorySalesData>>> = _categorySalesData

    // LiveData для данных о самых продаваемых товарах
    private val _topProducts = MutableLiveData<Resource<List<TopProductData>>>()
    val topProducts: LiveData<Resource<List<TopProductData>>> = _topProducts

    // LiveData для общих показателей текущего периода
    private val _currentPeriodSummary = MutableLiveData<Resource<SalesSummary>>()
    val currentPeriodSummary: LiveData<Resource<SalesSummary>> = _currentPeriodSummary

    // LiveData для общих показателей предыдущего периода
    private val _previousPeriodSummary = MutableLiveData<Resource<SalesSummary>>()
    val previousPeriodSummary: LiveData<Resource<SalesSummary>> = _previousPeriodSummary

    /**
     * Загружает все аналитические данные за указанный период
     * @param startDate начальная дата периода
     * @param endDate конечная дата периода
     * @param compareWithPrevious сравнивать ли с предыдущим аналогичным периодом
     */
    fun loadAnalyticsData(
        startDate: Date,
        endDate: Date,
        compareWithPrevious: Boolean = true
    ) {
        loadDailySalesData(startDate, endDate)
        loadCategorySalesData(startDate, endDate)
        loadTopProducts(startDate, endDate)
        loadSalesSummary(startDate, endDate)

        if (compareWithPrevious) {
            // Вычисляем предыдущий период той же длительности
            val periodDifference = endDate.time - startDate.time
            val previousEndDate = Date(startDate.time - 1) // день перед начальной датой
            val previousStartDate = Date(previousEndDate.time - periodDifference)

            loadPreviousPeriodSummary(previousStartDate, previousEndDate)
        }
    }

    /**
     * Загружает данные о продажах по дням
     */
    private fun loadDailySalesData(startDate: Date, endDate: Date) {
        _dailySalesData.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = orderRepository.getDailySalesData(startDate.time, endDate.time)

                if (result is Resource.Success) {
                    // Преобразуем сырые данные в формат для отображения
                    val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
                    val dailySales = result.data?.map { (timestamp, amount, count) ->
                        DailySalesData(
                            date = dateFormat.format(Date(timestamp)),
                            salesAmount = amount,
                            ordersCount = count
                        )
                    } ?: emptyList()

                    _dailySalesData.value = Resource.Success(dailySales)
                } else {
                    _dailySalesData.value = Resource.Error(
                        result.message ?: "Ошибка при загрузке данных о продажах по дням"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке данных о продажах по дням: ${e.message}")
                _dailySalesData.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает данные о продажах по категориям
     */
    private fun loadCategorySalesData(startDate: Date, endDate: Date) {
        _categorySalesData.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = orderRepository.getCategorySalesData(startDate.time, endDate.time)

                if (result is Resource.Success) {
                    val totalSales = result.data?.sumOf { it.salesAmount } ?: 0.0

                    // Преобразуем сырые данные в формат для отображения и рассчитываем проценты
                    val categorySales = result.data?.map { data ->
                        CategorySalesData(
                            categoryId = data.categoryId,
                            categoryName = data.categoryName,
                            salesAmount = data.salesAmount,
                            salesPercentage = if (totalSales > 0) (data.salesAmount / totalSales) * 100 else 0.0,
                            itemsSold = data.itemsSold
                        )
                    }?.sortedByDescending { it.salesAmount } ?: emptyList()

                    _categorySalesData.value = Resource.Success(categorySales)
                } else {
                    _categorySalesData.value = Resource.Error(
                        result.message ?: "Ошибка при загрузке данных о продажах по категориям"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке данных о продажах по категориям: ${e.message}")
                _categorySalesData.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает данные о самых продаваемых товарах
     */
    private fun loadTopProducts(startDate: Date, endDate: Date) {
        _topProducts.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = orderRepository.getTopSellingProducts(startDate.time, endDate.time, 10)

                if (result is Resource.Success) {
                    // Загружаем дополнительные данные о продуктах
                    val topProductsWithDetails = result.data?.mapNotNull { data ->
                        // Загружаем информацию о продукте
                        val productResult = productRepository.getProductById(data.productId)
                        if (productResult is Resource.Success && productResult.data != null) {
                            TopProductData(
                                product = productResult.data,
                                salesCount = data.salesCount,
                                salesAmount = data.salesAmount
                            )
                        } else {
                            null
                        }
                    }?.sortedByDescending { it.salesCount } ?: emptyList()

                    _topProducts.value = Resource.Success(topProductsWithDetails)
                } else {
                    _topProducts.value = Resource.Error(
                        result.message ?: "Ошибка при загрузке данных о топовых товарах"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке данных о топовых товарах: ${e.message}")
                _topProducts.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает обобщенные данные о продажах за текущий период
     */
    private fun loadSalesSummary(startDate: Date, endDate: Date) {
        _currentPeriodSummary.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = orderRepository.getSalesSummary(startDate.time, endDate.time)

                if (result is Resource.Success && result.data != null) {
                    val data = result.data
                    val summary = SalesSummary(
                        totalSales = data.totalSales,
                        totalOrders = data.ordersCount,
                        averageOrderValue = if (data.ordersCount > 0) data.totalSales / data.ordersCount else 0.0,
                        comparisonPercentage = 0.0 // заполним позже при сравнении с предыдущим периодом
                    )

                    _currentPeriodSummary.value = Resource.Success(summary)
                } else {
                    _currentPeriodSummary.value = Resource.Error(
                        result.message ?: "Ошибка при загрузке общих данных о продажах"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке общих данных о продажах: ${e.message}")
                _currentPeriodSummary.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает обобщенные данные о продажах за предыдущий период
     */
    private fun loadPreviousPeriodSummary(startDate: Date, endDate: Date) {
        _previousPeriodSummary.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = orderRepository.getSalesSummary(startDate.time, endDate.time)

                if (result is Resource.Success && result.data != null) {
                    val data = result.data
                    val summary = SalesSummary(
                        totalSales = data.totalSales,
                        totalOrders = data.ordersCount,
                        averageOrderValue = if (data.ordersCount > 0) data.totalSales / data.ordersCount else 0.0,
                        comparisonPercentage = 0.0 // не используется для предыдущего периода
                    )

                    _previousPeriodSummary.value = Resource.Success(summary)

                    // Теперь вычисляем процент изменения и обновляем текущий период
                    calculateComparisonPercentage()
                } else {
                    _previousPeriodSummary.value = Resource.Error(
                        result.message ?: "Ошибка при загрузке данных о продажах за предыдущий период"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке данных о продажах за предыдущий период: ${e.message}")
                _previousPeriodSummary.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Вычисляет процент изменения в сравнении с предыдущим периодом
     */
    private fun calculateComparisonPercentage() {
        val current = _currentPeriodSummary.value
        val previous = _previousPeriodSummary.value

        if (current is Resource.Success && previous is Resource.Success &&
            current.data != null && previous.data != null) {

            val currentSales = current.data.totalSales
            val previousSales = previous.data.totalSales

            val percentage = if (previousSales > 0) {
                ((currentSales - previousSales) / previousSales) * 100
            } else {
                100.0 // если в предыдущем периоде продаж не было, считаем рост как 100%
            }

            // Создаем новый объект с обновленным процентом сравнения
            val updatedSummary = current.data.copy(comparisonPercentage = percentage)
            _currentPeriodSummary.value = Resource.Success(updatedSummary)
        }
    }

    /**
     * Загружает данные об общей статистике продаж по товарам
     */
    fun loadProductsPerformanceStats() {
        viewModelScope.launch {
            try {
                // Здесь можно реализовать дополнительную аналитику по товарам
                // Например, статистику возвратов, рейтинги товаров и т.д.
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке статистики товаров: ${e.message}")
            }
        }
    }

    /**
     * Загружает данные о предзаказах и товарах "в ожидании"
     */
    fun loadPreorderStats() {
        viewModelScope.launch {
            try {
                // Здесь можно реализовать статистику по предзаказам
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке статистики предзаказов: ${e.message}")
            }
        }
    }

    /**
     * Формирует данные для предустановленных периодов (сегодня, неделя, месяц, год)
     */
    fun loadDataForPredefinedPeriod(periodType: PeriodType) {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time

        val startDate = when (periodType) {
            PeriodType.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.time
            }
            PeriodType.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.time
            }
            PeriodType.MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.time
            }
            PeriodType.YEAR -> {
                calendar.add(Calendar.YEAR, -1)
                calendar.time
            }
        }

        loadAnalyticsData(startDate, endDate)
    }

    /**
     * Перечисление типов периодов для предустановленных фильтров
     */
    enum class PeriodType {
        TODAY, WEEK, MONTH, YEAR
    }
}