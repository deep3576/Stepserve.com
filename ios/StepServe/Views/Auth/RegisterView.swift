import SwiftUI

@MainActor
class RegisterViewModel: ObservableObject {
    @Published var email = ""
    @Published var password = ""
    @Published var role = "customer"
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var didRegister = false

    let roles = ["customer", "provider"]

    func register() async {
        guard !email.isEmpty, password.count >= 6 else {
            errorMessage = "Email required and password must be 6+ characters"; return
        }
        isLoading = true; errorMessage = nil
        switch await API.register(email: email, password: password, role: role) {
        case .success(let token):
            TokenManager.shared.save(token: token.access_token, email: email, role: role)
            didRegister = true
        case .failure(let e):
            errorMessage = e
        }
        isLoading = false
    }
}

struct RegisterView: View {
    @StateObject private var vm = RegisterViewModel()
    @Environment(\.dismiss) var dismiss

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                VStack(spacing: 4) {
                    Text("Create Account").font(.title2).fontWeight(.bold)
                    Text("Join StepServe today").font(.subheadline).foregroundColor(.secondary)
                }
                .padding(.top, 16)

                if let err = vm.errorMessage { ErrorBanner(message: err) }

                VStack(spacing: 14) {
                    TextField("Email", text: $vm.email)
                        .keyboardType(.emailAddress).autocapitalization(.none)
                        .padding().background(Color(.systemGray6)).cornerRadius(10)

                    SecureField("Password (min 6 characters)", text: $vm.password)
                        .padding().background(Color(.systemGray6)).cornerRadius(10)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("I am a...").font(.subheadline).fontWeight(.medium)
                        HStack(spacing: 12) {
                            ForEach(vm.roles, id: \.self) { r in
                                Button(action: { vm.role = r }) {
                                    Text(r.capitalized)
                                        .fontWeight(.medium).padding(.horizontal, 20).padding(.vertical, 10)
                                        .background(vm.role == r ? Color.brandGreen : Color(.systemGray5))
                                        .foregroundColor(vm.role == r ? .white : .primary)
                                        .cornerRadius(10)
                                }
                            }
                        }
                    }
                }

                PrimaryButton(title: "Create Account", action: { Task { await vm.register() } }, isLoading: vm.isLoading)
            }
            .padding(24)
        }
        .navigationTitle("Register")
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: vm.didRegister) { done in if done { dismiss() } }
    }
}
