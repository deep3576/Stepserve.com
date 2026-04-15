from pathlib import Path
from typing import Any

from fastapi import APIRouter, Depends, File, HTTPException, Query, UploadFile, status
import pymysql

from app.api.deps import require_role
from app.core.enums import BookingStatus, PaymentStatus, UserRole
from app.db.session import get_connection
from app.schemas.marketplace import (
    BookingCreate,
    BookingResponse,
    CategoryCreate,
    PaymentCreate,
    PaymentResponse,
    ProviderProfileUpsert,
    ReviewCreate,
    ServiceCreate,
)

router = APIRouter(tags=["stepserve"])
UPLOAD_DIR = Path("uploads")
UPLOAD_DIR.mkdir(exist_ok=True)


@router.post("/categories", dependencies=[Depends(require_role(UserRole.admin))])
def create_category(
    payload: CategoryCreate,
    conn: pymysql.connections.Connection = Depends(get_connection),
):
    with conn.cursor() as cur:
        cur.execute(
            "INSERT INTO categories (name, slug) VALUES (%s, %s)",
            (payload.name, payload.slug),
        )
        category_id = cur.lastrowid
    conn.commit()
    return {"id": category_id, "name": payload.name, "slug": payload.slug}




@router.get("/categories")
def list_categories(
    conn: pymysql.connections.Connection = Depends(get_connection),
):
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT c.id, c.name, c.slug, COUNT(s.id) AS services_count
            FROM categories c
            LEFT JOIN services s ON s.category_id = c.id AND s.is_active = 1
            GROUP BY c.id, c.name, c.slug
            ORDER BY services_count DESC, c.name ASC
            """
        )
        return cur.fetchall()


@router.get("/stepserve/home")
@router.get("/market/home")
def stepserve_home(
    conn: pymysql.connections.Connection = Depends(get_connection),
):
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT c.id, c.name, c.slug, COUNT(s.id) AS services_count
            FROM categories c
            LEFT JOIN services s ON s.category_id = c.id AND s.is_active = 1
            GROUP BY c.id, c.name, c.slug
            ORDER BY services_count DESC, c.name ASC
            LIMIT 12
            """
        )
        categories = cur.fetchall()

        cur.execute(
            """
            SELECT s.id, s.title, s.description, s.price, s.category_id, p.full_name AS provider_name, p.location
            FROM services s
            JOIN provider_profiles p ON p.id = s.provider_id
            WHERE s.is_active = 1
            ORDER BY s.price DESC, s.id DESC
            LIMIT 8
            """
        )
        featured = cur.fetchall()

        cur.execute(
            """
            SELECT s.id, s.title, s.description, s.price, s.category_id, p.full_name AS provider_name, p.location
            FROM services s
            JOIN provider_profiles p ON p.id = s.provider_id
            WHERE s.is_active = 1
            ORDER BY s.id DESC
            LIMIT 24
            """
        )
        latest = cur.fetchall()

        cur.execute(
            """
            SELECT p.location, COUNT(s.id) AS listings_count
            FROM services s
            JOIN provider_profiles p ON p.id = s.provider_id
            WHERE s.is_active = 1 AND p.location IS NOT NULL AND p.location <> ''
            GROUP BY p.location
            ORDER BY listings_count DESC, p.location ASC
            LIMIT 10
            """
        )
        top_locations = cur.fetchall()

    return {
        "categories": categories,
        "featured": featured,
        "latest": latest,
        "top_locations": top_locations,
    }


@router.get("/search/services")
def search_services(
    conn: pymysql.connections.Connection = Depends(get_connection),
    query: str | None = Query(default=None),
    category_id: int | None = Query(default=None),
    location: str | None = Query(default=None),
    min_price: float | None = Query(default=None),
    max_price: float | None = Query(default=None),
):
    conditions = ["s.is_active = 1"]
    params: list[Any] = []

    if query:
        conditions.append("(s.title LIKE %s OR s.description LIKE %s)")
        params.extend([f"%{query}%", f"%{query}%"])
    if category_id:
        conditions.append("s.category_id = %s")
        params.append(category_id)
    if location:
        conditions.append("p.location LIKE %s")
        params.append(f"%{location}%")
    if min_price is not None:
        conditions.append("s.price >= %s")
        params.append(min_price)
    if max_price is not None:
        conditions.append("s.price <= %s")
        params.append(max_price)

    sql = f"""
        SELECT s.id, s.title, s.description, s.price, s.category_id, p.full_name AS provider_name, p.location
        FROM services s
        JOIN provider_profiles p ON s.provider_id = p.id
        WHERE {' AND '.join(conditions)}
        ORDER BY s.id DESC
        LIMIT 100
    """
    with conn.cursor() as cur:
        cur.execute(sql, tuple(params))
        return cur.fetchall()


@router.post("/providers/profile")
def upsert_provider_profile(
    payload: ProviderProfileUpsert,
    conn: pymysql.connections.Connection = Depends(get_connection),
    user: dict[str, Any] = Depends(require_role(UserRole.provider)),
):
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM provider_profiles WHERE user_id = %s LIMIT 1", (user["id"],))
        existing = cur.fetchone()
        if existing:
            cur.execute(
                """
                UPDATE provider_profiles
                SET full_name=%s, bio=%s, location=%s, hourly_rate=%s
                WHERE user_id=%s
                """,
                (payload.full_name, payload.bio, payload.location, payload.hourly_rate, user["id"]),
            )
            profile_id = existing["id"]
        else:
            cur.execute(
                """
                INSERT INTO provider_profiles (user_id, full_name, bio, location, hourly_rate)
                VALUES (%s, %s, %s, %s, %s)
                """,
                (user["id"], payload.full_name, payload.bio, payload.location, payload.hourly_rate),
            )
            profile_id = cur.lastrowid
    conn.commit()
    return {"id": profile_id, "user_id": user["id"], **payload.model_dump()}


@router.post("/providers/uploads")
def upload_provider_asset(
    file: UploadFile = File(...),
    conn: pymysql.connections.Connection = Depends(get_connection),
    user: dict[str, Any] = Depends(require_role(UserRole.provider)),
):
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM provider_profiles WHERE user_id = %s LIMIT 1", (user["id"],))
        profile = cur.fetchone()
        if not profile:
            raise HTTPException(status_code=400, detail="Provider profile required")

    safe_name = f"provider_{user['id']}_{file.filename}"
    filepath = UPLOAD_DIR / safe_name
    content = file.file.read()
    filepath.write_bytes(content)

    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO provider_uploads (provider_id, file_name, file_path, content_type, file_size)
            VALUES (%s, %s, %s, %s, %s)
            """,
            (profile["id"], file.filename, str(filepath), file.content_type, len(content)),
        )
        upload_id = cur.lastrowid
    conn.commit()
    return {"id": upload_id, "file_name": file.filename, "stored_path": str(filepath)}


@router.get("/providers/uploads")
def list_provider_uploads(
    conn: pymysql.connections.Connection = Depends(get_connection),
    user: dict[str, Any] = Depends(require_role(UserRole.provider)),
):
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM provider_profiles WHERE user_id=%s LIMIT 1", (user["id"],))
        profile = cur.fetchone()
        if not profile:
            return []
        cur.execute(
            "SELECT id, file_name, file_path, content_type, file_size, created_at FROM provider_uploads WHERE provider_id=%s ORDER BY id DESC",
            (profile["id"],),
        )
        return cur.fetchall()


@router.post("/services")
def create_service(
    payload: ServiceCreate,
    conn: pymysql.connections.Connection = Depends(get_connection),
    user: dict[str, Any] = Depends(require_role(UserRole.provider)),
):
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM provider_profiles WHERE user_id = %s LIMIT 1", (user["id"],))
        profile = cur.fetchone()
        if not profile:
            raise HTTPException(status_code=400, detail="Provider profile required")

        cur.execute(
            """
            INSERT INTO services (provider_id, category_id, title, description, price, is_active)
            VALUES (%s, %s, %s, %s, %s, %s)
            """,
            (profile["id"], payload.category_id, payload.title, payload.description, payload.price, 1),
        )
        service_id = cur.lastrowid
    conn.commit()
    return {"id": service_id, "provider_id": profile["id"], **payload.model_dump(), "is_active": True}


@router.get("/services")
def list_services(
    conn: pymysql.connections.Connection = Depends(get_connection),
    active_only: bool = True,
):
    with conn.cursor() as cur:
        if active_only:
            cur.execute("SELECT * FROM services WHERE is_active = 1 ORDER BY id DESC")
        else:
            cur.execute("SELECT * FROM services ORDER BY id DESC")
        return cur.fetchall()


@router.get("/customer/bookings")
def customer_bookings(
    conn: pymysql.connections.Connection = Depends(get_connection),
    user: dict[str, Any] = Depends(require_role(UserRole.customer)),
):
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT b.id, b.start_time, b.end_time, b.status, b.total_price, s.title AS service_title
            FROM bookings b
            JOIN services s ON s.id = b.service_id
            WHERE b.customer_id = %s
            ORDER BY b.id DESC
            """,
            (user["id"],),
        )
        return cur.fetchall()


@router.get("/provider/dashboard")
def provider_dashboard(
    conn: pymysql.connections.Connection = Depends(get_connection),
    user: dict[str, Any] = Depends(require_role(UserRole.provider)),
):
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM provider_profiles WHERE user_id=%s LIMIT 1", (user["id"],))
        profile = cur.fetchone()
        if not profile:
            return {"services": [], "bookings": [], "uploads": []}

        cur.execute("SELECT * FROM services WHERE provider_id=%s ORDER BY id DESC", (profile["id"],))
        services = cur.fetchall()

        cur.execute(
            """
            SELECT b.id, b.status, b.start_time, b.end_time, b.total_price
            FROM bookings b
            JOIN services s ON s.id=b.service_id
            WHERE s.provider_id=%s
            ORDER BY b.id DESC
            """,
            (profile["id"],),
        )
        bookings = cur.fetchall()

        cur.execute("SELECT * FROM provider_uploads WHERE provider_id=%s ORDER BY id DESC", (profile["id"],))
        uploads = cur.fetchall()

    return {"services": services, "bookings": bookings, "uploads": uploads}


@router.post("/bookings", response_model=BookingResponse)
def create_booking(
    payload: BookingCreate,
    conn: pymysql.connections.Connection = Depends(get_connection),
    user: dict[str, Any] = Depends(require_role(UserRole.customer)),
):
    if payload.end_time <= payload.start_time:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid booking window")

    with conn.cursor() as cur:
        cur.execute("SELECT id, price FROM services WHERE id=%s AND is_active=1 LIMIT 1", (payload.service_id,))
        service = cur.fetchone()
        if not service:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Service not found")

        duration_hours = max((payload.end_time - payload.start_time).total_seconds() / 3600, 1)
        total_price = round(float(service["price"]) * duration_hours, 2)

        cur.execute(
            """
            INSERT INTO bookings (service_id, customer_id, start_time, end_time, status, total_price)
            VALUES (%s, %s, %s, %s, %s, %s)
            """,
            (
                payload.service_id,
                user["id"],
                payload.start_time,
                payload.end_time,
                BookingStatus.pending.value,
                total_price,
            ),
        )
        booking_id = cur.lastrowid
    conn.commit()
    return BookingResponse(id=booking_id, status=BookingStatus.pending, total_price=total_price)


@router.post("/payments", response_model=PaymentResponse)
def create_payment(
    payload: PaymentCreate,
    conn: pymysql.connections.Connection = Depends(get_connection),
    user: dict[str, Any] = Depends(require_role(UserRole.customer)),
):
    with conn.cursor() as cur:
        cur.execute(
            "SELECT id, total_price FROM bookings WHERE id=%s AND customer_id=%s LIMIT 1",
            (payload.booking_id, user["id"]),
        )
        booking = cur.fetchone()
        if not booking:
            raise HTTPException(status_code=404, detail="Booking not found")

        cur.execute("SELECT id FROM payments WHERE booking_id=%s LIMIT 1", (payload.booking_id,))
        payment = cur.fetchone()
        if payment:
            cur.execute(
                """
                UPDATE payments SET status=%s, stripe_payment_intent_id=%s WHERE id=%s
                """,
                (PaymentStatus.paid.value, f"pi_demo_{payload.booking_id}", payment["id"]),
            )
            payment_id = payment["id"]
        else:
            cur.execute(
                """
                INSERT INTO payments (booking_id, stripe_payment_intent_id, amount, currency, status)
                VALUES (%s, %s, %s, %s, %s)
                """,
                (
                    payload.booking_id,
                    f"pi_demo_{payload.booking_id}",
                    booking["total_price"],
                    "CAD",
                    PaymentStatus.paid.value,
                ),
            )
            payment_id = cur.lastrowid

        cur.execute(
            "UPDATE bookings SET status=%s WHERE id=%s",
            (BookingStatus.confirmed.value, payload.booking_id),
        )
    conn.commit()
    return PaymentResponse(
        id=payment_id,
        status=PaymentStatus.paid,
        amount=float(booking["total_price"]),
        stripe_payment_intent_id=f"pi_demo_{payload.booking_id}",
    )


@router.post("/reviews")
def create_review(
    payload: ReviewCreate,
    conn: pymysql.connections.Connection = Depends(get_connection),
    user: dict[str, Any] = Depends(require_role(UserRole.customer)),
):
    with conn.cursor() as cur:
        cur.execute(
            "SELECT id, status FROM bookings WHERE id=%s AND customer_id=%s LIMIT 1",
            (payload.booking_id, user["id"]),
        )
        booking = cur.fetchone()
        if not booking or booking["status"] != BookingStatus.completed.value:
            raise HTTPException(status_code=400, detail="Booking not eligible for review")

        cur.execute(
            "INSERT INTO reviews (booking_id, rating, comment) VALUES (%s, %s, %s)",
            (payload.booking_id, payload.rating, payload.comment),
        )
        review_id = cur.lastrowid
    conn.commit()
    return {"id": review_id, **payload.model_dump()}


@router.get("/admin/overview")
def admin_overview(
    conn: pymysql.connections.Connection = Depends(get_connection),
    _: dict[str, Any] = Depends(require_role(UserRole.admin)),
):
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) AS count FROM users")
        users_count = cur.fetchone()["count"]
        cur.execute("SELECT COUNT(*) AS count FROM services")
        services_count = cur.fetchone()["count"]
        cur.execute("SELECT COUNT(*) AS count FROM bookings")
        bookings_count = cur.fetchone()["count"]
        cur.execute("SELECT COUNT(*) AS total FROM payments WHERE status='paid'")
        paid_total = float(cur.fetchone()["total"] or 0)

    return {
        "users_count": users_count,
        "services_count": services_count,
        "bookings_count": bookings_count,
        "paid_total": paid_total,
    }


@router.get("/admin/users")
def admin_users(
    conn: pymysql.connections.Connection = Depends(get_connection),
    _: dict[str, Any] = Depends(require_role(UserRole.admin)),
):
    with conn.cursor() as cur:
        cur.execute(
            "SELECT id, email, role, is_active, created_at FROM users ORDER BY id DESC LIMIT 500"
        )
        return cur.fetchall()


@router.patch("/admin/users/{user_id}/status")
def admin_update_user_status(
    user_id: int,
    active: bool,
    conn: pymysql.connections.Connection = Depends(get_connection),
    _: dict[str, Any] = Depends(require_role(UserRole.admin)),
):
    with conn.cursor() as cur:
        cur.execute("UPDATE users SET is_active=%s WHERE id=%s", (1 if active else 0, user_id))
    conn.commit()
    return {"user_id": user_id, "is_active": active}


@router.get("/admin/bookings")
def admin_bookings(
    conn: pymysql.connections.Connection = Depends(get_connection),
    _: dict[str, Any] = Depends(require_role(UserRole.admin)),
):
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT b.id, b.status, b.total_price, b.start_time, b.end_time, u.email AS customer_email
            FROM bookings b
            JOIN users u ON b.customer_id = u.id
            ORDER BY b.id DESC
            LIMIT 500
            """
        )
        return cur.fetchall()
