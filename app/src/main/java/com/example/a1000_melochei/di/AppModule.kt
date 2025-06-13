package com.example.a1000_melochei.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.a1000_melochei.data.repository.*
import com.example.a1000_melochei.data.source.local.CartCache
import com.example.a1000_melochei.data.source.local.PreferencesManager
import com.example.a1000_melochei.data.source.remote.FirebaseAuthSource
import com.example.a1000_melochei.data.source.remote.FirestoreSource
import com.example.a1000_melochei.data.source.remote.StorageSource
import com.example.a1000_melochei.service.NotificationService
import com.example.a1000_melochei.ui.admin.analytics.viewmodel.AnalyticsViewModel
import com.example.a1000_melochei.ui.admin.categories.viewmodel.CategoryViewModel
import com.example.a1000_melochei.ui.admin.dashboard.viewmodel.DashboardViewModel
import com.example.a1000_melochei.ui.admin.orders.viewmodel.AdminOrderViewModel
import com.example.a1000_melochei.ui.admin.products.viewmodel.AdminProductViewModel
import com.example.a1000_melochei.ui.auth.viewmodel.AuthViewModel
import com.example.a1000_melochei.ui.customer.cart.viewmodel.CartViewModel
import com.example.a1000_melochei.ui.customer.catalog.viewmodel.CatalogViewModel
import com.example.a1000_melochei.ui.customer.catalog.viewmodel.ProductViewModel
import com.example.a1000_melochei.ui.customer.home.viewmodel.HomeViewModel
import com.example.a1000_melochei.ui.customer.orders.viewmodel.OrderViewModel
import com.example.a1000_melochei.ui.customer.profile.viewmodel.ProfileViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Модуль основных компонентов приложения
 */
val appModule = module {
    // Firebase
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }

    // Локальные компоненты
    single { PreferencesManager(androidContext()) }
    single { CartCache(androidContext()) }
    single { NotificationService(androidContext()) }

    // Сервисы данных
    single { FirebaseAuthSource(get()) }
    single { FirestoreSource(get()) }
    single { StorageSource(get()) }
}

/**
 * Модуль источников данных и репозиториев
 */
val dataModule = module {
    // Репозитории по порядку зависимостей
    single { UserRepository(get<FirebaseAuthSource>(), get<FirestoreSource>()) }
    single { ProductRepository(get<FirestoreSource>(), get<StorageSource>()) }
    single { CategoryRepository(get<FirestoreSource>(), get<StorageSource>()) }
    single { CartRepository(get<FirestoreSource>(), get<CartCache>(), get<FirebaseAuth>()) }
    // OrderRepository зависит от CartRepository, поэтому определяем его последним
    single { OrderRepository(get<FirestoreSource>(), get<CartRepository>()) }
}

/**
 * Модуль ViewModels
 */
val viewModelModule = module {
    // Auth ViewModels
    viewModel { AuthViewModel(get()) }

    // Customer ViewModels
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { CatalogViewModel(get(), get()) }
    viewModel { ProductViewModel(get()) }
    viewModel { CartViewModel(get()) }
    viewModel { OrderViewModel(get()) }
    viewModel { ProfileViewModel(get()) }

    // Admin ViewModels
    viewModel { DashboardViewModel(get(), get(), get()) }
    viewModel { AdminProductViewModel(get()) }
    viewModel { CategoryViewModel(get()) }
    viewModel { AdminOrderViewModel(get()) }
    viewModel { AnalyticsViewModel(get(), get()) }
}

/**
 * Список всех модулей приложения
 */
val allModules = listOf(
    appModule,
    dataModule,
    viewModelModule
)