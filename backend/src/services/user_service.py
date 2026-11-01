from sqlalchemy.orm import Session
from src.repositories import user_repository
from src.services.auth_service import pwd_context

def update_user(db: Session, user_id: int, data):
    user = user_repository.get_user_by_id(db, user_id)
    if not user:
        return None

    update_data = data.dict(
        exclude_unset=True,
        exclude_none=True
    )

    if "email" in update_data:
        existing = user_repository.get_user_by_email(db, update_data["email"])

        if existing and existing.id != user_id:
            raise Exception("Email already exists")

    if "password" in update_data:
        update_data["password"] = pwd_context.hash(update_data["password"])

    return user_repository.update_user(db, user, update_data)

def delete_user(db: Session, user_id: int):
    user = user_repository.get_user_by_id(db, user_id)

    if not user:
        return False

    user_repository.delete_user(db, user)
    return True