package com.example.a1000_melochei.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель данных товара в корзине
 * @property id Уникальный идентификатор элемента корзины
 * @property productId Идентификатор товара
 * @property productName Название товара
 * @property productImage URL изображения товара
 * @property price Цена за единицу товара
 * @property originalPrice Оригинальная цена (до скидки)
 * @property quantity Количество товара
 * @property subtotal Общая стоимость (цена * количество)
 * @property categoryId Идентификатор категории товара
 * @property sku Артикул товара
 * @property addedAt Дата добавления товара в корзину
 */
@Parcelize
data class CartItem(
    val id: String = "",
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
     * Возвращает актуальную цену (с учетом скидки)
     */
    fun getActualPrice(): Double {
        return if (hasDiscount()) price else originalPrice
    }

    /**
     * Проверяет, есть ли скидка на товар
     */
    fun hasDiscount(): Boolean {
        return originalPrice > 0 && price < originalPrice
    }

    /**
     * Возвращает размер скидки в процентах
     */
    fun getDiscountPercentage(): Int {
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

    /**
     * Возвращает копию товара с пересчитанным subtotal
     */
    fun withRecalculatedSubtotal(): CartItem {
        return this.copy(subtotal = quantity * price)
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

        /**
         * Создает CartItem с базовыми данными
         */
        fun create(
            productId: String,
            productName: String,
            price: Double,
            quantity: Int = 1,
            productImage: String = ""
        ): CartItem {
            return CartItem(
                id = generateCartItemId(productId),
                productId = productId,
                productName = productName,
                productImage = productImage,
                price = price,
                originalPrice = price,
                quantity = quantity,
                subtotal = price * quantity
            )
        }

        /**
         * Генерирует уникальный ID для элемента корзины
         */
        private fun generateCartItemId(productId: String): String {
            return "${productId}_${System.currentTimeMillis()}"
        }
    }
}