package com.yourstore.app.ui.customer.profile.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.User
import com.yourstore.app.data.repository.UserRepository
import com.yourstore.app.data.source.local.PreferencesManager
import com.yourstore.app.util.ValidationUtils
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel для управления данными профиля пользователя
 */
class ProfileViewModel(
    private val userRepository: UserRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val TAG = "ProfileViewModel"

    // LiveData для профиля пользователя
    private val _userProfile = MutableLiveData<Resource<User>>()
    val userProfile: LiveData<Resource<User>> = _userProfile

    // LiveData для результата обновления профиля
    private val _updateProfileResult = MutableLiveData<Resource<Unit>>()
    val updateProfileResult: LiveData<Resource<Unit>> = _updateProfileResult

    // LiveData для результата смены пароля
    private val _changePasswordResult = MutableLiveData<Resource<Unit>>()
    val changePasswordResult: LiveData<Resource<Unit>> = _changePasswordResult

    // LiveData для результата добавления адреса
    private val _addAddressResult = MutableLiveData<Resource<Unit>>()
    val addAddressResult: LiveData<Resource<Unit>> = _addAddressResult

    // LiveData для результата удаления адреса
    private val _deleteAddressResult = MutableLiveData<Resource<Unit>>()
    val deleteAddressResult: LiveData<Resource<Unit>> = _deleteAddressResult

    // LiveData для результата установки адреса по умолчанию
    private val _setDefaultAddressResult = MutableLiveData<Resource<Unit>>()
    val setDefaultAddressResult: LiveData<Resource<Unit>> = _setDefaultAddressResult

    // LiveData для результата выхода из аккаунта
    private val _logoutResult = MutableLiveData<Resource<Unit>>()
    val logoutResult: LiveData<Resource<Unit>> = _logoutResult

    // LiveData для настроек приложения
    private val _isDarkMode = MutableLiveData<Boolean>()
    val isDarkMode: LiveData<Boolean> = _isDarkMode

    private val _notificationsEnabled = MutableLiveData<Boolean>()
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _crashReportingEnabled = MutableLiveData<Boolean>()
    val crashReportingEnabled: LiveData<Boolean> = _crashReportingEnabled

    // LiveData для валидации форм
    private val _profileFormValid = MutableLiveData<Boolean>()
    val profileFormValid: LiveData<Boolean> = _profileFormValid

    private val _passwordFormValid = MutableLiveData<Boolean>()
    val passwordFormValid: LiveData<Boolean> = _passwordFormValid

    private val _addressFormValid = MutableLiveData<Boolean>()
    val addressFormValid: LiveData<Boolean> = _addressFormValid

    init {
        // Инициализация настроек из PreferencesManager
        _isDarkMode.value = preferencesManager.getDarkMode()
        _notificationsEnabled.value = preferencesManager.getNotificationsEnabled()
        _crashReportingEnabled.value = preferencesManager.getCrashReportingEnabled()
    }

    /**
     * Загружает профиль пользователя
     */
    fun loadUserProfile() {
        _userProfile.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = userRepository.getUserProfile()
                _userProfile.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке профиля: ${e.message}")
                _userProfile.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Обновляет профиль пользователя
     */
    fun updateProfile(name: String, phone: String, avatarFile: File? = null) {
        _updateProfileResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = userRepository.updateUserProfile(name, phone, avatarFile)
                _updateProfileResult.value = result

                // Если успешно, обновляем данные профиля
                if (result is Resource.Success) {
                    loadUserProfile()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении профиля: ${e.message}")
                _updateProfileResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Изменяет пароль пользователя
     */
    fun changePassword(currentPassword: String, newPassword: String) {
        _changePasswordResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = userRepository.changePassword(currentPassword, newPassword)
                _changePasswordResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при смене пароля: ${e.message}")
                _changePasswordResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Добавляет новый адрес
     */
    fun addAddress(title: String, address: String, isDefault: Boolean = false) {
        _addAddressResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = userRepository.addAddress(title, address, isDefault)
                _addAddressResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при добавлении адреса: ${e.message}")
                _addAddressResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Обновляет существующий адрес
     */
    fun updateAddress(addressId: String, title: String, address: String, isDefault: Boolean = false) {
        _addAddressResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = userRepository.updateAddress(addressId, title, address, isDefault)
                _addAddressResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обновлении адреса: ${e.message}")
                _addAddressResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Удаляет адрес
     */
    fun deleteAddress(addressId: String) {
        _deleteAddressResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = userRepository.deleteAddress(addressId)
                _deleteAddressResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при удалении адреса: ${e.message}")
                _deleteAddressResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Устанавливает адрес по умолчанию
     */
    fun setDefaultAddress(addressId: String) {
        _setDefaultAddressResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = userRepository.setDefaultAddress(addressId)
                _setDefaultAddressResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при установке адреса по умолчанию: ${e.message}")
                _setDefaultAddressResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Выходит из аккаунта
     */
    fun logout() {
        _logoutResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = userRepository.logout()
                _logoutResult.value = result

                // Сбрасываем настройки пользователя при выходе
                if (result is Resource.Success) {
                    preferencesManager.clearUserPreferences()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при выходе из аккаунта: ${e.message}")
                _logoutResult.value = Resource.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    /**
     * Включает/выключает темную тему
     */
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesManager.setDarkMode(enabled)
                _isDarkMode.value = enabled
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при изменении темы: ${e.message}")
            }
        }
    }

    /**
     * Включает/выключает уведомления
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesManager.setNotificationsEnabled(enabled)
                _notificationsEnabled.value = enabled
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при изменении настроек уведомлений: ${e.message}")
            }
        }
    }

    /**
     * Включает/выключает сбор отчетов о сбоях
     */
    fun setCrashReportingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesManager.setCrashReportingEnabled(enabled)
                _crashReportingEnabled.value = enabled
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при изменении настроек сбора отчетов: ${e.message}")
            }
        }
    }

    /**
     * Валидация формы редактирования профиля
     */
    fun validateProfileForm(name: String, phone: String) {
        val isNameValid = name.length >= 2
        val isPhoneValid = ValidationUtils.isValidPhone(phone)

        _profileFormValid.value = isNameValid && isPhoneValid
    }

    /**
     * Валидация формы смены пароля
     */
    fun validatePasswordForm(currentPassword: String, newPassword: String, confirmPassword: String) {
        val isCurrentPasswordValid = currentPassword.length >= 6
        val isNewPasswordValid = ValidationUtils.isValidPassword(newPassword)
        val doPasswordsMatch = newPassword == confirmPassword

        _passwordFormValid.value = isCurrentPasswordValid && isNewPasswordValid && doPasswordsMatch
    }

    /**
     * Валидация формы адреса
     */
    fun validateAddressForm(title: String, address: String) {
        val isTitleValid = title.length >= 3
        val isAddressValid = address.length >= 5

        _addressFormValid.value = isTitleValid && isAddressValid
    }
}