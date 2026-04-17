from datetime import datetime

from pydantic import BaseModel, Field

from app.core.enums import BookingStatus, PaymentStatus


class CategoryCreate(BaseModel):
    name: str = Field(min_length=1, max_length=80)
    slug: str = Field(min_length=1, max_length=80)


class ProviderProfileUpsert(BaseModel):
    full_name: str = Field(min_length=1, max_length=120)
    bio: str | None = Field(default=None, max_length=2000)
    location: str | None = Field(default=None, max_length=120)
    hourly_rate: float | None = Field(default=None, gt=0, le=10_000)


class ServiceCreate(BaseModel):
    category_id: int
    title: str = Field(min_length=1, max_length=120)
    description: str = Field(default="", max_length=2000)
    price: float = Field(gt=0, le=100_000)


class ServiceUpdate(BaseModel):
    category_id: int | None = None
    title: str | None = Field(default=None, min_length=1, max_length=120)
    description: str | None = Field(default=None, max_length=2000)
    price: float | None = Field(default=None, gt=0, le=100_000)


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


class ListingPaymentResponse(BaseModel):
    id: int
    service_id: int
    amount: float
    currency: str
    status: str
    paid_at: datetime | None = None
