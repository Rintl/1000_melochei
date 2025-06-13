package com.example.a1000_melochei.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.databinding.ActivityAdminBinding
import com.example.a1000_melochei.ui.auth.LoginActivity
import com.example.a1000_melochei.ui.auth.viewmodel.AuthViewModel
import com.example.a1000_melochei.ui.admin.dashboard.viewmodel.DashboardViewModel
import com.example.a1000_melochei.util.showToast
import com.google.android.material.navigation.NavigationView
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Основная активность для административной части приложения.
 * Содержит навигационное меню и фрагменты для управления товарами, заказами и аналитикой.
 */
class AdminActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val authViewModel: AuthViewModel by viewModel()
    private val dashboardViewModel: DashboardViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupObservers()
        handleSpecialIntent()
        loadInitialData()
    }

    /**
     * Настраивает навигацию
     */
    private fun setupNavigation() {
        setSupportActionBar(binding.appBarMain.toolbar)

        // Настройка Navigation Component
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_admin) as NavHostFragment
        navController = navHostFragment.navController

        // Определяем главные фрагменты (без кнопки "Назад")
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_admin_dashboard,
                R.id.nav_admin_orders,
                R.id.nav_admin_products,
                R.id.nav_admin_categories,
                R.id.nav_admin_analytics
            ),
            binding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        binding.navView.setNavigationItemSelectedListener(this)
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
                    // Обновляем информацию в навигационном меню
                    updateNavigationHeader(user.getDisplayName(), user.email)
                }
            }
        }

        // Наблюдаем за результатом выхода
        authViewModel.logoutResult.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    showToast("Вы вышли из системы")
                    navigateToLogin()
                }
                is Resource.Error -> {
                    showToast("Ошибка при выходе: ${resource.message}")
                }
                is Resource.Loading -> {
                    // Показываем индикатор загрузки если нужно
                }
            }
        }

        // Наблюдаем за статистикой заказов для уведомлений
        dashboardViewModel.ordersStats.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val stats = resource.data
                    updateOrdersNotification(stats.newOrdersCount)
                }
                is Resource.Error -> {
                    // Скрываем уведомления при ошибке
                    updateOrdersNotification(0)
                }
                is Resource.Loading -> {
                    // Во время загрузки ничего не меняем
                }
            }
        }
    }

    /**
     * Обрабатывает специальные интенты
     */
    private fun handleSpecialIntent() {
        intent?.let { receivedIntent ->
            when {
                receivedIntent.getBooleanExtra("open_orders", false) -> {
                    navController.navigate(R.id.nav_admin_orders)
                }
                receivedIntent.getBooleanExtra("open_products", false) -> {
                    navController.navigate(R.id.nav_admin_products)
                }
                receivedIntent.getBooleanExtra("open_analytics", false) -> {
                    navController.navigate(R.id.nav_admin_analytics)
                }
                receivedIntent.getBooleanExtra("open_order_detail", false) -> {
                    val orderId = receivedIntent.getStringExtra("order_id")
                    if (!orderId.isNullOrEmpty()) {
                        val bundle = Bundle().apply {
                            putString("order_id", orderId)
                        }
                        navController.navigate(R.id.nav_admin_order_detail, bundle)
                    }
                }
            }
        }
    }

    /**
     * Загружает начальные данные
     */
    private fun loadInitialData() {
        // Проверяем текущего пользователя
        authViewModel.checkCurrentUser()

        // Загружаем данные дашборда
        dashboardViewModel.loadDashboardData()
    }

    /**
     * Обновляет информацию в заголовке навигационного меню
     */
    private fun updateNavigationHeader(name: String, email: String) {
        val headerView = binding.navView.getHeaderView(0)
        val tvName = headerView.findViewById<android.widget.TextView>(R.id.tv_admin_name)
        val tvEmail = headerView.findViewById<android.widget.TextView>(R.id.tv_admin_email)
        val tvRole = headerView.findViewById<android.widget.TextView>(R.id.tv_admin_role)

        tvName?.text = name
        tvEmail?.text = email
        tvRole?.text = "Администратор"
    }

    /**
     * Обновляет уведомление о новых заказах
     */
    private fun updateOrdersNotification(newOrdersCount: Int) {
        val menuItem = binding.navView.menu.findItem(R.id.nav_admin_orders)
        menuItem?.let { item ->
            if (newOrdersCount > 0) {
                item.title = "Заказы ($newOrdersCount)"
            } else {
                item.title = "Заказы"
            }
        }
    }

    /**
     * Обрабатывает выбор элементов навигационного меню
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_admin_dashboard -> {
                navController.navigate(R.id.nav_admin_dashboard)
            }
            R.id.nav_admin_orders -> {
                navController.navigate(R.id.nav_admin_orders)
            }
            R.id.nav_admin_products -> {
                navController.navigate(R.id.nav_admin_products)
            }
            R.id.nav_admin_categories -> {
                navController.navigate(R.id.nav_admin_categories)
            }
            R.id.nav_admin_analytics -> {
                navController.navigate(R.id.nav_admin_analytics)
            }
            R.id.nav_admin_settings -> {
                navController.navigate(R.id.nav_admin_settings)
            }
            R.id.nav_admin_help -> {
                showHelpDialog()
            }
            R.id.nav_admin_logout -> {
                showLogoutConfirmation()
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Показывает диалог справки
     */
    private fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("Справка")
            .setMessage("Административная панель позволяет:\n\n" +
                    "• Управлять заказами клиентов\n" +
                    "• Добавлять и редактировать товары\n" +
                    "• Управлять категориями товаров\n" +
                    "• Просматривать аналитику продаж\n" +
                    "• Настраивать параметры магазина")
            .setPositiveButton("Понятно", null)
            .show()
    }

    /**
     * Показывает диалог подтверждения выхода
     */
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Выход")
            .setMessage("Вы действительно хотите выйти из административной панели?")
            .setPositiveButton("Выйти") { _, _ ->
                authViewModel.logout()
            }
            .setNegativeButton("Отмена", null)
            .show()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Инфлейт меню в ActionBar
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Обработка нажатий на пункты меню ActionBar
        return when (item.itemId) {
            R.id.action_admin_refresh -> {
                // Обновление текущего фрагмента
                refreshCurrentFragment()
                true
            }
            R.id.action_admin_search -> {
                // Поиск
                showSearchDialog()
                true
            }
            R.id.action_admin_notifications -> {
                // Уведомления
                navController.navigate(R.id.nav_admin_notifications)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Показывает диалог поиска
     */
    private fun showSearchDialog() {
        AlertDialog.Builder(this)
            .setTitle("Поиск")
            .setMessage("Функция поиска будет реализована в следующих версиях")
            .setPositiveButton("OK", null)
            .show()
    }

    /**
     * Обновляет текущий фрагмент, если он поддерживает обновление
     */
    private fun refreshCurrentFragment() {
        val currentFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_admin)
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull()

        if (currentFragment is Refreshable) {
            currentFragment.refresh()
        } else {
            // Если фрагмент не поддерживает обновление, обновляем данные дашборда
            dashboardViewModel.refreshData()
            showToast("Данные обновлены")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        when {
            binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
            navController.currentDestination?.id == R.id.nav_admin_dashboard -> {
                // На главном экране дашборда показываем диалог выхода
                showExitConfirmation()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    /**
     * Показывает диалог подтверждения выхода из приложения
     */
    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Выход из приложения")
            .setMessage("Вы действительно хотите закрыть административную панель?")
            .setPositiveButton("Да") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    /**
     * Обрабатывает новые интенты
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
        handleSpecialIntent()
    }

    /**
     * Интерфейс для фрагментов, которые поддерживают обновление
     */
    interface Refreshable {
        fun refresh()
    }

    /**
     * Показывает быстрые действия в ActionBar
     */
    fun showQuickActions() {
        val quickActions = arrayOf(
            "Добавить товар",
            "Новый заказ",
            "Добавить категорию",
            "Посмотреть аналитику"
        )

        AlertDialog.Builder(this)
            .setTitle("Быстрые действия")
            .setItems(quickActions) { _, which ->
                when (which) {
                    0 -> navController.navigate(R.id.nav_admin_add_product)
                    1 -> navController.navigate(R.id.nav_admin_orders)
                    2 -> navController.navigate(R.id.nav_admin_add_category)
                    3 -> navController.navigate(R.id.nav_admin_analytics)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /**
     * Получает краткую сводку статистики для отображения в уведомлениях
     */
    fun getQuickSummary(): String {
        return dashboardViewModel.getQuickSummary()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Очищаем наблюдателей
        authViewModel.currentUser.removeObservers(this)
        authViewModel.logoutResult.removeObservers(this)
        dashboardViewModel.ordersStats.removeObservers(this)
    }
}