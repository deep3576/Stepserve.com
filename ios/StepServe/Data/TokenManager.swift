import Foundation

final class TokenManager: ObservableObject {
    static let shared = TokenManager()

    private let defaults = UserDefaults.standard
    private enum Keys {
        static let token = "ss_token"
        static let email = "ss_email"
        static let role  = "ss_role"
    }

    @Published var isLoggedIn: Bool = false
    @Published var role: String = ""
    @Published var email: String = ""

    private init() {
        isLoggedIn = defaults.string(forKey: Keys.token) != nil
        role  = defaults.string(forKey: Keys.role)  ?? ""
        email = defaults.string(forKey: Keys.email) ?? ""
    }

    var token: String? { defaults.string(forKey: Keys.token) }

    func save(token: String, email: String, role: String) {
        defaults.set(token, forKey: Keys.token)
        defaults.set(email, forKey: Keys.email)
        defaults.set(role,  forKey: Keys.role)
        DispatchQueue.main.async {
            self.isLoggedIn = true
            self.role  = role
            self.email = email
        }
    }

    func clear() {
        defaults.removeObject(forKey: Keys.token)
        defaults.removeObject(forKey: Keys.email)
        defaults.removeObject(forKey: Keys.role)
        DispatchQueue.main.async {
            self.isLoggedIn = false
            self.role  = ""
            self.email = ""
        }
    }
}
