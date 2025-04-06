package com.yourstore.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель данных корзины
 * @property id Уникальный идентификатор корзины
 * @property userId Идентификатор пользователя-владельца корзины
 * @property items Список товаров в корзине
 * @property subtotal Промежуточная сумма без учета доставки
 * @property deliveryFee Стоимость доставки
 * @property total Общая сумма заказа
 * @property updatedAt Дата последнего обновления корзины
 */
@Parcelize
data class Cart(
    val id: String = "",
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val total: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Модель данных общей суммы корзины
 * @property subtotal Промежуточная сумма без учета доставки
 * @property deliveryFee Стоимость доставки
 * @property total Общая сумма
 */
data class CartTotal(
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val total: Double = 0.0
)