#!/bin/sh
set -e

echo "Ожидание готовности PostgreSQL..."
python - << 'EOF'
import os
import sys
import time
import psycopg2

db_user = os.getenv("DB_USER", "postgres")
db_password = os.getenv("DB_PASSWORD", "postgres")
db_host = os.getenv("DB_HOST", "db")
db_port = int(os.getenv("DB_PORT", "5432"))
db_name = os.getenv("DB_NAME", "StudMap")

max_retries = 30
retry_interval = 2

for i in range(max_retries):
    try:
        conn = psycopg2.connect(
            user=db_user,
            password=db_password,
            host=db_host,
            port=db_port,
            dbname=db_name,
            connect_timeout=3
        )
        conn.close()
        print("База данных PostgreSQL готова к работе.")
        sys.exit(0)
    except Exception as e:
        print(f"Ожидание базы данных... ({i + 1}/{max_retries}): {e}")
        time.sleep(retry_interval)

print("Не удалось подключиться к базе данных за отведенное время.")
sys.exit(1)
EOF

echo "Проверка и создание таблиц базы данных..."
python - << 'EOF'
try:
    from src.db.database import engine, Base
    import src.models.user_model
    import src.models.organizer_model
    import src.models.event_model
    import src.models.favorite_place_model
    import src.models.user_place_model
    import src.models.user_event_model
    import src.models.organizer_event_model
    import src.models.token_blacklist_model
    Base.metadata.create_all(bind=engine)
    print("Таблицы базы данных успешно проверены/созданы.")
except Exception as e:
    print(f"Предупреждение при проверке таблиц: {e}")
EOF

if [ $# -gt 0 ]; then
    exec "$@"
else
    echo "Запуск сервера Uvicorn..."
    exec uvicorn src.app:app --host 0.0.0.0 --port 8000
fi
