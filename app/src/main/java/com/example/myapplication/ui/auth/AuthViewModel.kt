package ru.chsu.studentmap.ui.auth

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.chsu.studentmap.StudentMapApplication
import ru.chsu.studentmap.data.local.UserSession
import ru.chsu.studentmap.data.remote.toUserMessage

/**
 * Состояние формы аутентификации (вход / регистрация).
 *
 * @property isRegister Режим формы: true — регистрация нового аккаунта, false — вход в существующий
 * @property name Имя пользователя (только для регистрации)
 * @property surname Фамилия пользователя (только для регистрации)
 * @property email Электронная почта
 * @property password Пароль
 * @property isPasswordVisible Флаг отображения пароля в открытом виде (скрыть / показать по иконке глаза)
 * @property isLoading Флаг выполнения сетевого запроса к бэкенду (показывает индикатор загрузки)
 * @property error Сообщение об ошибке валидации или сетевого ответа бэкенда (null, если ошибки нет)
 * @property successMessage Сообщение об успешном действии
 */
data class AuthFormState(
    val isRegister: Boolean = false,
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel для управления процессами аутентификации, регистрации и профиля.
 * Связывает UI экраны (AuthScreen, ProfileScreen) с репозиторием [AuthRepository].
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    // Доступ к единому контейнеру зависимостей приложения
    private val container = (application as StudentMapApplication).container
    private val authRepository = container.authRepository

    /**
     * Реактивное состояние сессии текущего пользователя.
     * Автоматически обновляется при входе, выходе или изменении данных в SessionStore.
     */
    val session: StateFlow<UserSession> = container.sessionStore.session.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSession(null, null, null, null, null)
    )

    /** Состояние полей ввода и статуса формы авторизации */
    private val _form = MutableStateFlow(AuthFormState())
    val form: StateFlow<AuthFormState> = _form.asStateFlow()

    /** Переключение между режимом входа и режимом регистрации */
    fun toggleMode() = _form.update {
        it.copy(
            isRegister = !it.isRegister,
            error = null,
            successMessage = null
        )
    }

    /** Переключение видимости символов пароля */
    fun togglePasswordVisibility() = _form.update {
        it.copy(isPasswordVisible = !it.isPasswordVisible)
    }

    // Обработчики ввода текста в поля формы
    fun onName(v: String) = _form.update { it.copy(name = v, error = null) }
    fun onSurname(v: String) = _form.update { it.copy(surname = v, error = null) }
    fun onEmail(v: String) = _form.update { it.copy(email = v, error = null) }
    fun onPassword(v: String) = _form.update { it.copy(password = v, error = null) }
    fun clearError() = _form.update { it.copy(error = null) }

    /**
     * Отправка формы на сервер (регистрация или вход).
     *
     * Порядок работы:
     * 1. Клиентская валидация полей:
     *    - проверка корректности email через Android Patterns.EMAIL_ADDRESS
     *    - проверка заполнения имени и фамилии при регистрации
     *    - проверка длины пароля (не менее 6 символов при регистрации)
     * 2. Запуск корутины в [viewModelScope].
     * 3. Установка флага загрузки `isLoading = true`.
     * 4. Вызов соответствующего метода репозитория (`register` или `login`).
     * 5. При успехе — очистка пароля и флага загрузки.
     * 6. При ошибке — перехват исключения, перевод ошибки в понятный русский текст через [toUserMessage]
     *    и отображение ошибки пользователю.
     */
    fun submit() {
        val formValue = _form.value
        val email = formValue.email.trim()
        val password = formValue.password

        // Валидация Email
        if (email.isBlank()) {
            _form.update { it.copy(error = "Введите адрес электронной почты") }
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _form.update { it.copy(error = "Введите корректный email (например, student@chsu.ru)") }
            return
        }

        // Валидация пароля
        if (password.isBlank()) {
            _form.update { it.copy(error = "Введите пароль") }
            return
        }

        // Дополнительная валидация для режима регистрации
        if (formValue.isRegister) {
            val name = formValue.name.trim()
            val surname = formValue.surname.trim()

            if (name.isBlank()) {
                _form.update { it.copy(error = "Введите ваше имя") }
                return
            }
            if (surname.isBlank()) {
                _form.update { it.copy(error = "Введите вашу фамилию") }
                return
            }
            if (password.length < 6) {
                _form.update { it.copy(error = "Пароль должен содержать не менее 6 символов") }
                return
            }
        }

        // Выполнение сетевого запроса
        viewModelScope.launch {
            _form.update { it.copy(isLoading = true, error = null) }
            try {
                if (formValue.isRegister) {
                    // Регистрация на сервере + автоматический вход
                    authRepository.register(
                        name = formValue.name,
                        surname = formValue.surname,
                        email = email,
                        password = password
                    )
                } else {
                    // Вход по email и паролю
                    authRepository.login(
                        email = email,
                        password = password
                    )
                }
                // Успешное завершение: сбрасываем пароль и загрузку
                _form.update {
                    it.copy(
                        password = "",
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                // Отображаем локализованную ошибку
                _form.update {
                    it.copy(
                        isLoading = false,
                        error = e.toUserMessage()
                    )
                }
            }
        }
    }

    /**
     * Выход из учетной записи.
     * Отправляет POST /auth/logout на сервер и стирает локальную сессию.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    /**
     * Обновление данных профиля пользователя (имя, фамилия, email, новый пароль).
     * Отправляет PUT /users/update/{userId} на бэкенд.
     *
     * @param onComplete Функция обратного вызова с результатом (true при успехе, сообщение об ошибке или успехе)
     */
    fun updateProfile(
        name: String?,
        surname: String?,
        email: String?,
        password: String?,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                authRepository.updateProfile(
                    name = name,
                    surname = surname,
                    email = email,
                    password = password
                )
                onComplete(true, "Данные профиля успешно сохранены")
            } catch (e: Exception) {
                onComplete(false, e.toUserMessage())
            }
        }
    }

    /**
     * Обновляет данные профиля с сервера при открытии вкладки профиля.
     */
    fun refreshProfile() {
        viewModelScope.launch {
            authRepository.refreshProfile()
        }
    }
}
