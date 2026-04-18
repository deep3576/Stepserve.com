import SwiftUI

// MARK: - Forgot Password (Step 1: enter email)

@MainActor
class ForgotPasswordViewModel: ObservableObject {
    @Published var email = ""
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var codeSentTo: String?   // non-nil → navigate to reset screen

    func sendCode() async {
        guard !email.trimmingCharacters(in: .whitespaces).isEmpty else {
            errorMessage = "Please enter your email address"; return
        }
        isLoading = true; errorMessage = nil
        switch await API.forgotPassword(email: email.trimmingCharacters(in: .whitespaces)) {
        case .success:
            codeSentTo = email.trimmingCharacters(in: .whitespaces)
        case .failure(let e):
            errorMessage = e
        }
        isLoading = false
    }
}

struct ForgotPasswordView: View {
    @StateObject private var vm = ForgotPasswordViewModel()

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {

                // Icon
                ZStack {
                    Circle().fill(Color.brandLight).frame(width: 80, height: 80)
                    Image(systemName: "lock.rotation")
                        .font(.system(size: 34, weight: .semibold))
                        .foregroundColor(.brandGreen)
                }
                .padding(.top, 24)

                VStack(spacing: 6) {
                    Text("Reset your password")
                        .font(.title2).fontWeight(.bold)
                    Text("Enter your email and we'll send a 6-digit reset code.")
                        .font(.subheadline).foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }

                if let err = vm.errorMessage { ErrorBanner(message: err) }

                TextField("Email address", text: $vm.email)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                    .autocorrectionDisabled()
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(10)

                PrimaryButton(
                    title: "Send Reset Code",
                    action: { Task { await vm.sendCode() } },
                    isLoading: vm.isLoading
                )

                NavigationLink(destination: ResetPasswordView(email: vm.codeSentTo ?? vm.email),
                               isActive: Binding(
                                   get: { vm.codeSentTo != nil },
                                   set: { if !$0 { vm.codeSentTo = nil } }
                               )) { EmptyView() }

            }
            .padding(24)
        }
        .navigationTitle("Forgot Password")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Reset Password (Step 2: enter code + new password)

@MainActor
class ResetPasswordViewModel: ObservableObject {
    @Published var code = ""
    @Published var newPassword = ""
    @Published var confirmPassword = ""
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var didReset = false
    @Published var showPassword = false

    func resetPassword(email: String) async {
        guard code.count == 6, code.allSatisfy({ $0.isNumber }) else {
            errorMessage = "Enter the 6-digit code from your email"; return
        }
        guard newPassword.count >= 8 else {
            errorMessage = "Password must be at least 8 characters"; return
        }
        guard newPassword == confirmPassword else {
            errorMessage = "Passwords do not match"; return
        }
        isLoading = true; errorMessage = nil
        switch await API.resetPassword(email: email, code: code, newPassword: newPassword) {
        case .success:
            didReset = true
        case .failure(let e):
            errorMessage = e
        }
        isLoading = false
    }
}

struct ResetPasswordView: View {
    let email: String
    @StateObject private var vm = ResetPasswordViewModel()
    @Environment(\.dismiss) var dismiss

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {

                if vm.didReset {
                    // ── Success ──────────────────────────────────────────
                    ZStack {
                        Circle().fill(Color.brandLight).frame(width: 80, height: 80)
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 40))
                            .foregroundColor(.brandGreen)
                    }
                    .padding(.top, 24)

                    Text("Password updated!")
                        .font(.title2).fontWeight(.bold)
                    Text("Your password has been changed successfully.\nYou can now sign in with your new password.")
                        .font(.subheadline).foregroundColor(.secondary)
                        .multilineTextAlignment(.center)

                    PrimaryButton(title: "Back to Sign In", action: {
                        // Pop back to root of navigation stack (login)
                        dismiss()
                        dismiss()
                    })
                    .padding(.top, 8)

                } else {
                    // ── Enter code + new password ─────────────────────────
                    VStack(spacing: 6) {
                        Text("Check your email")
                            .font(.title2).fontWeight(.bold)
                        Text("Enter the 6-digit code sent to\n\(email)")
                            .font(.subheadline).foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding(.top, 24)

                    if let err = vm.errorMessage { ErrorBanner(message: err) }

                    // Code input — large and prominent
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Reset code").font(.caption).foregroundColor(.secondary)
                        TextField("• • • • • •", text: $vm.code)
                            .keyboardType(.numberPad)
                            .font(.system(size: 28, weight: .bold, design: .monospaced))
                            .multilineTextAlignment(.center)
                            .tracking(12)
                            .padding()
                            .background(Color(.systemGray6))
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(vm.code.count == 6 ? Color.brandGreen : Color.clear, lineWidth: 2)
                            )
                            .onChange(of: vm.code) { v in
                                if v.count > 6 { vm.code = String(v.prefix(6)) }
                                vm.code = v.filter { $0.isNumber }
                            }
                    }

                    // New password
                    VStack(alignment: .leading, spacing: 6) {
                        Text("New password").font(.caption).foregroundColor(.secondary)
                        ZStack(alignment: .trailing) {
                            Group {
                                if vm.showPassword {
                                    TextField("Minimum 8 characters", text: $vm.newPassword)
                                } else {
                                    SecureField("Minimum 8 characters", text: $vm.newPassword)
                                }
                            }
                            .padding().background(Color(.systemGray6)).cornerRadius(10)
                            Button(action: { vm.showPassword.toggle() }) {
                                Image(systemName: vm.showPassword ? "eye.slash" : "eye")
                                    .foregroundColor(.secondary).padding(.trailing, 14)
                            }
                        }
                    }

                    // Confirm password
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Confirm new password").font(.caption).foregroundColor(.secondary)
                        SecureField("Re-enter password", text: $vm.confirmPassword)
                            .padding()
                            .background(Color(.systemGray6))
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(
                                        !vm.confirmPassword.isEmpty && vm.confirmPassword != vm.newPassword
                                            ? Color.red.opacity(0.5) : Color.clear,
                                        lineWidth: 1.5
                                    )
                            )
                    }

                    PrimaryButton(
                        title: "Reset Password",
                        action: { Task { await vm.resetPassword(email: email) } },
                        isLoading: vm.isLoading,
                        disabled: vm.code.count != 6 || vm.newPassword.isEmpty || vm.confirmPassword.isEmpty
                    )

                    NavigationLink(destination: ForgotPasswordView()) {
                        Text("Resend code")
                            .font(.subheadline)
                            .foregroundColor(.brandGreen)
                    }
                }
            }
            .padding(24)
        }
        .navigationTitle("Enter Code")
        .navigationBarTitleDisplayMode(.inline)
    }
}
