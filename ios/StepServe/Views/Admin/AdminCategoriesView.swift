import SwiftUI

@MainActor
class AdminCategoriesViewModel: ObservableObject {
    @Published var categories: [Category] = []
    @Published var isLoading = false
    @Published var isCreating = false
    @Published var newName = ""
    @Published var newSlug = ""
    @Published var errorMessage: String?
    @Published var showCreateSheet = false

    func load() async {
        isLoading = true
        switch await API.categories() {
        case .success(let data): categories = data
        case .failure(let e):   errorMessage = e
        }
        isLoading = false
    }

    func create() async {
        guard !newName.isEmpty, !newSlug.isEmpty else { errorMessage = "Name and slug required"; return }
        isCreating = true
        switch await API.createCategory(CategoryCreate(name: newName, slug: newSlug)) {
        case .success(let cat):
            categories.append(cat)
            newName = ""; newSlug = ""
            showCreateSheet = false
        case .failure(let e):
            errorMessage = e
        }
        isCreating = false
    }

    func autoSlug() {
        newSlug = newName.lowercased()
            .replacingOccurrences(of: " ", with: "-")
            .filter { $0.isLetter || $0.isNumber || $0 == "-" }
    }
}

struct AdminCategoriesView: View {
    @StateObject private var vm = AdminCategoriesViewModel()

    var body: some View {
        Group {
            if vm.isLoading { LoadingView() }
            else {
                List {
                    if let err = vm.errorMessage {
                        ErrorBanner(message: err).listRowSeparator(.hidden)
                    }
                    ForEach(vm.categories) { cat in
                        HStack {
                            Image(systemName: "wrench.and.screwdriver.fill").foregroundColor(.brandGreen)
                            VStack(alignment: .leading) {
                                Text(cat.name).fontWeight(.medium)
                                Text(cat.slug).font(.caption).foregroundColor(.secondary)
                            }
                            Spacer()
                            if let count = cat.services_count {
                                Text("\(count) services").font(.caption).foregroundColor(.secondary)
                            }
                        }
                        .padding(.vertical, 2)
                    }
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("Categories")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(action: { vm.showCreateSheet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $vm.showCreateSheet) {
            createSheet
        }
        .task { await vm.load() }
        .refreshable { await vm.load() }
    }

    private var createSheet: some View {
        NavigationStack {
            Form {
                Section("New Category") {
                    TextField("Name (e.g. Guitar)", text: $vm.newName)
                        .onChange(of: vm.newName) { _ in vm.autoSlug() }
                    TextField("Slug (e.g. guitar)", text: $vm.newSlug)
                        .autocapitalization(.none)
                }
                if let err = vm.errorMessage {
                    Section { ErrorBanner(message: err) }
                }
            }
            .navigationTitle("Create Category")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    if vm.isCreating { ProgressView() }
                    else { Button("Create") { Task { await vm.create() } } }
                }
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { vm.showCreateSheet = false }
                }
            }
        }
    }
}
