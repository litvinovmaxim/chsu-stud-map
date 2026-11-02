from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from src.db.database import get_db
from src.services import favorite_place_service
from src.schemas import place_schema
from src.models.user_model import User
from src.services.auth_service import get_current_user

router = APIRouter(tags=["Favorites places"])


# Избранные места

@router.post("/places", response_model=place_schema.PlaceResponse)
def add_favorite_place(
    place: place_schema.PlaceCreate,
current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    try:
        return favorite_place_service.add_favorite_place(
            db, current_user.id, place
        )
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/places", response_model=list[place_schema.PlaceResponse])
def get_favorite_places(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return favorite_place_service.get_favorite_places(db, current_user.id)


@router.delete("/places/{place_id}")
def remove_favorite_place(
        place_id: int,
        current_user: User = Depends(get_current_user),
        db: Session = Depends(get_db)
):
    success = favorite_place_service.remove_favorite_place(
        db, current_user.id, place_id
    )

    if not success:
        raise HTTPException(status_code=404, detail="Favorite place not found")

    return {"message": "Favorite place deleted"}


# Избранные мероприятия

@router.post("/events/{user_id}/{event_id}")
def add_favorite_event(
    user_id: int,
    event_id: int,
    db: Session = Depends(get_db)
):
    try:
        return favorite_place_service.add_favorite_event(
            db, user_id, event_id
        )
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/events/{user_id}")
def get_favorite_events(
    user_id: int,
    db: Session = Depends(get_db)
):
    return favorite_place_service.get_favorite_events(db, user_id)


@router.delete("/events/{user_id}/{event_id}")
def remove_favorite_event(
    user_id: int,
    event_id: int,
    db: Session = Depends(get_db)
):
    success = favorite_place_service.remove_favorite_event(
        db, user_id, event_id
    )

    if not success:
        raise HTTPException(status_code=404, detail="Favorite event not found")

    return {"message": "Favorite event deleted"}
