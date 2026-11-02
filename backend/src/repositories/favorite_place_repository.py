from sqlalchemy.orm import Session
from src.models.user_place_model import UserPlace
from src.models.user_event_model import UserEvent
from src.models.favorite_place_model import FavoritePlace

# избранные места
def add_favorite_place(db: Session, user_id: int, place_id: dict):
    place = None

    # существует ли уже объект OpenStreetMap
    if place_data.get("osm_id") and place_data.get("osm_type"):
        place = db.query(FavoritePlace).filter(FavoritePlace.osm_id == place_data["osm_id"], FavoritePlace.osm_type == place_data["osm_type"]).first()

    # объекта ещё нет, создаём его
    if not place:
        place = FavoritePlace(**place_data)
        db.add(place)
        db.flush()

    # не добавляли ли это место ранее
    favorite = db.query(UserPlace).filter(UserPlace.id_user == user_id, UserPlace.id_place == place.id).first()

    if not favorite:
        favorite = UserPlace(id_user=user_id, id_place=place.id)
        db.add(favorite)

    db.commit()
    db.refresh(place)

    return place

def get_favorite_places(db: Session, user_id: int):
    return db.query(FavoritePlace).join(UserPlace, UserPlace.id_place == FavoritePlace.id).filter(UserPlace.id_user == user_id).all()

def remove_favorite_place(db: Session, user_id: int, place_id: int):
    favorite = db.query(UserPlace).filter(UserPlace.id_user == user_id, UserPlace.id_place == place_id).first()

    if favorite:
        db.delete(favorite)
        db.commit()

    return favorite

# избранные мероприятия
def add_favorite_event(db: Session, user_id: int, event_id: int):
    favorite = UserEvent(
        id_user=user_id,
        id_event=event_id
    )

    db.add(favorite)
    db.commit()
    db.refresh(favorite)
    return favorite

def get_favorite_events(db: Session, user_id: int):
    return db.query(UserEvent).filter(
        UserEvent.id_user == user_id
    ).all()

def remove_favorite_event(db: Session, user_id: int, event_id: int):
    favorite = db.query(UserEvent).filter(
        UserEvent.id_user == user_id,
        UserEvent.id_event == event_id
    ).first()

    if favorite:
        db.delete(favorite)
        db.commit()

    return favorite
