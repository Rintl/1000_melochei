package com.yourstore.app.util

/**
 * Класс, содержащий константы для всего приложения.
 * Используется для централизованного хранения идентификаторов, ключей, путей и других констант.
 */
object Constants {

    // Общие константы приложения
    const val APP_NAME = "1000 мелочей"
    const val DEFAULT_LANGUAGE = "ru"
    const val DATABASE_VERSION = 1
    const val DATABASE_NAME = "melochei_db"

    // Константы для Firebase
    const val COLLECTION_USERS = "users"
    const val COLLECTION_PRODUCTS = "products"
    const val COLLECTION_CATEGORIES = "categories"
    const val COLLECTION_ORDERS = "orders"
    const val COLLECTION_PROMOTIONS = "promotions"
    const val COLLECTION_DELIVERY_ZONES = "delivery_zones"
    const val COLLECTION_SETTINGS = "settings"

    const val STORAGE_PRODUCTS_FOLDER = "products"
    const val STORAGE_CATEGORIES_FOLDER = "categories"
    const val STORAGE_USER_AVATARS_FOLDER = "user_avatars"

    // Константы для SharedPreferences
    const val PREFERENCES_NAME = "melochei_preferences"
    const val PREF_USER_ID = "user_id"
    const val PREF_USER_NAME = "user_name"
    const val PREF_USER_EMAIL = "user_email"
    const val PREF_IS_ADMIN = "is_admin"
    const val PREF_DARK_MODE = "dark_mode"
    const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val PREF_CRASH_REPORTING_ENABLED = "crash_reporting_enabled"
    const val PREF_FCM_TOKEN = "fcm_token"
    const val PREF_LAST_SYNC_TIME = "last_sync_time"

    // Константы для работы с корзиной
    const val CART_CACHE_FILE = "cart_cache.json"
    const val MAX_CART_ITEMS = 99
    const val MIN_ORDER_AMOUNT = 5000 // Минимальная сумма заказа в тенге

    // Константы для работы с датами и временем
    const val DEFAULT_DATE_FORMAT = "dd.MM.yyyy"
    const val DEFAULT_TIME_FORMAT = "HH:mm"
    const val DEFAULT_DATETIME_FORMAT = "dd.MM.yyyy HH:mm"
    const val DEFAULT_DATETIME_FORMAT_WITH_SECONDS = "dd.MM.yyyy HH:mm:ss"
    const val SERVER_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    // Константы для пагинации
    const val DEFAULT_PAGE_SIZE = 20
    const val PRODUCTS_PAGE_SIZE = 30
    const val ORDERS_PAGE_SIZE = 15

    // Константы для уведомлений
    const val NOTIFICATION_CHANNEL_ORDER_ID = "order_notifications"
    const val NOTIFICATION_CHANNEL_PROMO_ID = "promo_notifications"
    const val NOTIFICATION_CHANNEL_SYSTEM_ID = "system_notifications"

    const val NOTIFICATION_NEW_ORDER_ID = 1001
    const val NOTIFICATION_ORDER_STATUS_ID = 2001
    const val NOTIFICATION_PROMO_ID = 3001
    const val NOTIFICATION_SYSTEM_ID = 4001

    // Константы для Intent Actions и Extras
    const val ACTION_UPDATE_DATA = "com.yourstore.app.ACTION_UPDATE_DATA"
    const val ACTION_ORDER_STATUS_CHANGED = "com.yourstore.app.ACTION_ORDER_STATUS_CHANGED"
    const val ACTION_LOGOUT = "com.yourstore.app.ACTION_LOGOUT"

    const val EXTRA_ORDER_ID = "order_id"
    const val EXTRA_PRODUCT_ID = "product_id"
    const val EXTRA_CATEGORY_ID = "category_id"
    const val EXTRA_NOTIFICATION_TYPE = "notification_type"

    // Константы для работы с заказами
    const val ORDER_STATUS_PENDING = "pending"           // Ожидает обработки
    const val ORDER_STATUS_PROCESSING = "processing"     // В обработке
    const val ORDER_STATUS_SHIPPING = "shipping"         // Доставляется
    const val ORDER_STATUS_DELIVERED = "delivered"       // Доставлен
    const val ORDER_STATUS_COMPLETED = "completed"       // Завершен
    const val ORDER_STATUS_CANCELLED = "cancelled"       // Отменен

    const val DELIVERY_METHOD_PICKUP = "pickup"          // Самовывоз
    const val DELIVERY_METHOD_DELIVERY = "delivery"      // Доставка

    const val PAYMENT_METHOD_CASH = "cash"               // Наличными
    const val PAYMENT_METHOD_CARD = "card"               // Картой при получении
    const val PAYMENT_METHOD_KASPI = "kaspi"             // Перевод на Kaspi

    // Константы для работы с товарами
    const val LOW_STOCK_THRESHOLD = 5                    // Порог низкого остатка товара

    // Константы для аналитики
    const val ANALYTICS_PERIOD_TODAY = "today"
    const val ANALYTICS_PERIOD_YESTERDAY = "yesterday"
    const val ANALYTICS_PERIOD_WEEK = "week"
    const val ANALYTICS_PERIOD_MONTH = "month"
    const val ANALYTICS_PERIOD_YEAR = "year"

    // Константы для прав доступа
    const val ADMIN_CODE = "123456"                      // Код для регистрации администратора

    // Константы для валидации
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_PASSWORD_LENGTH = 20
    const val MIN_NAME_LENGTH = 2
    const val MIN_ADDRESS_LENGTH = 5
    const val PHONE_REGEX = "^\\+?[0-9]{10,15}$"
    const val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"

    // Константы для работы с файлами
    const val TEMP_FILE_PREFIX = "melochei_temp_"
    const val CSV_MIME_TYPE = "text/csv"
    const val EXCEL_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val PDF_MIME_TYPE = "application/pdf"
    const val IMAGE_QUALITY = 85
    const val MAX_IMAGE_WIDTH = 1200
    const val MAX_IMAGE_HEIGHT = 1200

    // Константы для работы с API
    const val CONNECTION_TIMEOUT = 30000L                // 30 секунд
    const val READ_TIMEOUT = 30000L                      // 30 секунд
    const val WRITE_TIMEOUT = 30000L                     // 30 секунд

    // Константы для системных настроек
    const val DEFAULT_DELIVERY_COST = 1000               // Стоимость доставки в тенге
    const val FREE_DELIVERY_THRESHOLD = 20000            // Порог бесплатной доставки в тенге

    const val NOTIFICATION_CHANNEL_ORDER_ID = "channel_orders"
    const val NOTIFICATION_CHANNEL_PROMO_ID = "channel_promo"
    const val NOTIFICATION_CHANNEL_SYSTEM_ID = "channel_system"

    // ID уведомлений
    const val NOTIFICATION_NEW_ORDER_ID = 1001
    const val NOTIFICATION_ORDER_STATUS_ID = 1002
    const val NOTIFICATION_PROMO_ID = 2001
    const val NOTIFICATION_SYSTEM_ID = 3001

    // Действия для broadcast receiver
    const val ACTION_UPDATE_DATA = "com.yourstore.app.ACTION_UPDATE_DATA"

    // Ключи для SharedPreferences
    const val PREF_USER_ID = "user_id"
    const val PREF_FCM_TOKEN = "fcm_token"
    const val PREF_DARK_MODE = "dark_mode"
    const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"

    // Ключи для Intent
    const val EXTRA_PRODUCT_ID = "PRODUCT_ID"
    const val EXTRA_CATEGORY_ID = "CATEGORY_ID"
    const val EXTRA_ORDER_ID = "ORDER_ID"

    const val USERS_COLLECTION = "users"
    const val PRODUCTS_COLLECTION = "products"
    const val CATEGORIES_COLLECTION = "categories"
    const val ORDERS_COLLECTION = "orders"
    const val CARTS_COLLECTION = "carts"

    // Константы для Intent Actions
    const val ACTION_UPDATE_DATA = "com.yourstore.app.ACTION_UPDATE_DATA"
    const val ACTION_VIEW_PRODUCT = "com.yourstore.app.ACTION_VIEW_PRODUCT"
    const val ACTION_VIEW_CATEGORY = "com.yourstore.app.ACTION_VIEW_CATEGORY"
    const val ACTION_VIEW_ORDER = "com.yourstore.app.ACTION_VIEW_ORDER"

    // Константы для уведомлений
    const val NOTIFICATION_CHANNEL_ORDER_ID = "order_notifications"
    const val NOTIFICATION_CHANNEL_PROMO_ID = "promo_notifications"
    const val NOTIFICATION_CHANNEL_SYSTEM_ID = "system_notifications"

    const val NOTIFICATION_NEW_ORDER_ID = 1001
    const val NOTIFICATION_ORDER_STATUS_ID = 1002
    const val NOTIFICATION_PROMO_ID = 1003
    const val NOTIFICATION_SYSTEM_ID = 1004

    // Константы для FileProvider
    const val FILE_PROVIDER_AUTHORITY = "com.yourstore.app.fileprovider"

    // Лимиты и ограничения
    const val MAX_PRODUCT_IMAGES = 5
    const val MAX_CART_ITEMS = 99
    const val PRICE_DECIMALS = 2

    // Константы для аналитики заказов
    const val MIN_ORDER_AMOUNT = 1000.0 // Минимальная сумма заказа
    const val FREE_DELIVERY_THRESHOLD = 10000.0 // Порог для бесплатной доставки

    // Лимиты для пагинации
    const val DEFAULT_PAGE_SIZE = 20
}