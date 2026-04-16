from contextlib import asynccontextmanager
from collections.abc import AsyncIterator

from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware
import pymysql

from app.api.routes import auth, marketplace
from app.core.config import settings
from app.db.session import get_connection


@asynccontextmanager
async def lifespan(_: FastAPI) -> AsyncIterator[None]:
    yield


app = FastAPI(title=settings.app_name, debug=settings.debug, lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://localhost:5174",
        "http://127.0.0.1:5173",
        "http://127.0.0.1:5174",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health(conn: pymysql.connections.Connection = Depends(get_connection)) -> dict[str, str]:
    with conn.cursor() as cur:
        cur.execute("SELECT 1")
    return {"status": "ok", "db": "ok"}


app.include_router(auth.router, prefix="/api/v1")
app.include_router(marketplace.router, prefix="/api/v1")
