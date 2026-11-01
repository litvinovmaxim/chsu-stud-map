from fastapi import APIRouter, Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session

from src.db.database import get_db
from src.services import auth_service
from src.models import User
from src.services.auth_service import create_access_token
from src.services.auth_service import revoke_token

security = HTTPBearer()

router = APIRouter(tags=["Auth"])


@router.post("/register")
def register(
    name: str,
    surname: str,
    email: str,
    password: str,
    db: Session = Depends(get_db)
):
    auth_service.register_user(name, surname, email, password, db)
    return {"status_code": 201, "message": "User registered"}


@router.post("/login")
def login(email: str, password: str, db: Session = Depends(get_db)):
    user = auth_service.login_user(email, password, db)

    access_token = create_access_token({
        "sub": str(user.id)
    })

    return {
        "access_token": access_token,
        "token_type": "bearer"
    }


@router.post("/logout")
def logout(
    db: Session = Depends(get_db),
    credentials: HTTPAuthorizationCredentials = Depends(security)
):
    token = credentials.credentials
    user = auth_service.verify_token(token)

    return {
        "status_code": 200,
        "message": revoke_token(token, db)
    }