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
import com.yourstore.app.databinding.ActivityAdminLoginBinding
import com.yourstore.app.ui.admin.AdminActivity
import com.yourstore.app.ui.auth.viewmodel.AuthViewModel
import com.yourstore.app.util.ValidationUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Экран входа для администратора
 */
class AdminLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminLoginBinding
    private val viewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupObservers()
        setupListeners()
    }

    private fun setupViews() {
        binding.tvTitle.text = getString(R.string.admin_login_title)
        binding.tvSubtitle.text = getString(R.string.admin_login_subtitle)
        binding.toolbar.title = getString(R.string.admin_login)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupObservers() {
        // Наблюдение за результатом входа администратора
        viewModel.adminLoginResult.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    navigateToAdminScreen()
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(result.message ?: getString(R.string.admin_login_error))
                }
            }
        })

        // Наблюдение за состоянием валидации формы
        viewModel.adminFormValid.observe(this, Observer { isValid ->
            binding.btnAdminLogin.isEnabled = isValid
        })
    }

    private fun setupListeners() {
        // Ввод email
        binding.etAdminEmail.doOnTextChanged { text, _, _, _ ->
            binding.tilAdminEmail.error = if (ValidationUtils.isValidEmail(text.toString())) null
            else getString(R.string.invalid_email)

            viewModel.validateAdminForm(
                binding.etAdminEmail.text.toString(),
                binding.etAdminPassword.text.toString(),
                binding.etAdminCode.text.toString()
            )
        }

        // Ввод пароля
        binding.etAdminPassword.doOnTextChanged { text, _, _, _ ->
            binding.tilAdminPassword.error = if (ValidationUtils.isValidPassword(text.toString())) null
            else getString(R.string.invalid_password)

            viewModel.validateAdminForm(
                binding.etAdminEmail.text.toString(),
                binding.etAdminPassword.text.toString(),
                binding.etAdminCode.text.toString()
            )
        }

        // Ввод кода администратора
        binding.etAdminCode.doOnTextChanged { text, _, _, _ ->
            binding.tilAdminCode.error = if (text.toString().length >= 6) null
            else getString(R.string.invalid_admin_code)

            viewModel.validateAdminForm(
                binding.etAdminEmail.text.toString(),
                binding.etAdminPassword.text.toString(),
                binding.etAdminCode.text.toString()
            )
        }

        // Кнопка входа
        binding.btnAdminLogin.setOnClickListener {
            val email = binding.etAdminEmail.text.toString()
            val password = binding.etAdminPassword.text.toString()
            val adminCode = binding.etAdminCode.text.toString()
            viewModel.loginAsAdmin(email, password, adminCode)
        }

        // Переход на обычный экран входа
        binding.tvRegularLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnAdminLogin.isEnabled = !isLoading
        binding.etAdminEmail.isEnabled = !isLoading
        binding.etAdminPassword.isEnabled = !isLoading
        binding.etAdminCode.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToAdminScreen() {
        startActivity(Intent(this, AdminActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}