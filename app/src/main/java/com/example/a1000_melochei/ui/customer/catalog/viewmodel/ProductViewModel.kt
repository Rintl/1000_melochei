package com.example.a1000_melochei.ui.customer.catalog.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Product
import com.example.a1000_melochei.data.repository.CartRepository
import com.example.a1000_melochei.data.repository.ProductRepository
import kotlinx.coroutines.launch

/**
 * ViewModel для управления данными о товаре и взаимодействия с ним
 */
class ProductViewModel(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val TAG = "ProductViewModel"

    // LiveData для информации о товаре
    private val _product = MutableLiveData<Resource<Product>>()
    val product: LiveData<Resource<Product>> = _product

    // LiveData для результата добавления в корзину
    private val _addToCartResult = MutableLiveData<Resource<Unit>>()
    val addToCartResult: LiveData<Resource<Unit>> = _addToCartResult

    // LiveData для списка похожих товаров
    private val _similarProducts = MutableLiveData<Resource<List<Product>>>()
    val similarProducts: LiveData<Resource<List<Product>>> = _similarProducts

    /**
     * Загружает информацию о товаре по его ID
     */
    fun loadProduct(productId: String) {
        _product.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = productRepository.getProductById(productId)
                _product.value = result

                // Если товар успешно загружен, загружаем похожие товары
                if (result is Resource.Success && result.data != null) {
                    loadSimilarProducts(result.data.categoryId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке товара: ${e.message}")
                _product.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает список похожих товаров из той же категории
     */
    private fun loadSimilarProducts(categoryId: String) {
        _similarProducts.value = Resource.Loading()

        viewModelScope.launch {
            try {
                // Получаем текущий товар для исключения его из списка похожих
                val currentProductId = _product.value?.data?.id ?: ""

                val result = productRepository.getSimilarProducts(categoryId, currentProductId, 5)
                _similarProducts.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке похожих товаров: ${e.message}")
                _similarProducts.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Добавляет товар в корзину
     */
    fun addToCart(product: Product, quantity: Int = 1) {
        _addToCartResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                // Проверка наличия товара в нужном количестве
                if (product.availableQuantity < quantity) {
                    _addToCartResult.value = Resource.Error("Недостаточное количество товара на складе")
                    return@launch
                }

                // Добавление товара в корзину через репозиторий
                val result = cartRepository.addToCart(product, quantity)
                _addToCartResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при добавлении товара в корзину: ${e.message}")
                _addToCartResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Добавляет/удаляет товар из избранного
     */
    fun toggleFavorite(product: Product) {
        viewModelScope.launch {
            try {
                val newFavoriteStatus = !product.isFavorite

                // Обновляем статус избранного в репозитории
                val result = productRepository.setProductFavorite(product.id, newFavoriteStatus)

                // Если успешно, обновляем локальный объект товара
                if (result is Resource.Success) {
                    // Обновляем LiveData с продуктом
                    val updatedProduct = product.copy(isFavorite = newFavoriteStatus)
                    _product.value = Resource.Success(updatedProduct)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при изменении статуса избранного: ${e.message}")
                // В случае ошибки не меняем UI, так как операция не удалась
            }
        }
    }

    /**
     * Увеличивает счетчик просмотров товара
     */
    fun incrementViewCount() {
        viewModelScope.launch {
            try {
                // Получаем текущий товар
                val currentProduct = _product.value?.data ?: return@launch

                // Увеличиваем счетчик просмотров в репозитории
                productRepository.incrementProductViewCount(currentProduct.id)

                // Обратите внимание, что мы не обновляем UI, так как это фоновая операция
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при увеличении счетчика просмотров: ${e.message}")
                // Игнорируем ошибку, так как это не критическая операция
            }
        }
    }
}