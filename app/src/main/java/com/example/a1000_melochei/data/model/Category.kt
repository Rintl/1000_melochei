package com.example.a1000_melochei.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель данных категории товаров
 * @property id Уникальный идентификатор категории
 * @property name Название категории
 * @property description Описание категории
 * @property imageUrl URL изображения категории
 * @property parentId Идентификатор родительской категории (для подкатегорий)
 * @property productCount Количество товаров в категории
 * @property sortOrder Порядок сортировки категории
 * @property isActive Флаг, указывающий активна ли категория
 * @property createdAt Время создания категории
 * @property updatedAt Время последнего обновления категории
 */
@Parcelize
data class Category(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val parentId: String = "",
    val productCount: Int = 0,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {

    /**
     * Проверяет, является ли категория подкатегорией
     */
    fun isSubcategory(): Boolean = parentId.isNotEmpty()

    /**
     * Проверяет, является ли категория главной (корневой)
     */
    fun isRootCategory(): Boolean = parentId.isEmpty()

    /**
     * Возвращает количество товаров в категории в читаемом формате
     */
    fun getProductCountString(): String {
        return when {
            productCount == 0 -> "Нет товаров"
            productCount == 1 -> "1 товар"
            productCount in 2..4 -> "$productCount товара"
            else -> "$productCount товаров"
        }
    }

    /**
     * Проверяет валидность данных категории
     */
    fun isValid(): Boolean {
        return name.isNotBlank() &&
                name.trim().length >= 2 &&
                sortOrder >= 0
    }

    /**
     * Возвращает копию категории с обновленным временем изменения
     */
    fun withUpdatedTime(): Category {
        return this.copy(updatedAt = System.currentTimeMillis())
    }

    /**
     * Возвращает копию категории с новым количеством товаров
     */
    fun withProductCount(newCount: Int): Category {
        return this.copy(
            productCount = maxOf(0, newCount), // Не может быть отрицательным
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Возвращает копию категории с измененным статусом активности
     */
    fun withActiveStatus(active: Boolean): Category {
        return this.copy(
            isActive = active,
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        /**
         * Создает пустую категорию для заполнения формы
         */
        fun empty(): Category {
            return Category()
        }

        /**
         * Проверяет валидность имени категории
         */
        fun isValidName(name: String): Boolean {
            return name.isNotBlank() &&
                    name.trim().length >= 2 &&
                    name.trim().length <= 50
        }

        /**
         * Проверяет валидность описания категории
         */
        fun isValidDescription(description: String): Boolean {
            return description.length <= 500
        }
    }
}