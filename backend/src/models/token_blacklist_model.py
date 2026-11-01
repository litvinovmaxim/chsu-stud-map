from sqlalchemy import Column, String, DateTime
from src.db.database import Base


class TokenBlacklist(Base):
    """
    Хранит токены при выходе из аккаунта
    """
    __tablename__ = 'token_blacklist'

    token = Column(String(1024), primary_key=True)  # Токен
    expires_at = Column(DateTime, nullable=False)  # Дата удаления записи