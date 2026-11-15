from src.db.database import SessionLocal
from src.parser.event_parser import parse_and_save


def main():
    db = SessionLocal()

    try:
        parse_and_save(db)
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


if __name__ == "__main__":
    main()