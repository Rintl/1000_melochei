package com.example.a1000_melochei.ui.customer.catalog.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Category
import com.example.a1000_melochei.data.model.Product
import com.example.a1000_melochei.data.repository.CategoryRepository
import com.example.a1000_melochei.data.repository.ProductRepository
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ViewModel для управления данными каталога товаров
 */
class CatalogViewModel(
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val TAG = "CatalogViewModel"

    // LiveData для списка категорий
    private val _categories = MutableLiveData<Resource<List<Category>>>()
    val categories: LiveData<Resource<List<Category>>> = _categories

    // LiveData для выбранной категории
    private val _category = MutableLiveData<Resource<Category>>()
    val category: LiveData<Resource<Category>> = _category

    // LiveData для товаров в категории
    private val _categoryProducts = MutableLiveData<Resource<List<Product>>>()
    val categoryProducts: LiveData<Resource<List<Product>>> = _categoryProducts

    // LiveData для результатов поиска
    private val _searchResults = MutableLiveData<Resource<List<Product>>>()
    val searchResults: LiveData<Resource<List<Product>>> = _searchResults

    // Временное хранилище для неотфильтрованных товаров категории
    private var originalCategoryProducts: List<Product> = emptyList()

    // Временное хранилище для неотфильтрованных результатов поиска
    private var originalSearchResults: List<Product> = emptyList()

    /**
     * Загружает список всех категорий
     */
    fun loadCategories() {
        _categories.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = categoryRepository.getCategories()
                _categories.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке категорий: ${e.message}")
                _categories.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает информацию о категории и её товарах
     */
    fun loadCategoryWithProducts(categoryId: String) {
        _category.value = Resource.Loading()
        _categoryProducts.value = Resource.Loading()

        viewModelScope.launch {
            try {
                // Загружаем информацию о категории
                val categoryResult = categoryRepository.getCategoryById(categoryId)
                _category.value = categoryResult

                // Загружаем товары этой категории
                val productsResult = productRepository.getProductsByCategory(categoryId)

                if (productsResult is Resource.Success) {
                    originalCategoryProducts = productsResult.data ?: emptyList()
                    _categoryProducts.value = productsResult
                } else {
                    _categoryProducts.value = productsResult
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке категории и товаров: ${e.message}")
                _category.value = Resource.Error(e.message ?: "Неизвестная ошибка")
                _categoryProducts.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Выполняет поиск товаров по заданным критериям
     */
    fun searchProducts(
        query: String = "",
        categoryId: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        inStockOnly: Boolean = false,
        onSaleOnly: Boolean = false
    ) {
        _searchResults.value = Resource.Loading()

        viewModelScope.launch {
            try {
                // Вызов репозитория для получения результатов поиска
                val searchResult = productRepository.searchProducts(
                    query = query,
                    categoryId = categoryId,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    inStockOnly = inStockOnly,
                    onSaleOnly = onSaleOnly
                )

                if (searchResult is Resource.Success) {
                    originalSearchResults = searchResult.data ?: emptyList()
                    _searchResults.value = searchResult
                } else {
                    _searchResults.value = searchResult
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при поиске товаров: ${e.message}")
                _searchResults.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Сортировка товаров в категории по популярности
     */
    fun sortProductsByPopularity() {
        val products = originalCategoryProducts.sortedByDescending { it.rating * it.reviewCount }
        _categoryProducts.value = Resource.Success(products)
    }

    /**
     * Сортировка товаров в категории по цене
     * @param ascending true - по возрастанию, false - по убыванию
     */
    fun sortProductsByPrice(ascending: Boolean) {
        val products = if (ascending) {
            originalCategoryProducts.sortedBy { it.discountPrice ?: it.price }
        } else {
            originalCategoryProducts.sortedByDescending { it.discountPrice ?: it.price }
        }
        _categoryProducts.value = Resource.Success(products)
    }

    /**
     * Сортировка товаров в категории по названию
     * @param ascending true - по алфавиту (А-Я), false - против алфавита (Я-А)
     */
    fun sortProductsByName(ascending: Boolean) {
        val products = if (ascending) {
            originalCategoryProducts.sortedBy { it.name.lowercase(Locale.getDefault()) }
        } else {
            originalCategoryProducts.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
        }
        _categoryProducts.value = Resource.Success(products)
    }

    /**
     * Фильтрация товаров по наличию
     */
    fun filterProductsByAvailability(inStockOnly: Boolean) {
        val filteredProducts = if (inStockOnly) {
            originalCategoryProducts.filter { it.availableQuantity > 0 }
        } else {
            originalCategoryProducts
        }
        _categoryProducts.value = Resource.Success(filteredProducts)
    }

    /**
     * Фильтрация товаров по наличию акций
     */
    fun filterProductsByPromotion(onSaleOnly: Boolean) {
        val filteredProducts = if (onSaleOnly) {
            originalCategoryProducts.filter { it.discountPrice != null && it.discountPrice < it.price }
        } else {
            originalCategoryProducts
        }
        _categoryProducts.value = Resource.Success(filteredProducts)
    }

    /**
     * Сортировка результатов поиска по релевантности
     */
    fun sortSearchResultsByRelevance() {
        // При сортировке по релевантности возвращаем исходные результаты
        _searchResults.value = Resource.Success(originalSearchResults)
    }

    /**
     * Сортировка результатов поиска по цене
     */
    fun sortSearchResultsByPrice(ascending: Boolean) {
        val sortedResults = if (ascending) {
            originalSearchResults.sortedBy { it.discountPrice ?: it.price }
        } else {
            originalSearchResults.sortedByDescending { it.discountPrice ?: it.price }
        }
        _searchResults.value = Resource.Success(sortedResults)
    }

    /**
     * Сортировка результатов поиска по названию
     */
    fun sortSearchResultsByName(ascending: Boolean) {
        val sortedResults = if (ascending) {
            originalSearchResults.sortedBy { it.name.lowercase(Locale.getDefault()) }
        } else {
            originalSearchResults.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
        }
        _searchResults.value = Resource.Success(sortedResults)
    }
}