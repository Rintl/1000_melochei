package com.yourstore.app.ui.admin.products.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourstore.app.R
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Category
import com.yourstore.app.data.model.Product
import com.yourstore.app.data.repository.CategoryRepository
import com.yourstore.app.data.repository.ProductRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel для управления товарами в административной панели.
 */
class AdminProductViewModel(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val TAG = "AdminProductViewModel"

    /**
     * Класс для представления результатов импорта товаров
     */
    data class ImportResult(
        val successCount: Int,
        val updatedCount: Int,
        val failedCount: Int,
        val errors: List<String>
    )

    // LiveData для списка товаров
    private val _products = MutableLiveData<Resource<List<Product>>>()
    val products: LiveData<Resource<List<Product>>> = _products

    // LiveData для информации о товаре
    private val _product = MutableLiveData<Resource<Product>>()
    val product: LiveData<Resource<Product>> = _product

    // LiveData для списка категорий
    private val _categories = MutableLiveData<Resource<List<Category>>>()
    val categories: LiveData<Resource<List<Category>>> = _categories

    // LiveData для результата добавления товара
    private val _addProductResult = MutableLiveData<Resource<Unit>>()
    val addProductResult: LiveData<Resource<Unit>> = _addProductResult

    // LiveData для результата обновления товара
    private val _updateProductResult = MutableLiveData<Resource<Unit>>()
    val updateProductResult: LiveData<Resource<Unit>> = _updateProductResult

    // LiveData для результата удаления товара
    private val _deleteProductResult = MutableLiveData<Resource<Unit>>()
    val deleteProductResult: LiveData<Resource<Unit>> = _deleteProductResult

    // LiveData для результата обновления статуса товара
    private val _updateProductStatusResult = MutableLiveData<Resource<Unit>>()
    val updateProductStatusResult: LiveData<Resource<Unit>> = _updateProductStatusResult

    // LiveData для результата импорта товаров
    private val _importProductsResult = MutableLiveData<Resource<ImportResult>>()
    val importProductsResult: LiveData<Resource<ImportResult>> = _importProductsResult

    // LiveData для валидности формы товара
    private val _productFormValid = MutableLiveData<Boolean>()
    val productFormValid: LiveData<Boolean> = _productFormValid

    // Временное хранилище для оригинального списка товаров
    private var originalProducts: List<Product> = emptyList()

    // Текущее состояние фильтрации и сортировки
    private var currentCategoryFilter: String? = null
    private var currentAvailabilityFilter: Boolean? = null
    private var currentPromotionFilter: Boolean? = null
    private var currentLowStockFilter: Boolean? = null
    private var currentSearchQuery: String = ""
    private var currentSortOrder: SortOrder = SortOrder.NAME_ASC

    // Типы сортировки
    enum class SortOrder {
        NAME_ASC, NAME_DESC, PRICE_ASC, PRICE_DESC, STOCK_ASC, STOCK_DESC, DATE_NEWEST, DATE_OLDEST
    }

    /**
     * Загружает список всех товаров
     */
    fun loadProducts() {
        _products.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = productRepository.getAllProducts()

                if (result is Resource.Success) {
                    originalProducts = result.data ?: emptyList()
                    applyFiltersAndSort()
                } else {
                    _products.value = result
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке товаров: ${e.message}")
                _products.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает информацию о конкретном товаре
     */
    fun loadProduct(productId: String) {
        _product.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = productRepository.getProductById(productId)
                _product.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке товара: ${e.message}")
                _product.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает список всех категорий
     */
    fun loadCategories() {
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
     * Добавляет новый товар
     */
    fun addProduct(
        name: String,
        description: String,
        price: Double,
        discountPrice: Double?,
        availableQuantity: Int,
        categoryId: String,
        sku: String,
        specifications: Map<String, String>,
        imageFiles: List<File>
    ) {
        _addProductResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = productRepository.addProduct(
                    name = name,
                    description = description,
                    price = price,
                    discountPrice = discountPrice,
                    availableQuantity = availableQuantity,
                    categoryId = categoryId,
                    sku = sku,
                    specifications = specifications,
                    imageFiles = imageFiles
                )
                _addProductResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при добавлении товара: ${e.message}")
                _addProductResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Обновляет существующий товар
     */
    fun updateProduct(
        productId: String,
        name: String,
        description: String,
        price: Double,
        discountPrice: Double?,
        availableQuantity: Int,
        categoryId: String,
        sku: String,
        specifications: Map<String, String>,
        imageFiles: List<File>,
        imagesToDelete: List<String>
    ) {
        _updateProductResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = productRepository.updateProduct(
                    productId = productId,
                    name = name,
                    description = description,
                    price = price,
                    discountPrice = discountPrice,
                    availableQuantity = availableQuantity,
                    categoryId = categoryId,
                    sku = sku,
                    specifications = specifications,
                    imageFiles = imageFiles,
                    imagesToDelete = imagesToDelete
                )
                _updateProductResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении товара: ${e.message}")
                _updateProductResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Удаляет товар
     */
    fun deleteProduct(productId: String) {
        _deleteProductResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = productRepository.deleteProduct(productId)
                _deleteProductResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при удалении товара: ${e.message}")
                _deleteProductResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Изменяет статус наличия товара
     */
    fun toggleProductStatus(productId: String, isAvailable: Boolean) {
        _updateProductStatusResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = productRepository.updateProductAvailability(
                    productId = productId,
                    quantity = if (isAvailable) 1 else 0 // Минимальное количество или 0
                )
                _updateProductStatusResult.value = result

                // Обновляем список товаров
                loadProducts()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении статуса товара: ${e.message}")
                _updateProductStatusResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Импортирует товары из файла
     */
    fun importProducts(
        file: File,
        fileType: String,
        defaultCategoryId: String?,
        updateExisting: Boolean
    ) {
        _importProductsResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = productRepository.importProducts(
                    file = file,
                    fileType = fileType,
                    defaultCategoryId = defaultCategoryId,
                    updateExisting = updateExisting
                )

                if (result is Resource.Success) {
                    val importStats = result.data
                    if (importStats != null) {
                        val importResult = ImportResult(
                            successCount = importStats.successCount,
                            updatedCount = importStats.updatedCount,
                            failedCount = importStats.failedCount,
                            errors = importStats.errors
                        )
                        _importProductsResult.value = Resource.Success(importResult)
                    } else {
                        _importProductsResult.value = Resource.Error("Ошибка при получении статистики импорта")
                    }
                } else {
                    _importProductsResult.value = Resource.Error(result.message ?: "Ошибка импорта товаров")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при импорте товаров: ${e.message}")
                _importProductsResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Генерирует шаблон CSV для импорта товаров
     */
    fun generateCSVTemplate(context: Context) {
        viewModelScope.launch {
            try {
                val fileName = "products_template_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.csv"
                val headerRow = "name,description,price,discount_price,category_id,sku,available_quantity,specifications,image_urls"
                val exampleRow = "\"Example Product\",\"Product description\",1000,900,category_id,SKU123,10,\"Material: Wood; Color: Brown\",\"https://example.com/image1.jpg,https://example.com/image2.jpg\""

                val content = "$headerRow\n$exampleRow"

                // Создаем временный файл
                val file = File(context.getExternalFilesDir(null), fileName)
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(content.toByteArray())
                }

                // Создаем Uri для временного файла
                val uri = Uri.fromFile(file)

                // Открываем файл для просмотра/скачивания
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "text/csv")
                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    // Если нет приложения для просмотра CSV, показываем сообщение
                    throw Exception(context.getString(R.string.no_app_to_view_csv))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при создании шаблона CSV: ${e.message}")
                // Обрабатываем ошибку (например, показываем Toast)
            }
        }
    }

    /**
     * Генерирует шаблон Excel для импорта товаров
     */
    fun generateExcelTemplate(context: Context) {
        viewModelScope.launch {
            try {
                // В данной реализации мы просто вызываем метод генерации CSV-шаблона,
                // так как для создания Excel-файла требуется дополнительная библиотека (например, Apache POI)
                // В реальном приложении этот метод бы создавал Excel-файл
                generateCSVTemplate(context)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при создании шаблона Excel: ${e.message}")
                // Обрабатываем ошибку
            }
        }
    }

    /**
     * Экспортирует товары в CSV-файл
     */
    fun exportProductsToCSV(context: Context) {
        viewModelScope.launch {
            try {
                val result = productRepository.exportProductsToCSV(context)

                if (result is Resource.Error) {
                    throw Exception(result.message ?: "Ошибка экспорта товаров")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при экспорте товаров в CSV: ${e.message}")
                // Обрабатываем ошибку
            }
        }
    }

    /**
     * Экспортирует товары в Excel-файл
     */
    fun exportProductsToExcel(context: Context) {
        viewModelScope.launch {
            try {
                val result = productRepository.exportProductsToExcel(context)

                if (result is Resource.Error) {
                    throw Exception(result.message ?: "Ошибка экспорта товаров")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при экспорте товаров в Excel: ${e.message}")
                // Обрабатываем ошибку
            }
        }
    }

    /**
     * Фильтрует товары по категории
     */
    fun filterProductsByCategory(categoryId: String?) {
        currentCategoryFilter = categoryId
        applyFiltersAndSort()
    }

    /**
     * Фильтрует товары по наличию
     */
    fun filterProductsByAvailability(inStockOnly: Boolean) {
        currentAvailabilityFilter = if (inStockOnly) true else null
        applyFiltersAndSort()
    }

    /**
     * Фильтрует товары по акциям
     */
    fun filterProductsByPromotion(onSaleOnly: Boolean) {
        currentPromotionFilter = if (onSaleOnly) true else null
        applyFiltersAndSort()
    }

    /**
     * Фильтрует товары по низкому остатку
     */
    fun filterProductsByLowStock(lowStockOnly: Boolean) {
        currentLowStockFilter = if (lowStockOnly) true else null
        applyFiltersAndSort()
    }

    /**
     * Поиск товаров по названию и SKU
     */
    fun searchProducts(query: String) {
        currentSearchQuery = query
        applyFiltersAndSort()
    }

    /**
     * Сортировка товаров по имени
     */
    fun sortProductsByName(ascending: Boolean) {
        currentSortOrder = if (ascending) SortOrder.NAME_ASC else SortOrder.NAME_DESC
        applyFiltersAndSort()
    }

    /**
     * Сортировка товаров по цене
     */
    fun sortProductsByPrice(ascending: Boolean) {
        currentSortOrder = if (ascending) SortOrder.PRICE_ASC else SortOrder.PRICE_DESC
        applyFiltersAndSort()
    }

    /**
     * Сортировка товаров по остатку
     */
    fun sortProductsByStock(ascending: Boolean) {
        currentSortOrder = if (ascending) SortOrder.STOCK_ASC else SortOrder.STOCK_DESC
        applyFiltersAndSort()
    }

    /**
     * Сортировка товаров по дате добавления
     */
    fun sortProductsByDateAdded(ascending: Boolean) {
        currentSortOrder = if (ascending) SortOrder.DATE_OLDEST else SortOrder.DATE_NEWEST
        applyFiltersAndSort()
    }

    /**
     * Применяет текущие фильтры и сортировку к списку товаров
     */
    private fun applyFiltersAndSort() {
        var filteredList = originalProducts

        // Применяем фильтр по категории
        if (currentCategoryFilter != null) {
            filteredList = filteredList.filter { it.categoryId == currentCategoryFilter }
        }

        // Применяем фильтр по наличию
        if (currentAvailabilityFilter == true) {
            filteredList = filteredList.filter { it.availableQuantity > 0 }
        }

        // Применяем фильтр по акциям
        if (currentPromotionFilter == true) {
            filteredList = filteredList.filter { it.discountPrice != null && it.discountPrice < it.price }
        }

        // Применяем фильтр по низкому остатку
        if (currentLowStockFilter == true) {
            filteredList = filteredList.filter { it.availableQuantity in 1..5 }
        }

        // Применяем поиск
        if (currentSearchQuery.isNotEmpty()) {
            val query = currentSearchQuery.lowercase()
            filteredList = filteredList.filter {
                it.name.lowercase().contains(query) || it.sku.lowercase().contains(query)
            }
        }

        // Применяем сортировку
        filteredList = when (currentSortOrder) {
            SortOrder.NAME_ASC -> filteredList.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> filteredList.sortedByDescending { it.name.lowercase() }
            SortOrder.PRICE_ASC -> filteredList.sortedBy { it.discountPrice ?: it.price }
            SortOrder.PRICE_DESC -> filteredList.sortedByDescending { it.discountPrice ?: it.price }
            SortOrder.STOCK_ASC -> filteredList.sortedBy { it.availableQuantity }
            SortOrder.STOCK_DESC -> filteredList.sortedByDescending { it.availableQuantity }
            SortOrder.DATE_NEWEST -> filteredList.sortedByDescending { it.createdAt }
            SortOrder.DATE_OLDEST -> filteredList.sortedBy { it.createdAt }
        }

        _products.value = Resource.Success(filteredList)
    }

    /**
     * Устанавливает валидность формы товара
     */
    fun setProductFormValid(isValid: Boolean) {
        _productFormValid.value = isValid
    }
}