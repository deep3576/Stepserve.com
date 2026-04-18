import Foundation

// All API calls as static async methods, using NetworkClient.
enum API {
    private static let net = NetworkClient.shared

    // MARK: - Auth
    static func login(email: String, password: String) async -> APIResult<TokenResponse> {
        await net.request(path: "/auth/login", method: "POST", body: LoginRequest(email: email, password: password), authenticated: false)
    }

    static func register(email: String, password: String, role: String) async -> APIResult<TokenResponse> {
        await net.request(path: "/auth/register", method: "POST", body: RegisterRequest(email: email, password: password, role: role), authenticated: false)
    }

    static func me() async -> APIResult<UserMe> {
        await net.request(path: "/auth/me")
    }

    // MARK: - Home / Marketplace
    static func home() async -> APIResult<HomeData> {
        await net.request(path: "/stepserve/home", authenticated: false)
    }

    static func categories() async -> APIResult<[Category]> {
        await net.request(path: "/categories", authenticated: false)
    }

    static func services() async -> APIResult<[Service]> {
        await net.request(path: "/services", authenticated: false)
    }

    static func searchServices(query: String = "", categoryId: Int? = nil, location: String = "", minPrice: String = "", maxPrice: String = "") async -> APIResult<[Service]> {
        var params: [String] = []
        if !query.isEmpty    { params.append("query=\(query.urlEncoded)") }
        if let c = categoryId { params.append("category_id=\(c)") }
        if !location.isEmpty { params.append("location=\(location.urlEncoded)") }
        if !minPrice.isEmpty { params.append("min_price=\(minPrice)") }
        if !maxPrice.isEmpty { params.append("max_price=\(maxPrice)") }
        let qs = params.isEmpty ? "" : "?\(params.joined(separator: "&"))"
        return await net.request(path: "/search/services\(qs)", authenticated: false)
    }

    // MARK: - Provider
    static func upsertProfile(_ body: ProviderProfileUpsert) async -> APIResult<ProviderProfile> {
        await net.request(path: "/providers/profile", method: "POST", body: body)
    }

    static func providerDashboard() async -> APIResult<ProviderDashboard> {
        await net.request(path: "/provider/dashboard")
    }

    static func providerListings() async -> APIResult<[Service]> {
        await net.request(path: "/provider/listings")
    }

    static func createService(_ body: ServiceCreate) async -> APIResult<Service> {
        await net.request(path: "/services", method: "POST", body: body)
    }

    static func updateService(id: Int, body: ServiceUpdate) async -> APIResult<Service> {
        await net.request(path: "/services/\(id)", method: "PATCH", body: body)
    }

    static func deleteService(id: Int) async -> APIResult<Service> {
        await net.request(path: "/services/\(id)", method: "DELETE")
    }

    static func payListing(serviceId: Int) async -> APIResult<ListingPaymentResponse> {
        await net.request(path: "/listings/\(serviceId)/pay", method: "POST")
    }

    // MARK: - Documents
    static func providerUploads() async -> APIResult<[ProviderUpload]> {
        await net.request(path: "/providers/uploads")
    }

    static func uploadDocument(data: Data, fileName: String, mimeType: String) async -> APIResult<ProviderUpload> {
        await net.upload(path: "/providers/uploads", fileData: data, fileName: fileName, mimeType: mimeType)
    }

    // MARK: - Bookings
    static func createBooking(_ body: BookingCreate) async -> APIResult<BookingResponse> {
        await net.request(path: "/bookings", method: "POST", body: body)
    }

    static func customerBookings() async -> APIResult<[CustomerBooking]> {
        await net.request(path: "/customer/bookings")
    }

    // MARK: - Payments
    static func createPayment(_ body: PaymentCreate) async -> APIResult<PaymentResponse> {
        await net.request(path: "/payments", method: "POST", body: body)
    }

    // MARK: - Reviews
    static func createReview(_ body: ReviewCreate) async -> APIResult<EmptyResponse> {
        await net.request(path: "/reviews", method: "POST", body: body)
    }

    // MARK: - Admin
    static func adminOverview() async -> APIResult<AdminOverview> {
        await net.request(path: "/admin/overview")
    }

    static func adminUsers() async -> APIResult<[AdminUser]> {
        await net.request(path: "/admin/users")
    }

    static func adminSetUserStatus(userId: Int, active: Bool) async -> APIResult<EmptyResponse> {
        await net.request(path: "/admin/users/\(userId)/status?active=\(active)", method: "PATCH")
    }

    static func adminBookings() async -> APIResult<[AdminBooking]> {
        await net.request(path: "/admin/bookings")
    }

    static func createCategory(_ body: CategoryCreate) async -> APIResult<Category> {
        await net.request(path: "/categories", method: "POST", body: body)
    }
}

// Placeholder for endpoints that return a JSON object we don't need to parse
struct EmptyResponse: Decodable {}

private extension String {
    var urlEncoded: String {
        addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? self
    }
}
