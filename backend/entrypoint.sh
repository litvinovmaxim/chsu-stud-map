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

echo "Применение миграций Alembic..."
python - << 'EOF'
import os
import sys
from alembic.config import Config
from alembic import command

db_user = os.getenv("DB_USER", "postgres")
db_password = os.getenv("DB_PASSWORD", "postgres")
db_host = os.getenv("DB_HOST", "db")
db_port = os.getenv("DB_PORT", "5432")
db_name = os.getenv("DB_NAME", "StudMap")

db_url = f"postgresql://{db_user}:{db_password}@{db_host}:{db_port}/{db_name}"

try:
    alembic_cfg = Config("alembic.ini")
    alembic_cfg.set_main_option("sqlalchemy.url", db_url)

    # Проверяем состояние базы: созданы ли уже таблицы и инициализирован ли alembic
    import psycopg2
    conn = psycopg2.connect(
        user=db_user,
        password=db_password,
        host=db_host,
        port=int(db_port),
        dbname=db_name
    )
    with conn.cursor() as cur:
        cur.execute("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'alembic_version');")
        has_alembic = cur.fetchone()[0]
        cur.execute("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'events');")
        has_tables = cur.fetchone()[0]
    conn.close()

    if not has_alembic and has_tables:
        print("База данных уже содержит таблицы, созданные до Alembic. Синхронизируем состояние: alembic stamp head...")
        command.stamp(alembic_cfg, "head")
        print("База данных успешно привязана к текущей версии миграций (stamp head).")
    else:
        command.upgrade(alembic_cfg, "head")
        print("Миграции Alembic успешно применены.")
except Exception as e:
    print(f"Ошибка при применении миграций Alembic: {e}")
    sys.exit(1)
EOF

if [ $# -gt 0 ]; then
    exec "$@"
else
    echo "Запуск сервера Uvicorn..."
    exec uvicorn src.app:app --host 0.0.0.0 --port 8000
fi
