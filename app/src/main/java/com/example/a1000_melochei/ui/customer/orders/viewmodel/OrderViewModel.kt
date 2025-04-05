package com.yourstore.app.ui.customer.orders.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Order
import com.yourstore.app.data.repository.CartRepository
import com.yourstore.app.data.repository.OrderRepository
import kotlinx.coroutines.launch

/**
 * ViewModel для управления данными заказов
 */
class OrderViewModel(
    private val orderRepository: OrderRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val TAG = "OrderViewModel"

    // LiveData для списка заказов
    private val _orders = MutableLiveData<Resource<List<Order>>>()
    val orders: LiveData<Resource<List<Order>>> = _orders

    // LiveData для деталей заказа
    private val _orderDetails = MutableLiveData<Resource<Order>>()
    val orderDetails: LiveData<Resource<Order>> = _orderDetails

    // LiveData для результата отмены заказа
    private val _cancelOrderResult = MutableLiveData<Resource<Unit>>()
    val cancelOrderResult: LiveData<Resource<Unit>> = _cancelOrderResult

    // Оригинальный список заказов (без фильтрации)
    private var originalOrders: List<Order> = emptyList()

    /**
     * Загружает список заказов пользователя
     */
    fun loadOrders() {
        _orders.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = orderRepository.getUserOrders()

                if (result is Resource.Success) {
                    originalOrders = result.data ?: emptyList()
                    _orders.value = result
                } else {
                    _orders.value = result
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке заказов: ${e.message}")
                _orders.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Фильтрует заказы по статусу
     * @param statuses список статусов для фильтрации, null - показать все
     */
    fun filterOrders(statuses: List<String>?) {
        if (statuses == null) {
            _orders.value = Resource.Success(originalOrders)
            return
        }

        val filteredOrders = originalOrders.filter { order ->
            order.status.name.lowercase() in statuses
        }

        _orders.value = Resource.Success(filteredOrders)
    }

    /**
     * Загружает подробную информацию о заказе
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
     * Отменяет заказ
     */
    fun cancelOrder(orderId: String) {
        _cancelOrderResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = orderRepository.cancelOrder(orderId)
                _cancelOrderResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при отмене заказа: ${e.message}")
                _cancelOrderResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Добавляет все товары из заказа в корзину (повторный заказ)
     */
    fun reorderItems(order: Order) {
        viewModelScope.launch {
            try {
                // Добавляем каждый товар из заказа в корзину
                order.items.forEach { orderItem ->
                    cartRepository.reorderItem(orderItem)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при повторении заказа: ${e.message}")
                // В случае ошибки не прерываем выполнение, пытаемся добавить как можно больше товаров
            }
        }
    }
}