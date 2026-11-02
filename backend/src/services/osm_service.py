
import httpx

OVERPASS_URL = "https://overpass-api.de/api/interpreter"

CATEGORY_FILTERS = {
    "dormitories": [
        '"building"="dormitory"',
        '"amenity"="dormitory"'
    ],
    "buildings": [
        '"building"="university"',
        '"building"="college"',
        '"building"="school"',
        '"amenity"="university"',
        '"amenity"="college"'
    ],
    "libraries": [
        '"amenity"="library"'
    ],
    "shops": [
        '"shop"'
    ],
    "cafes": [
        '"amenity"="cafe"'
    ],
    "pharmacies": [
        '"amenity"="pharmacy"'
    ],
    "sports": [
        '"leisure"="sports_centre"',
        '"leisure"="stadium"'
    ]
}

# Названия категорий, которые может отправлять Android
CATEGORY_ALIASES = {
    "university": "buildings",
    "университет": "buildings",
    "учебные корпуса": "buildings",
    "университетские здания": "buildings"
}


def get_places_from_osm(
    latitude: float,
    longitude: float,
    radius: int = 1000,
    categories: list[str] = None
):
    if not -90 <= latitude <= 90:
        raise ValueError("Invalid latitude")

    if not -180 <= longitude <= 180:
        raise ValueError("Invalid longitude")

    if radius <= 0:
        raise ValueError("Radius must be positive")

    # Нормализуем категории
    normalized_categories = []

    if categories:
        for category in categories:
            category = category.strip().lower()
            category = CATEGORY_ALIASES.get(category, category)

            if category not in CATEGORY_FILTERS:
                raise ValueError(f"Unknown category: {category}")

            if category not in normalized_categories:
                normalized_categories.append(category)

    selectors = []

    # Если категории выбраны — ищем только их
    if normalized_categories:
        for category in normalized_categories:
            for tag in CATEGORY_FILTERS[category]:
                selectors.append(
                    f'nwr(around:{radius},{latitude},{longitude})[{tag}];'
                )

    # Если категории не выбраны — ищем все объекты
    else:
        raise ValueError("Выберите категорию для поиска")

    query = f"""
    [out:json];
    (
        {"".join(selectors)}
    );
    out center tags;
    """

    response = httpx.post(
        OVERPASS_URL,
        data={"data": query},
        headers={
            "User-Agent": "StudMap/1.0",
            "Accept": "application/json"
        },
        timeout=60
    )

    response.raise_for_status()

    elements = response.json().get("elements", [])

    places = []
    seen = set()

    for element in elements:
        tags = element.get("tags", {})

        coordinates = element.get("center", element)

        lat = coordinates.get("lat")
        lon = coordinates.get("lon")

        if lat is None or lon is None:
            continue

        osm_key = (element["type"], element["id"])

        if osm_key in seen:
            continue

        # Определяем категорию объекта
        category = get_category(tags, normalized_categories)

        # Если фильтры выбраны, пропускаем неподходящие объекты
        if normalized_categories and not category:
            continue

        seen.add(osm_key)

        address = tags.get("addr:full")

        if not address:
            street = tags.get("addr:street", "")
            house = tags.get("addr:housenumber", "")
            address = f"{street} {house}".strip() or None

        places.append({
            "name": tags.get("name", "Без названия"),
            "address": address,
            "latitude": lat,
            "longitude": lon,
            "category": category,
            "osm_type": element["type"],
            "osm_id": element["id"]
        })

    return places


def get_category(tags: dict, selected_categories: list[str]):
    for category in selected_categories:
        filters = CATEGORY_FILTERS[category]

        for tag in filters:
            key_value = tag.strip('"').split("=")

            if len(key_value) == 2:
                key, value = key_value

                if tags.get(key) == value:
                    return category

            elif len(key_value) == 1:
                if key_value[0] in tags:
                    return category

    return None
