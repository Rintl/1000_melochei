package com.example.a1000_melochei.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.a1000_melochei.R


/**
 * Утилитный класс для работы с разрешениями Android.
 * Облегчает проверку, запрос и обработку разрешений для приложения.
 */
object PermissionUtils {

    // Категории разрешений
    object Permissions {
        // Камера
        val CAMERA = arrayOf(Manifest.permission.CAMERA)

        // Хранилище файлов (с учетом версий Android)
        val STORAGE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        // Местоположение
        val LOCATION = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    /**
     * Проверяет, предоставлено ли указанное разрешение.
     *
     * @param context Контекст приложения
     * @param permission Разрешение для проверки
     * @return true, если разрешение предоставлено
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Проверяет, предоставлены ли все указанные разрешения.
     *
     * @param context Контекст приложения
     * @param permissions Массив разрешений для проверки
     * @return true, если все разрешения предоставлены
     */
    fun arePermissionsGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { isPermissionGranted(context, it) }
    }

    /**
     * Запрашивает разрешения для активности.
     *
     * @param activity Активность
     * @param permissions Массив разрешений для запроса
     * @param requestCode Код запроса для обработки результата
     */
    fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    /**
     * Запрашивает разрешения для фрагмента.
     *
     * @param fragment Фрагмент
     * @param permissions Массив разрешений для запроса
     * @param requestCode Код запроса для обработки результата
     */
    fun requestPermissions(fragment: Fragment, permissions: Array<String>, requestCode: Int) {
        fragment.requestPermissions(permissions, requestCode)
    }

    /**
     * Использует современный подход с ActivityResultLauncher для запроса разрешений.
     *
     * @param launcher Зарегистрированный лаунчер для запроса разрешений
     * @param permissions Массив разрешений для запроса
     */
    fun requestPermissionsWithLauncher(
        launcher: ActivityResultLauncher<Array<String>>,
        permissions: Array<String>
    ) {
        launcher.launch(permissions)
    }

    /**
     * Проверяет, нужно ли показывать объяснение для запроса разрешения.
     *
     * @param activity Активность
     * @param permission Разрешение для проверки
     * @return true, если нужно показать объяснение
     */
    fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * Показывает диалог объяснения перед запросом разрешений.
     *
     * @param context Контекст
     * @param title Заголовок диалога
     * @param message Сообщение диалога
     * @param onPositiveClick Действие при положительном ответе
     * @param onNegativeClick Действие при отрицательном ответе (по умолчанию ничего не делает)
     */
    fun showPermissionRationaleDialog(
        context: Context,
        title: String,
        message: String,
        onPositiveClick: () -> Unit,
        onNegativeClick: () -> Unit = {}
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { _, _ -> onPositiveClick() }
            .setNegativeButton(R.string.cancel) { _, _ -> onNegativeClick() }
            .create()
            .show()
    }

    /**
     * Показывает диалог с инструкциями по включению разрешений в настройках приложения.
     * Используется, когда пользователь ранее отклонил разрешение с флагом "Больше не спрашивать".
     *
     * @param context Контекст
     * @param message Сообщение диалога
     */
    fun showSettingsDialog(context: Context, message: String) {
        AlertDialog.Builder(context)
            .setTitle(R.string.permission_required)
            .setMessage(message)
            .setPositiveButton(R.string.settings) { _, _ ->
                // Открыть настройки приложения
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            .show()
    }

    /**
     * Проверяет результаты запроса разрешений.
     *
     * @param grantResults Результаты запроса разрешений
     * @return true, если все разрешения были предоставлены
     */
    fun areAllPermissionsGranted(grantResults: IntArray): Boolean {
        return grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }

    /**
     * Создает лаунчер для запроса разрешений (для использования с API ActivityResult).
     *
     * @param fragment Фрагмент
     * @param onResult Обратный вызов с результатом запроса разрешений
     * @return ActivityResultLauncher для запроса разрешений
     */
    fun createPermissionLauncher(
        fragment: Fragment,
        onResult: (Map<String, Boolean>) -> Unit
    ): ActivityResultLauncher<Array<String>> {
        return fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            onResult(permissions)
        }
    }

    /**
     * Проверяет, нужно ли запрашивать разрешения на хранение для Android 10+ (API 29+).
     * С Android 10 для некоторых сценариев достаточно использовать Scoped Storage
     * без запроса разрешений READ/WRITE_EXTERNAL_STORAGE.
     *
     * @return true, если нужно запрашивать разрешения на хранение
     */
    fun isStoragePermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    }

    /**
     * Проверяет, предоставлены ли разрешения для камеры.
     *
     * @param context Контекст приложения
     * @return true, если разрешения для камеры предоставлены
     */
    fun hasCameraPermissions(context: Context): Boolean {
        return arePermissionsGranted(context, Permissions.CAMERA)
    }

    /**
     * Проверяет, предоставлены ли разрешения для хранилища.
     *
     * @param context Контекст приложения
     * @return true, если разрешения для хранилища предоставлены
     */
    fun hasStoragePermissions(context: Context): Boolean {
        return arePermissionsGranted(context, Permissions.STORAGE)
    }

    /**
     * Проверяет, предоставлены ли разрешения для местоположения.
     *
     * @param context Контекст приложения
     * @return true, если разрешения для местоположения предоставлены
     */
    fun hasLocationPermissions(context: Context): Boolean {
        return arePermissionsGranted(context, Permissions.LOCATION)
    }
}