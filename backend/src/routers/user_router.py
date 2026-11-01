from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from src.db.database import get_db
from src.services import user_service
from src.schemas import user_schema

router = APIRouter(tags=["Users"])

@router.put("/update/{user_id}", response_model=user_schema.UserResponse)
def update_user(user_id: int, data: user_schema.UserUpdate, db: Session = Depends(get_db)):
    try:
        user = user_service.update_user(db, user_id, data)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    return user