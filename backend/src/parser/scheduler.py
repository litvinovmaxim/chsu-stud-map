from apscheduler.schedulers.background import BackgroundScheduler
from src.db.database import SessionLocal
from src.parser.event_parser import parse_and_save

scheduler = BackgroundScheduler()


def run_parser_job():
    db = SessionLocal()

    try:
        print("Запуск автоматического парсера...")
        parse_and_save(db)
        print("Парсер успешно завершил работу")

    except Exception as e:
        print(f"Ошибка автоматического парсера: {e}")

    finally:
        db.close()


def start_scheduler():
    if not scheduler.running:
        scheduler.add_job(
            run_parser_job,
            trigger="cron",
            day_of_week="sat",
            hour=3,
            minute=0,
            timezone="Europe/Moscow",
            id="events_parser",
            replace_existing=True,
            max_instances=1
        )

        scheduler.start()


def stop_scheduler():
    if scheduler.running:
        scheduler.shutdown()
