from datetime import datetime
from pathlib import Path
from typing import Any

from fastapi import APIRouter, Depends, File, HTTPException, Query, Request, UploadFile, status
import pymysql

from app.api.deps import require_role
from app.core.email import (
    booking_confirmation_html,
    listing_invoice_html,
    new_booking_admin_html,
    payment_receipt_html,
    send_email,
)
from app.core.enums import BookingStatus, PaymentStatus, UserRole
from app.core.limiter import limiter
from app.core.config import settings
from app.db.session import get_connection
from app.schemas.marketplace import (
    BookingCreate,
    BookingResponse,
    CategoryCreate,
    ListingPaymentResponse,
    PaymentCreate,
    PaymentResponse,
    ProviderProfileUpsert,
    ReviewCreate,
    ServiceCreate,
    ServiceUpdate,
)

router = APIRouter(tags=["stepserve"])
UPLOAD_DIR = Path("uploads").resolve()
UPLOAD_DIR.mkdir(exist_ok=True)

# Rate limit strings driven from config.ini [rate_limits]
_PUB = f"{settings.rl_public_per_minute}/minute"
_PAY = f"{settings.rl_payment_per_minute}/minute"
_ADM = "120/minute"  # admin endpoints — authenticated, higher ceiling

MAX_BOOKING_DAYS = 30


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
@limiter.limit(_PUB)
def list_categories(
    request: Request,
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
@limiter.limit(_PUB)
def stepserve_home(
    request: Request,
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
@limiter.limit(_PUB)
def search_services(
    request: Request,
    conn: pymysql.connections.Connection = Depends(get_connection),
    query: str | None = Query(default=None),
    category_id: int | None = Query(default=None),
    location: str | None = Query(default=None),
    min_price: float | None = Query(default=None),
    max_price: float | None = Query(default=None),
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
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
        LIMIT %s OFFSET %s
    """
    with conn.cursor() as cur:
        cur.execute(sql, (*params, limit, offset))
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

    ALLOWED_CONTENT_TYPES = {"application/pdf", "image/jpeg", "image/png"}
    MAX_UPLOAD_BYTES = 10 * 1024 * 1024  # 10 MB

    if file.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(status_code=400, detail="Only PDF, JPG, and PNG files are allowed.")

    content = file.file.read(MAX_UPLOAD_BYTES + 1)
    if len(content) > MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=400, detail="File too large — maximum size is 10 MB.")

    # Strip any path components from the filename to prevent traversal
    safe_filename = Path(file.filename or "upload").name
    safe_name = f"provider_{user['id']}_{safe_filename}"
    filepath = UPLOAD_DIR / safe_name
    filepath.write_bytes(content)

    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO provider_uploads (provider_id, file_name, file_path, content_type, file_size)
            VALUES (%s, %s, %s, %s, %s)
            """,
            (profile["id"], safe_filename, str(filepath), file.content_type, len(content)),
        )
        upload_id = cur.lastrowid
    conn.commit()
    return {"id": upload_id, "file_name": safe_filename, "stored_path": str(filepath)}


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
            raise HTTPException(status_code=400, detail="Provider profile required before creating a listing")

        # Listing starts inactive until $5 payment is made
        cur.execute(
            """
            INSERT INTO services (provider_id, category_id, title, description, price, is_active)
            VALUES (%s, %s, %s, %s, %s, 0)
            """,
            (profile["id"], payload.category_id, payload.title, payload.description, payload.price),
        )
        service_id = cur.lastrowid

        # Create pending listing payment record ($5 CAD)
        cur.execute(
            """
            INSERT INTO listing_payments (service_id, provider_id, amount, currency, status)
            VALUES (%s, %s, 5.00, 'CAD', 'pending')
            """,
            (service_id, profile["id"]),
        )
    conn.commit()
    return {
        "id": service_id,
        "provider_id": profile["id"],
        **payload.model_dump(),
        "is_active": False,
        "payment_status": "pending",
        "listing_fee": 5.00,
    }


@router.post("/listings/{service_id}/pay", response_model=ListingPaymentResponse)
@limiter.limit(_PAY)
def pay_listing(
    request: Request,
    service_id: int,
    conn: pymysql.connections.Connection = Depends(get_connection),
    user: dict[str, Any] = Depends(require_role(UserRole.provider)),
):
    """Pay the $5 listing fee to activate a service. (Demo: no real Stripe charge.)"""
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM provider_profiles WHERE user_id = %s LIMIT 1", (user["id"],))
        profile = cur.fetchone()
        if not profile:
            raise HTTPException(status_code=400, detail="Provider profile not found")

        # FOR UPDATE locks the row to prevent duplicate payment race conditions
        cur.execute(
            "SELECT id, status FROM listing_payments WHERE service_id=%s AND provider_id=%s LIMIT 1 FOR UPDATE",
            (service_id, profile["id"]),
        )
        lp = cur.fetchone()
        if not lp:
            raise HTTPException(status_code=404, detail="Listing payment record not found")
        if lp["status"] == "paid":
            raise HTTPException(status_code=400, detail="Listing already paid")

        fake_pi = f"pi_listing_{service_id}_{user['id']}"
        cur.execute(
            "UPDATE listing_payments SET status='paid', stripe_payment_intent_id=%s, paid_at=NOW() WHERE id=%s",
            (fake_pi, lp["id"]),
        )
        cur.execute("UPDATE services SET is_active=1 WHERE id=%s", (service_id,))

        cur.execute("SELECT * FROM listing_payments WHERE id=%s", (lp["id"],))
        updated = cur.fetchone()

        # Fetch service title for the invoice
        cur.execute("SELECT title FROM services WHERE id=%s LIMIT 1", (service_id,))
        svc = cur.fetchone()
        service_title = svc["title"] if svc else f"Service #{service_id}"

    conn.commit()

    # Send listing payment invoice to provider (fire-and-forget; errors are logged not raised)
    provider_name = user.get("email", "Provider").split("@")[0].capitalize()
    send_email(
        to=user["email"],
        subject=f"Invoice: Your StepServe listing is now live — INV-LST-{updated['id']:06d}",
        html=listing_invoice_html(
            provider_name=provider_name,
            provider_email=user["email"],
            service_title=service_title,
            service_id=service_id,
            invoice_id=updated["id"],
            amount=float(updated["amount"]),
            currency=updated.get("currency", "CAD"),
            stripe_pi=fake_pi,
            paid_at=updated.get("paid_at"),
        ),
    )
    if settings.email_admin_notify:
        send_email(
            to=settings.email_admin_notify,
            subject=f"[StepServe] New listing payment — {service_title}",
            html=listing_invoice_html(
                provider_name=provider_name,
                provider_email=user["email"],
                service_title=service_title,
                service_id=service_id,
                invoice_id=updated["id"],
                amount=float(updated["amount"]),
                currency=updated.get("currency", "CAD"),
                stripe_pi=fake_pi,
                paid_at=updated.get("paid_at"),
            ),
        )

    return ListingPaymentResponse(**updated)


@router.patch("/services/{service_id}")
def update_service(
    service_id: int,
    payload: ServiceUpdate,
    conn: pymysql.connections.Connection = Depends(get_connection),
    user: dict[str, Any] = Depends(require_role(UserRole.provider)),
):
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM provider_profiles WHERE user_id = %s LIMIT 1", (user["id"],))
        profile = cur.fetchone()
        if not profile:
            raise HTTPException(status_code=400, detail="Provider profile not found")

        cur.execute(
            "SELECT id FROM services WHERE id=%s AND provider_id=%s LIMIT 1",
            (service_id, profile["id"]),
        )
        if not cur.fetchone():
            raise HTTPException(status_code=404, detail="Listing not found or not yours")

        updates = {k: v for k, v in payload.model_dump().items() if v is not None}
        if not updates:
            raise HTTPException(status_code=400, detail="No fields to update")

        set_clause = ", ".join(f"{k}=%s" for k in updates)
        cur.execute(
            f"UPDATE services SET {set_clause} WHERE id=%s",
            (*updates.values(), service_id),
        )
    conn.commit()
    with conn.cursor() as cur:
        cur.execute("SELECT * FROM services WHERE id=%s", (service_id,))
        return cur.fetchone()


@router.delete("/services/{service_id}")
def deactivate_service(
    service_id: int,
    conn: pymysql.connections.Connection = Depends(get_connection),
    user: dict[str, Any] = Depends(require_role(UserRole.provider)),
):
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM provider_profiles WHERE user_id = %s LIMIT 1", (user["id"],))
        profile = cur.fetchone()
        if not profile:
            raise HTTPException(status_code=400, detail="Provider profile not found")

        cur.execute(
            "SELECT id FROM services WHERE id=%s AND provider_id=%s LIMIT 1",
            (service_id, profile["id"]),
        )
        if not cur.fetchone():
            raise HTTPException(status_code=404, detail="Listing not found or not yours")

        cur.execute("UPDATE services SET is_active=0 WHERE id=%s", (service_id,))
    conn.commit()
    return {"id": service_id, "is_active": False}


@router.get("/provider/listings")
def provider_listings(
    conn: pymysql.connections.Connection = Depends(get_connection),
    user: dict[str, Any] = Depends(require_role(UserRole.provider)),
):
    """Return all listings for the logged-in provider, with payment status."""
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM provider_profiles WHERE user_id=%s LIMIT 1", (user["id"],))
        profile = cur.fetchone()
        if not profile:
            return []
        cur.execute(
            """
            SELECT s.id, s.title, s.description, s.price, s.is_active, s.created_at,
                   c.name AS category_name,
                   COALESCE(lp.status, 'pending') AS payment_status,
                   lp.amount AS listing_fee,
                   lp.paid_at
            FROM services s
            LEFT JOIN categories c ON c.id = s.category_id
            LEFT JOIN listing_payments lp ON lp.service_id = s.id
            WHERE s.provider_id = %s
            ORDER BY s.id DESC
            """,
            (profile["id"],),
        )
        return cur.fetchall()


@router.get("/services")
def list_services(
    conn: pymysql.connections.Connection = Depends(get_connection),
):
    """Public endpoint — returns active services only."""
    with conn.cursor() as cur:
        cur.execute("SELECT * FROM services WHERE is_active = 1 ORDER BY id DESC")
        return cur.fetchall()


@router.get("/admin/services", dependencies=[Depends(require_role(UserRole.admin))])
@limiter.limit(_ADM)
def admin_list_all_services(
    request: Request,
    conn: pymysql.connections.Connection = Depends(get_connection),
):
    """Admin endpoint — returns all services regardless of active status."""
    with conn.cursor() as cur:
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
@limiter.limit(_PAY)
def create_booking(
    request: Request,
    payload: BookingCreate,
    conn: pymysql.connections.Connection = Depends(get_connection),
    user: dict[str, Any] = Depends(require_role(UserRole.customer)),
):
    if payload.end_time <= payload.start_time:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid booking window")

    duration_days = (payload.end_time - payload.start_time).days
    if duration_days > MAX_BOOKING_DAYS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Booking duration cannot exceed {MAX_BOOKING_DAYS} days",
        )

    with conn.cursor() as cur:
        cur.execute(
            "SELECT id, price, title FROM services WHERE id=%s AND is_active=1 LIMIT 1",
            (payload.service_id,),
        )
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

    # Send booking confirmation to customer + admin notification
    customer_name = user["email"].split("@")[0].capitalize()
    send_email(
        to=user["email"],
        subject=f"Booking Confirmed — {service['title']} (BK-{booking_id:06d})",
        html=booking_confirmation_html(
            customer_name=customer_name,
            customer_email=user["email"],
            service_title=service["title"],
            booking_id=booking_id,
            start_time=payload.start_time,
            end_time=payload.end_time,
            total_price=total_price,
        ),
    )
    if settings.email_admin_notify:
        send_email(
            to=settings.email_admin_notify,
            subject=f"[StepServe] New booking BK-{booking_id:06d} — {service['title']}",
            html=new_booking_admin_html(
                customer_email=user["email"],
                service_title=service["title"],
                booking_id=booking_id,
                start_time=payload.start_time,
                end_time=payload.end_time,
                total_price=total_price,
            ),
        )

    return BookingResponse(id=booking_id, status=BookingStatus.pending, total_price=total_price)


@router.post("/payments", response_model=PaymentResponse)
@limiter.limit(_PAY)
def create_payment(
    request: Request,
    payload: PaymentCreate,
    conn: pymysql.connections.Connection = Depends(get_connection),
    user: dict[str, Any] = Depends(require_role(UserRole.customer)),
):
    with conn.cursor() as cur:
        # FOR UPDATE prevents concurrent double-payment on the same booking
        cur.execute(
            """
            SELECT b.id, b.total_price, b.start_time, b.end_time, s.title AS service_title
            FROM bookings b
            JOIN services s ON b.service_id = s.id
            WHERE b.id=%s AND b.customer_id=%s
            LIMIT 1 FOR UPDATE
            """,
            (payload.booking_id, user["id"]),
        )
        booking = cur.fetchone()
        if not booking:
            raise HTTPException(status_code=404, detail="Booking not found")

        cur.execute("SELECT id FROM payments WHERE booking_id=%s LIMIT 1 FOR UPDATE", (payload.booking_id,))
        payment = cur.fetchone()
        stripe_pi = f"pi_demo_{payload.booking_id}"
        if payment:
            cur.execute(
                "UPDATE payments SET status=%s, stripe_payment_intent_id=%s WHERE id=%s",
                (PaymentStatus.paid.value, stripe_pi, payment["id"]),
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
                    stripe_pi,
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

    # Send payment receipt to customer
    customer_name = user["email"].split("@")[0].capitalize()
    send_email(
        to=user["email"],
        subject=f"Payment Receipt — {booking['service_title']} (RCP-{payment_id:06d})",
        html=payment_receipt_html(
            customer_name=customer_name,
            customer_email=user["email"],
            service_title=booking["service_title"],
            booking_id=payload.booking_id,
            payment_id=payment_id,
            amount=float(booking["total_price"]),
            stripe_pi=stripe_pi,
            paid_at=datetime.utcnow(),
        ),
    )

    return PaymentResponse(
        id=payment_id,
        status=PaymentStatus.paid,
        amount=float(booking["total_price"]),
        stripe_payment_intent_id=stripe_pi,
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
@limiter.limit(_ADM)
def admin_overview(
    request: Request,
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
        cur.execute("SELECT COALESCE(SUM(amount), 0) AS total FROM listing_payments WHERE status='paid'")
        paid_total = float(cur.fetchone()["total"] or 0)

    return {
        "users_count": users_count,
        "services_count": services_count,
        "bookings_count": bookings_count,
        "paid_total": paid_total,
    }


@router.get("/admin/users")
@limiter.limit(_ADM)
def admin_users(
    request: Request,
    conn: pymysql.connections.Connection = Depends(get_connection),
    _: dict[str, Any] = Depends(require_role(UserRole.admin)),
):
    with conn.cursor() as cur:
        cur.execute(
            "SELECT id, email, role, is_active, created_at FROM users ORDER BY id DESC LIMIT 500"
        )
        return cur.fetchall()


@router.patch("/admin/users/{user_id}/status")
@limiter.limit(_ADM)
def admin_update_user_status(
    request: Request,
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
@limiter.limit(_ADM)
def admin_bookings(
    request: Request,
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
