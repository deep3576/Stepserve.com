package com.stepserve.app.data.api

import com.google.gson.annotations.SerializedName

// ── Auth ─────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String,
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val role: String, // "provider" or "customer"
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
)

data class UserMe(
    val id: Int,
    val email: String,
    val role: String,
    @SerializedName("is_active") val isActive: Boolean,
)

// ── Categories ────────────────────────────────────────────────────────────────

data class Category(
    val id: Int,
    val name: String,
    val slug: String,
    @SerializedName("services_count") val servicesCount: Int = 0,
)

// ── Services ─────────────────────────────────────────────────────────────────

data class Service(
    val id: Int,
    val title: String,
    val description: String = "",
    val price: Double,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("provider_name") val providerName: String? = null,
    val location: String? = null,
    @SerializedName("is_active") val isActive: Int = 1,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("listing_fee") val listingFee: Double? = null,
    @SerializedName("paid_at") val paidAt: String? = null,
)

data class ServiceCreate(
    @SerializedName("category_id") val categoryId: Int,
    val title: String,
    val description: String,
    val price: Double,
)

data class ServiceUpdate(
    @SerializedName("category_id") val categoryId: Int? = null,
    val title: String? = null,
    val description: String? = null,
    val price: Double? = null,
)

// ── Home ─────────────────────────────────────────────────────────────────────

data class TopLocation(
    val location: String,
    @SerializedName("listings_count") val listingsCount: Int,
)

data class HomeData(
    val categories: List<Category>,
    val featured: List<Service>,
    val latest: List<Service>,
    @SerializedName("top_locations") val topLocations: List<TopLocation>,
)

// ── Provider Profile ─────────────────────────────────────────────────────────

data class ProviderProfileUpsert(
    @SerializedName("full_name") val fullName: String,
    val bio: String? = null,
    val location: String? = null,
    @SerializedName("hourly_rate") val hourlyRate: Double? = null,
)

data class ProviderProfile(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("full_name") val fullName: String,
    val bio: String? = null,
    val location: String? = null,
    @SerializedName("hourly_rate") val hourlyRate: Double? = null,
)

// ── Uploads ──────────────────────────────────────────────────────────────────

data class ProviderUpload(
    val id: Int,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("file_path") val filePath: String,
    @SerializedName("content_type") val contentType: String,
    @SerializedName("file_size") val fileSize: Long,
    @SerializedName("created_at") val createdAt: String? = null,
)

// ── Listing Payment ───────────────────────────────────────────────────────────

data class ListingPaymentResponse(
    val id: Int,
    @SerializedName("service_id") val serviceId: Int,
    val amount: Double,
    val currency: String,
    val status: String,
    @SerializedName("paid_at") val paidAt: String? = null,
)

// ── Bookings ─────────────────────────────────────────────────────────────────

data class BookingCreate(
    @SerializedName("service_id") val serviceId: Int,
    @SerializedName("start_time") val startTime: String, // ISO-8601
    @SerializedName("end_time") val endTime: String,
)

data class BookingResponse(
    val id: Int,
    val status: String,
    @SerializedName("total_price") val totalPrice: Double,
)

data class CustomerBooking(
    val id: Int,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    val status: String,
    @SerializedName("total_price") val totalPrice: Double,
    @SerializedName("service_title") val serviceTitle: String,
)

// ── Payments ─────────────────────────────────────────────────────────────────

data class PaymentCreate(
    @SerializedName("booking_id") val bookingId: Int,
)

data class PaymentResponse(
    val id: Int,
    val status: String,
    val amount: Double,
    @SerializedName("stripe_payment_intent_id") val stripePaymentIntentId: String? = null,
)

// ── Reviews ──────────────────────────────────────────────────────────────────

data class ReviewCreate(
    @SerializedName("booking_id") val bookingId: Int,
    val rating: Int,
    val comment: String? = null,
)

// ── Provider Dashboard ────────────────────────────────────────────────────────

data class ProviderDashboard(
    val services: List<Service>,
    val bookings: List<ProviderBooking>,
    val uploads: List<ProviderUpload>,
)

data class ProviderBooking(
    val id: Int,
    val status: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("total_price") val totalPrice: Double,
)

// ── Admin ─────────────────────────────────────────────────────────────────────

data class AdminOverview(
    @SerializedName("users_count") val usersCount: Int,
    @SerializedName("services_count") val servicesCount: Int,
    @SerializedName("bookings_count") val bookingsCount: Int,
    @SerializedName("paid_total") val paidTotal: Double,
)

data class AdminUser(
    val id: Int,
    val email: String,
    val role: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String? = null,
)

data class AdminUserStatusUpdate(
    val active: Boolean,
)

data class AdminBooking(
    val id: Int,
    val status: String,
    @SerializedName("total_price") val totalPrice: Double,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("customer_email") val customerEmail: String,
)

data class CategoryCreate(
    val name: String,
    val slug: String,
)

// ── Generic API error ────────────────────────────────────────────────────────

data class ApiError(
    val detail: String = "Unknown error",
)
