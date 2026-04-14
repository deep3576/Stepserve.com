from collections.abc import Generator

import pymysql
from pymysql.cursors import DictCursor

from app.core.config import settings


def get_connection() -> Generator[pymysql.connections.Connection, None, None]:
    conn = pymysql.connect(
        host=settings.mysql_host,
        user=settings.mysql_user,
        password=settings.mysql_password,
        database=settings.mysql_db,
        port=settings.mysql_port,
        charset="utf8mb4",
        cursorclass=DictCursor,
        autocommit=False,
    )
    try:
        yield conn
    finally:
        conn.close()
