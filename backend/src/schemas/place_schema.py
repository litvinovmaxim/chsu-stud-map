from pydantic import BaseModel
from typing import Optional

class PlaceCreate(BaseModel):
    name: str
    address: Optional[str] = None
    latitude: float
    longitude: float
    category: Optional[str] = None
    osm_type: Optional[str] = None
    osm_id: Optional[int] = None

'''class PlaceUpdate(BaseModel):
    name: Optional[str] = None
    address: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None'''

class PlaceResponse(BaseModel):
    id: int
    name: str
    address: Optional[str] = None
    latitude: float
    longitude: float
    category: Optional[str] = None
    osm_type: Optional[str] = None
    osm_id: Optional[int] = None

    class Config:
        from_attributes = True