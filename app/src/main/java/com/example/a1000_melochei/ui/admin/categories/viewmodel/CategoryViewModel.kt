package com.yourstore.app.ui.admin.categories.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Category
import com.yourstore.app.data.repository.CategoryRepository
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
     * Загружает информацию о конкретной категории
     */
    fun loadCategory(categoryId: String) {
        _category.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = categoryRepository.getCategoryById(categoryId)
                _category.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке категории: ${e.message}")
                _category.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Добавляет новую категорию
     */
    fun addCategory(name: String, description: String, imageFile: File?, isActive: Boolean = true) {
        _addCategoryResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = categoryRepository.addCategory(name, description, imageFile, isActive)
                _addCategoryResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при добавлении категории: ${e.message}")
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
        imageFile: File?,
        isActive: Boolean
    ) {
        _updateCategoryResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = categoryRepository.updateCategory(
                    categoryId, name, description, imageFile, isActive
                )
                _updateCategoryResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении категории: ${e.message}")
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
                val result = categoryRepository.deleteCategory(categoryId)
                _deleteCategoryResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при удалении категории: ${e.message}")
                _deleteCategoryResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Валидирует форму категории
     */
    fun validateForm(name: String, description: String) {
        val isNameValid = name.trim().isNotEmpty() && name.length >= 2

        // Описание не обязательно, но если есть, должно быть валидным
        val isDescriptionValid = description.isEmpty() || description.length >= 5

        _formValid.value = isNameValid && isDescriptionValid
    }

    /**
     * Переключает статус категории (активна/неактивна)
     */
    fun toggleCategoryStatus(categoryId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                categoryRepository.toggleCategoryStatus(categoryId, isActive)
                // Обновляем список категорий после изменения статуса
                loadCategories()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при изменении статуса категории: ${e.message}")
            }
        }
    }

    /**
     * Изменяет порядок категорий
     */
    fun reorderCategories(categoryIds: List<String>) {
        viewModelScope.launch {
            try {
                categoryRepository.reorderCategories(categoryIds)
                // Обновляем список категорий после изменения порядка
                loadCategories()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при изменении порядка категорий: ${e.message}")
            }
        }
    }
}