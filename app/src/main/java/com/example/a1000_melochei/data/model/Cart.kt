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
     * Возвращает пустую корзину
     */
    fun cleared(): Cart {
        return this.copy(
            items = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        /**
         * Создает пустую корзину
         */
        fun empty(id: String = ""): Cart {
            return Cart(id = id)
        }
    }
}

/**
 * Модель товара в корзине
 * @property productId Идентификатор товара
 * @property productName Название товара
 * @property productImage URL изображения товара
 * @property price Цена за единицу товара
 * @property originalPrice Оригинальная цена (если есть скидка)
 * @property quantity Количество товара в корзине
 * @property subtotal Общая стоимость позиции (price * quantity)
 * @property categoryId Идентификатор категории товара
 * @property sku Артикул товара
 * @property addedAt Время добавления в корзину
 */
@Parcelize
data class CartItem(
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val quantity: Int = 1,
    val subtotal: Double = 0.0,
    val categoryId: String = "",
    val sku: String = "",
    val addedAt: Long = System.currentTimeMillis()
) : Parcelable {

    /**
     * Проверяет, есть ли скидка на товар
     */
    fun hasDiscount(): Boolean {
        return originalPrice > 0 && originalPrice > price
    }

    /**
     * Возвращает размер скидки в процентах
     */
    fun getDiscountPercent(): Int {
        return if (hasDiscount()) {
            ((originalPrice - price) / originalPrice * 100).toInt()
        } else {
            0
        }
    }

    /**
     * Возвращает отформатированную цену
     */
    fun getFormattedPrice(): String {
        return "${price.toInt()} ₸"
    }

    /**
     * Возвращает отформатированную оригинальную цену
     */
    fun getFormattedOriginalPrice(): String {
        return if (hasDiscount()) "${originalPrice.toInt()} ₸" else ""
    }

    /**
     * Возвращает отформатированную общую стоимость
     */
    fun getFormattedSubtotal(): String {
        return "${subtotal.toInt()} ₸"
    }

    /**
     * Проверяет валидность данных товара в корзине
     */
    fun isValid(): Boolean {
        return productId.isNotBlank() &&
                productName.isNotBlank() &&
                price >= 0 &&
                quantity > 0 &&
                subtotal >= 0
    }

    /**
     * Возвращает копию товара с новым количеством
     */
    fun withQuantity(newQuantity: Int): CartItem {
        return this.copy(
            quantity = newQuantity.coerceAtLeast(1),
            subtotal = newQuantity * price
        )
    }

    /**
     * Возвращает копию товара с обновленной ценой
     */
    fun withPrice(newPrice: Double): CartItem {
        return this.copy(
            price = newPrice.coerceAtLeast(0.0),
            subtotal = quantity * newPrice
        )
    }

    companion object {
        /**
         * Создает CartItem из Product
         */
        fun fromProduct(product: Product, quantity: Int = 1): CartItem {
            return CartItem(
                productId = product.id,
                productName = product.name,
                productImage = product.getMainImage(),
                price = product.price,
                originalPrice = product.originalPrice,
                quantity = quantity,
                subtotal = product.price * quantity,
                categoryId = product.categoryId,
                sku = product.sku,
                addedAt = System.currentTimeMillis()
            )
        }

        /**
         * Создает пустой CartItem
         */
        fun empty(): CartItem {
            return CartItem()
        }
    }
}