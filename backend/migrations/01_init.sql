-- Инициализация схемы базы данных StudMap

-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    surname VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255)
);

-- Таблица организаторов
CREATE TABLE IF NOT EXISTS organizers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    surname VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    phone_number VARCHAR(50)
);

-- Таблица мероприятий
CREATE TABLE IF NOT EXISTS events (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255),
    type VARCHAR(100),
    level VARCHAR(100),
    event_date TIMESTAMPTZ,
    status VARCHAR(50),
    location VARCHAR(255),
    description TEXT,
    source VARCHAR(255)
);

-- Таблица избранных мест
CREATE TABLE IF NOT EXISTS favorite_places (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    address VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    category VARCHAR(100),
    osm_type VARCHAR(50),
    osm_id BIGINT
);

-- Таблица черного списка токенов
CREATE TABLE IF NOT EXISTS token_blacklist (
    token VARCHAR(1024) PRIMARY KEY,
    expires_at TIMESTAMP NOT NULL
);

-- Таблица связей пользователей и мест
CREATE TABLE IF NOT EXISTS users_places (
    id_user INTEGER REFERENCES users(id) ON DELETE CASCADE,
    id_place INTEGER REFERENCES favorite_places(id) ON DELETE CASCADE,
    PRIMARY KEY (id_user, id_place)
);

-- Таблица связей пользователей и мероприятий
CREATE TABLE IF NOT EXISTS users_events (
    id_user INTEGER REFERENCES users(id) ON DELETE CASCADE,
    id_event INTEGER REFERENCES events(id) ON DELETE CASCADE,
    PRIMARY KEY (id_user, id_event)
);

-- Таблица связей организаторов и мероприятий
CREATE TABLE IF NOT EXISTS organizers_events (
    id_organizer INTEGER REFERENCES organizers(id) ON DELETE CASCADE,
    id_event INTEGER REFERENCES events(id) ON DELETE CASCADE,
    PRIMARY KEY (id_organizer, id_event)
);

-- Индексы
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_events_date ON events(event_date);
CREATE INDEX IF NOT EXISTS idx_favorite_places_osm ON favorite_places(osm_id, osm_type);
