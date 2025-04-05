package com.yourstore.app.ui.customer.home.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Category
import com.yourstore.app.data.model.Product
import com.yourstore.app.data.model.Promotion
import com.yourstore.app.data.repository.CategoryRepository
import com.yourstore.app.data.repository.ProductRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * ViewModel для главной страницы клиентского интерфейса.
 * Отвечает за загрузку данных для отображения на главной странице:
 * - промо-акции
 * - категории товаров
 * - популярные товары
 */
class HomeViewModel(
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val TAG = "HomeViewModel"

    // LiveData для промо-акций
    private val _promotions = MutableLiveData<Resource<List<Promotion>>>()
    val promotions: LiveData<Resource<List<Promotion>>> = _promotions

    // LiveData для категорий товаров
    private val _categories = MutableLiveData<Resource<List<Category>>>()
    val categories: LiveData<Resource<List<Category>>> = _categories

    // LiveData для популярных товаров
    private val _popularProducts = MutableLiveData<Resource<List<Product>>>()
    val popularProducts: LiveData<Resource<List<Product>>> = _popularProducts

    /**
     * Загрузка всех необходимых данных для главной страницы
     */
    fun loadData() {
        loadPromotions()
        loadCategories()
        loadPopularProducts()
    }

    /**
     * Загрузка промо-акций
     */
    private fun loadPromotions() {
        _promotions.value = Resource.Loading()

        viewModelScope.launch {
            try {
                // Здесь должен быть вызов к репозиторию акций.
                // Для временного решения создадим несколько фиктивных акций
                val dummyPromotions = listOf(
                    Promotion(
                        id = "1",
                        title = "Скидка 20% на инструменты",
                        description = "Скидка 20% на весь ручной инструмент",
                        imageUrl = "https://example.com/promo1.jpg",
                        startDate = System.currentTimeMillis(),
                        endDate = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000, // неделя
                        type = "category",
                        categoryId = "tools",
                        productId = null,
                        discountPercent = 20
                    ),
                    Promotion(
                        id = "2",
                        title = "Распродажа красок",
                        description = "Большая распродажа красок и лаков",
                        imageUrl = "https://example.com/promo2.jpg",
                        startDate = System.currentTimeMillis(),
                        endDate = System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000, // две недели
                        type = "category",
                        categoryId = "paints",
                        productId = null,
                        discountPercent = 15
                    )
                )

                _promotions.value = Resource.Success(dummyPromotions)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке промо-акций: ${e.message}")
                _promotions.value = Resource.Error("Не удалось загрузить акции: ${e.message}")
            }
        }
    }

    /**
     * Загрузка категорий товаров
     */
    private fun loadCategories() {
        _categories.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = categoryRepository.getCategories()

                if (result.data != null) {
                    _categories.value = Resource.Success(result.data)
                } else {
                    _categories.value = Resource.Error(result.message ?: "Неизвестная ошибка при загрузке категорий")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке категорий: ${e.message}")
                _categories.value = Resource.Error("Не удалось загрузить категории: ${e.message}")
            }
        }
    }

    /**
     * Загрузка популярных товаров
     */
    private fun loadPopularProducts() {
        _popularProducts.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = productRepository.getPopularProducts(limit = 10)

                if (result.data != null) {
                    _popularProducts.value = Resource.Success(result.data)
                } else {
                    _popularProducts.value = Resource.Error(result.message ?: "Неизвестная ошибка при загрузке популярных товаров")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке популярных товаров: ${e.message}")
                _popularProducts.value = Resource.Error("Не удалось загрузить популярные товары: ${e.message}")
            }
        }
    }

    /**
     * Метод для обновления данных вручную (например, при свайпе вниз)
     */
    fun refresh() {
        loadData()
    }
}