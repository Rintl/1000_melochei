package com.example.a1000_melochei.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель корзины покупок
 * @property id Идентификатор корзины (обычно совпадает с userId)
 * @property items Список товаров в корзине
 * @property createdAt Время создания корзины
 * @property updatedAt Время последнего обновления корзины
 */
@Parcelize
data class Cart(
    val id: String = "",
    val items: List<CartItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {

    /**
     * Возвращает общее количество товаров в корзине
     */
    fun getTotalItemsCount(): Int {
        return items.sumOf { it.quantity }
    }

    /**
     * Возвращает общую сумму корзины
     */
    fun getTotalAmount(): Double {
        return items.sumOf { it.subtotal }
    }

    /**
     * Возвращает отформатированную общую сумму
     */
    fun getFormattedTotal(): String {
        return "${getTotalAmount().toInt()} ₸"
    }

    /**
     * Проверяет, пустая ли корзина
     */
    fun isEmpty(): Boolean = items.isEmpty()

    /**
     * Проверяет, есть ли товар в корзине
     */
    fun containsProduct(productId: String): Boolean {
        return items.any { it.productId == productId }
    }

    /**
     * Возвращает товар из корзины по ID
     */
    fun getItem(productId: String): CartItem? {
        return items.find { it.productId == productId }
    }

    /**
     * Возвращает количество конкретного товара в корзине
     */
    fun getItemQuantity(productId: String): Int {
        return getItem(productId)?.quantity ?: 0
    }

    /**
     * Проверяет, можно ли добавить товар в корзину
     */
    fun canAddItem(productId: String, additionalQuantity: Int = 1): Boolean {
        val currentQuantity = getItemQuantity(productId)
        return (currentQuantity + additionalQuantity) <= 99 // MAX_CART_ITEMS
    }

    /**
     * Возвращает копию корзины с добавленным товаром
     */
    fun withAddedItem(cartItem: CartItem): Cart {
        val existingItemIndex = items.indexOfFirst { it.productId == cartItem.productId }

        val updatedItems = if (existingItemIndex != -1) {
            // Обновляем существующий товар
            items.toMutableList().apply {
                val existingItem = this[existingItemIndex]
                val newQuantity = (existingItem.quantity + cartItem.quantity).coerceAtMost(99)
                this[existingItemIndex] = existingItem.copy(
                    quantity = newQuantity,
                    subtotal = newQuantity * existingItem.price
                )
            }
        } else {
            // Добавляем новый товар
            items + cartItem.copy(subtotal = cartItem.quantity * cartItem.price)
        }

        return this.copy(
            items = updatedItems,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Возвращает копию корзины с удаленным товаром
     */
    fun withRemovedItem(productId: String): Cart {
        return this.copy(
            items = items.filter { it.productId != productId },
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Возвращает копию корзины с обновленным количеством товара
     */
    fun withUpdatedItemQuantity(productId: String, newQuantity: Int): Cart {
        val updatedItems = items.map { item ->
            if (item.productId == productId) {
                item.copy(
                    quantity = newQuantity.coerceIn(1, 99),
                    subtotal = newQuantity * item.price
                )
            } else {
                item
            }
        }

        return this.copy(
            items = updatedItems,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Возвращает копию пустой корзины
     */
    fun emptyCopy(): Cart {
        return this.copy(
            items = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Проверяет валидность корзины
     */
    fun isValid(): Boolean {
        return items.all { it.isValid() }
    }

    companion object {
        /**
         * Создает пустую корзину
         */
        fun empty(userId: String = ""): Cart {
            return Cart(id = userId)
        }

        /**
         * Максимальное количество товаров одного типа в корзине
         */
        const val MAX_ITEM_QUANTITY = 99

        /**
         * Максимальное количество разных товаров в корзине
         */
        const val MAX_CART_ITEMS = 50
    }
}

/**
 * Модель общих данных корзины для отображения
 */
@Parcelize
data class CartTotal(
    val itemsCount: Int = 0,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val total: Double = 0.0
) : Parcelable {

    /**
     * Возвращает отформатированную общую сумму
     */
    fun getFormattedTotal(): String {
        return "${total.toInt()} ₸"
    }

    /**
     * Возвращает отформатированную подсумму
     */
    fun getFormattedSubtotal(): String {
        return "${subtotal.toInt()} ₸"
    }

    /**
     * Возвращает отформатированную скидку
     */
    fun getFormattedDiscount(): String {
        return "-${discount.toInt()} ₸"
    }

    /**
     * Возвращает отформатированную стоимость доставки
     */
    fun getFormattedDeliveryFee(): String {
        return if (deliveryFee > 0) "${deliveryFee.toInt()} ₸" else "Бесплатно"
    }

    /**
     * Проверяет, есть ли скидка
     */
    fun hasDiscount(): Boolean = discount > 0

    /**
     * Проверяет, бесплатная ли доставка
     */
    fun isFreeDelivery(): Boolean = deliveryFee == 0.0
}