package com.yourstore.app.data.source.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yourstore.app.data.model.CartItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Класс для локального кэширования корзины пользователя.
 * Использует DataStore для хранения данных.
 */
class CartCache(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cart_cache")
        private val CART_ITEMS_KEY = stringPreferencesKey("cart_items")
        private val gson = Gson()
    }

    /**
     * Получение элементов корзины из кэша
     * @return Flow со списком элементов корзины
     */
    fun getCartItems(): Flow<List<CartItem>> {
        return context.dataStore.data.map { preferences ->
            val cartItemsJson = preferences[CART_ITEMS_KEY] ?: "[]"
            val type = object : TypeToken<List<CartItem>>() {}.type
            gson.fromJson(cartItemsJson, type)
        }
    }

    /**
     * Сохранение элементов корзины в кэш
     * @param cartItems Список элементов корзины
     */
    suspend fun saveCartItems(cartItems: List<CartItem>) {
        val cartItemsJson = gson.toJson(cartItems)
        context.dataStore.edit { preferences ->
            preferences[CART_ITEMS_KEY] = cartItemsJson
        }
    }

    /**
     * Добавление элемента в корзину
     * @param cartItem Элемент корзины
     */
    suspend fun addCartItem(cartItem: CartItem) {
        context.dataStore.edit { preferences ->
            val cartItemsJson = preferences[CART_ITEMS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<CartItem>>() {}.type
            val cartItems: MutableList<CartItem> = gson.fromJson(cartItemsJson, type)

            // Проверяем, есть ли уже такой товар в корзине
            val existingItemIndex = cartItems.indexOfFirst { it.productId == cartItem.productId }
            if (existingItemIndex != -1) {
                // Обновляем количество, если товар уже есть
                val existingItem = cartItems[existingItemIndex]
                val newQuantity = existingItem.quantity + cartItem.quantity
                val updatedItem = existingItem.copy(quantity = newQuantity)
                cartItems[existingItemIndex] = updatedItem
            } else {
                // Добавляем новый товар
                cartItems.add(cartItem)
            }

            preferences[CART_ITEMS_KEY] = gson.toJson(cartItems)
        }
    }

    /**
     * Обновление количества товара в корзине
     * @param cartItemId ID элемента корзины
     * @param quantity Новое количество
     */
    suspend fun updateCartItemQuantity(cartItemId: String, quantity: Int) {
        context.dataStore.edit { preferences ->
            val cartItemsJson = preferences[CART_ITEMS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<CartItem>>() {}.type
            val cartItems: MutableList<CartItem> = gson.fromJson(cartItemsJson, type)

            val itemIndex = cartItems.indexOfFirst { it.id == cartItemId }
            if (itemIndex != -1) {
                val item = cartItems[itemIndex]
                cartItems[itemIndex] = item.copy(quantity = quantity)
            }

            preferences[CART_ITEMS_KEY] = gson.toJson(cartItems)
        }
    }

    /**
     * Удаление элемента из корзины
     * @param cartItemId ID элемента корзины
     */
    suspend fun removeCartItem(cartItemId: String) {
        context.dataStore.edit { preferences ->
            val cartItemsJson = preferences[CART_ITEMS_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<CartItem>>() {}.type
            val cartItems: MutableList<CartItem> = gson.fromJson(cartItemsJson, type)

            cartItems.removeIf { it.id == cartItemId }

            preferences[CART_ITEMS_KEY] = gson.toJson(cartItems)
        }
    }

    /**
     * Полная очистка корзины
     */
    suspend fun clearCart() {
        context.dataStore.edit { preferences ->
            preferences[CART_ITEMS_KEY] = "[]"
        }
    }

    /**
     * Получение общего количества товаров в корзине
     * @return Flow с количеством товаров
     */
    fun getCartItemCount(): Flow<Int> {
        return getCartItems().map { items ->
            items.sumOf { it.quantity }
        }
    }

    /**
     * Проверка, есть ли товар в корзине
     * @param productId ID товара
     * @return Flow с результатом проверки (true - товар в корзине)
     */
    fun isProductInCart(productId: String): Flow<Boolean> {
        return getCartItems().map { items ->
            items.any { it.productId == productId }
        }
    }
}