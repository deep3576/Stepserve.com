from datetime import datetime

from pydantic import BaseModel, Field

from app.core.enums import BookingStatus, PaymentStatus


class CategoryCreate(BaseModel):
    name: str
    slug: str


class ProviderProfileUpsert(BaseModel):
    full_name: str
    bio: str | None = None
    location: str | None = None
    hourly_rate: float | None = None


class ServiceCreate(BaseModel):
    category_id: int
    title: str
    description: str
    price: float = Field(gt=0)


class BookingCreate(BaseModel):
    service_id: int
    start_time: datetime
    end_time: datetime


class PaymentCreate(BaseModel):
    booking_id: int


class ReviewCreate(BaseModel):
    booking_id: int
    rating: int = Field(ge=1, le=5)
    comment: str | None = None


class BookingResponse(BaseModel):
    id: int
    status: BookingStatus
    total_price: float


class PaymentResponse(BaseModel):
    id: int
    status: PaymentStatus
    amount: float
    stripe_payment_intent_id: str | None
