from fastapi import FastAPI

from app.api.routes import auth, marketplace
from app.core.config import settings

app = FastAPI(title=settings.app_name, debug=settings.debug)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.on_event("startup")
def startup() -> None:
    return None


app.include_router(auth.router, prefix="/api/v1")
app.include_router(marketplace.router, prefix="/api/v1")
