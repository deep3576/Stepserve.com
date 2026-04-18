package com.stepserve.app.data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ─────────────────────────────────────────────────────────────────

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<TokenResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<TokenResponse>

    @GET("auth/me")
    suspend fun me(): Response<UserMe>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Response<MessageResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): Response<MessageResponse>

    // ── Public marketplace ───────────────────────────────────────────────────

    @GET("stepserve/home")
    suspend fun home(): Response<HomeData>

    @GET("categories")
    suspend fun categories(): Response<List<Category>>

    @GET("search/services")
    suspend fun searchServices(
        @Query("query") query: String? = null,
        @Query("category_id") categoryId: Int? = null,
        @Query("location") location: String? = null,
        @Query("min_price") minPrice: Double? = null,
        @Query("max_price") maxPrice: Double? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): Response<List<Service>>

    @GET("services")
    suspend fun listServices(): Response<List<Service>>

    // ── Provider profile ─────────────────────────────────────────────────────

    @POST("providers/profile")
    suspend fun upsertProfile(@Body body: ProviderProfileUpsert): Response<ProviderProfile>

    // ── Provider uploads ──────────────────────────────────────────────────────

    @Multipart
    @POST("providers/uploads")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<ProviderUpload>

    @GET("providers/uploads")
    suspend fun listUploads(): Response<List<ProviderUpload>>

    // ── Services (provider) ──────────────────────────────────────────────────

    @POST("services")
    suspend fun createService(@Body body: ServiceCreate): Response<Service>

    @PATCH("services/{service_id}")
    suspend fun updateService(
        @Path("service_id") serviceId: Int,
        @Body body: ServiceUpdate,
    ): Response<Service>

    @DELETE("services/{service_id}")
    suspend fun deactivateService(@Path("service_id") serviceId: Int): Response<Map<String, Any>>

    @GET("provider/listings")
    suspend fun providerListings(): Response<List<Service>>

    @GET("provider/dashboard")
    suspend fun providerDashboard(): Response<ProviderDashboard>

    // ── Listing payment ───────────────────────────────────────────────────────

    @POST("listings/{service_id}/pay")
    suspend fun payListing(@Path("service_id") serviceId: Int): Response<ListingPaymentResponse>

    // ── Bookings ─────────────────────────────────────────────────────────────

    @POST("bookings")
    suspend fun createBooking(@Body body: BookingCreate): Response<BookingResponse>

    @GET("customer/bookings")
    suspend fun customerBookings(): Response<List<CustomerBooking>>

    // ── Payments ─────────────────────────────────────────────────────────────

    @POST("payments")
    suspend fun createPayment(@Body body: PaymentCreate): Response<PaymentResponse>

    // ── Reviews ──────────────────────────────────────────────────────────────

    @POST("reviews")
    suspend fun createReview(@Body body: ReviewCreate): Response<Map<String, Any>>

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GET("admin/overview")
    suspend fun adminOverview(): Response<AdminOverview>

    @GET("admin/users")
    suspend fun adminUsers(): Response<List<AdminUser>>

    @PATCH("admin/users/{user_id}/status")
    suspend fun adminUpdateUserStatus(
        @Path("user_id") userId: Int,
        @Query("active") active: Boolean,
    ): Response<Map<String, Any>>

    @GET("admin/bookings")
    suspend fun adminBookings(): Response<List<AdminBooking>>

    @GET("admin/services")
    suspend fun adminServices(): Response<List<Service>>

    @POST("categories")
    suspend fun createCategory(@Body body: CategoryCreate): Response<Category>
}
