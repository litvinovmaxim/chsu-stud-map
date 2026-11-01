
from sqlalchemy.orm import Session
from src.models.user_model import User
from src.models.token_blacklist_model import TokenBlacklist
from datetime import datetime, timedelta
from fastapi import HTTPException
from passlib.context import CryptContext
import jwt
import os
import secrets

# Настройки JWT
SECRET_KEY = secrets.token_hex(32)

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

        # Проверяем, не находится ли токен в чёрном списке
        if db is not None:
            blacklisted_token = db.query(TokenBlacklist).filter(
                TokenBlacklist.token == token
            ).first()

            if blacklisted_token:
                raise HTTPException(
                    status_code=401,
                    detail="Token has been revoked"
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


# Добавление токена в чёрный список при выходе
def revoke_token(token: str, db: Session):
    try:
        decoded = jwt.decode(
            token,
            SECRET_KEY,
            algorithms=[ALGORITHM]
        )

        expires_at = datetime.utcfromtimestamp(decoded["exp"])

    except jwt.PyJWTError:
        raise HTTPException(
            status_code=401,
            detail="Invalid token"
        )

    existing_token = db.query(TokenBlacklist).filter(
        TokenBlacklist.token == token
    ).first()

    if existing_token:
        return existing_token

    blacklist_entry = TokenBlacklist(
        token=token,
        expires_at=expires_at
    )

    db.add(blacklist_entry)
    db.commit()
    db.refresh(blacklist_entry)

    return blacklist_entry


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
