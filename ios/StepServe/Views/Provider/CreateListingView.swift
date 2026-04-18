import SwiftUI

@MainActor
class CreateListingViewModel: ObservableObject {
    @Published var categories: [Category] = []
    @Published var selectedCategoryId: Int = 0
    @Published var title = ""
    @Published var description = ""
    @Published var price = ""
    @Published var state: CreateState = .idle
    @Published var serviceId: Int?

    enum CreateState { case idle, loading, success, payLoading, paySuccess, error(String) }

    func loadCategories() async {
        if case .success(let cats) = await API.categories() {
            categories = cats
        }
    }

    func create() async {
        guard !title.isEmpty, let priceVal = Double(price), selectedCategoryId != 0 else {
            state = .error("Please fill in all required fields"); return
        }
        state = .loading
        switch await API.createService(ServiceCreate(
            category_id: selectedCategoryId, title: title, description: description, price: priceVal
        )) {
        case .success(let svc):
            serviceId = svc.id
            state = .success
        case .failure(let e):
            state = .error(e)
        }
    }

    func payListing() async {
        guard let id = serviceId else { return }
        state = .payLoading
        switch await API.payListing(serviceId: id) {
        case .success: state = .paySuccess
        case .failure(let e): state = .error(e)
        }
    }
}

struct CreateListingView: View {
    @StateObject private var vm = CreateListingViewModel()
    @Environment(\.dismiss) var dismiss

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                VStack(spacing: 4) {
                    Text("List your service").font(.title3).fontWeight(.bold).frame(maxWidth: .infinity, alignment: .leading)
                    Text("A one-time CAD $5 fee activates your listing").font(.caption).foregroundColor(.secondary).frame(maxWidth: .infinity, alignment: .leading)
                }

                // Pay success
                if case .paySuccess = vm.state {
                    SuccessBanner(message: "Payment done! Your listing is now live.")
                    PrimaryButton(title: "Go to My Listings", action: { dismiss() })
                    return
                }

                // Created — show pay button
                if case .success = vm.state {
                    SuccessBanner(message: "Listing created! Pay CAD $5 to make it live.")
                    PrimaryButton(
                        title: vm.state == .payLoading ? "Processing payment…" : "Pay CAD $5 to Activate",
                        action: { Task { await vm.payListing() } },
                        isLoading: vm.state == .payLoading
                    )
                    return
                }

                if case .error(let e) = vm.state { ErrorBanner(message: e) }

                // Category picker
                VStack(alignment: .leading, spacing: 6) {
                    Text("Category *").font(.subheadline).fontWeight(.medium)
                    Picker("Category", selection: $vm.selectedCategoryId) {
                        Text("Select a category").tag(0)
                        ForEach(vm.categories) { cat in
                            Text(cat.name).tag(cat.id)
                        }
                    }
                    .pickerStyle(.menu)
                    .padding().background(Color(.systemGray6)).cornerRadius(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }

                // Title
                VStack(alignment: .leading, spacing: 6) {
                    Text("Title *").font(.subheadline).fontWeight(.medium)
                    TextField("e.g. Piano Lessons for Beginners", text: $vm.title)
                        .padding().background(Color(.systemGray6)).cornerRadius(10)
                }

                // Price
                VStack(alignment: .leading, spacing: 6) {
                    Text("Hourly Rate (CAD) *").font(.subheadline).fontWeight(.medium)
                    HStack {
                        Text("$").foregroundColor(.secondary)
                        TextField("0.00", text: $vm.price).keyboardType(.decimalPad)
                    }
                    .padding().background(Color(.systemGray6)).cornerRadius(10)
                }

                // Description
                VStack(alignment: .leading, spacing: 6) {
                    Text("Description").font(.subheadline).fontWeight(.medium)
                    TextEditor(text: $vm.description)
                        .frame(minHeight: 100)
                        .padding(8).background(Color(.systemGray6)).cornerRadius(10)
                }

                PrimaryButton(
                    title: vm.state == .loading ? "Creating…" : "Create Listing",
                    action: { Task { await vm.create() } },
                    isLoading: vm.state == .loading
                )
            }
            .padding(20)
        }
        .navigationTitle("New Listing")
        .navigationBarTitleDisplayMode(.inline)
        .task { await vm.loadCategories() }
    }
}

// Equatable for state
extension CreateListingViewModel.CreateState: Equatable {
    static func == (lhs: CreateListingViewModel.CreateState, rhs: CreateListingViewModel.CreateState) -> Bool {
        switch (lhs, rhs) {
        case (.idle, .idle), (.loading, .loading), (.success, .success),
             (.payLoading, .payLoading), (.paySuccess, .paySuccess): return true
        default: return false
        }
    }
}
