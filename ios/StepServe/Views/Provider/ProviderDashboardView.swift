import SwiftUI

@MainActor
class ProviderDashboardViewModel: ObservableObject {
    @Published var dashboard: ProviderDashboard?
    @Published var isLoading = false
    @Published var errorMessage: String?

    func load() async {
        isLoading = true
        switch await API.providerDashboard() {
        case .success(let d): dashboard = d
        case .failure(let e): errorMessage = e
        }
        isLoading = false
    }
}

struct ProviderDashboardView: View {
    @StateObject private var vm = ProviderDashboardViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                if vm.isLoading {
                    LoadingView().frame(height: 200)
                } else if let d = vm.dashboard {
                    // Stats row
                    HStack(spacing: 12) {
                        StatCard(title: "Listings", value: "\(d.services.count)", icon: "list.bullet.rectangle")
                        StatCard(title: "Bookings", value: "\(d.bookings.count)", icon: "calendar")
                        StatCard(title: "Documents", value: "\(d.uploads.count)", icon: "doc.fill")
                    }

                    // Quick actions
                    SectionHeader(title: "Quick Actions")
                    HStack(spacing: 12) {
                        NavigationLink(destination: CreateListingView()) {
                            actionCard(icon: "plus.circle.fill", label: "New Listing")
                        }
                        NavigationLink(destination: ProviderListingsView()) {
                            actionCard(icon: "list.bullet", label: "My Listings")
                        }
                        NavigationLink(destination: ProviderDocumentsView()) {
                            actionCard(icon: "doc.badge.plus", label: "Documents")
                        }
                    }

                    // Recent bookings
                    if !d.bookings.isEmpty {
                        SectionHeader(title: "Recent Bookings")
                        ForEach(d.bookings.prefix(5)) { b in
                            providerBookingRow(b)
                        }
                    }
                }

                if let err = vm.errorMessage { ErrorBanner(message: err) }
            }
            .padding(16)
        }
        .navigationTitle("Provider Dashboard")
        .task { await vm.load() }
        .refreshable { await vm.load() }
    }

    @ViewBuilder
    private func actionCard(icon: String, label: String) -> some View {
        VStack(spacing: 8) {
            Image(systemName: icon).font(.title2).foregroundColor(.brandGreen)
            Text(label).font(.caption).fontWeight(.medium).multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity).padding(14)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 3)
        .foregroundColor(.primary)
    }

    @ViewBuilder
    private func providerBookingRow(_ b: ProviderBooking) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(b.service_title ?? "Booking #\(b.id)").fontWeight(.semibold).lineLimit(1)
                Spacer()
                StatusBadge(status: b.status)
            }
            Text(b.customer_email ?? "—").font(.caption).foregroundColor(.secondary)
            Text("CAD \(String(format: "%.2f", b.total_price))").font(.caption).foregroundColor(.brandGreen)
        }
        .padding(14)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 2)
    }
}
