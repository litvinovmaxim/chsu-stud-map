from sqlalchemy.orm import Session
from src.repositories import favorite_place_repository
from src.repositories import place_repository
from src.repositories import event_repository

def add_favorite_place(db: Session, user_id: int, place_data):
    return favorite_place_repository.add_favorite_place(db, user_id, place_data.dict())

def get_favorite_places(db: Session, user_id: int):
    return favorite_place_repository.get_favorite_places(db, user_id)

def remove_favorite_place(db: Session, user_id: int, place_id: int):
    favorite = favorite_place_repository.remove_favorite_place(db, user_id, place_id)
    if not favorite:
        return None
    return True

def add_favorite_event(db: Session, user_id: int, event_id: int):
    event = event_repository.get_event_by_id(db, event_id)

    if not event:
        raise Exception("Event not found")

    return favorite_place_repository.add_favorite_event(db, user_id, event_id)

def get_favorite_events(db: Session, user_id: int):
    return favorite_place_repository.get_favorite_events(db, user_id)

def remove_favorite_event(db: Session, user_id: int, event_id: int):
    favorite = favorite_place_repository.remove_favorite_event(db, user_id, event_id)
    if not favorite:
        return None
    return True
