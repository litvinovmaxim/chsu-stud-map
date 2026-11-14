package ru.chsu.studentmap.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp


/**
 * Экран аутентификации (вход и регистрация в StudMap).
 * 
 * Позволяет:
 * 1. Выполнить регистрацию нового студента (POST /auth/register): имя, фамилия, почта, пароль.
 * 2. Выполнить вход в существующий аккаунт (POST /auth/login): почта и пароль.
 * 3. Переключать видимость пароля для удобного ввода.
 * 4. Просматривать локализованные ошибки валидации и сетевые ошибки от FastAPI бэкенда.
 * 5. Вернуться назад в меню профиля.
 *
 * @param state Текущее состояние формы из [AuthViewModel]
 * @param onName Обработчик ввода имени
 * @param onSurname Обработчик ввода фамилии
 * @param onEmail Обработчик ввода электронной почты
 * @param onPassword Обработчик ввода пароля
 * @param onTogglePasswordVisibility Переключение скрытия/отображения символов пароля
 * @param onToggleMode Переключение режима (Вход <-> Регистрация)
 * @param onSubmit Нажатие кнопки подтверждения формы
 * @param onBack Нажатие кнопки "Назад"
 */
@Composable
fun AuthScreen(
    state: AuthFormState,
    onName: (String) -> Unit,
    onSurname: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit = {},
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад в меню"
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (state.isRegister) "Регистрация" else "Вход в аккаунт",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Приветственный заголовок и краткое описание
            Text(
                text = if (state.isRegister) "Создание аккаунта StudMap" else "Добро пожаловать!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (state.isRegister)
                    "Заполните данные для создания профиля и синхронизации с сервером"
                else
                    "Войдите, чтобы получить доступ к профилю, избранным корпусам и событиям",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Дополнительные поля для регистрации (Имя и Фамилия)
            if (state.isRegister) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onName,
                    label = { Text("Имя") },
                    placeholder = { Text("Например: Иван") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.surname,
                    onValueChange = onSurname,
                    label = { Text("Фамилия") },
                    placeholder = { Text("Например: Иванов") },
                    leadingIcon = {
                        Icon(Icons.Default.Badge, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                Spacer(Modifier.height(12.dp))
            }

            // Поле ввода почты
            OutlinedTextField(
                value = state.email,
                onValueChange = onEmail,
                label = { Text("Электронная почта") },
                placeholder = { Text("example@chsu.ru") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(Modifier.height(12.dp))

            // Поле ввода пароля с переключением видимости
            OutlinedTextField(
                value = state.password,
                onValueChange = onPassword,
                label = { Text("Пароль") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (state.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (state.isPasswordVisible) "Скрыть пароль" else "Показать пароль"
                        )
                    }
                },
                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onSubmit()
                })
            )

            // Отображение сообщения об ошибке
            if (state.error != null) {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Главная кнопка отправки формы (Вход или Регистрация)
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSubmit()
                },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Подключение...")
                } else {
                    Text(
                        text = if (state.isRegister) "Зарегистрироваться" else "Войти",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Кнопка переключения между режимами
            TextButton(onClick = onToggleMode) {
                Text(
                    text = if (state.isRegister)
                        "Уже есть аккаунт? Войти"
                    else
                        "Нет аккаунта? Зарегистрироваться"
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
