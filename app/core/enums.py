from enum import Enum


class UserRole(str, Enum):
    customer = "customer"
    provider = "provider"
    admin = "admin"


class BookingStatus(str, Enum):
    pending = "pending"
    confirmed = "confirmed"
    in_progress = "in_progress"
    completed = "completed"
    canceled = "canceled"


class PaymentStatus(str, Enum):
    requires_payment = "requires_payment"
    paid = "paid"
    refunded = "refunded"
    failed = "failed"
