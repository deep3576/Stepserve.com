from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Request, status
import pymysql

from app.api.deps import get_current_user
from app.core.config import settings
from app.core.limiter import limiter
from app.core.security import create_access_token, get_password_hash, verify_password
from app.db.session import get_connection
from app.schemas.auth import LoginRequest, RegisterRequest, TokenResponse

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
