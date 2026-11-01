from sqlalchemy.orm import sessionmaker, DeclarativeBase
from sqlalchemy import create_engine, MetaData
from src.configs.env import DB_HOST, DB_NAME, DB_PASSWORD, DB_PORT, DB_USER

SQLALCHEMY_DATABASE_URI = f"postgresql+psycopg2://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

# движок
engine = create_engine(SQLALCHEMY_DATABASE_URI)
my_metadata = MetaData()

# класс сессии
SessionLocal = sessionmaker(bind=engine)

# базовый класс для моделей
class Base(DeclarativeBase): pass

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

''''# создание таблиц
def create_tables():
    Base.metadata.create_all(bind=engine)'''
