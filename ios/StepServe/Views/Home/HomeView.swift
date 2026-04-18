import SwiftUI

@MainActor
class HomeViewModel: ObservableObject {
    @Published var homeData: HomeData?
    @Published var isLoading = false
    @Published var errorMessage: String?

    func load() async {
        isLoading = true; errorMessage = nil
        switch await API.home() {
        case .success(let data): homeData = data
        case .failure(let e):   errorMessage = e
        }
        isLoading = false
    }
}

struct HomeView: View {
    @StateObject private var vm = HomeViewModel()

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                heroSection
                if vm.isLoading {
                    LoadingView().frame(height: 300)
                } else if let err = vm.errorMessage {
                    ErrorBanner(message: err).padding()
                } else if let data = vm.homeData {
                    contentSection(data)
                }
            }
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { ToolbarItem(placement: .principal) { StepServeLogo() } }
        .toolbarBackground(Color.brandGreen, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .task { await vm.load() }
        .refreshable { await vm.load() }
    }

    // MARK: - Hero
    private var heroSection: some View {
        ZStack(alignment: .bottomLeading) {
            LinearGradient(
                colors: [Color.brandGreen, Color.brandGreenDark],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
            .frame(height: 150)

            VStack(alignment: .leading, spacing: 6) {
                Text("Find Music Teachers")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(.white)
                Text("Book lessons near you in Canada")
                    .font(.system(size: 14))
                    .foregroundColor(.white.opacity(0.85))
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 20)
        }
    }

    // MARK: - Content
    @ViewBuilder
    private func contentSection(_ data: HomeData) -> some View {
        VStack(spacing: 0) {

            // ── Categories ───────────────────────────────────────────────
            if !data.categories.isEmpty {
                VStack(spacing: 12) {
                    SectionHeader(title: "Browse Categories", actionTitle: "See All") {
                        // handled by tab nav
                    }
                    VStack(spacing: 10) {
                        ForEach(data.categories) { cat in
                            NavigationLink(destination: SearchView(initialCategoryId: cat.id, initialCategoryName: cat.name)) {
                                CategoryCard(category: cat, onTap: {})
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 16)
                }
                .padding(.top, 20)
            }

            // ── Featured services ────────────────────────────────────────
            if !data.featured.isEmpty {
                VStack(spacing: 10) {
                    SectionHeader(title: "Featured Services")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(data.featured) { svc in
                                NavigationLink(destination: ServiceDetailView(service: svc)) {
                                    ServiceCardCompact(service: svc)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                }
                .padding(.top, 20)
            }

            // ── Top locations ────────────────────────────────────────────
            if !data.top_locations.isEmpty {
                VStack(spacing: 10) {
                    SectionHeader(title: "Popular Cities")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            ForEach(data.top_locations) { loc in
                                NavigationLink(destination: SearchView(initialLocation: loc.location)) {
                                    HStack(spacing: 6) {
                                        Image(systemName: "mappin.circle.fill")
                                            .foregroundColor(.brandGreen).font(.system(size: 14))
                                        VStack(alignment: .leading, spacing: 1) {
                                            Text(loc.location)
                                                .font(.system(size: 13, weight: .semibold))
                                                .foregroundColor(.brandGreen)
                                                .lineLimit(1)
                                            Text("\(loc.listings_count) listings")
                                                .font(.system(size: 10))
                                                .foregroundColor(.gray500)
                                        }
                                    }
                                    .padding(.horizontal, 14).padding(.vertical, 10)
                                    .background(Color.brandLight)
                                    .cornerRadius(10)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                }
                .padding(.top, 20)
            }

            // ── Latest listings ──────────────────────────────────────────
            if !data.latest.isEmpty {
                VStack(spacing: 10) {
                    SectionHeader(title: "Latest Listings")
                    VStack(spacing: 10) {
                        ForEach(data.latest) { svc in
                            NavigationLink(destination: ServiceDetailView(service: svc)) {
                                ServiceCard(service: svc)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 16)
                }
                .padding(.top, 20)
            }

            Spacer().frame(height: 24)
        }
    }
}
