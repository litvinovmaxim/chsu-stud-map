from fastapi import APIRouter, HTTPException, Query
from src.services import osm_service


router = APIRouter(tags=["Map"])


@router.get("/map/places")
def get_map_places(
    latitude: float,
    longitude: float,
    radius: int = 1000,
    categories: list[str] = Query(default=[])
):
    try:
        return osm_service.get_places_from_osm(
            latitude,
            longitude,
            radius,
            categories
        )

    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    except Exception as e:
        raise HTTPException(status_code=502, detail=str(e))