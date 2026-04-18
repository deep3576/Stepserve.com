import SwiftUI

@main
struct StepServeApp: App {
    @StateObject private var tokenManager = TokenManager.shared

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(tokenManager)
        }
    }
}
