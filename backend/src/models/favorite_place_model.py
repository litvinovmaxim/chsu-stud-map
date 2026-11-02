from sqlalchemy import Column, Integer, String, Float
from sqlalchemy.orm import relationship
from src.db.database import Base


class FavoritePlace(Base):
    __tablename__ = "favorite_places"

    id = Column(Integer, primary_key=True)
    name = Column(String)
    address = Column(String)
    latitude = Column(Float)
    longitude = Column(Float)
    category = Column(String, nullable=True)
    osm_type = Column(String, nullable=True)
    osm_id = Column(Integer, nullable=True)

    # связи
    users = relationship("UserPlace", back_populates="place")
