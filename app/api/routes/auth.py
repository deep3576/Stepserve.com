import random
import string
from datetime import datetime, timedelta, timezone
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Request, status
import pymysql

from app.api.deps import get_current_user
from app.core.config import settings
from app.core.email import password_reset_html, send_email
from app.core.limiter import limiter
from app.core.security import create_access_token, get_password_hash, verify_password
from app.db.session import get_connection
from app.schemas.auth import (
    ForgotPasswordRequest,
    LoginRequest,
    RegisterRequest,
    ResetPasswordRequest,
    TokenResponse,
)

router = APIRouter(prefix="/auth", tags=["auth"])

_REG = f"{settings.rl_register_per_hour}/hour"
_LOGIN = f"{settings.rl_login_per_minute}/minute"


@router.post("/register", response_model=TokenResponse)
@limiter.limit(_REG)
def register(
    request: Request,
    payload: RegisterRequest,
    conn: pymysql.connections.Connection = Depends(get_connection),
) -> TokenResponse:
    if payload.role.value == "admin":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Self-service admin registration is not allowed")

    with conn.cursor() as cur:
        cur.execute("SELECT id FROM users WHERE email = %s LIMIT 1", (payload.email,))
        if cur.fetchone():
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Email already exists")

        cur.execute(
            """
            INSERT INTO users (email, password_hash, role, is_active)
            VALUES (%s, %s, %s, %s)
            """,
            (payload.email, get_password_hash(payload.password), payload.role.value, 1),
        )
    conn.commit()
    token = create_access_token(payload.email)
    return TokenResponse(access_token=token)


@router.post("/login", response_model=TokenResponse)
@limiter.limit(_LOGIN)
def login(
    request: Request,
    payload: LoginRequest,
    conn: pymysql.connections.Connection = Depends(get_connection),
) -> TokenResponse:
    with conn.cursor() as cur:
        cur.execute("SELECT email, password_hash FROM users WHERE email = %s LIMIT 1", (payload.email,))
        user = cur.fetchone()

    if not user or not verify_password(payload.password, user["password_hash"]):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")
    token = create_access_token(user["email"])
    return TokenResponse(access_token=token)


@router.get("/me")
def me(user: dict[str, Any] = Depends(get_current_user)) -> dict[str, Any]:
    return {"id": user["id"], "email": user["email"], "role": user["role"], "is_active": user["is_active"]}


_FORGOT = f"{settings.rl_login_per_minute}/minute"
_RESET_EXPIRES_MINUTES = 15


@router.post("/forgot-password", status_code=status.HTTP_200_OK)
@limiter.limit(_FORGOT)
def forgot_password(
    request: Request,
    payload: ForgotPasswordRequest,
    conn: pymysql.connections.Connection = Depends(get_connection),
) -> dict[str, str]:
    """Send a 6-digit reset code to the user's email. Always returns 200 to avoid email enumeration."""
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM users WHERE email = %s LIMIT 1", (payload.email,))
        user = cur.fetchone()

    if user:
        # Generate a secure 6-digit numeric code
        code = "".join(random.choices(string.digits, k=6))
        expires_at = datetime.now(timezone.utc) + timedelta(minutes=_RESET_EXPIRES_MINUTES)

        with conn.cursor() as cur:
            # Invalidate any previous unused tokens for this email
            cur.execute(
                "UPDATE password_reset_tokens SET used=1 WHERE email=%s AND used=0",
                (payload.email,),
            )
            cur.execute(
                "INSERT INTO password_reset_tokens (email, code, expires_at) VALUES (%s, %s, %s)",
                (payload.email, code, expires_at.strftime("%Y-%m-%d %H:%M:%S")),
            )
        conn.commit()

        send_email(
            to=payload.email,
            subject="Your StepServe password reset code",
            html=password_reset_html(email=payload.email, code=code, expires_minutes=_RESET_EXPIRES_MINUTES),
        )

    return {"message": "If that email is registered you will receive a reset code shortly."}


@router.post("/reset-password", status_code=status.HTTP_200_OK)
@limiter.limit(_FORGOT)
def reset_password(
    request: Request,
    payload: ResetPasswordRequest,
    conn: pymysql.connections.Connection = Depends(get_connection),
) -> dict[str, str]:
    """Validate the 6-digit code and set a new password."""
    now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT id FROM password_reset_tokens
            WHERE email=%s AND code=%s AND used=0 AND expires_at > %s
            ORDER BY created_at DESC LIMIT 1
            """,
            (payload.email, payload.code, now),
        )
        token_row = cur.fetchone()

    if not token_row:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid or expired reset code. Please request a new one.",
        )

    with conn.cursor() as cur:
        cur.execute(
            "UPDATE users SET password_hash=%s WHERE email=%s",
            (get_password_hash(payload.new_password), payload.email),
        )
        cur.execute(
            "UPDATE password_reset_tokens SET used=1 WHERE id=%s",
            (token_row["id"],),
        )
    conn.commit()

    return {"message": "Password updated successfully. You can now log in."}
