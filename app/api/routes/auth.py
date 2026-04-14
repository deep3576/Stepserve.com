from fastapi import APIRouter, Depends, HTTPException, status
import pymysql

from app.core.security import create_access_token, get_password_hash, verify_password
from app.db.session import get_connection
from app.schemas.auth import LoginRequest, RegisterRequest, TokenResponse

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", response_model=TokenResponse)
def register(
    payload: RegisterRequest,
    conn: pymysql.connections.Connection = Depends(get_connection),
) -> TokenResponse:
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
def login(
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
