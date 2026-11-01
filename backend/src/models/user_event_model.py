from sqlalchemy import Column, Integer, ForeignKey
from sqlalchemy.orm import relationship
from src.db.database import Base


class UserEvent(Base):
    __tablename__ = "users_events"

    id_user = Column(Integer, ForeignKey("users.id"), primary_key=True)
    id_event = Column(Integer, ForeignKey("events.id"), primary_key=True)

    # связи
    user = relationship("User", back_populates="events")
    event = relationship("Event", back_populates="users")
