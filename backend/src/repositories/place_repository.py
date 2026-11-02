from sqlalchemy.orm import Session
from src.models.favorite_place_model import FavoritePlace

def create_place(db: Session, place_data: dict):
    place = FavoritePlace(**place_data)
    db.add(place)
    db.commit()
    db.refresh(place)
    return place

def get_place_by_id(db: Session, place_id: int):
    return db.query(FavoritePlace).filter(
        FavoritePlace.id == place_id
    ).first()

def get_places(db: Session):
    return db.query(FavoritePlace).all()

def update_place(db: Session, place: FavoritePlace, data: dict):
    for key, value in data.items():
        setattr(place, key, value)

    db.commit()
    db.refresh(place)
    return place

def delete_place(db: Session, place: FavoritePlace):
    db.delete(place)
    db.commit()
