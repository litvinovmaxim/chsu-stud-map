from sqlalchemy.orm import Session, selectinload
from sqlalchemy import or_
from src.models.event_model import Event
from src.models.user_event_model import UserEvent
from src.models.organizer_event_model import OrganizerEvent


def event_to_dict(event, is_favorite=False):
    """Преобразует мероприятие в данные для ответа API"""

    organizers = []

    for link in event.organizers:
        organizer = link.organizer

        if organizer:
            organizers.append({
                "id": organizer.id,
                "name": organizer.name,
                "surname": organizer.surname,
                "email": organizer.email,
                "phone_number": organizer.phone_number
            })

    return {
        "id": event.id,
        "title": event.title,
        "type": event.type,
        "level": event.level,
        "event_date": event.event_date,
        "status": event.status,
        "location": event.location,
        "description": event.description,
        "source": event.source,
        "organizers": organizers,
        "is_favorite": is_favorite
    }


def get_events(
    db: Session,
    user_id: int,
    search: str = None,
    event_type: str = None,
    level: str = None,
    status: str = None
):
    """Получает список мероприятий с необязательными фильтрами"""

    query = db.query(Event).options(
        selectinload(Event.organizers).joinedload(
            OrganizerEvent.organizer
        )
    )

    if search:
        search_filter = f"%{search}%"

        query = query.filter(
            or_(
                Event.title.ilike(search_filter),
                Event.description.ilike(search_filter),
                Event.location.ilike(search_filter)
            )
        )

    if event_type:
        query = query.filter(Event.type == event_type)

    if level:
        query = query.filter(Event.level == level)

    if status:
        query = query.filter(Event.status == status)

    events = query.order_by(Event.event_date.asc()).all()

    event_ids = [event.id for event in events]

    favorite_ids = set()

    if event_ids:
        favorite_ids = {
            row[0]
            for row in db.query(UserEvent.id_event)
            .filter(
                UserEvent.id_user == user_id,
                UserEvent.id_event.in_(event_ids)
            )
            .all()
        }

    return [
        event_to_dict(
            event,
            is_favorite=event.id in favorite_ids
        )
        for event in events
    ]


def get_event_by_id(db: Session, event_id: int, user_id: int):
    """Получает подробную информацию об одном мероприятии"""

    event = (
        db.query(Event)
        .options(
            selectinload(Event.organizers).joinedload(
                OrganizerEvent.organizer
            )
        )
        .filter(Event.id == event_id)
        .first()
    )

    if not event:
        return None

    is_favorite = (
        db.query(UserEvent)
        .filter(
            UserEvent.id_user == user_id,
            UserEvent.id_event == event_id
        )
        .first()
        is not None
    )

    return event_to_dict(event, is_favorite)


def get_favorite_events(db: Session, user_id: int):
    """Получает избранные мероприятия пользователя"""

    events = (
        db.query(Event)
        .join(UserEvent, UserEvent.id_event == Event.id)
        .options(
            selectinload(Event.organizers).joinedload(
                OrganizerEvent.organizer
            )
        )
        .filter(UserEvent.id_user == user_id)
        .order_by(Event.event_date.asc())
        .all()
    )

    return [
        event_to_dict(event, is_favorite=True)
        for event in events
    ]


def add_favorite_event(db: Session, user_id: int, event_id: int):
    """Добавляет мероприятие в избранное"""

    event = db.query(Event).filter(Event.id == event_id).first()

    if not event:
        return None

    favorite = (
        db.query(UserEvent)
        .filter(
            UserEvent.id_user == user_id,
            UserEvent.id_event == event_id
        )
        .first()
    )

    if not favorite:
        favorite = UserEvent(
            id_user=user_id,
            id_event=event_id
        )

        db.add(favorite)
        db.commit()

    return True


def remove_favorite_event(db: Session, user_id: int, event_id: int):
    """Удаляет мероприятие из избранного"""

    favorite = (
        db.query(UserEvent)
        .filter(
            UserEvent.id_user == user_id,
            UserEvent.id_event == event_id
        )
        .first()
    )

    if favorite:
        db.delete(favorite)
        db.commit()

    return True
