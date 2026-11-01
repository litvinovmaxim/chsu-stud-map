from contextlib import asynccontextmanager
from fastapi import FastAPI
import src.models
from starlette.middleware.cors import CORSMiddleware


from src.routers import (
    user_router,
    auth_router,
    organizer_router,
    event_router,
    place_router,
    favorite_place_router,
)

@asynccontextmanager
async def lifespan(app: FastAPI):
    yield

app = FastAPI(title="StudMap", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.include_router(auth_router.router, prefix="/auth")
app.include_router(user_router.router, prefix="/users")
app.include_router(organizer_router.router, prefix="/organizers")
app.include_router(event_router.router, prefix="/events")
app.include_router(place_router.router, prefix="/places")
app.include_router(favorite_place_router.router, prefix="/favorites")

def create_app():
    return app