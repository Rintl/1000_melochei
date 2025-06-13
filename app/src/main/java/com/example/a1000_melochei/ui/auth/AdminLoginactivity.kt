package com.example.a1000_melochei.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.databinding.ActivityAdminLoginBinding
import com.example.a1000_melochei.ui.admin.AdminActivity
import com.example.a1000_melochei.ui.auth.viewmodel.AuthViewModel
import com.example.a1000_melochei.util.showToast
import com.example.a1000_melochei.util.hideKeyboard
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Активность для авторизации администраторов.
 * Предоставляет специальный интерфейс для входа администраторов с кодом доступа.
 */
class AdminLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminLoginBinding
    private val authViewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        setupListeners()
        handleIntent()
    }

    /**
     * Настраивает пользовательский интерфейс
     */
    private fun setupUI() {
        // Настраиваем ActionBar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.admin_login_title)
        }

        // Устанавливаем начальное состояние
        updateLoginButtonState(false)

        // Показываем предупреждение
        binding.tvWarning.text = getString(R.string.admin_login_warning)
    }

    /**
     * Обрабатывает переданные данные
     */
    private fun handleIntent() {
        // Если email был передан из предыдущего экрана
        val email = intent.getStringExtra("email")
        if (!email.isNullOrEmpty()) {
            binding.etEmail.setText(email)
        }
    }

    /**
     * Настраивает слушатели событий
     */
    private fun setupListeners() {
        // Слушатели изменения текста
        binding.etEmail.doOnTextChanged { _, _, _, _ ->
            updateFormValidation()
            clearFieldError(binding.tilEmail)
        }

        binding.etPassword.doOnTextChanged { _, _, _, _ ->
            updateFormValidation()
            clearFieldError(binding.tilPassword)
        }

        binding.etAdminCode.doOnTextChanged { _, _, _, _ ->
            updateFormValidation()
            clearFieldError(binding.tilAdminCode)
        }

        // Кнопка входа
        binding.btnLogin.setOnClickListener {
            hideKeyboard()
            performAdminLogin()
        }

        // Ссылка на обычный вход
        binding.tvRegularLogin.setOnClickListener {
            navigateToRegularLogin()
        }
    }

    /**
     * Настраивает наблюдателей ViewModel
     */
    private fun setupObservers() {
        // Наблюдаем за валидацией формы
        authViewModel.adminFormValid.observe(this) { isValid ->
            updateLoginButtonState(isValid)
        }

        // Наблюдаем за результатом авторизации администратора
        authViewModel.adminLoginResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoadingState(true)
                }
                is Resource.Success -> {
                    showLoadingState(false)
                    showToast("Добро пожаловать, администратор!")

                    // Переходим к административной панели
                    navigateToAdmin()
                }
                is Resource.Error -> {
                    showLoadingState(false)
                    handleLoginError(resource.message)
                }
            }
        }
    }

    /**
     * Обновляет валидацию формы
     */
    private fun updateFormValidation() {
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        val adminCode = binding.etAdminCode.text.toString()

        authViewModel.updateAdminForm(email, password, adminCode)
    }

    /**
     * Выполняет авторизацию администратора
     */
    private fun performAdminLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val adminCode = binding.etAdminCode.text.toString().trim()

        // Дополнительная валидация
        if (!validateInput(email, password, adminCode)) {
            return
        }

        // Очищаем предыдущие результаты
        authViewModel.clearResults()

        // Выполняем авторизацию администратора
        authViewModel.loginAdmin(email, password, adminCode)
    }

    /**
     * Валидирует введенные данные
     */
    private fun validateInput(email: String, password: String, adminCode: String): Boolean {
        var isValid = true

        // Проверяем email
        val emailError = authViewModel.getEmailValidationError(email)
        if (emailError != null) {
            binding.tilEmail.error = emailError
            isValid = false
        }

        // Проверяем пароль
        val passwordError = authViewModel.getPasswordValidationError(password)
        if (passwordError != null) {
            binding.tilPassword.error = passwordError
            isValid = false
        }

        // Проверяем код администратора
        if (adminCode.isBlank()) {
            binding.tilAdminCode.error = "Введите код администратора"
            isValid = false
        } else if (adminCode.length < 4) {
            binding.tilAdminCode.error = "Код администратора должен содержать не менее 4 символов"
            isValid = false
        }

        return isValid
    }

    /**
     * Обрабатывает ошибки авторизации
     */
    private fun handleLoginError(errorMessage: String?) {
        val message = errorMessage ?: getString(R.string.auth_failed)

        when {
            message.contains("email", ignoreCase = true) -> {
                binding.tilEmail.error = message
            }
            message.contains("password", ignoreCase = true) -> {
                binding.tilPassword.error = message
            }
            message.contains("код", ignoreCase = true) ||
                    message.contains("code", ignoreCase = true) -> {
                binding.tilAdminCode.error = message
                // Очищаем поле кода для безопасности
                binding.etAdminCode.setText("")
            }
            else -> {
                showToast(message)
            }
        }
    }

    /**
     * Обновляет состояние кнопки входа
     */
    private fun updateLoginButtonState(isEnabled: Boolean) {
        binding.btnLogin.isEnabled = isEnabled
        binding.btnLogin.alpha = if (isEnabled) 1.0f else 0.5f
    }

    /**
     * Показывает/скрывает состояние загрузки
     */
    private fun showLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etAdminCode.isEnabled = !isLoading

        if (isLoading) {
            binding.btnLogin.text = getString(R.string.loading)
        } else {
            binding.btnLogin.text = getString(R.string.login)
        }
    }

    /**
     * Очищает ошибку поля
     */
    private fun clearFieldError(textInputLayout: com.google.android.material.textfield.TextInputLayout) {
        textInputLayout.error = null
    }

    /**
     * Переходит к обычному экрану авторизации
     */
    private fun navigateToRegularLogin() {
        finish() // Просто закрываем текущую активность
    }

    /**
     * Переходит к административной панели
     */
    private fun navigateToAdmin() {
        val intent = Intent(this, AdminActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Обрабатывает нажатие кнопки "Назад" в ActionBar
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Обрабатывает нажатие кнопки "Назад"
     */
    override fun onBackPressed() {
        super.onBackPressed()
        // Очищаем введенный код администратора для безопасности
        binding.etAdminCode.setText("")
    }

    /**
     * Очищает наблюдателей и данные при уничтожении активности
     */
    override fun onDestroy() {
        super.onDestroy()

        // Очищаем наблюдателей
        authViewModel.adminLoginResult.removeObservers(this)
        authViewModel.adminFormValid.removeObservers(this)

        // Очищаем поле кода администратора для безопасности
        binding.etAdminCode.setText("")
    }
}