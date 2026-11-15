from datetime import datetime
from typing import Optional
from pydantic import BaseModel, ConfigDict, Field

class OrganizerInEvent(BaseModel):
    id: int
    name: Optional[str] = None
    surname: Optional[str] = None
    email: Optional[str] = None
    phone_number: Optional[str] = None
    model_config = ConfigDict(from_attributes=True)

class EventResponse(BaseModel):
    id: int
    title: Optional[str] = None
    type: Optional[str] = None
    level: Optional[str] = None
    event_date: Optional[datetime] = None
    status: Optional[str] = None
    location: Optional[str] = None
    description: Optional[str] = None
    source: Optional[str] = None

    organizers: list[OrganizerInEvent] = Field(default_factory=list)
    is_favorite: bool = False

    model_config = ConfigDict(from_attributes=True)