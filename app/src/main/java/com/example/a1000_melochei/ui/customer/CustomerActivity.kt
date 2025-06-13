package com.example.a1000_melochei.ui.customer

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
import com.example.a1000_melochei.databinding.ActivityCustomerBinding
import com.example.a1000_melochei.ui.auth.LoginActivity
import com.example.a1000_melochei.ui.auth.viewmodel.AuthViewModel
import com.example.a1000_melochei.ui.customer.cart.viewmodel.CartViewModel
import com.example.a1000_melochei.util.showToast
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.navigation.NavigationView
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Основная активность для клиентской части приложения.
 * Содержит навигационное меню и фрагменты для работы с каталогом, корзиной и заказами.
 */
class CustomerActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityCustomerBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val authViewModel: AuthViewModel by viewModel()
    private val cartViewModel: CartViewModel by viewModel()

    private var cartBadge: BadgeDrawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerBinding.inflate(layoutInflater)
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
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_customer) as NavHostFragment
        navController = navHostFragment.navController

        // Определяем главные фрагменты (без кнопки "Назад")
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_catalog,
                R.id.nav_cart,
                R.id.nav_orders,
                R.id.nav_profile
            ),
            binding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        binding.navView.setNavigationItemSelectedListener(this)

        // Настройка Bottom Navigation
        binding.appBarMain.contentMain.bottomNavigation.setupWithNavController(navController)

        // Настройка бейджа для корзины
        setupCartBadge()
    }

    /**
     * Настраивает бейдж корзины
     */
    private fun setupCartBadge() {
        val bottomNav = binding.appBarMain.contentMain.bottomNavigation
        cartBadge = bottomNav.getOrCreateBadge(R.id.nav_cart)
        cartBadge?.isVisible = false
    }

    /**
     * Настраивает наблюдателей
     */
    private fun setupObservers() {
        // Наблюдаем за текущим пользователем
        authViewModel.currentUser.observe(this) { user ->
            if (user == null) {
                // Пользователь не авторизован, переходим к экрану входа
                navigateToLogin()
            } else {
                // Обновляем информацию в навигационном меню
                updateNavigationHeader(user.getDisplayName(), user.email)
            }
        }

        // Наблюдаем за корзиной
        cartViewModel.cart.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val cart = resource.data
                    updateCartBadge(cart.getTotalItemsCount())
                }
                is Resource.Error -> {
                    // При ошибке скрываем бейдж
                    updateCartBadge(0)
                }
                is Resource.Loading -> {
                    // Во время загрузки ничего не меняем
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
    }

    /**
     * Обрабатывает специальные интенты
     */
    private fun handleSpecialIntent() {
        intent?.let { receivedIntent ->
            when {
                receivedIntent.getBooleanExtra("open_cart", false) -> {
                    navController.navigate(R.id.nav_cart)
                }
                receivedIntent.getBooleanExtra("open_orders", false) -> {
                    navController.navigate(R.id.nav_orders)
                }
                receivedIntent.getBooleanExtra("open_order_detail", false) -> {
                    val orderId = receivedIntent.getStringExtra("order_id")
                    if (!orderId.isNullOrEmpty()) {
                        // Переходим к деталям заказа
                        val bundle = Bundle().apply {
                            putString("order_id", orderId)
                        }
                        navController.navigate(R.id.nav_order_detail, bundle)
                    }
                }
                receivedIntent.getBooleanExtra("open_promotions", false) -> {
                    navController.navigate(R.id.nav_promotions)
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

        // Загружаем корзину
        cartViewModel.loadCart()
    }

    /**
     * Обновляет информацию в заголовке навигационного меню
     */
    private fun updateNavigationHeader(name: String, email: String) {
        val headerView = binding.navView.getHeaderView(0)
        val tvName = headerView.findViewById<android.widget.TextView>(R.id.tv_user_name)
        val tvEmail = headerView.findViewById<android.widget.TextView>(R.id.tv_user_email)

        tvName?.text = name
        tvEmail?.text = email
    }

    /**
     * Обновляет бейдж корзины
     */
    private fun updateCartBadge(itemsCount: Int) {
        cartBadge?.let { badge ->
            if (itemsCount > 0) {
                badge.number = itemsCount
                badge.isVisible = true
            } else {
                badge.isVisible = false
            }
        }
    }

    /**
     * Обрабатывает выбор элементов навигационного меню
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                navController.navigate(R.id.nav_home)
            }
            R.id.nav_catalog -> {
                navController.navigate(R.id.nav_catalog)
            }
            R.id.nav_cart -> {
                navController.navigate(R.id.nav_cart)
            }
            R.id.nav_orders -> {
                navController.navigate(R.id.nav_orders)
            }
            R.id.nav_profile -> {
                navController.navigate(R.id.nav_profile)
            }
            R.id.nav_promotions -> {
                navController.navigate(R.id.nav_promotions)
            }
            R.id.nav_settings -> {
                navController.navigate(R.id.nav_settings)
            }
            R.id.nav_help -> {
                navController.navigate(R.id.nav_help)
            }
            R.id.nav_logout -> {
                showLogoutConfirmation()
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Показывает диалог подтверждения выхода
     */
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Выход")
            .setMessage("Вы действительно хотите выйти из аккаунта?")
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
        menuInflater.inflate(R.menu.customer_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                navController.navigate(R.id.nav_search)
                true
            }
            R.id.action_notifications -> {
                navController.navigate(R.id.nav_notifications)
                true
            }
            else -> super.onOptionsItemSelected(item)
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
            navController.currentDestination?.id == R.id.nav_home -> {
                // На главном экране показываем диалог выхода
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
            .setMessage("Вы действительно хотите закрыть приложение?")
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
     * Обновляет текущий фрагмент, если он поддерживает обновление
     */
    fun refreshCurrentFragment() {
        val currentFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_customer)
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull()

        if (currentFragment is Refreshable) {
            currentFragment.refresh()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Очищаем наблюдателей
        authViewModel.currentUser.removeObservers(this)
        authViewModel.logoutResult.removeObservers(this)
        cartViewModel.cart.removeObservers(this)
    }
}