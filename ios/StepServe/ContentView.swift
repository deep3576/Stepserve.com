import SwiftUI

/// Root view — shows splash briefly, then routes to TabView (logged-in) or AuthView (guest).
struct RootView: View {
    @EnvironmentObject var tokenManager: TokenManager
    @State private var showSplash = true

    var body: some View {
        if showSplash {
            SplashView()
                .onAppear {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                        showSplash = false
                    }
                }
        } else if tokenManager.isLoggedIn {
            MainTabView()
        } else {
            AuthLandingView()
        }
    }
}

// MARK: - Splash
struct SplashView: View {
    var body: some View {
        ZStack {
            Color.brandGreen.ignoresSafeArea()
            VStack(spacing: 12) {
                Image(systemName: "music.note.list")
                    .font(.system(size: 64))
                    .foregroundColor(.white)
                Text("StepServe")
                    .font(.largeTitle).fontWeight(.bold).foregroundColor(.white)
                Text("Music lessons marketplace")
                    .font(.subheadline).foregroundColor(.white.opacity(0.8))
            }
        }
    }
}

// MARK: - Auth landing (guest)
struct AuthLandingView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Hero
                ZStack {
                    Color.brandGreen.ignoresSafeArea()
                    VStack(spacing: 10) {
                        Image(systemName: "music.note.list").font(.system(size: 56)).foregroundColor(.white)
                        Text("StepServe").font(.largeTitle).fontWeight(.bold).foregroundColor(.white)
                        Text("Find music teachers near you").font(.subheadline).foregroundColor(.white.opacity(0.85))
                    }
                    .padding(.top, 60).padding(.bottom, 40)
                }
                .frame(maxHeight: 260)

                VStack(spacing: 16) {
                    NavigationLink(destination: LoginView()) {
                        Text("Log In")
                            .fontWeight(.semibold).frame(maxWidth: .infinity).padding()
                            .background(Color.brandGreen).foregroundColor(.white).cornerRadius(12)
                    }
                    NavigationLink(destination: RegisterView()) {
                        Text("Create Account")
                            .fontWeight(.semibold).frame(maxWidth: .infinity).padding()
                            .background(Color(.systemGray5)).foregroundColor(.primary).cornerRadius(12)
                    }
                    NavigationLink(destination: HomeView()) {
                        Text("Browse as Guest")
                            .font(.subheadline).foregroundColor(.brandGreen)
                    }
                }
                .padding(24)
                Spacer()
            }
            .navigationBarHidden(true)
        }
    }
}

// MARK: - Main tab view (authenticated)
struct MainTabView: View {
    @EnvironmentObject var tokenManager: TokenManager

    var body: some View {
        TabView {
            NavigationStack { HomeView() }
                .tabItem { Label("Home", systemImage: "house") }

            NavigationStack { SearchView() }
                .tabItem { Label("Search", systemImage: "magnifyingglass") }

            if tokenManager.role == "customer" {
                NavigationStack { CustomerBookingsView() }
                    .tabItem { Label("Bookings", systemImage: "calendar") }
            }

            if tokenManager.role == "provider" {
                NavigationStack { ProviderDashboardView() }
                    .tabItem { Label("Dashboard", systemImage: "chart.bar") }
            }

            if tokenManager.role == "admin" {
                NavigationStack { AdminOverviewView() }
                    .tabItem { Label("Admin", systemImage: "shield") }
            }

            NavigationStack { ProfileView() }
                .tabItem { Label("Profile", systemImage: "person") }
        }
        .accentColor(.brandGreen)
    }
}
