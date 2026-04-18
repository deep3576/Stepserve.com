import SwiftUI

@MainActor
class ProviderListingsViewModel: ObservableObject {
    @Published var listings: [Service] = []
    @Published var isLoading = false
    @Published var payingId: Int?
    @Published var errorMessage: String?
    @Published var successMessage: String?

    func load() async {
        isLoading = true
        switch await API.providerListings() {
        case .success(let data): listings = data
        case .failure(let e):   errorMessage = e
        }
        isLoading = false
    }

    func payListing(_ id: Int) async {
        payingId = id
        switch await API.payListing(serviceId: id) {
        case .success: successMessage = "Listing activated!"; await load()
        case .failure(let e): errorMessage = e
        }
        payingId = nil
    }

    func deleteListing(_ id: Int) async {
        switch await API.deleteService(id: id) {
        case .success: await load()
        case .failure(let e): errorMessage = e
        }
    }
}

struct ProviderListingsView: View {
    @StateObject private var vm = ProviderListingsViewModel()

    var body: some View {
        Group {
            if vm.isLoading { LoadingView() }
            else if vm.listings.isEmpty {
                EmptyState(icon: "list.bullet.rectangle", title: "No listings", message: "Create your first service listing")
            } else {
                List {
                    if let msg = vm.successMessage {
                        SuccessBanner(message: msg).listRowSeparator(.hidden)
                    }
                    if let err = vm.errorMessage {
                        ErrorBanner(message: err).listRowSeparator(.hidden)
                    }
                    ForEach(vm.listings) { svc in
                        listingRow(svc)
                            .listRowSeparator(.hidden)
                            .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                    }
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("My Listings")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                NavigationLink(destination: CreateListingView()) {
                    Image(systemName: "plus")
                }
            }
        }
        .task { await vm.load() }
        .refreshable { await vm.load() }
    }

    @ViewBuilder
    private func listingRow(_ svc: Service) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(svc.title).fontWeight(.semibold).lineLimit(1)
                Spacer()
                if svc.is_active == true {
                    StatusBadge(status: "active")
                } else {
                    StatusBadge(status: "draft")
                }
            }
            HStack {
                Text("CAD \(String(format: "%.0f", svc.price))/hr").font(.caption).foregroundColor(.brandGreen)
                Spacer()
                Text(svc.payment_status?.uppercased() ?? "PENDING").font(.caption2).foregroundColor(.secondary)
            }
            if svc.payment_status != "paid" {
                PrimaryButton(
                    title: vm.payingId == svc.id ? "Processing…" : "Pay CAD 5 to Activate",
                    action: { Task { await vm.payListing(svc.id) } },
                    isLoading: vm.payingId == svc.id
                )
            }
        }
        .padding(14)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 3)
        .swipeActions(edge: .trailing) {
            Button(role: .destructive) { Task { await vm.deleteListing(svc.id) } } label: {
                Label("Deactivate", systemImage: "trash")
            }
        }
    }
}
