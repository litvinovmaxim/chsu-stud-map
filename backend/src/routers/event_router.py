from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from src.db.database import get_db
from src.models.user_model import User
from src.schemas.event_schema import EventResponse
from src.services.auth_service import get_current_user
from src.services import event_service

router = APIRouter(tags=["Events"])

# Избранные мероприятия текущего пользователя
@router.get("/favorites/my", response_model=list[EventResponse])
def get_my_favorite_events(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return event_service.get_favorite_events(
        db,
        current_user.id
    )


# Поиск мероприятий
@router.get("/search", response_model=list[EventResponse])
def search_events(
    search: str | None = Query(default=None),
    status: str | None = Query(default=None),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return event_service.get_events(
        db=db,
        user_id=current_user.id,
        search=search,
        status=status
    )


# Получить все мероприятия
@router.get("/", response_model=list[EventResponse])
def get_events(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return event_service.get_events(
        db=db,
        user_id=current_user.id,
        search=None,
        event_type=None,
        level=None,
        status=None
    )


# Подробная информация о мероприятии
@router.get("/{event_id}", response_model=EventResponse)
def get_event(
    event_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    event = event_service.get_event_by_id(
        db,
        event_id,
        current_user.id
    )

    if not event:
        raise HTTPException(
            status_code=404,
            detail="Event not found"
        )

    return event


# Добавить мероприятие в избранное
@router.post("/{event_id}/favorite")
def add_event_to_favorites(
    event_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    result = event_service.add_favorite_event(
        db,
        current_user.id,
        event_id
    )

    if not result:
        raise HTTPException(
            status_code=404,
            detail="Event not found"
        )

    return {"is_favorite": True}


# Удалить мероприятие из избранного
@router.delete("/{event_id}/favorite")
def remove_event_from_favorites(
    event_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    event_service.remove_favorite_event(
        db,
        current_user.id,
        event_id
    )

    return {"is_favorite": False}
