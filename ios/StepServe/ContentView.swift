import SwiftUI

/// Root view — shows splash briefly, then routes to TabView or Auth.
struct RootView: View {
    @EnvironmentObject var tokenManager: TokenManager
    @State private var showSplash = true

    var body: some View {
        Group {
            if showSplash {
                SplashView()
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
                            withAnimation(.easeOut(duration: 0.3)) { showSplash = false }
                        }
                    }
            } else if tokenManager.isLoggedIn {
                MainTabView()
            } else {
                AuthLandingView()
            }
        }
    }
}

// MARK: - Splash
struct SplashView: View {
    @State private var scale: CGFloat = 0.8
    @State private var opacity: Double = 0

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color.brandGreen, Color.brandGreenDark],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 20) {
                // Big logo icon
                ZStack {
                    Circle()
                        .fill(Color.white.opacity(0.15))
                        .frame(width: 110, height: 110)
                    Image(systemName: "house.fill")
                        .font(.system(size: 52, weight: .bold))
                        .foregroundColor(.white)
                }

                VStack(spacing: 6) {
                    Text("StepServe")
                        .font(.system(size: 36, weight: .heavy))
                        .foregroundColor(.white)
                        .tracking(-0.5)
                    Text("Local Services Marketplace")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.white.opacity(0.8))
                        .tracking(0.3)
                }
            }
            .scaleEffect(scale)
            .opacity(opacity)
            .onAppear {
                withAnimation(.spring(response: 0.5, dampingFraction: 0.7)) {
                    scale = 1.0
                    opacity = 1.0
                }
            }
        }
    }
}

// MARK: - Auth landing (guest)
struct AuthLandingView: View {
    var body: some View {
        NavigationStack {
            ZStack {
                Color(.systemGroupedBackground).ignoresSafeArea()

                VStack(spacing: 0) {
                    // Green header with logo
                    ZStack(alignment: .bottomLeading) {
                        LinearGradient(
                            colors: [Color.brandGreen, Color.brandGreenDark],
                            startPoint: .topLeading, endPoint: .bottomTrailing
                        )
                        .frame(height: 280)
                        .ignoresSafeArea(edges: .top)

                        VStack(alignment: .leading, spacing: 12) {
                            StepServeLogo(size: 26)
                            Spacer().frame(height: 8)
                            Text("Connect with trusted local\nservice pros near you")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(.white.opacity(0.9))
                                .lineSpacing(4)
                        }
                        .padding(.horizontal, 24)
                        .padding(.bottom, 30)
                    }

                    // Action buttons
                    VStack(spacing: 14) {
                        NavigationLink(destination: LoginView()) {
                            Text("Log In")
                                .font(.system(size: 16, weight: .semibold))
                                .frame(maxWidth: .infinity).frame(height: 52)
                                .background(Color.brandGreen).foregroundColor(.white)
                                .cornerRadius(12)
                                .shadow(color: Color.brandGreen.opacity(0.3), radius: 4, y: 2)
                        }

                        NavigationLink(destination: RegisterView()) {
                            Text("Create Account")
                                .font(.system(size: 16, weight: .semibold))
                                .frame(maxWidth: .infinity).frame(height: 52)
                                .background(Color(.systemBackground))
                                .foregroundColor(Color.brandGreen)
                                .cornerRadius(12)
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.brandGreen, lineWidth: 1.5))
                        }

                        NavigationLink(destination: HomeViewWrapper()) {
                            Text("Browse as Guest →")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(.gray)
                        }
                        .padding(.top, 4)
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 28)

                    Spacer()
                }
            }
            .navigationBarHidden(true)
        }
    }
}

// Wrapper to satisfy NavigationLink (HomeView needs @EnvironmentObject)
struct HomeViewWrapper: View {
    var body: some View { HomeView() }
}

// MARK: - Main tab view (authenticated)
struct MainTabView: View {
    @EnvironmentObject var tokenManager: TokenManager

    var body: some View {
        TabView {
            NavigationStack { HomeView() }
                .tabItem { Label("Home", systemImage: "house.fill") }

            NavigationStack { SearchView() }
                .tabItem { Label("Search", systemImage: "magnifyingglass") }

            if tokenManager.role == "customer" {
                NavigationStack { CustomerBookingsView() }
                    .tabItem { Label("Bookings", systemImage: "calendar") }
            }

            if tokenManager.role == "provider" {
                NavigationStack { ProviderDashboardView() }
                    .tabItem { Label("Dashboard", systemImage: "chart.bar.fill") }
            }

            if tokenManager.role == "admin" {
                NavigationStack { AdminOverviewView() }
                    .tabItem { Label("Admin", systemImage: "shield.fill") }
            }

            NavigationStack { ProfileView() }
                .tabItem { Label("Profile", systemImage: "person.fill") }
        }
        .tint(Color.brandGreen)
    }
}
