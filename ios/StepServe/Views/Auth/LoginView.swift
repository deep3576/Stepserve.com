import SwiftUI

@MainActor
class LoginViewModel: ObservableObject {
    @Published var email = ""
    @Published var password = ""
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var didLogin = false

    func login() async {
        guard !email.isEmpty, !password.isEmpty else {
            errorMessage = "Please enter email and password"; return
        }
        isLoading = true; errorMessage = nil
        switch await API.login(email: email, password: password) {
        case .success(let token):
            // Fetch role from /auth/me after setting token temporarily
            TokenManager.shared.save(token: token.access_token, email: email, role: "")
            switch await API.me() {
            case .success(let user):
                TokenManager.shared.save(token: token.access_token, email: user.email, role: user.role)
                didLogin = true
            case .failure(let e):
                errorMessage = e
            }
        case .failure(let e):
            errorMessage = e
        }
        isLoading = false
    }
}

struct LoginView: View {
    @StateObject private var vm = LoginViewModel()
    @EnvironmentObject var tokenManager: TokenManager
    @Environment(\.dismiss) var dismiss

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                VStack(spacing: 4) {
                    Text("Welcome back").font(.title2).fontWeight(.bold)
                    Text("Sign in to your account").font(.subheadline).foregroundColor(.secondary)
                }
                .padding(.top, 16)

                if let err = vm.errorMessage { ErrorBanner(message: err) }

                VStack(spacing: 14) {
                    TextField("Email", text: $vm.email)
                        .keyboardType(.emailAddress).autocapitalization(.none)
                        .padding().background(Color(.systemGray6)).cornerRadius(10)

                    SecureField("Password", text: $vm.password)
                        .padding().background(Color(.systemGray6)).cornerRadius(10)
                }

                PrimaryButton(title: "Log In", action: { Task { await vm.login() } }, isLoading: vm.isLoading)

                NavigationLink("Don't have an account? Register") {
                    RegisterView()
                }
                .font(.subheadline).foregroundColor(.brandGreen)
            }
            .padding(24)
        }
        .navigationTitle("Log In")
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: vm.didLogin) { logged in
            if logged { dismiss() }
        }
    }
}
