from sqlalchemy.orm import Session
from src.repositories import favorite_place_repository

def add_favorite_place(db: Session, user_id: int, place_data):
    return favorite_place_repository.add_favorite_place(
        db, user_id, place_data.model_dump()
    )


def get_favorite_places(db: Session, user_id: int):
    return favorite_place_repository.get_favorite_places(db, user_id)


def remove_favorite_place(db: Session, user_id: int, place_id: int):
    favorite = favorite_place_repository.remove_favorite_place(
        db, user_id, place_id
    )

    if not favorite:
        return None

    return True

