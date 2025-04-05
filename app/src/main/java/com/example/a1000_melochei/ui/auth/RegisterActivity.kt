package com.yourstore.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.yourstore.app.R
import com.yourstore.app.data.common.Resource
import com.yourstore.app.databinding.ActivityRegisterBinding
import com.yourstore.app.ui.customer.CustomerActivity
import com.yourstore.app.ui.auth.viewmodel.AuthViewModel
import com.yourstore.app.util.ValidationUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Экран регистрации нового пользователя
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupObservers()
        setupListeners()
    }

    private fun setupViews() {
        binding.tvTitle.text = getString(R.string.register_title)
        binding.tvSubtitle.text = getString(R.string.register_subtitle)
        binding.toolbar.title = getString(R.string.register)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupObservers() {
        // Наблюдение за результатом регистрации
        viewModel.registerResult.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    navigateToCustomerScreen()
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(result.message ?: getString(R.string.register_error))
                }
            }
        })

        // Наблюдение за состоянием валидации формы
        viewModel.registerFormValid.observe(this, Observer { isValid ->
            binding.btnRegister.isEnabled = isValid
        })
    }

    private fun setupListeners() {
        // Ввод имени
        binding.etName.doOnTextChanged { text, _, _, _ ->
            binding.tilName.error = if (text.toString().length >= 2) null
            else getString(R.string.invalid_name)

            validateForm()
        }

        // Ввод телефона
        binding.etPhone.doOnTextChanged { text, _, _, _ ->
            binding.tilPhone.error = if (ValidationUtils.isValidPhone(text.toString())) null
            else getString(R.string.invalid_phone)

            validateForm()
        }

        // Ввод email
        binding.etEmail.doOnTextChanged { text, _, _, _ ->
            binding.tilEmail.error = if (ValidationUtils.isValidEmail(text.toString())) null
            else getString(R.string.invalid_email)

            validateForm()
        }

        // Ввод пароля
        binding.etPassword.doOnTextChanged { text, _, _, _ ->
            binding.tilPassword.error = if (ValidationUtils.isValidPassword(text.toString())) null
            else getString(R.string.invalid_password)

            validateForm()
        }

        // Подтверждение пароля
        binding.etConfirmPassword.doOnTextChanged { text, _, _, _ ->
            val password = binding.etPassword.text.toString()
            binding.tilConfirmPassword.error = if (text.toString() == password) null
            else getString(R.string.passwords_dont_match)

            validateForm()
        }

        // Ввод адреса
        binding.etAddress.doOnTextChanged { text, _, _, _ ->
            binding.tilAddress.error = if (text.toString().length >= 5) null
            else getString(R.string.invalid_address)

            validateForm()
        }

        // Кнопка регистрации
        binding.btnRegister.setOnClickListener {
            register()
        }

        // Переход на экран входа
        binding.tvLogin.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun validateForm() {
        viewModel.validateRegisterForm(
            name = binding.etName.text.toString(),
            phone = binding.etPhone.text.toString(),
            email = binding.etEmail.text.toString(),
            password = binding.etPassword.text.toString(),
            confirmPassword = binding.etConfirmPassword.text.toString(),
            address = binding.etAddress.text.toString()
        )
    }

    private fun register() {
        viewModel.register(
            name = binding.etName.text.toString(),
            phone = binding.etPhone.text.toString(),
            email = binding.etEmail.text.toString(),
            password = binding.etPassword.text.toString(),
            address = binding.etAddress.text.toString()
        )
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading

        // Отключаем все поля ввода при загрузке
        binding.etName.isEnabled = !isLoading
        binding.etPhone.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
        binding.etAddress.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToCustomerScreen() {
        Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, CustomerActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}