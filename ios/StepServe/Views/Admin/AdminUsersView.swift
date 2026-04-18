import SwiftUI

@MainActor
class AdminUsersViewModel: ObservableObject {
    @Published var users: [AdminUser] = []
    @Published var filtered: [AdminUser] = []
    @Published var searchText = ""
    @Published var isLoading = false
    @Published var errorMessage: String?

    func load() async {
        isLoading = true
        switch await API.adminUsers() {
        case .success(let data):
            users = data
            filtered = data
        case .failure(let e):
            errorMessage = e
        }
        isLoading = false
    }

    func filter() {
        if searchText.isEmpty { filtered = users }
        else { filtered = users.filter { $0.email.localizedCaseInsensitiveContains(searchText) } }
    }

    func toggleStatus(_ user: AdminUser) async {
        let newActive = user.is_active == 0
        switch await API.adminSetUserStatus(userId: user.id, active: newActive) {
        case .success: await load()
        case .failure(let e): errorMessage = e
        }
    }
}

struct AdminUsersView: View {
    @StateObject private var vm = AdminUsersViewModel()

    var body: some View {
        Group {
            if vm.isLoading { LoadingView() }
            else {
                List {
                    if let err = vm.errorMessage {
                        ErrorBanner(message: err).listRowSeparator(.hidden)
                    }
                    ForEach(vm.filtered) { user in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(user.email).font(.subheadline).fontWeight(.medium).lineLimit(1)
                                HStack(spacing: 8) {
                                    Text(user.role.capitalized).font(.caption).foregroundColor(.secondary)
                                    StatusBadge(status: user.is_active == 1 ? "active" : "inactive")
                                }
                            }
                            Spacer()
                            Button(action: { Task { await vm.toggleStatus(user) } }) {
                                Text(user.is_active == 1 ? "Deactivate" : "Activate")
                                    .font(.caption).fontWeight(.medium)
                                    .padding(.horizontal, 10).padding(.vertical, 5)
                                    .background(user.is_active == 1 ? Color.red.opacity(0.1) : Color.green.opacity(0.1))
                                    .foregroundColor(user.is_active == 1 ? .red : .green)
                                    .cornerRadius(8)
                            }
                            .buttonStyle(.plain)
                        }
                        .padding(.vertical, 4)
                    }
                }
                .listStyle(.plain)
                .searchable(text: $vm.searchText, prompt: "Search by email")
                .onChange(of: vm.searchText) { _ in vm.filter() }
            }
        }
        .navigationTitle("Users")
        .task { await vm.load() }
        .refreshable { await vm.load() }
    }
}
