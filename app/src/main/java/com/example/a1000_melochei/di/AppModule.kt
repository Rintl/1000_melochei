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
    viewModel { HomeViewModel(get(), get()) } // CategoryRepository, ProductRepository
    viewModel { CatalogViewModel(get(), get()) } // CategoryRepository, ProductRepository
    viewModel { ProductViewModel(get()) } // ProductRepository
    viewModel { CartViewModel(get(), get(), get()) } // CartRepository, UserRepository, OrderRepository
    viewModel { OrderViewModel(get(), get()) } // OrderRepository, CartRepository
    viewModel { ProfileViewModel(get(), get()) } // UserRepository, PreferencesManager

    // Admin ViewModels
    viewModel { DashboardViewModel(get(), get(), get()) } // OrderRepository, ProductRepository, CategoryRepository
    viewModel { AdminProductViewModel(get(), get()) } // ProductRepository, CategoryRepository
    viewModel { CategoryViewModel(get()) } // CategoryRepository
    viewModel { AdminOrderViewModel(get()) } // OrderRepository
    viewModel { AnalyticsViewModel(get(), get()) } // OrderRepository, CategoryRepository
}

/**
 * Список всех модулей приложения
 */
val allModules = listOf(
    appModule,
    dataModule,
    viewModelModule
)