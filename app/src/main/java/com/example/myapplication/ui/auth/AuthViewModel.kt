package ru.chsu.studentmap.ui.auth

import android.app.Application
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

data class AuthFormState(
    val isRegister: Boolean = false,
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as StudentMapApplication).container

    val session: StateFlow<UserSession> = container.sessionStore.session.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserSession(null, null, null, null, null)
    )

    private val _form = MutableStateFlow(AuthFormState())
    val form: StateFlow<AuthFormState> = _form.asStateFlow()

    fun toggleMode() = _form.update { it.copy(isRegister = !it.isRegister, error = null) }

    fun onName(v: String) = _form.update { it.copy(name = v) }
    fun onSurname(v: String) = _form.update { it.copy(surname = v) }
    fun onEmail(v: String) = _form.update { it.copy(email = v) }
    fun onPassword(v: String) = _form.update { it.copy(password = v) }

    fun submit() {
        val form = _form.value
        if (form.email.isBlank() || form.password.isBlank()) {
            _form.update { it.copy(error = "Введите почту и пароль") }
            return
        }
        viewModelScope.launch {
            _form.update { it.copy(isLoading = true, error = null) }
            try {
                if (form.isRegister) {
                    container.authRepository.register(form.name, form.surname, form.email, form.password)
                } else {
                    container.authRepository.login(form.email, form.password)
                }
                _form.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _form.update { it.copy(isLoading = false, error = e.toUserMessage()) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch { container.authRepository.logout() }
    }
}
