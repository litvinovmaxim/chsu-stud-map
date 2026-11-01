from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from src.db.database import get_db
from src.services import user_service
from src.schemas import user_schema
from src.services.auth_service import get_current_user
from src.models.user_model import User

router = APIRouter(tags=["Users"])

@router.put("/update/{user_id}", response_model=user_schema.UserResponse)
def update_user(data: user_schema.UserUpdate, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    try:
        user = user_service.update_user(db, current_user.id, data)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    return user

'''@router.delete("/delete/{user_id}")
def delete_user(user_id: int, db: Session = Depends(get_db)):
    result = user_service.delete_user(db, user_id)

    if not result:
        raise HTTPException(status_code=404, detail="User not found")

    return {"message": "Account successfully deleted"}'''