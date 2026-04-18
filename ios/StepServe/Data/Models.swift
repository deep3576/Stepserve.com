import Foundation

// MARK: - Auth
struct LoginRequest: Encodable {
    let email: String
    let password: String
}

struct RegisterRequest: Encodable {
    let email: String
    let password: String
    let role: String
}

struct TokenResponse: Decodable {
    let access_token: String
    let token_type: String
}

struct ForgotPasswordRequest: Encodable {
    let email: String
}

struct ResetPasswordRequest: Encodable {
    let email: String
    let code: String
    let new_password: String
}

struct MessageResponse: Decodable {
    let message: String
}

struct UserMe: Decodable {
    let id: Int
    let email: String
    let role: String
    let is_active: Int
}

// MARK: - Categories
struct Category: Decodable, Identifiable {
    let id: Int
    let name: String
    let slug: String
    let services_count: Int?
}

struct CategoryCreate: Encodable {
    let name: String
    let slug: String
}

// MARK: - Services
struct Service: Decodable, Identifiable {
    let id: Int
    let provider_id: Int?
    let category_id: Int?
    let title: String
    let description: String?
    let price: Double
    let is_active: Bool?
    let created_at: String?
    let provider_name: String?
    let location: String?
    let payment_status: String?
    let listing_fee: Double?
    let paid_at: String?
    let category_name: String?
}

struct ServiceCreate: Encodable {
    let category_id: Int
    let title: String
    let description: String
    let price: Double
}

struct ServiceUpdate: Encodable {
    let title: String?
    let description: String?
    let price: Double?
    let category_id: Int?
}

// MARK: - Home
struct HomeData: Decodable {
    let categories: [Category]
    let featured: [Service]
    let latest: [Service]
    let top_locations: [TopLocation]
}

struct TopLocation: Decodable, Identifiable {
    var id: String { location }
    let location: String
    let listings_count: Int
}

// MARK: - Provider Profile
struct ProviderProfileUpsert: Encodable {
    let full_name: String
    let bio: String
    let location: String
    let hourly_rate: Double
}

struct ProviderProfile: Decodable {
    let id: Int
    let user_id: Int
    let full_name: String
    let bio: String?
    let location: String?
    let hourly_rate: Double?
}

struct ProviderUpload: Decodable, Identifiable {
    let id: Int
    let file_name: String
    let content_type: String?
    let file_size: Int?
    let created_at: String?
}

// MARK: - Listing Payment
struct ListingPaymentResponse: Decodable {
    let id: Int
    let service_id: Int
    let amount: Double
    let currency: String
    let status: String
    let paid_at: String?
}

// MARK: - Bookings
struct BookingCreate: Encodable {
    let service_id: Int
    let start_time: String
    let end_time: String
}

struct BookingResponse: Decodable {
    let id: Int
    let status: String
    let total_price: Double
}

struct CustomerBooking: Decodable, Identifiable {
    let id: Int
    let service_id: Int?
    let service_title: String?
    let start_time: String
    let end_time: String
    let total_price: Double
    let status: String
    let created_at: String?
}

// MARK: - Payments
struct PaymentCreate: Encodable {
    let booking_id: Int
}

struct PaymentResponse: Decodable {
    let id: Int
    let status: String
    let amount: Double
    let stripe_payment_intent_id: String?
}

// MARK: - Reviews
struct ReviewCreate: Encodable {
    let booking_id: Int
    let rating: Int
    let comment: String
}

// MARK: - Provider Dashboard
struct ProviderDashboard: Decodable {
    let services: [Service]
    let bookings: [ProviderBooking]
    let uploads: [ProviderUpload]
}

struct ProviderBooking: Decodable, Identifiable {
    let id: Int
    let customer_email: String?
    let service_title: String?
    let start_time: String
    let end_time: String
    let total_price: Double
    let status: String
}

// MARK: - Admin
struct AdminOverview: Decodable {
    let users_count: Int
    let services_count: Int
    let bookings_count: Int
    let paid_total: Double
}

struct AdminUser: Decodable, Identifiable {
    let id: Int
    let email: String
    let role: String
    let is_active: Int
    let created_at: String?
}

struct AdminBooking: Decodable, Identifiable {
    let id: Int
    let customer_email: String?
    let service_title: String?
    let start_time: String
    let end_time: String
    let total_price: Double
    let status: String
}

// MARK: - API Error
struct APIError: Decodable {
    let detail: String
}
