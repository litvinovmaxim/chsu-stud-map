Запуск сервера
uvicorn src.app:app --host 0.0.0.0 --port 8000 --reload 

Точки доступа 

Auth
/auth/register                    - POST   - Регистрация
/auth/login                       - POST   - Вход
/auth/lodout                      - POST   - Выход

Users
users/update/{user_id}            - PUT    - Обноваление профиля (поменять имя, фамилию, пароль, почту)


Events
/events/			GET	Получение всех мероприятий
/events/search			GET	Поиск мероприятий по названию и фильтрация по статусу
/events/favorites/my		GET	Получение избранных мероприятий пользователя
/events/{event_id}		GET	Получение подробной информации о мероприятии
/events/{event_id}/favorite	POST	Добавление мероприятия в избранное
/events/{event_id}/favorite	DELETE	Удаление мероприятия из избранного


Favorite places
/favorites/places		POST	Добавление места в избранное
/favorites/places		GET	Получение избранных мест пользователя
/favorites/places/{place_id}	DELETE	Удаление места из избранного


запустить парсер:
сначала скачать пакеты:
pip install "APScheduler<4"
pip install beautifulsoup4
pip install playwright
playwright install chromium


запуск парсера: python -m src.parser.run_parser
парсер обновляется только в сб в 3:00
