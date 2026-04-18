import SwiftUI

struct ServiceDetailView: View {
    let service: Service
    @EnvironmentObject var tokenManager: TokenManager

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Header card
                ZStack(alignment: .bottomLeading) {
                    Color.brandGreen.frame(height: 140)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(service.title).font(.title3).fontWeight(.bold).foregroundColor(.white)
                        Text("CAD \(service.price, specifier: "%.0f")/hr")
                            .font(.subheadline).fontWeight(.semibold).foregroundColor(.white.opacity(0.9))
                    }
                    .padding(20)
                }

                VStack(alignment: .leading, spacing: 16) {
                    // Meta
                    HStack(spacing: 20) {
                        if let loc = service.location, !loc.isEmpty {
                            Label(loc, systemImage: "mappin.circle.fill").font(.subheadline).foregroundColor(.secondary)
                        }
                        if let cat = service.category_name {
                            Label(cat, systemImage: "tag").font(.subheadline).foregroundColor(.secondary)
                        }
                    }

                    if let name = service.provider_name, !name.isEmpty {
                        HStack {
                            Image(systemName: "person.circle.fill").font(.title2).foregroundColor(.brandGreen)
                            VStack(alignment: .leading) {
                                Text(name).fontWeight(.semibold)
                                Text("Provider").font(.caption).foregroundColor(.secondary)
                            }
                        }
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.brandLight)
                        .cornerRadius(12)
                    }

                    if let desc = service.description, !desc.isEmpty {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("About this service").fontWeight(.semibold)
                            Text(desc).font(.subheadline).foregroundColor(.secondary).fixedSize(horizontal: false, vertical: true)
                        }
                    }

                    Divider()

                    // Price summary
                    HStack {
                        VStack(alignment: .leading) {
                            Text("Hourly Rate").font(.caption).foregroundColor(.secondary)
                            Text("CAD \(service.price, specifier: "%.2f")").font(.title3).fontWeight(.bold).foregroundColor(.brandGreen)
                        }
                        Spacer()
                        if tokenManager.isLoggedIn && tokenManager.role == "customer" {
                            NavigationLink(destination: BookingView(service: service)) {
                                Text("Book Now")
                                    .fontWeight(.semibold).padding(.horizontal, 24).padding(.vertical, 12)
                                    .background(Color.brandGreen).foregroundColor(.white).cornerRadius(12)
                            }
                        } else if !tokenManager.isLoggedIn {
                            NavigationLink(destination: LoginView()) {
                                Text("Log in to Book")
                                    .fontWeight(.semibold).padding(.horizontal, 16).padding(.vertical, 12)
                                    .background(Color.brandGreen).foregroundColor(.white).cornerRadius(12)
                            }
                        }
                    }
                }
                .padding(20)
            }
        }
        .navigationTitle("Service Details")
        .navigationBarTitleDisplayMode(.inline)
    }
}
