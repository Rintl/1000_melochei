package com.yourstore.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Перечисление возможных статусов заказа
 */
enum class OrderStatus {
    PENDING,      // Ожидает обработки
    PROCESSING,   // В обработке
    SHIPPING,     // Доставляется
    DELIVERED,    // Доставлен
    COMPLETED,    // Завершен
    CANCELLED     // Отменен
}

/**
 * Модель данных заказа
 * @property id Уникальный идентификатор заказа
 * @property number Номер заказа (для отображения пользователю)
 * @property userId Идентификатор пользователя-заказчика
 * @property userName Имя пользователя-заказчика
 * @property userPhone Телефон пользователя-заказчика
 * @property items Список товаров в заказе
 * @property status Статус заказа
 * @property createdAt Дата создания заказа
 * @property updatedAt Дата последнего обновления заказа
 * @property deliveryMethod Способ доставки ("delivery" или "pickup")
 * @property address Адрес доставки (для delivery)
 * @property pickupPoint Индекс пункта самовывоза (для pickup)
 * @property deliveryDate Дата доставки
 * @property paymentMethod Способ оплаты
 * @property comment Комментарий к заказу
 * @property subtotal Промежуточная сумма без учета доставки
 * @property deliveryFee Стоимость доставки
 * @property total Общая сумма заказа
 */
@Parcelize
data class Order(
    val id: String = "",
    val number: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val items: List<OrderItem> = emptyList(),
    val status: OrderStatus = OrderStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deliveryMethod: String = "",
    val address: Address? = null,
    val pickupPoint: Int? = null,
    val deliveryDate: Date? = null,
    val paymentMethod: String = "",
    val comment: String = "",
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val total: Double = 0.0
) : Parcelable

/**
 * Модель данных товара в заказе
 * @property id Уникальный идентификатор элемента заказа
 * @property productId Идентификатор товара
 * @property name Название товара
 * @property price Цена товара на момент заказа
 * @property quantity Количество товара
 * @property imageUrl URL изображения товара
 */
@Parcelize
data class OrderItem(
    val id: String = "",
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val imageUrl: String = ""
) : Parcelable