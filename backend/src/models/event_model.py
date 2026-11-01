from sqlalchemy import Column, Integer, String, Text, DateTime
from sqlalchemy.orm import relationship
from src.db.database import Base


class Event(Base):
    __tablename__ = "events"

    id = Column(Integer, primary_key=True)
    title = Column(String)
    type = Column(String)
    level = Column(String)
    event_date = Column(DateTime(timezone=True))
    status = Column(String)
    location = Column(String)
    description = Column(Text)
    source = Column(String)

    # связи
    users = relationship("UserEvent", back_populates="event")
    organizers = relationship("OrganizerEvent", back_populates="event")
