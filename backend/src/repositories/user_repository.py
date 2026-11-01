from sqlalchemy.orm import Session
from src.models.user_model import User

def create_user(db: Session, user_data: dict):
    user = User(**user_data)
    db.add(user)
    db.commit()
    db.refresh(user)
    return user

def get_user_by_id(db: Session, user_id: int):
    return db.query(User).filter(User.id == user_id).first()

def get_user_by_email(db: Session, email: str):
    return db.query(User).filter(User.email == email).first()

def update_user(db: Session, user: User, data: dict):
    for key, value in data.items():
        setattr(user, key, value)

    db.commit()
    db.refresh(user)
    return user

'''def delete_user(db: Session, user: User):
    db.delete(user)
    db.commit()'''