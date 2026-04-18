import SwiftUI

@MainActor
class BookingViewModel: ObservableObject {
    @Published var bookingState: BookingState = .idle
    @Published var payState: PayState = .idle
    var bookingId: Int?

    enum BookingState { case idle, loading, success(BookingResponse), error(String) }
    enum PayState     { case idle, loading, success(PaymentResponse), error(String) }

    func createBooking(serviceId: Int, startIso: String, endIso: String) async {
        bookingState = .loading
        switch await API.createBooking(BookingCreate(service_id: serviceId, start_time: startIso, end_time: endIso)) {
        case .success(let b):
            bookingId = b.id
            bookingState = .success(b)
        case .failure(let e):
            bookingState = .error(e)
        }
    }

    func payBooking() async {
        guard let id = bookingId else { return }
        payState = .loading
        switch await API.createPayment(PaymentCreate(booking_id: id)) {
        case .success(let p): payState = .success(p)
        case .failure(let e): payState = .error(e)
        }
    }
}

struct BookingView: View {
    let service: Service
    @StateObject private var vm = BookingViewModel()

    @State private var selectedDate = Calendar.current.date(byAdding: .day, value: 1, to: Date())!
    @State private var startHour = 10
    @State private var durationHours = 1

    private var endHour: Int { min(startHour + durationHours, 23) }
    private var totalPrice: Double { service.price * Double(durationHours) }

    private var startIso: String {
        let fmt = DateFormatter(); fmt.dateFormat = "yyyy-MM-dd"
        return "\(fmt.string(from: selectedDate))T\(String(format: "%02d", startHour)):00:00"
    }
    private var endIso: String {
        let fmt = DateFormatter(); fmt.dateFormat = "yyyy-MM-dd"
        return "\(fmt.string(from: selectedDate))T\(String(format: "%02d", endHour)):00:00"
    }

    private var next7Days: [Date] {
        (1...7).compactMap { Calendar.current.date(byAdding: .day, value: $0, to: Date()) }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Service summary card
                HStack {
                    Image(systemName: "music.note").font(.title2).foregroundColor(.brandGreen)
                    VStack(alignment: .leading) {
                        Text(service.title).fontWeight(.semibold)
                        Text("CAD \(service.price, specifier: "%.0f")/hr").font(.caption).foregroundColor(.brandGreen)
                    }
                    Spacer()
                }
                .padding().background(Color.brandLight).cornerRadius(12)

                // Payment success
                if case .success(let pay) = vm.payState {
                    SuccessBanner(message: "Payment confirmed! Check your email for receipt.")
                    payReceiptCard(pay)
                    return
                }

                // Booking success → show pay button
                if case .success(let booking) = vm.bookingState {
                    SuccessBanner(message: "Booking created! Complete payment to confirm.")
                    bookingSummaryCard(booking)
                    if case .error(let e) = vm.payState { ErrorBanner(message: e) }
                    PrimaryButton(
                        title: vm.payState == .loading ? "Processing…" : "Pay CAD \(String(format: "%.2f", booking.total_price))",
                        action: { Task { await vm.payBooking() } },
                        isLoading: vm.payState == .loading
                    )
                    return
                }

                // Date picker
                VStack(alignment: .leading, spacing: 10) {
                    Text("Select Date").fontWeight(.semibold)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(next7Days, id: \.self) { date in
                                let selected = Calendar.current.isDate(date, inSameDayAs: selectedDate)
                                Button(action: { selectedDate = date }) {
                                    VStack(spacing: 4) {
                                        Text(date.formatted(.dateTime.weekday(.abbreviated))).font(.caption2)
                                        Text(date.formatted(.dateTime.day())).font(.subheadline).fontWeight(.bold)
                                    }
                                    .frame(width: 44, height: 56)
                                    .background(selected ? Color.brandGreen : Color(.systemGray5))
                                    .foregroundColor(selected ? .white : .primary)
                                    .cornerRadius(10)
                                }
                            }
                        }
                    }
                }

                // Start time
                VStack(alignment: .leading, spacing: 10) {
                    Text("Start Time").fontWeight(.semibold)
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 4), spacing: 8) {
                        ForEach(stride(from: 8, through: 20, by: 2).map { $0 }, id: \.self) { hour in
                            Button(action: { startHour = hour }) {
                                Text(String(format: "%02d:00", hour)).font(.caption).fontWeight(.medium)
                                    .padding(.vertical, 8)
                                    .frame(maxWidth: .infinity)
                                    .background(startHour == hour ? Color.brandGreen : Color(.systemGray5))
                                    .foregroundColor(startHour == hour ? .white : .primary)
                                    .cornerRadius(8)
                            }
                        }
                    }
                }

                // Duration
                VStack(alignment: .leading, spacing: 10) {
                    Text("Duration").fontWeight(.semibold)
                    HStack {
                        Button(action: { if durationHours > 1 { durationHours -= 1 } }) {
                            Image(systemName: "minus.circle.fill").font(.title2).foregroundColor(.brandGreen)
                        }
                        Text("\(durationHours) hr\(durationHours > 1 ? "s" : "")")
                            .font(.title3).fontWeight(.bold).frame(width: 80, alignment: .center)
                        Button(action: { if durationHours < 8 { durationHours += 1 } }) {
                            Image(systemName: "plus.circle.fill").font(.title2).foregroundColor(.brandGreen)
                        }
                    }
                }

                // Summary card
                VStack(spacing: 8) {
                    Text("Booking Summary").fontWeight(.bold).frame(maxWidth: .infinity, alignment: .leading)
                    Divider()
                    summaryRow("Date", selectedDate.formatted(date: .abbreviated, time: .omitted))
                    summaryRow("Time", String(format: "%02d:00 – %02d:00", startHour, endHour))
                    summaryRow("Duration", "\(durationHours) hr\(durationHours > 1 ? "s" : "")")
                    summaryRow("Rate", "CAD \(String(format: "%.0f", service.price))/hr")
                    Divider()
                    HStack {
                        Text("Total").fontWeight(.bold)
                        Spacer()
                        Text("CAD \(String(format: "%.2f", totalPrice))").fontWeight(.bold).foregroundColor(.brandGreen)
                    }
                }
                .padding().background(Color(.systemGray6)).cornerRadius(12)

                if case .error(let e) = vm.bookingState { ErrorBanner(message: e) }

                PrimaryButton(
                    title: vm.bookingState == .loading ? "Creating booking…" : "Continue to Payment",
                    action: { Task { await vm.createBooking(serviceId: service.id, startIso: startIso, endIso: endIso) } },
                    isLoading: vm.bookingState == .loading
                )
            }
            .padding(20)
        }
        .navigationTitle("Book Session")
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func summaryRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label).foregroundColor(.secondary).font(.subheadline)
            Spacer()
            Text(value).font(.subheadline)
        }
    }

    @ViewBuilder
    private func bookingSummaryCard(_ booking: BookingResponse) -> some View {
        VStack(spacing: 8) {
            summaryRow("Booking #", String(format: "BK-%06d", booking.id))
            summaryRow("Total", "CAD \(String(format: "%.2f", booking.total_price))")
            summaryRow("Status", booking.status.uppercased())
        }
        .padding().background(Color(.systemGray6)).cornerRadius(12)
    }

    @ViewBuilder
    private func payReceiptCard(_ pay: PaymentResponse) -> some View {
        VStack(spacing: 8) {
            summaryRow("Receipt #", String(format: "RCP-%06d", pay.id))
            summaryRow("Amount", "CAD \(String(format: "%.2f", pay.amount))")
            summaryRow("Status", pay.status.uppercased())
            if let ref = pay.stripe_payment_intent_id { summaryRow("Reference", ref) }
        }
        .padding().background(Color(.systemGray6)).cornerRadius(12)
    }
}

// Equatable conformance for state enum comparison
extension BookingViewModel.PayState: Equatable {
    static func == (lhs: BookingViewModel.PayState, rhs: BookingViewModel.PayState) -> Bool {
        switch (lhs, rhs) {
        case (.idle, .idle), (.loading, .loading): return true
        default: return false
        }
    }
}

extension BookingViewModel.BookingState: Equatable {
    static func == (lhs: BookingViewModel.BookingState, rhs: BookingViewModel.BookingState) -> Bool {
        switch (lhs, rhs) {
        case (.idle, .idle), (.loading, .loading): return true
        default: return false
        }
    }
}
