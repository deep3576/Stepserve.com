import SwiftUI

@MainActor
class AdminBookingsViewModel: ObservableObject {
    @Published var bookings: [AdminBooking] = []
    @Published var filtered: [AdminBooking] = []
    @Published var searchText = ""
    @Published var isLoading = false
    @Published var errorMessage: String?

    func load() async {
        isLoading = true
        switch await API.adminBookings() {
        case .success(let data):
            bookings = data
            filtered = data
        case .failure(let e):
            errorMessage = e
        }
        isLoading = false
    }

    func filter() {
        if searchText.isEmpty { filtered = bookings }
        else {
            filtered = bookings.filter {
                ($0.customer_email ?? "").localizedCaseInsensitiveContains(searchText) ||
                ($0.service_title ?? "").localizedCaseInsensitiveContains(searchText)
            }
        }
    }
}

struct AdminBookingsView: View {
    @StateObject private var vm = AdminBookingsViewModel()

    var body: some View {
        Group {
            if vm.isLoading { LoadingView() }
            else if vm.filtered.isEmpty {
                EmptyState(icon: "calendar", title: "No bookings", message: "No bookings found")
            } else {
                List(vm.filtered) { booking in
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(booking.service_title ?? "Booking #\(booking.id)")
                                .fontWeight(.semibold).lineLimit(1)
                            Spacer()
                            StatusBadge(status: booking.status)
                        }
                        Text(booking.customer_email ?? "—").font(.caption).foregroundColor(.secondary)
                        HStack {
                            Label(shortDate(booking.start_time), systemImage: "clock").font(.caption).foregroundColor(.secondary)
                            Spacer()
                            Text("CAD \(String(format: "%.2f", booking.total_price))").font(.caption).fontWeight(.semibold).foregroundColor(.brandGreen)
                        }
                    }
                    .padding(.vertical, 4)
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("All Bookings")
        .searchable(text: $vm.searchText, prompt: "Search by email or service")
        .onChange(of: vm.searchText) { _ in vm.filter() }
        .task { await vm.load() }
        .refreshable { await vm.load() }
        .overlay {
            if let err = vm.errorMessage {
                VStack { Spacer(); ErrorBanner(message: err).padding() }
            }
        }
    }

    private func shortDate(_ iso: String) -> String {
        String(iso.prefix(16)).replacingOccurrences(of: "T", with: " ")
    }
}
