import SwiftUI

@MainActor
class ProfileViewModel: ObservableObject {
    @Published var fullName = ""
    @Published var bio = ""
    @Published var location = ""
    @Published var hourlyRate = ""
    @Published var isLoading = false
    @Published var isSaving = false
    @Published var errorMessage: String?
    @Published var successMessage: String?

    func loadProfile() async {
        // Try to load existing profile
        // (We use the provider dashboard which includes profile info indirectly)
    }

    func saveProfile() async {
        guard !fullName.isEmpty else { errorMessage = "Name required"; return }
        let rate = Double(hourlyRate) ?? 0
        isSaving = true; errorMessage = nil
        switch await API.upsertProfile(ProviderProfileUpsert(
            full_name: fullName, bio: bio, location: location, hourly_rate: rate
        )) {
        case .success: successMessage = "Profile updated!"
        case .failure(let e): errorMessage = e
        }
        isSaving = false
    }
}

struct ProfileView: View {
    @EnvironmentObject var tokenManager: TokenManager
    @StateObject private var vm = ProfileViewModel()

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Avatar
                ZStack {
                    Circle().fill(Color.brandGreen).frame(width: 80, height: 80)
                    Text(tokenManager.email.prefix(1).uppercased())
                        .font(.largeTitle).fontWeight(.bold).foregroundColor(.white)
                }
                .padding(.top, 8)

                Text(tokenManager.email).font(.subheadline).foregroundColor(.secondary)
                Text(tokenManager.role.capitalized)
                    .font(.caption).fontWeight(.semibold)
                    .padding(.horizontal, 12).padding(.vertical, 4)
                    .background(Color.brandLight).foregroundColor(.brandGreen).cornerRadius(10)

                Divider()

                // Role-specific quick links
                if tokenManager.role == "provider" {
                    VStack(spacing: 12) {
                        SectionHeader(title: "Provider Actions")
                        profileLink(icon: "chart.bar.fill", title: "Dashboard") { AnyView(ProviderDashboardView()) }
                        profileLink(icon: "list.bullet.rectangle", title: "My Listings") { AnyView(ProviderListingsView()) }
                        profileLink(icon: "doc.badge.plus", title: "Documents") { AnyView(ProviderDocumentsView()) }
                    }

                    Divider()

                    // Provider profile form
                    VStack(alignment: .leading, spacing: 14) {
                        SectionHeader(title: "Update Profile")
                        if let msg = vm.successMessage { SuccessBanner(message: msg) }
                        if let err = vm.errorMessage   { ErrorBanner(message: err) }

                        TextField("Full Name *", text: $vm.fullName)
                            .padding().background(Color(.systemGray6)).cornerRadius(10)
                        TextField("Location (city)", text: $vm.location)
                            .padding().background(Color(.systemGray6)).cornerRadius(10)
                        HStack {
                            Text("CAD $").foregroundColor(.secondary)
                            TextField("Hourly Rate", text: $vm.hourlyRate).keyboardType(.decimalPad)
                        }
                        .padding().background(Color(.systemGray6)).cornerRadius(10)
                        TextEditor(text: $vm.bio)
                            .frame(minHeight: 80).padding(8)
                            .background(Color(.systemGray6)).cornerRadius(10)

                        PrimaryButton(title: "Save Profile", action: { Task { await vm.saveProfile() } }, isLoading: vm.isSaving)
                    }
                } else if tokenManager.role == "customer" {
                    VStack(spacing: 12) {
                        SectionHeader(title: "Customer Actions")
                        profileLink(icon: "calendar", title: "My Bookings") { AnyView(CustomerBookingsView()) }
                    }
                } else if tokenManager.role == "admin" {
                    VStack(spacing: 12) {
                        SectionHeader(title: "Admin Actions")
                        profileLink(icon: "shield.fill", title: "Admin Overview") { AnyView(AdminOverviewView()) }
                    }
                }

                Divider()

                // Logout
                Button(action: { tokenManager.clear() }) {
                    HStack {
                        Image(systemName: "rectangle.portrait.and.arrow.right").foregroundColor(.red)
                        Text("Log Out").foregroundColor(.red).fontWeight(.medium)
                    }
                    .frame(maxWidth: .infinity).padding()
                    .background(Color.red.opacity(0.08)).cornerRadius(12)
                }
            }
            .padding(20)
        }
        .navigationTitle("Profile")
    }

    @ViewBuilder
    private func profileLink<Dest: View>(icon: String, title: String, dest: @escaping () -> Dest) -> some View {
        NavigationLink(destination: dest()) {
            HStack {
                Image(systemName: icon).foregroundColor(.brandGreen).frame(width: 24)
                Text(title).foregroundColor(.primary)
                Spacer()
                Image(systemName: "chevron.right").font(.caption).foregroundColor(.secondary)
            }
            .padding(14)
            .background(Color(.systemBackground))
            .cornerRadius(10)
            .shadow(color: .black.opacity(0.04), radius: 2)
        }
    }
}
