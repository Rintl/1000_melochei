package com.yourstore.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.yourstore.app.R
import com.yourstore.app.data.repository.OrderRepository
import com.yourstore.app.data.repository.UserRepository
import com.yourstore.app.databinding.ActivityAdminBinding
import com.yourstore.app.ui.auth.LoginActivity
import org.koin.android.ext.android.inject

/**
 * Главная активность для интерфейса администратора.
 * Содержит боковое навигационное меню для перехода между разделами
 * администраторской панели.
 */
class AdminActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var toggle: ActionBarDrawerToggle

    private val userRepository: UserRepository by inject()
    private val orderRepository: OrderRepository by inject()
    private val firebaseAuth: FirebaseAuth by inject()

    private var newOrdersBadge: BadgeDrawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigation()
        setupNavigationHeader()
        setupNewOrdersBadge()
        observeNewOrders()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.appBarAdmin.toolbar)
    }

    private fun setupNavigation() {
        // Получаем NavHostFragment и NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_admin) as NavHostFragment
        navController = navHostFragment.navController

        // Настройка AppBarConfiguration с учетом бокового меню
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_admin_dashboard,
                R.id.navigation_admin_products,
                R.id.navigation_admin_categories,
                R.id.navigation_admin_orders,
                R.id.navigation_admin_analytics
            ),
            binding.drawerLayout
        )

        // Настройка ActionBar и NavigationView
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        binding.navView.setNavigationItemSelectedListener(this)

        // Настройка ActionBarDrawerToggle для открытия/закрытия ящика навигации
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.appBarAdmin.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Слушаем изменения пункта назначения для обновления UI
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Обновление заголовка
            when (destination.id) {
                R.id.navigation_admin_dashboard -> supportActionBar?.title = getString(R.string.title_dashboard)
                R.id.navigation_admin_products -> supportActionBar?.title = getString(R.string.title_products)
                R.id.navigation_admin_categories -> supportActionBar?.title = getString(R.string.title_categories)
                R.id.navigation_admin_orders -> supportActionBar?.title = getString(R.string.title_orders)
                R.id.navigation_admin_analytics -> supportActionBar?.title = getString(R.string.title_analytics)
            }
        }
    }

    private fun setupNavigationHeader() {
        // Получаем View заголовка навигационного меню
        val headerView = binding.navView.getHeaderView(0)

        // Обновляем имя администратора в шапке меню
        val tvAdminName = headerView.findViewById<android.widget.TextView>(R.id.tv_admin_name)
        val tvAdminEmail = headerView.findViewById<android.widget.TextView>(R.id.tv_admin_email)

        // Текущий пользователь Firebase
        val currentUser = firebaseAuth.currentUser

        if (currentUser != null) {
            // Загружаем данные пользователя
            userRepository.getUserProfileAsFlow().observeForever { result ->
                result.data?.let { user ->
                    tvAdminName.text = user.name
                    tvAdminEmail.text = user.email
                }
            }
        } else {
            // Если пользователь не авторизован, перенаправляем на экран входа
            navigateToLogin()
        }
    }

    private fun setupNewOrdersBadge() {
        // Создаем бейдж для пункта меню заказов
        val menuItem = binding.navView.menu.findItem(R.id.navigation_admin_orders)
        newOrdersBadge = binding.navView.getOrCreateBadge(menuItem.itemId)
        newOrdersBadge?.isVisible = false
    }

    private fun observeNewOrders() {
        // Наблюдаем за новыми заказами
        orderRepository.getNewOrdersCountAsFlow().observeForever { count ->
            if (count > 0) {
                newOrdersBadge?.number = count
                newOrdersBadge?.isVisible = true
            } else {
                newOrdersBadge?.isVisible = false
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Обработка нажатий на пункты навигационного меню
        when (item.itemId) {
            R.id.navigation_admin_dashboard,
            R.id.navigation_admin_products,
            R.id.navigation_admin_categories,
            R.id.navigation_admin_orders,
            R.id.navigation_admin_analytics -> {
                // Навигация к фрагментам через NavController
                navController.navigate(item.itemId)
            }
            R.id.nav_admin_settings -> {
                // Переход к настройкам
                // В текущей реализации выводим сообщение
                AlertDialog.Builder(this)
                    .setTitle(R.string.settings)
                    .setMessage(R.string.settings_not_implemented)
                    .setPositiveButton(R.string.ok, null)
                    .show()
            }
            R.id.nav_admin_logout -> {
                // Выход из аккаунта
                showLogoutConfirmation()
            }
        }

        // Закрываем ящик навигации
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout_confirmation_title)
            .setMessage(R.string.logout_confirmation_message)
            .setPositiveButton(R.string.logout) { _, _ ->
                logout()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun logout() {
        // Выход из аккаунта
        firebaseAuth.signOut()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
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
                val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_admin)
                    ?.childFragmentManager?.fragments?.firstOrNull()

                // Вызываем метод обновления, если фрагмент поддерживает интерфейс Refreshable
                if (currentFragment is Refreshable) {
                    currentFragment.refresh()
                }
                true
            }
            R.id.action_admin_search -> {
                // В текущей реализации выводим сообщение
                AlertDialog.Builder(this)
                    .setTitle(R.string.search)
                    .setMessage(R.string.search_not_implemented)
                    .setPositiveButton(R.string.ok, null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        // Если ящик навигации открыт, закрываем его
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Интерфейс для фрагментов, которые поддерживают обновление
     */
    interface Refreshable {
        fun refresh()
    }
}