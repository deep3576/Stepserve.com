import SwiftUI
import UniformTypeIdentifiers

@MainActor
class ProviderDocumentsViewModel: ObservableObject {
    @Published var uploads: [ProviderUpload] = []
    @Published var isLoading = false
    @Published var isUploading = false
    @Published var errorMessage: String?
    @Published var successMessage: String?

    func load() async {
        isLoading = true
        switch await API.providerUploads() {
        case .success(let data): uploads = data
        case .failure(let e):   errorMessage = e
        }
        isLoading = false
    }

    func upload(data: Data, fileName: String, mimeType: String) async {
        isUploading = true; errorMessage = nil
        switch await API.uploadDocument(data: data, fileName: fileName, mimeType: mimeType) {
        case .success(let u):
            uploads.insert(u, at: 0)
            successMessage = "'\(u.file_name)' uploaded successfully!"
        case .failure(let e):
            errorMessage = e
        }
        isUploading = false
    }
}

struct ProviderDocumentsView: View {
    @StateObject private var vm = ProviderDocumentsViewModel()
    @State private var showFilePicker = false

    var body: some View {
        Group {
            if vm.isLoading { LoadingView() }
            else {
                List {
                    if let msg = vm.successMessage {
                        SuccessBanner(message: msg).listRowSeparator(.hidden)
                    }
                    if let err = vm.errorMessage {
                        ErrorBanner(message: err).listRowSeparator(.hidden)
                    }
                    if vm.uploads.isEmpty {
                        EmptyState(icon: "doc.badge.plus", title: "No documents", message: "Upload certifications or insurance documents")
                            .listRowSeparator(.hidden)
                    } else {
                        ForEach(vm.uploads) { upload in
                            HStack {
                                Image(systemName: iconFor(upload.content_type)).font(.title2).foregroundColor(.brandGreen)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(upload.file_name).font(.subheadline).fontWeight(.medium).lineLimit(1)
                                    if let size = upload.file_size {
                                        Text(formatSize(size)).font(.caption).foregroundColor(.secondary)
                                    }
                                }
                                Spacer()
                                Image(systemName: "checkmark.circle.fill").foregroundColor(.green)
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("Documents")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                if vm.isUploading {
                    ProgressView()
                } else {
                    Button(action: { showFilePicker = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
        }
        .fileImporter(isPresented: $showFilePicker,
                      allowedContentTypes: [.pdf, .jpeg, .png],
                      allowsMultipleSelection: false) { result in
            handleFileSelection(result)
        }
        .task { await vm.load() }
        .refreshable { await vm.load() }
    }

    private func handleFileSelection(_ result: Result<[URL], Error>) {
        guard case .success(let urls) = result, let url = urls.first else { return }
        guard url.startAccessingSecurityScopedResource() else { return }
        defer { url.stopAccessingSecurityScopedResource() }
        guard let data = try? Data(contentsOf: url) else { return }
        let mime = url.pathExtension.lowercased() == "pdf" ? "application/pdf" :
                   url.pathExtension.lowercased() == "png" ? "image/png" : "image/jpeg"
        Task { await vm.upload(data: data, fileName: url.lastPathComponent, mimeType: mime) }
    }

    private func iconFor(_ type: String?) -> String {
        guard let t = type else { return "doc.fill" }
        if t.contains("pdf") { return "doc.fill" }
        if t.contains("image") { return "photo.fill" }
        return "doc.fill"
    }

    private func formatSize(_ bytes: Int) -> String {
        let kb = Double(bytes) / 1024
        if kb < 1024 { return String(format: "%.1f KB", kb) }
        return String(format: "%.1f MB", kb / 1024)
    }
}
