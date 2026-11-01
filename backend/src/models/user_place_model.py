from sqlalchemy import Column, Integer, ForeignKey
from sqlalchemy.orm import relationship
from src.db.database import Base


class UserPlace(Base):
    __tablename__ = "users_places"

    id_user = Column(Integer, ForeignKey("users.id"), primary_key=True)
    id_place = Column(Integer, ForeignKey("favorite_places.id"), primary_key=True)

    # связи
    user = relationship("User", back_populates="places")
    place = relationship("FavoritePlace", back_populates="users")
