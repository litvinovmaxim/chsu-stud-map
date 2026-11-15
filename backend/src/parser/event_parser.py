from datetime import datetime
from zoneinfo import ZoneInfo
from urllib.parse import urljoin
import re
from bs4 import BeautifulSoup
from playwright.sync_api import sync_playwright
from sqlalchemy.orm import Session
from src.models.event_model import Event
from src.models.organizer_model import Organizer
from src.models.organizer_event_model import OrganizerEvent


BASE_URL = "https://www.chsu.ru"

CALENDAR_URL = (
    "https://www.chsu.ru/meropriyatiya/kalendar/"
    "?period=month"
)

TIMEZONE = ZoneInfo("Europe/Moscow")


def get_calendar_html(page):
    """Получает HTML календаря с сайта ЧГУ"""

    page.goto(
        CALENDAR_URL,
        wait_until="domcontentloaded",
        timeout=60000
    )

    page.wait_for_selector(
        ".events_calendar",
        timeout=60000
    )

    return page.content()


def get_text(element):
    """Получает текст HTML-элемента"""

    if element:
        return element.get_text(" ", strip=True)

    return None


def get_event_link(record):
    """Получает ссылку на страницу мероприятия"""

    if record.name == "a":
        href = record.get("href")
    else:
        link = record.find("a", href=True)
        href = link.get("href") if link else None

    if href:
        return urljoin(BASE_URL, href)

    return None


def parse_event_date(day, start_time):
    """Объединяет дату и время начала мероприятия"""

    if not day:
        return None

    try:
        if start_time:
            event_datetime = datetime.strptime(
                f"{day.strip()} {start_time.strip()}",
                "%Y-%m-%d %H:%M"
            )
        else:
            event_datetime = datetime.strptime(
                day.strip(),
                "%Y-%m-%d"
            )

        return event_datetime.replace(tzinfo=TIMEZONE)

    except ValueError:
        return None



def parse_calendar(html):
    """Извлекает мероприятия из HTML календаря"""

    soup = BeautifulSoup(html, "html.parser")
    events = []

    calendar = soup.select_one(".events_calendar")

    if not calendar:
        raise ValueError(
            "В HTML не найден контейнер .events_calendar"
        )

    records = calendar.select(".record")

    for record in records:

        # Дата мероприятия
        day_element = record.find_parent(
            class_="day",
            attrs={"data-day": True}
        )

        if not day_element:
            continue

        event_day = day_element.get("data-day")

        # Название и дополнительный заголовок
        info = record.select_one(".info")

        title = get_text(
            info.select_one(".title") if info else None
        )

        subtitle = get_text(
            info.select_one(".sub_title") if info else None
        )

        if subtitle and title:
            title = f"{subtitle} — {title}"
        elif subtitle:
            title = subtitle

        if not title:
            continue

        # Тип мероприятия
        event_type = get_text(
            record.select_one(".P_G_TYPE span")
        )

        # Уровень мероприятия
        event_level = get_text(
            record.select_one(".P_G_LEVEL span")
        )

        # Описание
        description = get_text(
            record.select_one(".description")
        )

        # Время начала
        start_time = get_text(
            record.select_one(".time .start")
        )

        # Дата и время
        event_datetime = parse_event_date(
            event_day,
            start_time
        )

        if not event_datetime:
            continue

        # Ссылка на мероприятие
        source = get_event_link(record)

        event = {
            "title": title,
            "type": event_type,
            "level": event_level,
            "event_date": event_datetime,
            "status": None,
            "location": None,
            "description": description,
            "source": source
        }

        events.append(event)

    return events


def save_events(db: Session, events):
    """Сохраняет мероприятия без повторного добавления"""

    added = 0
    updated = 0

    try:
        for event_data in events:

            source = event_data["source"]

            existing_event = None

            # Сначала ищем по ссылке
            if source:
                existing_event = (
                    db.query(Event)
                    .filter(Event.source == source)
                    .first()
                )

            # Если ссылки нет, ищем по названию и дате
            else:
                existing_event = (
                    db.query(Event)
                    .filter(
                        Event.title == event_data["title"],
                        Event.event_date == event_data["event_date"]
                    )
                    .first()
                )

            if existing_event:

                # Обновляем существующее мероприятие
                for field, value in event_data.items():
                    if field in ("location", "status"):
                        continue

                    setattr(existing_event, field, value)

                updated += 1

            else:

                db.add(Event(**event_data))
                added += 1

        db.commit()

    except Exception:
        db.rollback()
        raise

    return added, updated



def get_person_from_block(soup, heading_text):
    """Извлекает данные организатора"""

    heading = soup.find(
        "h3",
        string=lambda text: (
            text and text.strip() == heading_text
        )
    )

    if not heading:
        return None

    paragraph = heading.find_next_sibling("p")

    if not paragraph:
        return None

    # Получаем ФИО
    full_text = paragraph.get_text(" ", strip=True)

    full_name = re.split(
        r"Email\s*:|Телефон\s*:",
        full_text,
        maxsplit=1,
        flags=re.IGNORECASE
    )[0].strip()

    if not full_name:
        return None

    name_parts = full_name.split()

    if len(name_parts) < 2:
        return None

    # В ФИО сначала фамилия, затем имя и отчество
    surname = name_parts[0]
    name = " ".join(name_parts[1:])

    # Email
    email_element = paragraph.select_one(
        'a[href^="mailto:"]'
    )

    email = None

    if email_element:
        email = (
            email_element.get("href")
            .replace("mailto:", "")
            .strip()
        )

    # Телефон
    phone_element = paragraph.select_one(
        'a[href^="tel:"]'
    )

    phone = None

    if phone_element:
        phone = phone_element.get_text(strip=True)

    return {
        "name": name,
        "surname": surname,
        "email": email,
        "phone_number": phone
    }


def get_responsible_person(soup):
    """
    Сначала ищет организатора.
    Если его нет — ищет контактное лицо.
    """

    person = get_person_from_block(
        soup,
        "Организатор"
    )

    if person:
        # Телефон контактного лица не приписываем организатору
        person["phone_number"] = None
        return person

    # Если организатора нет, берём контактное лицо
    person = get_person_from_block(
        soup,
        "Контактное лицо"
    )

    return person


def get_event_details(page, event_url):
    """
    Открывает страницу мероприятия
    """

    if not event_url:
        return None, None, None

    try:
        page.goto(
            event_url,
            wait_until="domcontentloaded",
            timeout=60000
        )

        soup = BeautifulSoup(
            page.content(),
            "html.parser"
        )

        # Место проведения
        location_element = soup.select_one(
            ".where .place_name"
        )

        location = get_text(location_element)

        # Организатор или контактное лицо
        person = get_responsible_person(soup)

        # Статус мероприятия
        status_element = soup.select_one(".status")

        if status_element:
            status_text = get_text(status_element)

            status_classes = status_element.get("class", [])

            if (
                "S_ENDED" in status_classes
                or (
                    status_text
                    and "Мероприятие окончено" in status_text
                )
            ):
                status = "Окончено"
            else:
                status = "Активно"

        else:
            status = "Активно"

        return location, person, status

    except Exception as error:

        print(
            f"Ошибка при обработке страницы {event_url}: {error}"
        )

        return None, None, None


def save_organizer(db: Session, person, event_id):
    """Сохраняет организатора и связывает его с мероприятием"""

    if not person:
        return

    organizer = None

    email = person["email"] or None

    # Ищем по email
    if email:
        organizer = (
            db.query(Organizer)
            .filter(Organizer.email == email)
            .first()
        )

    # Если не нашли, ищем по ФИО
    if not organizer:

        organizer = (
            db.query(Organizer)
            .filter(
                Organizer.surname == person["surname"],
                Organizer.name == person["name"]
            )
            .first()
        )

    # Если человека нет в БД, создаём
    if not organizer:

        organizer = Organizer(
            name=person["name"],
            surname=person["surname"],
            email=email,
            phone_number=person["phone_number"]
        )

        db.add(organizer)
        db.flush()

    # Проверяем, существует ли связь
    existing_link = (
        db.query(OrganizerEvent)
        .filter(
            OrganizerEvent.id_organizer == organizer.id,
            OrganizerEvent.id_event == event_id
        )
        .first()
    )

    if not existing_link:

        db.add(
            OrganizerEvent(
                id_organizer=organizer.id,
                id_event=event_id
            )
        )



def parse_and_save(db: Session):
    """Получает календарь, сохраняет мероприятия и их подробности"""

    print("Получаем календарь с сайта ЧГУ...")

    with sync_playwright() as p:

        browser = p.chromium.launch(headless=True)

        try:
            page = browser.new_page()

            # Получаем календарь
            html = get_calendar_html(page)

            print("Извлекаем мероприятия...")

            events = parse_calendar(html)

            print("Сохраняем мероприятия в базу данных...")

            added, updated = save_events(db, events)

            print(f"Найдено мероприятий: {len(events)}")
            print(f"Добавлено в БД: {added}")
            print(f"Обновлено в БД: {updated}")

            # Переходим на страницы мероприятий
            print("\nПолучаем подробности мероприятий...")

            details_updated = 0
            organizers_saved = 0

            for event_data in events:

                source = event_data["source"]

                if not source:
                    continue

                # Находим мероприятие в БД
                event = (
                    db.query(Event)
                    .filter(Event.source == source)
                    .first()
                )

                if not event:
                    continue

                # Получаем место, ответственного и статус
                location, person, status = get_event_details(
                    page,
                    source
                )

                # Обновляем место проведения
                if location:
                    event.location = location

                # Обновляем статус
                if status:
                    event.status = status

                # Сохраняем организатора или контактное лицо
                if person:
                    save_organizer(
                        db,
                        person,
                        event.id
                    )

                    organizers_saved += 1

                details_updated += 1

                print(
                    f"Обработано: {event.title} | "
                    f"Статус: {event.status} | "
                    f"Место: {event.location}"
                )

            # Сохраняем все подробности
            db.commit()

            print("\nПарсинг завершён.")
            print(f"Обработано страниц: {details_updated}")
            print(f"Найдено ответственных: {organizers_saved}")

            return events

        except Exception:

            db.rollback()
            raise

        finally:

            browser.close()
