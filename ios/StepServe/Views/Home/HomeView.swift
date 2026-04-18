import SwiftUI

@MainActor
class HomeViewModel: ObservableObject {
    @Published var homeData: HomeData?
    @Published var isLoading = false
    @Published var errorMessage: String?

    func load() async {
        isLoading = true
        switch await API.home() {
        case .success(let data): homeData = data
        case .failure(let e):   errorMessage = e
        }
        isLoading = false
    }
}

struct HomeView: View {
    @StateObject private var vm = HomeViewModel()
    @EnvironmentObject var tokenManager: TokenManager

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Hero header
                ZStack(alignment: .bottomLeading) {
                    Color.brandGreen.frame(height: 160)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Find Music Teachers").font(.title2).fontWeight(.bold).foregroundColor(.white)
                        Text("Book lessons near you").font(.subheadline).foregroundColor(.white.opacity(0.85))
                    }
                    .padding(20)
                }

                if vm.isLoading {
                    LoadingView().frame(height: 300)
                } else if let data = vm.homeData {
                    VStack(alignment: .leading, spacing: 24) {
                        // Categories
                        if !data.categories.isEmpty {
                            SectionHeader(title: "Browse Categories")
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 10) {
                                    ForEach(data.categories) { cat in
                                        NavigationLink(destination: SearchView(initialCategoryId: cat.id, initialCategoryName: cat.name)) {
                                            VStack(spacing: 6) {
                                                Image(systemName: "music.note")
                                                    .font(.title2).foregroundColor(.brandGreen)
                                                    .frame(width: 52, height: 52)
                                                    .background(Color.brandLight)
                                                    .cornerRadius(14)
                                                Text(cat.name).font(.caption).lineLimit(1)
                                            }
                                            .frame(width: 70)
                                        }
                                        .foregroundColor(.primary)
                                    }
                                }
                                .padding(.horizontal)
                            }
                        }

                        // Featured
                        if !data.featured.isEmpty {
                            SectionHeader(title: "Featured Services").padding(.horizontal)
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 12) {
                                    ForEach(data.featured) { svc in
                                        NavigationLink(destination: ServiceDetailView(service: svc)) {
                                            ServiceCard(service: svc).frame(width: 260)
                                        }
                                        .foregroundColor(.primary)
                                    }
                                }
                                .padding(.horizontal)
                            }
                        }

                        // Top Locations
                        if !data.top_locations.isEmpty {
                            SectionHeader(title: "Popular Cities").padding(.horizontal)
                            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                                ForEach(data.top_locations) { loc in
                                    NavigationLink(destination: SearchView(initialLocation: loc.location)) {
                                        HStack {
                                            Image(systemName: "mappin.circle.fill").foregroundColor(.brandGreen)
                                            VStack(alignment: .leading) {
                                                Text(loc.location).font(.caption).fontWeight(.semibold).lineLimit(1)
                                                Text("\(loc.listings_count) listings").font(.caption2).foregroundColor(.secondary)
                                            }
                                            Spacer()
                                        }
                                        .padding(10)
                                        .background(Color(.systemBackground))
                                        .cornerRadius(10)
                                        .shadow(color: .black.opacity(0.05), radius: 2)
                                    }
                                    .foregroundColor(.primary)
                                }
                            }
                            .padding(.horizontal)
                        }

                        // Latest
                        if !data.latest.isEmpty {
                            SectionHeader(title: "Latest Listings").padding(.horizontal)
                            VStack(spacing: 10) {
                                ForEach(data.latest) { svc in
                                    NavigationLink(destination: ServiceDetailView(service: svc)) {
                                        ServiceCard(service: svc)
                                    }
                                    .foregroundColor(.primary)
                                }
                            }
                            .padding(.horizontal)
                        }
                    }
                    .padding(.vertical, 20)
                }

                if let err = vm.errorMessage {
                    ErrorBanner(message: err).padding()
                }
            }
        }
        .navigationTitle("StepServe")
        .navigationBarTitleDisplayMode(.large)
        .task { await vm.load() }
        .refreshable { await vm.load() }
    }
}
