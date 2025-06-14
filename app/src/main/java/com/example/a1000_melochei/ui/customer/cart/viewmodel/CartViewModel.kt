package com.example.a1000_melochei.ui.customer.cart.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.CartItem
import com.example.a1000_melochei.data.model.CartTotal
import com.example.a1000_melochei.data.repository.CartRepository
import com.example.a1000_melochei.data.repository.OrderRepository
import com.example.a1000_melochei.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel для управления корзиной покупок и оформления заказа
 */
class CartViewModel(
    private val cartRepository: CartRepository,
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val TAG = "CartViewModel"

    // LiveData для списка товаров в корзине
    private val _cartItems = MutableLiveData<Resource<List<CartItem>>>()
    val cartItems: LiveData<Resource<List<CartItem>>> = _cartItems

    // LiveData для общей суммы корзины
    private val _cartTotal = MutableLiveData<CartTotal>()
    val cartTotal: LiveData<CartTotal> = _cartTotal

    // LiveData для результата обновления количества
    private val _updateQuantityResult = MutableLiveData<Resource<Unit>>()
    val updateQuantityResult: LiveData<Resource<Unit>> = _updateQuantityResult

    // LiveData для результата удаления товара
    private val _removeItemResult = MutableLiveData<Resource<Unit>>()
    val removeItemResult: LiveData<Resource<Unit>> = _removeItemResult

    // LiveData для списка адресов пользователя
    private val _userAddresses = MutableLiveData<Resource<List<com.yourstore.app.data.model.Address>>>()
    val userAddresses: LiveData<Resource<List<com.yourstore.app.data.model.Address>>> = _userAddresses

    // LiveData для результата оформления заказа
    private val _placeOrderResult = MutableLiveData<Resource<String>>()
    val placeOrderResult: LiveData<Resource<String>> = _placeOrderResult

    /**
     * Загружает содержимое корзины
     */
    fun loadCart() {
        _cartItems.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = cartRepository.getCartItems()
                _cartItems.value = result

                // Обновляем общую сумму корзины
                updateCartTotal()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке корзины: ${e.message}")
                _cartItems.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает корзину для оформления заказа
     */
    fun loadCartForCheckout() {
        loadCart()
    }

    /**
     * Обновляет количество товара в корзине
     */
    fun updateCartItemQuantity(cartItemId: String, quantity: Int) {
        _updateQuantityResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = cartRepository.updateCartItemQuantity(cartItemId, quantity)
                _updateQuantityResult.value = result

                // Если успешно, обновляем всю корзину
                if (result is Resource.Success) {
                    loadCart()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении количества: ${e.message}")
                _updateQuantityResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Удаляет товар из корзины
     */
    fun removeCartItem(cartItemId: String) {
        _removeItemResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = cartRepository.removeCartItem(cartItemId)
                _removeItemResult.value = result

                // Если успешно, обновляем всю корзину
                if (result is Resource.Success) {
                    loadCart()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при удалении товара: ${e.message}")
                _removeItemResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Очищает всю корзину
     */
    fun clearCart() {
        viewModelScope.launch {
            try {
                cartRepository.clearCart()
                loadCart()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при очистке корзины: ${e.message}")
                // Повторно загружаем корзину, чтобы показать актуальное состояние
                loadCart()
            }
        }
    }

    /**
     * Загружает список адресов пользователя
     */
    fun loadUserAddresses() {
        _userAddresses.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val userProfile = userRepository.getUserProfile()

                if (userProfile is Resource.Success && userProfile.data != null) {
                    _userAddresses.value = Resource.Success(userProfile.data.addresses)
                } else {
                    _userAddresses.value = Resource.Error(
                        userProfile.message ?: "Не удалось загрузить адреса"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке адресов: ${e.message}")
                _userAddresses.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Рассчитывает стоимость доставки
     */
    fun calculateDeliveryFee(addressId: String?, deliveryMethod: String) {
        viewModelScope.launch {
            try {
                val deliveryFee = if (deliveryMethod == "pickup") {
                    // Самовывоз бесплатный
                    0.0
                } else if (addressId != null) {
                    // Рассчитываем стоимость доставки для выбранного адреса
                    val result = cartRepository.calculateDeliveryFee(addressId)
                    result.data ?: 0.0
                } else {
                    0.0
                }

                // Обновляем общую сумму с учетом новой стоимости доставки
                updateCartTotal(deliveryFee)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при расчете стоимости доставки: ${e.message}")
                // В случае ошибки, устанавливаем стоимость доставки 0
                updateCartTotal(0.0)
            }
        }
    }

    /**
     * Обновляет общую сумму корзины
     */
    private fun updateCartTotal(deliveryFee: Double = 0.0) {
        viewModelScope.launch {
            try {
                val cartItems = _cartItems.value

                if (cartItems is Resource.Success && cartItems.data != null) {
                    var subtotal = 0.0

                    // Рассчитываем общую сумму товаров
                    cartItems.data.forEach { item ->
                        subtotal += (item.discountPrice ?: item.price) * item.quantity
                    }

                    // Формируем объект с общей суммой
                    val total = subtotal + deliveryFee
                    val cartTotal = CartTotal(
                        subtotal = subtotal,
                        deliveryFee = deliveryFee,
                        total = total
                    )

                    _cartTotal.value = cartTotal
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении общей суммы: ${e.message}")
            }
        }
    }

    /**
     * Оформляет заказ
     */
    fun placeOrder(
        addressId: String?,
        deliveryMethod: String,
        pickupPoint: Int?,
        deliveryDate: Date?,
        paymentMethod: String,
        comment: String = ""
    ) {
        _placeOrderResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                // Проверяем, что корзина не пуста
                val cartItems = _cartItems.value
                if (cartItems !is Resource.Success || cartItems.data.isNullOrEmpty()) {
                    _placeOrderResult.value = Resource.Error("Корзина пуста")
                    return@launch
                }

                // Проверяем, что общая сумма расчитана
                val cartTotal = _cartTotal.value
                if (cartTotal == null) {
                    _placeOrderResult.value = Resource.Error("Не удалось рассчитать сумму заказа")
                    return@launch
                }

                // Оформляем заказ
                val result = orderRepository.placeOrder(
                    addressId = addressId,
                    deliveryMethod = deliveryMethod,
                    pickupPoint = pickupPoint,
                    deliveryDate = deliveryDate,
                    paymentMethod = paymentMethod,
                    comment = comment,
                    cartTotal = cartTotal
                )

                _placeOrderResult.value = result

                // Если заказ успешно оформлен, очищаем корзину
                if (result is Resource.Success) {
                    cartRepository.clearCart()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при оформлении заказа: ${e.message}")
                _placeOrderResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Возвращает минимальную сумму заказа для доставки
     */
    fun getMinOrderAmount(): Double {
        // Здесь должна быть логика получения минимальной суммы из конфигурации
        // В данном случае устанавливаем фиксированное значение
        return 5000.0
    }
}