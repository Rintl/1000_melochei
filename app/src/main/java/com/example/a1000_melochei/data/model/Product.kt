package com.example.a1000_melochei.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель данных товара
 * @property id Уникальный идентификатор товара
 * @property name Название товара
 * @property description Описание товара
 * @property categoryId Идентификатор категории
 * @property categoryName Название категории (для отображения)
 * @property price Цена товара
 * @property discountPrice Цена со скидкой (если есть)
 * @property originalPrice Оригинальная цена (если есть скидка)
 * @property sku Артикул товара
 * @property barcode Штрих-код товара
 * @property quantity Количество на складе
 * @property minQuantity Минимальное количество для заказа
 * @property maxQuantity Максимальное количество для заказа
 * @property images Список изображений товара
 * @property specifications Характеристики товара
 * @property tags Теги для поиска
 * @property isActive Активен ли товар
 * @property isFeatured Рекомендуемый товар
 * @property weight Вес товара (в граммах)
 * @property dimensions Размеры товара
 * @property salesCount Количество продаж
 * @property viewsCount Количество просмотров
 * @property rating Рейтинг товара
 * @property reviewsCount Количество отзывов
 * @property createdAt Время создания
 * @property updatedAt Время последнего обновления
 */
@Parcelize
data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val price: Double = 0.0,
    val discountPrice: Double = 0.0,
    val originalPrice: Double = 0.0,
    val sku: String = "",
    val barcode: String = "",
    val quantity: Int = 0,
    val minQuantity: Int = 1,
    val maxQuantity: Int = 99,
    val images: List<String> = emptyList(),
    val specifications: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList(),
    val isActive: Boolean = true,
    val isFeatured: Boolean = false,
    val weight: Double = 0.0,
    val dimensions: ProductDimensions = ProductDimensions(),
    val salesCount: Int = 0,
    val viewsCount: Int = 0,
    val rating: Float = 0.0f,
    val reviewsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {

    /**
     * Возвращает статус наличия товара
     */
    fun getStockStatus(): StockStatus {
        return when {
            quantity <= 0 -> StockStatus.OUT_OF_STOCK
            quantity <= 5 -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
    }

    /**
     * Проверяет, есть ли товар в наличии
     */
    fun isInStock(): Boolean {
        return quantity > 0
    }

    /**
     * Проверяет, есть ли скидка на товар
     */
    fun hasDiscount(): Boolean {
        return discountPrice > 0 && discountPrice < price
    }

    /**
     * Возвращает размер скидки в процентах
     */
    fun getDiscountPercent(): Int {
        return if (hasDiscount()) {
            ((price - discountPrice) / price * 100).toInt()
        } else {
            0
        }
    }

    /**
     * Возвращает главное изображение товара
     */
    fun getMainImage(): String {
        return images.firstOrNull() ?: ""
    }

    /**
     * Проверяет валидность данных товара
     */
    fun isValid(): Boolean {
        return name.isNotBlank() &&
                price >= 0 &&
                categoryId.isNotBlank() &&
                quantity >= 0 &&
                minQuantity >= 1 &&
                maxQuantity >= minQuantity
    }

    /**
     * Возвращает форматированную цену
     */
    fun getFormattedPrice(): String {
        val effectivePrice = if (hasDiscount()) discountPrice else price
        return "${effectivePrice.toInt()} ₸"
    }

    /**
     * Возвращает форматированную оригинальную цену
     */
    fun getFormattedOriginalPrice(): String {
        return if (hasDiscount()) "${price.toInt()} ₸" else ""
    }

    /**
     * Возвращает статус наличия в виде строки
     */
    fun getStockStatusText(): String {
        return when (getStockStatus()) {
            StockStatus.IN_STOCK -> "В наличии"
            StockStatus.LOW_STOCK -> "Заканчивается"
            StockStatus.OUT_OF_STOCK -> "Нет в наличии"
        }
    }

    /**
     * Возвращает копию товара с обновленным временем
     */
    fun withUpdatedTime(): Product {
        return this.copy(updatedAt = System.currentTimeMillis())
    }

    /**
     * Возвращает копию товара с новым количеством
     */
    fun withQuantity(newQuantity: Int): Product {
        return this.copy(
            quantity = maxOf(0, newQuantity),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Возвращает копию товара с увеличенным счетчиком просмотров
     */
    fun withIncrementedViews(): Product {
        return this.copy(
            viewsCount = viewsCount + 1,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Переопределенный toString для корректного отображения в логах
     */
    override fun toString(): String {
        return "Product(id='$id', name='$name', price=$price, quantity=$quantity)"
    }

    companion object {
        /**
         * Создает пустой товар
         */
        fun empty(): Product {
            return Product()
        }

        /**
         * Проверяет валидность названия товара
         */
        fun isValidName(name: String): Boolean {
            return name.isNotBlank() &&
                    name.trim().length >= 2 &&
                    name.trim().length <= 100
        }

        /**
         * Проверяет валидность цены
         */
        fun isValidPrice(price: Double): Boolean {
            return price >= 0
        }

        /**
         * Проверяет валидность SKU
         */
        fun isValidSku(sku: String): Boolean {
            return sku.isNotBlank() && sku.length <= 50
        }
    }
}

/**
 * Размеры товара
 */
@Parcelize
data class ProductDimensions(
    val length: Double = 0.0, // см
    val width: Double = 0.0,  // см
    val height: Double = 0.0  // см
) : Parcelable {

    /**
     * Возвращает объем товара в кубических сантиметрах
     */
    fun getVolume(): Double {
        return length * width * height
    }

    /**
     * Проверяет, заданы ли размеры
     */
    fun isDefined(): Boolean {
        return length > 0 && width > 0 && height > 0
    }

    /**
     * Возвращает размеры в виде строки
     */
    override fun toString(): String {
        return if (isDefined()) {
            "${length}x${width}x${height} см"
        } else {
            "Размеры не указаны"
        }
    }
}

/**
 * Статус наличия товара
 */
enum class StockStatus {
    IN_STOCK,     // В наличии
    LOW_STOCK,    // Заканчивается
    OUT_OF_STOCK  // Нет в наличии
}