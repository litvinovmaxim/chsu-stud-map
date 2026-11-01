from sqlalchemy import Column, Integer, ForeignKey
from sqlalchemy.orm import relationship
from src.db.database import Base


class OrganizerEvent(Base):
    __tablename__ = "organizers_events"

    id_organizer = Column(Integer, ForeignKey("organizers.id"), primary_key=True)
    id_event = Column(Integer, ForeignKey("events.id"), primary_key=True)

    # связи
    organizer = relationship("Organizer", back_populates="events")
    event = relationship("Event", back_populates="organizers")
