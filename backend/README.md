Запуск сервера
uvicorn src.app:app --host 0.0.0.0 --port 8000 --reload 

Точки доступа 

Auth
/auth/register                    - POST   - Регистрация
/auth/login                       - POST   - Вход
/auth/lodout                      - POST   - Выход