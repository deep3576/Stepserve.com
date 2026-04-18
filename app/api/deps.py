from typing import Any

from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
import pymysql

from app.core.enums import UserRole
from app.core.security import decode_access_token
from app.db.session import get_connection

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login")


UserDict = dict[str, Any]


def get_current_user(
    conn: pymysql.connections.Connection = Depends(get_connection),
    token: str = Depends(oauth2_scheme),
) -> UserDict:
    subject = decode_access_token(token)
    if not subject:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")

    with conn.cursor() as cur:
        cur.execute(
            "SELECT id, email, role, is_active FROM users WHERE email = %s LIMIT 1",
            (subject,),
        )
        user = cur.fetchone()

    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")
    if not user["is_active"]:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Account is disabled")
    return user


def require_role(*roles: UserRole):
    def _inner(user: UserDict = Depends(get_current_user)) -> UserDict:
        if user["role"] not in [r.value for r in roles]:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Insufficient permissions")
        return user

    return _inner
