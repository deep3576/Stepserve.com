import SwiftUI

@MainActor
class AdminOverviewViewModel: ObservableObject {
    @Published var overview: AdminOverview?
    @Published var isLoading = false
    @Published var errorMessage: String?

    func load() async {
        isLoading = true
        switch await API.adminOverview() {
        case .success(let d): overview = d
        case .failure(let e): errorMessage = e
        }
        isLoading = false
    }
}

struct AdminOverviewView: View {
    @StateObject private var vm = AdminOverviewViewModel()

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                if vm.isLoading { LoadingView().frame(height: 200) }
                else if let o = vm.overview {
                    // Stats grid
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        StatCard(title: "Users", value: "\(o.users_count)", icon: "person.3.fill")
                        StatCard(title: "Services", value: "\(o.services_count)", icon: "list.bullet.rectangle")
                        StatCard(title: "Bookings", value: "\(o.bookings_count)", icon: "calendar")
                        StatCard(title: "Revenue", value: "CAD \(String(format: "%.0f", o.paid_total))", icon: "dollarsign.circle.fill")
                    }
                }

                if let err = vm.errorMessage { ErrorBanner(message: err) }

                // Navigation cards
                SectionHeader(title: "Manage")
                VStack(spacing: 12) {
                    adminNavCard(icon: "person.3.fill", title: "Users", subtitle: "View and manage user accounts") { AnyView(AdminUsersView()) }
                    adminNavCard(icon: "calendar", title: "Bookings", subtitle: "All platform bookings") { AnyView(AdminBookingsView()) }
                    adminNavCard(icon: "tag.fill", title: "Categories", subtitle: "Manage service categories") { AnyView(AdminCategoriesView()) }
                }
            }
            .padding(16)
        }
        .navigationTitle("Admin")
        .task { await vm.load() }
        .refreshable { await vm.load() }
    }

    @ViewBuilder
    private func adminNavCard<Dest: View>(icon: String, title: String, subtitle: String, dest: @escaping () -> Dest) -> some View {
        NavigationLink(destination: dest()) {
            HStack(spacing: 14) {
                Image(systemName: icon).font(.title2).foregroundColor(.brandGreen).frame(width: 36)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).fontWeight(.semibold).foregroundColor(.primary)
                    Text(subtitle).font(.caption).foregroundColor(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right").font(.caption).foregroundColor(.secondary)
            }
            .padding(14)
            .background(Color(.systemBackground))
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.05), radius: 3)
        }
    }
}
