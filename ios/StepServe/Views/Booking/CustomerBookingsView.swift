import SwiftUI

@MainActor
class CustomerBookingsViewModel: ObservableObject {
    @Published var bookings: [CustomerBooking] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    func load() async {
        isLoading = true
        switch await API.customerBookings() {
        case .success(let data): bookings = data
        case .failure(let e):   errorMessage = e
        }
        isLoading = false
    }
}

struct CustomerBookingsView: View {
    @StateObject private var vm = CustomerBookingsViewModel()

    var body: some View {
        Group {
            if vm.isLoading {
                LoadingView()
            } else if vm.bookings.isEmpty {
                EmptyState(icon: "calendar.badge.plus", title: "No bookings yet", message: "Browse services and book your first appointment")
            } else {
                List(vm.bookings) { booking in
                    BookingRow(booking: booking)
                        .listRowSeparator(.hidden)
                        .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("My Bookings")
        .task { await vm.load() }
        .refreshable { await vm.load() }
        .overlay {
            if let err = vm.errorMessage {
                VStack { Spacer(); ErrorBanner(message: err).padding() }
            }
        }
    }
}

struct BookingRow: View {
    let booking: CustomerBooking

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(booking.service_title ?? "Service #\(booking.service_id ?? 0)")
                    .fontWeight(.semibold).lineLimit(1)
                Spacer()
                StatusBadge(status: booking.status)
            }
            HStack(spacing: 16) {
                Label(formatTime(booking.start_time), systemImage: "clock")
                    .font(.caption).foregroundColor(.secondary)
                Label("CAD \(String(format: "%.2f", booking.total_price))", systemImage: "dollarsign.circle")
                    .font(.caption).foregroundColor(.brandGreen)
            }
        }
        .padding(14)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 3)
    }

    private func formatTime(_ iso: String) -> String {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let d = f.date(from: iso) {
            return d.formatted(date: .abbreviated, time: .shortened)
        }
        return String(iso.prefix(16)).replacingOccurrences(of: "T", with: " ")
    }
}
