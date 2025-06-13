package com.example.a1000_melochei.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель данных заказа
 * @property id Уникальный идентификатор заказа
 * @property userId Идентификатор пользователя
 * @property orderNumber Номер заказа для отображения
 * @property items Список товаров в заказе
 * @property status Статус заказа
 * @property deliveryType Тип доставки
 * @property deliveryAddress Адрес доставки
 * @property customerInfo Информация о покупателе
 * @property subtotal Сумма товаров без доставки
 * @property deliveryFee Стоимость доставки
 * @property discount Размер скидки
 * @property total Общая сумма заказа
 * @property paymentMethod Метод оплаты
 * @property paymentStatus Статус оплаты
 * @property notes Комментарии к заказу
 * @property estimatedDeliveryDate Предполагаемая дата доставки
 * @property actualDeliveryDate Фактическая дата доставки
 * @property createdAt Время создания заказа
 * @property updatedAt Время последнего обновления
 * @property statusHistory История изменения статусов
 */
@Parcelize
data class Order(
    val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val items: List<OrderItem> = emptyList(),
    val status: OrderStatus = OrderStatus.PENDING,
    val deliveryType: DeliveryType = DeliveryType.DELIVERY,
    val deliveryAddress: Address? = null,
    val customerInfo: CustomerInfo = CustomerInfo(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val notes: String = "",
    val estimatedDeliveryDate: Long? = null,
    val actualDeliveryDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val statusHistory: List<OrderStatusHistory> = emptyList()
) : Parcelable {

    /**
     * Возвращает общее количество товаров в заказе
     */
    fun getTotalItemsCount(): Int {
        return items.sumOf { it.quantity }
    }

    /**
     * Проверяет, можно ли отменить заказ
     */
    fun canBeCancelled(): Boolean {
        return status in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING)
    }

    /**
     * Проверяет, завершен ли заказ
     */
    fun isCompleted(): Boolean {
        return status in listOf(OrderStatus.DELIVERED, OrderStatus.COMPLETED)
    }

    /**
     * Проверяет, отменен ли заказ
     */
    fun isCancelled(): Boolean {
        return status == OrderStatus.CANCELLED
    }

    /**
     * Возвращает отформатированную общую сумму
     */
    fun getFormattedTotal(): String {
        return "${total.toInt()} ₸"
    }

    /**
     * Возвращает отформатированную сумму товаров
     */
    fun getFormattedSubtotal(): String {
        return "${subtotal.toInt()} ₸"
    }

    /**
     * Возвращает отформатированную стоимость доставки
     */
    fun getFormattedDeliveryFee(): String {
        return if (deliveryFee > 0) "${deliveryFee.toInt()} ₸" else "Бесплатно"
    }

    /**
     * Возвращает описание статуса заказа
     */
    fun getStatusDescription(): String {
        return when (status) {
            OrderStatus.PENDING -> "Ожидает подтверждения"
            OrderStatus.CONFIRMED -> "Подтвержден"
            OrderStatus.PROCESSING -> "В обработке"
            OrderStatus.READY_FOR_DELIVERY -> "Готов к доставке"
            OrderStatus.IN_DELIVERY -> "В доставке"
            OrderStatus.DELIVERED -> "Доставлен"
            OrderStatus.COMPLETED -> "Выполнен"
            OrderStatus.CANCELLED -> "Отменен"
            OrderStatus.RETURNED -> "Возвращен"
        }
    }

    /**
     * Возвращает копию заказа с новым статусом
     */
    fun withStatus(newStatus: OrderStatus, comment: String = ""): Order {
        val newStatusHistory = statusHistory + OrderStatusHistory(
            status = newStatus,
            comment = comment,
            timestamp = System.currentTimeMillis()
        )

        return this.copy(
            status = newStatus,
            updatedAt = System.currentTimeMillis(),
            statusHistory = newStatusHistory
        )
    }

    companion object {
        /**
         * Генерирует номер заказа
         */
        fun generateOrderNumber(): String {
            val timestamp = System.currentTimeMillis()
            val random = (1000..9999).random()
            return "1000-$timestamp-$random"
        }

        /**
         * Создает пустой заказ
         */
        fun empty(): Order {
            return Order()
        }
    }
}

/**
 * Товар в заказе
 */
@Parcelize
data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val quantity: Int = 1,
    val subtotal: Double = 0.0
) : Parcelable {

    /**
     * Возвращает отформатированную цену
     */
    fun getFormattedPrice(): String {
        return "${price.toInt()} ₸"
    }

    /**
     * Возвращает отформатированную сумму
     */
    fun getFormattedSubtotal(): String {
        return "${subtotal.toInt()} ₸"
    }

    /**
     * Проверяет, есть ли скидка на товар
     */
    fun hasDiscount(): Boolean {
        return originalPrice > 0 && originalPrice > price
    }
}

/**
 * Информация о покупателе
 */
@Parcelize
data class CustomerInfo(
    val name: String = "",
    val phone: String = "",
    val email: String = ""
) : Parcelable

/**
 * История изменения статуса заказа
 */
@Parcelize
data class OrderStatusHistory(
    val status: OrderStatus = OrderStatus.PENDING,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Статус заказа
 */
enum class OrderStatus {
    PENDING,              // Ожидает подтверждения
    CONFIRMED,            // Подтвержден
    PROCESSING,           // В обработке
    READY_FOR_DELIVERY,   // Готов к доставке
    IN_DELIVERY,          // В доставке
    DELIVERED,            // Доставлен
    COMPLETED,            // Выполнен
    CANCELLED,            // Отменен
    RETURNED              // Возвращен
}

/**
 * Тип доставки
 */
enum class DeliveryType {
    DELIVERY,   // Доставка
    PICKUP      // Самовывоз
}

/**
 * Метод оплаты
 */
enum class PaymentMethod {
    CASH,           // Наличные
    CARD,           // Банковская карта
    KASPI_PAY,      // Kaspi Pay
    BANK_TRANSFER   // Банковский перевод
}

/**
 * Статус оплаты
 */
enum class PaymentStatus {
    PENDING,    // Ожидает оплаты
    PAID,       // Оплачен
    FAILED,     // Ошибка оплаты
    REFUNDED    // Возвращен
}