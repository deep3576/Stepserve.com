import SwiftUI

@MainActor
class SearchViewModel: ObservableObject {
    @Published var results: [Service] = []
    @Published var categories: [Category] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    @Published var query = ""
    @Published var selectedCategoryId: Int? = nil
    @Published var location = ""
    @Published var minPrice = ""
    @Published var maxPrice = ""

    func loadCategories() async {
        if case .success(let cats) = await API.categories() {
            categories = cats
        }
    }

    func search() async {
        isLoading = true; errorMessage = nil
        switch await API.searchServices(
            query: query, categoryId: selectedCategoryId,
            location: location, minPrice: minPrice, maxPrice: maxPrice
        ) {
        case .success(let data): results = data
        case .failure(let e):   errorMessage = e
        }
        isLoading = false
    }
}

struct SearchView: View {
    @StateObject private var vm = SearchViewModel()
    var initialCategoryId: Int? = nil
    var initialCategoryName: String? = nil
    var initialLocation: String? = nil
    @State private var showFilters = false

    var body: some View {
        VStack(spacing: 0) {
            // Search bar
            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass").foregroundColor(.secondary)
                TextField("Search services…", text: $vm.query)
                    .autocapitalization(.none)
                    .onSubmit { Task { await vm.search() } }
                Button(action: { showFilters.toggle() }) {
                    Image(systemName: "slider.horizontal.3")
                        .foregroundColor(vm.selectedCategoryId != nil || !vm.location.isEmpty ? .brandGreen : .secondary)
                }
            }
            .padding(12)
            .background(Color(.systemGray6))
            .cornerRadius(12)
            .padding(.horizontal).padding(.top, 8)

            // Category chips
            if !vm.categories.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        CategoryChip(
                            category: Category(id: 0, name: "All", slug: "all"),
                            selected: vm.selectedCategoryId == nil,
                            onTap: { vm.selectedCategoryId = nil; Task { await vm.search() } }
                        )
                        ForEach(vm.categories) { cat in
                            CategoryChip(
                                category: cat,
                                selected: vm.selectedCategoryId == cat.id,
                                onTap: {
                                    vm.selectedCategoryId = (vm.selectedCategoryId == cat.id) ? nil : cat.id
                                    Task { await vm.search() }
                                }
                            )
                        }
                    }
                    .padding(.horizontal).padding(.vertical, 8)
                }
            }

            Divider()

            if vm.isLoading {
                LoadingView()
            } else if vm.results.isEmpty {
                EmptyState(icon: "music.note.list", title: "No services found", message: "Try different search terms or filters")
            } else {
                List(vm.results) { svc in
                    NavigationLink(destination: ServiceDetailView(service: svc)) {
                        ServiceCard(service: svc)
                    }
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                }
                .listStyle(.plain)
            }

            if let err = vm.errorMessage {
                ErrorBanner(message: err).padding()
            }
        }
        .navigationTitle("Search")
        .sheet(isPresented: $showFilters) {
            FilterSheet(vm: vm)
        }
        .task {
            await vm.loadCategories()
            if let cat = initialCategoryId { vm.selectedCategoryId = cat }
            if let loc = initialLocation   { vm.location = loc }
            await vm.search()
        }
    }
}

struct FilterSheet: View {
    @ObservedObject var vm: SearchViewModel
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section("Location") {
                    TextField("City or province", text: $vm.location)
                }
                Section("Price Range (CAD/hr)") {
                    HStack {
                        TextField("Min", text: $vm.minPrice).keyboardType(.numberPad)
                        Text("–")
                        TextField("Max", text: $vm.maxPrice).keyboardType(.numberPad)
                    }
                }
            }
            .navigationTitle("Filters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Apply") { dismiss(); Task { await vm.search() } }
                }
                ToolbarItem(placement: .cancellationAction) {
                    Button("Reset") {
                        vm.location = ""; vm.minPrice = ""; vm.maxPrice = ""
                        dismiss(); Task { await vm.search() }
                    }
                }
            }
        }
    }
}
