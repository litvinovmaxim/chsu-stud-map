from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session
from src.models.user_model import User
from src.db.database import get_db
from src.repositories import user_repository
#from src.models.token_blacklist_model import TokenBlacklist
from datetime import datetime, timedelta
from fastapi import HTTPException, Depends
from passlib.context import CryptContext
import jwt
import os
import secrets

security = HTTPBearer()
# Настройки JWT
SECRET_KEY = "studmap_my_secret_key_2026_very_long"

ALGORITHM = "HS256"
EXP_TIME = timedelta(minutes=30)

# Настройки хеширования паролей
pwd_context = CryptContext(
    schemes=["pbkdf2_sha256"],
    deprecated="auto"
)


# Создание JWT-токена
def create_access_token(data: dict):
    payload = data.copy()

    payload.update({
        "exp": datetime.utcnow() + EXP_TIME
    })

    return jwt.encode(
        payload,
        SECRET_KEY,
        algorithm=ALGORITHM
    )


# Проверка JWT-токена
def verify_token(token: str, db: Session = None):
    try:
        decoded = jwt.decode(
            token,
            SECRET_KEY,
            algorithms=[ALGORITHM]
        )
        return [decoded, token]

    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=401,
            detail="Signature expired. Please log in again."
        )

    except jwt.PyJWTError:
        raise HTTPException(
            status_code=401,
            detail="Could not validate credentials"
        )


# Регистрация пользователя
def register_user(
    name: str,
    surname: str,
    email: str,
    password: str,
    db: Session
):
    existing_user = db.query(User).filter(
        User.email == email
    ).first()

    if existing_user:
        raise HTTPException(
            status_code=400,
            detail="Email already exists"
        )

    hashed_password = pwd_context.hash(password)

    user = User(
        name=name,
        surname=surname,
        email=email,
        password=hashed_password
    )

    db.add(user)
    db.commit()
    db.refresh(user)

    return user


# Авторизация пользователя
def login_user(email: str, password: str, db: Session):
    user = db.query(User).filter(
        User.email == email
    ).first()

    if not user:
        raise HTTPException(
            status_code=401,
            detail="Invalid email or password"
        )

    if not pwd_context.verify(password, user.password):
        raise HTTPException(
            status_code=401,
            detail="Invalid email or password"
        )

    return user

# текущий пользователь
def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: Session = Depends(get_db)
):
    token = credentials.credentials

    try:
        decoded, _ = verify_token(token)
        user_id = int(decoded["sub"])

    except HTTPException:
        raise

    except (ValueError, KeyError, TypeError):
        raise HTTPException(
            status_code=401,
            detail="Invalid token"
        )

    user = user_repository.get_user_by_id(db, user_id)

    if not user:
        raise HTTPException(
            status_code=401,
            detail="User not found"
        )

    return user