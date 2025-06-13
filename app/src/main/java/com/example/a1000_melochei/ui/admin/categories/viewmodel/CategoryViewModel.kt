package com.example.a1000_melochei.ui.admin.categories.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Category
import com.example.a1000_melochei.data.repository.CategoryRepository
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel для управления категориями товаров в административной панели
 */
class CategoryViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val TAG = "CategoryViewModel"

    // LiveData для списка категорий
    private val _categories = MutableLiveData<Resource<List<Category>>>()
    val categories: LiveData<Resource<List<Category>>> = _categories

    // LiveData для отдельной категории
    private val _category = MutableLiveData<Resource<Category>>()
    val category: LiveData<Resource<Category>> = _category

    // LiveData для результата добавления категории
    private val _addCategoryResult = MutableLiveData<Resource<Unit>>()
    val addCategoryResult: LiveData<Resource<Unit>> = _addCategoryResult

    // LiveData для результата обновления категории
    private val _updateCategoryResult = MutableLiveData<Resource<Unit>>()
    val updateCategoryResult: LiveData<Resource<Unit>> = _updateCategoryResult

    // LiveData для результата удаления категории
    private val _deleteCategoryResult = MutableLiveData<Resource<Unit>>()
    val deleteCategoryResult: LiveData<Resource<Unit>> = _deleteCategoryResult

    // LiveData для валидации формы
    private val _formValid = MutableLiveData<Boolean>()
    val formValid: LiveData<Boolean> = _formValid

    // LiveData для загрузки изображения
    private val _imageUploadResult = MutableLiveData<Resource<String>>()
    val imageUploadResult: LiveData<Resource<String>> = _imageUploadResult

    // Данные формы для валидации
    private var categoryName: String = ""
    private var categoryDescription: String = ""
    private var selectedImageUri: Uri? = null

    /**
     * Загружает список всех категорий
     */
    fun loadCategories() {
        _categories.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = categoryRepository.getCategories()
                _categories.value = result

                Log.d(TAG, "Загружено категорий: ${result.getDataOrNull()?.size ?: 0}")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке категорий: ${e.message}", e)
                _categories.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает информацию о конкретной категории
     */
    fun loadCategory(categoryId: String) {
        _category.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = categoryRepository.getCategoryById(categoryId)
                _category.value = result

                // Заполняем данные формы для редактирования
                result.getDataOrNull()?.let { category ->
                    categoryName = category.name
                    categoryDescription = category.description
                    validateForm()
                }

                Log.d(TAG, "Загружена категория: ${result.getDataOrNull()?.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке категории: ${e.message}", e)
                _category.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Добавляет новую категорию
     */
    fun addCategory(
        name: String,
        description: String,
        imageUri: Uri? = null,
        parentId: String = "",
        sortOrder: Int = 0
    ) {
        _addCategoryResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                // Сначала загружаем изображение, если оно есть
                var imageUrl = ""
                if (imageUri != null) {
                    val uploadResult = categoryRepository.uploadCategoryImage(imageUri)
                    when (uploadResult) {
                        is Resource.Success -> {
                            imageUrl = uploadResult.data
                        }
                        is Resource.Error -> {
                            _addCategoryResult.value = Resource.Error(
                                "Ошибка загрузки изображения: ${uploadResult.message}"
                            )
                            return@launch
                        }
                        is Resource.Loading -> { /* Игнорируем */ }
                    }
                }

                // Создаем категорию
                val category = Category(
                    name = name.trim(),
                    description = description.trim(),
                    imageUrl = imageUrl,
                    parentId = parentId,
                    sortOrder = sortOrder,
                    isActive = true,
                    productCount = 0
                )

                val result = categoryRepository.addCategory(category)
                _addCategoryResult.value = result

                if (result is Resource.Success) {
                    Log.d(TAG, "Категория добавлена: $name")
                    loadCategories() // Обновляем список категорий
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при добавлении категории: ${e.message}", e)
                _addCategoryResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Обновляет существующую категорию
     */
    fun updateCategory(
        categoryId: String,
        name: String,
        description: String,
        imageUri: Uri? = null,
        existingImageUrl: String = "",
        parentId: String = "",
        sortOrder: Int = 0,
        isActive: Boolean = true
    ) {
        _updateCategoryResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                // Загружаем новое изображение, если оно выбрано
                var imageUrl = existingImageUrl
                if (imageUri != null) {
                    val uploadResult = categoryRepository.uploadCategoryImage(imageUri)
                    when (uploadResult) {
                        is Resource.Success -> {
                            imageUrl = uploadResult.data
                            // Удаляем старое изображение, если оно было
                            if (existingImageUrl.isNotEmpty()) {
                                categoryRepository.deleteImage(existingImageUrl)
                            }
                        }
                        is Resource.Error -> {
                            _updateCategoryResult.value = Resource.Error(
                                "Ошибка загрузки изображения: ${uploadResult.message}"
                            )
                            return@launch
                        }
                        is Resource.Loading -> { /* Игнорируем */ }
                    }
                }

                // Обновляем категорию
                val updatedCategory = Category(
                    id = categoryId,
                    name = name.trim(),
                    description = description.trim(),
                    imageUrl = imageUrl,
                    parentId = parentId,
                    sortOrder = sortOrder,
                    isActive = isActive
                ).withUpdatedTime()

                val result = categoryRepository.updateCategory(updatedCategory)
                _updateCategoryResult.value = result

                if (result is Resource.Success) {
                    Log.d(TAG, "Категория обновлена: $name")
                    loadCategories() // Обновляем список категорий
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении категории: ${e.message}", e)
                _updateCategoryResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Удаляет категорию
     */
    fun deleteCategory(categoryId: String) {
        _deleteCategoryResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                // Сначала проверяем, есть ли товары в категории
                val categoryResult = categoryRepository.getCategoryById(categoryId)
                if (categoryResult is Resource.Success) {
                    val category = categoryResult.data
                    if (category.productCount > 0) {
                        _deleteCategoryResult.value = Resource.Error(
                            "Нельзя удалить категорию с товарами. Сначала переместите товары в другую категорию."
                        )
                        return@launch
                    }

                    // Удаляем категорию
                    val result = categoryRepository.deleteCategory(categoryId)
                    _deleteCategoryResult.value = result

                    if (result is Resource.Success) {
                        Log.d(TAG, "Категория удалена: ${category.name}")

                        // Удаляем изображение категории
                        if (category.imageUrl.isNotEmpty()) {
                            categoryRepository.deleteImage(category.imageUrl)
                        }

                        loadCategories() // Обновляем список категорий
                    }
                } else {
                    _deleteCategoryResult.value = Resource.Error("Категория не найдена")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при удалении категории: ${e.message}", e)
                _deleteCategoryResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает изображение категории
     */
    fun uploadCategoryImage(imageUri: Uri) {
        _imageUploadResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = categoryRepository.uploadCategoryImage(imageUri)
                _imageUploadResult.value = result

                if (result is Resource.Success) {
                    Log.d(TAG, "Изображение загружено: ${result.data}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке изображения: ${e.message}", e)
                _imageUploadResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Загружает изображение из файла
     */
    fun uploadCategoryImageFromFile(imageFile: File) {
        _imageUploadResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = categoryRepository.uploadCategoryImageFromFile(imageFile)
                _imageUploadResult.value = result

                if (result is Resource.Success) {
                    Log.d(TAG, "Изображение загружено из файла: ${result.data}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке изображения из файла: ${e.message}", e)
                _imageUploadResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Обновляет данные формы для валидации
     */
    fun updateFormData(name: String, description: String, imageUri: Uri? = null) {
        categoryName = name
        categoryDescription = description
        selectedImageUri = imageUri
        validateForm()
    }

    /**
     * Валидирует форму добавления/редактирования категории
     */
    private fun validateForm() {
        val isNameValid = Category.isValidName(categoryName)
        val isDescriptionValid = Category.isValidDescription(categoryDescription)

        val isValid = isNameValid && isDescriptionValid

        _formValid.value = isValid

        Log.d(TAG, "Валидация формы: name=$isNameValid, description=$isDescriptionValid, valid=$isValid")
    }

    /**
     * Получает список активных категорий
     */
    fun getActiveCategories(): LiveData<Resource<List<Category>>> {
        val activeCategoriesLiveData = MutableLiveData<Resource<List<Category>>>()

        viewModelScope.launch {
            try {
                val result = categoryRepository.getActiveCategories()
                activeCategoriesLiveData.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке активных категорий: ${e.message}", e)
                activeCategoriesLiveData.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }

        return activeCategoriesLiveData
    }

    /**
     * Получает список родительских категорий (для создания подкатегорий)
     */
    fun getParentCategories(): LiveData<Resource<List<Category>>> {
        val parentCategoriesLiveData = MutableLiveData<Resource<List<Category>>>()

        viewModelScope.launch {
            try {
                val result = categoryRepository.getParentCategories()
                parentCategoriesLiveData.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке родительских категорий: ${e.message}", e)
                parentCategoriesLiveData.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }

        return parentCategoriesLiveData
    }

    /**
     * Изменяет статус активности категории
     */
    fun toggleCategoryStatus(categoryId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                val result = categoryRepository.updateCategoryStatus(categoryId, isActive)

                if (result is Resource.Success) {
                    Log.d(TAG, "Статус категории изменен: $categoryId -> $isActive")
                    loadCategories() // Обновляем список
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при изменении статуса категории: ${e.message}", e)
            }
        }
    }

    /**
     * Ищет категории по названию
     */
    fun searchCategories(query: String): LiveData<Resource<List<Category>>> {
        val searchResultsLiveData = MutableLiveData<Resource<List<Category>>>()

        viewModelScope.launch {
            try {
                val result = categoryRepository.searchCategories(query)
                searchResultsLiveData.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при поиске категорий: ${e.message}", e)
                searchResultsLiveData.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }

        return searchResultsLiveData
    }

    /**
     * Обновляет порядок сортировки категорий
     */
    fun updateCategoriesOrder(categories: List<Category>) {
        viewModelScope.launch {
            try {
                val result = categoryRepository.updateCategoriesOrder(categories)

                if (result is Resource.Success) {
                    Log.d(TAG, "Порядок категорий обновлен")
                    loadCategories() // Обновляем список
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении порядка категорий: ${e.message}", e)
            }
        }
    }

    /**
     * Очищает результаты операций
     */
    fun clearResults() {
        _addCategoryResult.value = Resource.Loading()
        _updateCategoryResult.value = Resource.Loading()
        _deleteCategoryResult.value = Resource.Loading()
        _imageUploadResult.value = Resource.Loading()
    }

    /**
     * Сбрасывает данные формы
     */
    fun resetForm() {
        categoryName = ""
        categoryDescription = ""
        selectedImageUri = null
        _formValid.value = false
    }
}