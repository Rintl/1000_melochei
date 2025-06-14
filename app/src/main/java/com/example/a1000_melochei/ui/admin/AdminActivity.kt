package com.example.a1000_melochei.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.a1000_melochei.R
import com.example.a1000_melochei.databinding.ActivityAdminBinding
import com.example.a1000_melochei.ui.auth.LoginActivity
import com.example.a1000_melochei.ui.auth.viewmodel.AuthViewModel
import com.example.a1000_melochei.ui.admin.dashboard.viewmodel.DashboardViewModel
import com.example.a1000_melochei.util.showToast
import com.google.android.material.badge.BadgeDrawable
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Основная активность для административной части приложения.
 * Содержит навигационное меню и фрагменты для управления товарами, заказами и аналитикой.
 */
class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val authViewModel: AuthViewModel by viewModel()
    private val dashboardViewModel: DashboardViewModel by viewModel()

    private var ordersBadge: BadgeDrawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupObservers()
        setupClickListeners()
        handleSpecialIntent()
        loadInitialData()
    }

    /**
     * Настраивает навигацию
     */
    private fun setupNavigation() {
        // Настройка Navigation Component
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Определяем главные фрагменты (без кнопки "Назад")
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard,
                R.id.navigation_products,
                R.id.navigation_categories,
                R.id.navigation_orders,
                R.id.navigation_analytics
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)

        // Создаем badge для заказов
        ordersBadge = binding.bottomNavigation.getOrCreateBadge(R.id.navigation_orders)
        ordersBadge?.isVisible = false
    }

    /**
     * Настраивает наблюдателей
     */
    private fun setupObservers() {
        // Наблюдаем за текущим пользователем
        authViewModel.currentUser.observe(this) { user ->
            when {
                user == null -> {
                    // Пользователь не авторизован
                    navigateToLogin()
                }
                !user.isAdmin -> {
                    // Пользователь не является администратором
                    showToast("У вас нет прав администратора")
                    navigateToLogin()
                }
                else -> {
                    // Пользователь - администратор, можем работать
                    updateAdminInfo(user.name, user.email)
                }
            }
        }

        // Наблюдаем за статистикой заказов
        dashboardViewModel.ordersStats.observe(this) { stats ->
            updateOrdersBadge(stats?.newOrdersCount ?: 0)
        }

        // Наблюдаем за новыми заказами
        dashboardViewModel.newOrdersCount.observe(this) { count ->
            updateOrdersBadge(count)
        }
    }

    /**
     * Настраивает обработчики кликов
     */
    private fun setupClickListeners() {
        // FAB для быстрого добавления товара
        binding.fabAdd.setOnClickListener {
            // Навигация к экрану добавления товара
            navController.navigate(R.id.action_global_add_product)
        }
    }

    /**
     * Обновляет информацию администратора
     */
    private fun updateAdminInfo(name: String, email: String) {
        supportActionBar?.title = "Админ панель"
        supportActionBar?.subtitle = name
    }

    /**
     * Обновляет счетчик новых заказов
     */
    private fun updateOrdersBadge(count: Int) {
        ordersBadge?.apply {
            isVisible = count > 0
            number = count
        }

        // Также обновляем текстовый badge если он есть
        if (count > 0) {
            binding.tvOrdersBadge.visibility = View.VISIBLE
            binding.tvOrdersBadge.text = count.toString()
        } else {
            binding.tvOrdersBadge.visibility = View.GONE
        }
    }

    /**
     * Обрабатывает специальные Intent'ы
     */
    private fun handleSpecialIntent() {
        intent?.let { intent ->
            when {
                intent.hasExtra("navigate_to") -> {
                    val destination = intent.getStringExtra("navigate_to")
                    when (destination) {
                        "orders" -> navController.navigate(R.id.navigation_orders)
                        "products" -> navController.navigate(R.id.navigation_products)
                        "analytics" -> navController.navigate(R.id.navigation_analytics)
                    }
                }
                intent.hasExtra("order_id") -> {
                    val orderId = intent.getLongExtra("order_id", -1)
                    if (orderId != -1L) {
                        val bundle = Bundle().apply {
                            putLong("order_id", orderId)
                        }
                        navController.navigate(R.id.action_global_order_detail, bundle)
                    }
                }
            }
        }
    }

    /**
     * Загружает начальные данные
     */
    private fun loadInitialData() {
        dashboardViewModel.loadDashboardData()
    }

    /**
     * Переходит к экрану авторизации
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Показывает диалог выхода из аккаунта
     */
    fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Выход")
            .setMessage("Вы действительно хотите выйти из административной панели?")
            .setPositiveButton("Выйти") { _, _ ->
                authViewModel.logout()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}