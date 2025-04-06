package com.yourstore.app.ui.admin.orders.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Order
import com.yourstore.app.data.model.OrderStatus
import com.yourstore.app.data.repository.OrderRepository
import kotlinx.coroutines.launch

/**
 * ViewModel для управления заказами в админской панели
 */
class AdminOrderViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val TAG = "AdminOrderViewModel"

    // LiveData для списка всех заказов
    private val _orders = MutableLiveData<Resource<List<Order>>>()
    val orders: LiveData<Resource<List<Order>>> = _orders

    // LiveData для отфильтрованного списка заказов
    private val _filteredOrders = MutableLiveData<Resource<List<Order>>>()
    val filteredOrders: LiveData<Resource<List<Order>>> = _filteredOrders

    // LiveData для детальной информации о заказе
    private val _orderDetails = MutableLiveData<Resource<Order>>()
    val orderDetails: LiveData<Resource<Order>> = _orderDetails

    // LiveData для результата обновления статуса заказа
    private val _updateStatusResult = MutableLiveData<Resource<Unit>>()
    val updateStatusResult: LiveData<Resource<Unit>> = _updateStatusResult

    // LiveData для счетчика новых заказов
    private val _newOrdersCount = MutableLiveData<Int>()
    val newOrdersCount: LiveData<Int> = _newOrdersCount

    // Оригинальный список заказов (без фильтрации)
    private var originalOrders: List<Order> = emptyList()

    /**
     * Загружает список всех заказов
     */
    fun loadOrders() {
        _orders.value = Resource.Loading()
        _filteredOrders.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = orderRepository.getAllOrders()

                if (result is Resource.Success) {
                    originalOrders = result.data ?: emptyList()
                    _orders.value = result
                    _filteredOrders.value = result

                    // Обновляем счетчик новых заказов
                    updateNewOrdersCount()
                } else {
                    _orders.value = result
                    _filteredOrders.value = result
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке заказов: ${e.message}")
                _orders.value = Resource.Error(e.message ?: "Неизвестная ошибка")
                _filteredOrders.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает детальную информацию о заказе по его идентификатору
     */
    fun loadOrderDetails(orderId: String) {
        _orderDetails.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = orderRepository.getOrderById(orderId)
                _orderDetails.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке деталей заказа: ${e.message}")
                _orderDetails.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Обновляет статус заказа
     */
    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        _updateStatusResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = orderRepository.updateOrderStatus(orderId, newStatus)
                _updateStatusResult.value = result

                // Если статус успешно обновлен, обновляем детали заказа
                if (result is Resource.Success) {
                    loadOrderDetails(orderId)
                    // Также обновляем список всех заказов
                    loadOrders()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении статуса заказа: ${e.message}")
                _updateStatusResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Фильтрует заказы по статусу
     * @param statuses список статусов для фильтрации, null - показать все
     */
    fun filterOrdersByStatus(statuses: List<OrderStatus>? = null) {
        if (statuses == null) {
            _filteredOrders.value = Resource.Success(originalOrders)
            return
        }

        val filteredList = originalOrders.filter { order ->
            order.status in statuses
        }

        _filteredOrders.value = Resource.Success(filteredList)
    }

    /**
     * Фильтрует заказы по периоду создания
     * @param fromTimestamp начальная дата (timestamp)
     * @param toTimestamp конечная дата (timestamp)
     */
    fun filterOrdersByPeriod(fromTimestamp: Long, toTimestamp: Long) {
        val filteredList = originalOrders.filter { order ->
            order.createdAt in fromTimestamp..toTimestamp
        }

        _filteredOrders.value = Resource.Success(filteredList)
    }

    /**
     * Поиск заказов по номеру или имени клиента
     * @param query поисковый запрос
     */
    fun searchOrders(query: String) {
        if (query.isBlank()) {
            _filteredOrders.value = Resource.Success(originalOrders)
            return
        }

        val searchQuery = query.lowercase().trim()
        val filteredList = originalOrders.filter { order ->
            order.number.lowercase().contains(searchQuery) ||
                    order.userName.lowercase().contains(searchQuery) ||
                    order.userPhone.lowercase().contains(searchQuery)
        }

        _filteredOrders.value = Resource.Success(filteredList)
    }

    /**
     * Сортирует заказы по дате создания
     * @param ascending true - по возрастанию, false - по убыванию
     */
    fun sortOrdersByDate(ascending: Boolean) {
        val currentList = _filteredOrders.value?.data ?: return

        val sortedList = if (ascending) {
            currentList.sortedBy { it.createdAt }
        } else {
            currentList.sortedByDescending { it.createdAt }
        }

        _filteredOrders.value = Resource.Success(sortedList)
    }

    /**
     * Сортирует заказы по статусу
     */
    fun sortOrdersByStatus() {
        val currentList = _filteredOrders.value?.data ?: return

        val statusPriority = mapOf(
            OrderStatus.PENDING to 0,
            OrderStatus.PROCESSING to 1,
            OrderStatus.SHIPPING to 2,
            OrderStatus.DELIVERED to 3,
            OrderStatus.COMPLETED to 4,
            OrderStatus.CANCELLED to 5
        )

        val sortedList = currentList.sortedBy { statusPriority[it.status] }
        _filteredOrders.value = Resource.Success(sortedList)
    }

    /**
     * Обновляет счетчик новых заказов
     */
    private fun updateNewOrdersCount() {
        val pendingOrders = originalOrders.count { it.status == OrderStatus.PENDING }
        _newOrdersCount.value = pendingOrders
    }

    /**
     * Отправляет уведомление клиенту о статусе заказа
     */
    fun sendOrderStatusNotification(orderId: String) {
        viewModelScope.launch {
            try {
                orderRepository.sendOrderStatusNotification(orderId)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при отправке уведомления: ${e.message}")
            }
        }
    }

    /**
     * Очищает отметку о просмотре нового заказа
     */
    fun markOrderAsViewed(orderId: String) {
        viewModelScope.launch {
            try {
                orderRepository.markOrderAsViewed(orderId)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при отметке заказа как просмотренного: ${e.message}")
            }
        }
    }
}