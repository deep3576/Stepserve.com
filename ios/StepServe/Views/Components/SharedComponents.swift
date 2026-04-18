import SwiftUI

// MARK: - Brand Colors  (matches website: #0a7c5c primary, #085e47 dark, #f2f9f6 light)
extension Color {
    static let brandGreen     = Color(red: 0.039, green: 0.486, blue: 0.361) // #0a7c5c
    static let brandGreenDark = Color(red: 0.031, green: 0.369, blue: 0.278) // #085e47
    static let brandLight     = Color(red: 0.949, green: 0.976, blue: 0.965) // #f2f9f6
    static let gray500        = Color(red: 0.533, green: 0.533, blue: 0.533) // #888888
    static let gray300        = Color(red: 0.800, green: 0.800, blue: 0.800) // #cccccc
}

// MARK: - StepServe Logo
struct StepServeLogo: View {
    var tint: Color = .white
    var size: CGFloat = 22

    var body: some View {
        HStack(spacing: 10) {
            ZStack {
                RoundedRectangle(cornerRadius: 9)
                    .fill(tint.opacity(0.2))
                    .frame(width: size + 12, height: size + 12)
                Image(systemName: "house.fill")
                    .font(.system(size: size * 0.85, weight: .semibold))
                    .foregroundColor(tint)
            }
            VStack(alignment: .leading, spacing: 0) {
                Text("StepServe")
                    .font(.system(size: size, weight: .heavy))
                    .foregroundColor(tint)
                Text("Local Services")
                    .font(.system(size: size * 0.5, weight: .medium))
                    .foregroundColor(tint.opacity(0.75))
                    .tracking(0.3)
            }
        }
    }
}

// MARK: - PrimaryButton
struct PrimaryButton: View {
    let title: String
    let action: () -> Void
    var isLoading: Bool = false
    var disabled: Bool = false

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if isLoading { ProgressView().tint(.white).scaleEffect(0.9) }
                Text(title).fontWeight(.semibold).font(.system(size: 15))
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(disabled || isLoading ? Color.gray.opacity(0.35) : Color.brandGreen)
            .foregroundColor(.white)
            .cornerRadius(12)
            .shadow(color: Color.brandGreen.opacity(0.3), radius: 4, y: 2)
        }
        .disabled(disabled || isLoading)
    }
}

// MARK: - Banners
struct SuccessBanner: View {
    let message: String
    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "checkmark.circle.fill").foregroundColor(.brandGreen).font(.system(size: 16))
            Text(message).font(.system(size: 13)).foregroundColor(.brandGreen).fixedSize(horizontal: false, vertical: true)
            Spacer()
        }
        .padding(12)
        .background(Color(red: 0.882, green: 0.961, blue: 0.882))
        .cornerRadius(10)
    }
}

struct ErrorBanner: View {
    let message: String
    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "xmark.circle.fill").foregroundColor(.red).font(.system(size: 16))
            Text(message).font(.system(size: 13)).foregroundColor(.red).fixedSize(horizontal: false, vertical: true)
            Spacer()
        }
        .padding(12)
        .background(Color(red: 0.992, green: 0.918, blue: 0.918))
        .cornerRadius(10)
    }
}

// MARK: - LoadingView
struct LoadingView: View {
    var body: some View {
        VStack(spacing: 14) {
            ProgressView().scaleEffect(1.4).tint(.brandGreen)
            Text("Loading…").font(.system(size: 14)).foregroundColor(.gray500)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - EmptyState
struct EmptyState: View {
    let icon: String
    let title: String
    let message: String
    var body: some View {
        VStack(spacing: 14) {
            ZStack {
                Circle().fill(Color.brandLight).frame(width: 72, height: 72)
                Image(systemName: icon).font(.system(size: 30)).foregroundColor(.brandGreen)
            }
            Text(title).font(.headline).fontWeight(.bold)
            Text(message).font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)
        }
        .padding(40)
        .frame(maxWidth: .infinity)
    }
}

// MARK: - SectionHeader
struct SectionHeader: View {
    let title: String
    var actionTitle: String? = nil
    var onAction: (() -> Void)? = nil

    var body: some View {
        HStack {
            HStack(spacing: 8) {
                Rectangle().fill(Color.brandGreen).frame(width: 4, height: 20).cornerRadius(2)
                Text(title).font(.system(size: 16, weight: .bold))
            }
            Spacer()
            if let a = actionTitle, let fn = onAction {
                Button(a, action: fn).font(.system(size: 12)).foregroundColor(.brandGreen)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 6)
    }
}

// MARK: - CategoryCard (list-style, full row)
struct CategoryCard: View {
    let category: Category
    var selected: Bool = false
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(selected ? Color.white.opacity(0.2) : Color.brandLight)
                        .frame(width: 46, height: 46)
                    Image(systemName: iconFor(category.name))
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(selected ? .white : .brandGreen)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(category.name)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(selected ? .white : .primary)
                        .lineLimit(1)
                    Text("\(category.services_count ?? 0) listing\((category.services_count ?? 0) == 1 ? "" : "s")")
                        .font(.system(size: 12))
                        .foregroundColor(selected ? .white.opacity(0.75) : .gray500)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(selected ? .white.opacity(0.6) : .gray300)
            }
            .padding(14)
            .background(selected ? Color.brandGreen : Color(.systemBackground))
            .cornerRadius(14)
            .shadow(color: .black.opacity(selected ? 0 : 0.06), radius: 4, x: 0, y: 2)
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(selected ? Color.clear : Color(.systemGray5), lineWidth: 1)
            )
        }
    }

    private func iconFor(_ name: String) -> String {
        let n = name.lowercased()
        if n.contains("clean") || n.contains("maid") || n.contains("housekeep") { return "sparkles" }
        if n.contains("landscape") || n.contains("lawn") || n.contains("garden") || n.contains("yard") { return "leaf.fill" }
        if n.contains("plumb") || n.contains("pipe") || n.contains("drain") { return "wrench.fill" }
        if n.contains("electric") || n.contains("wiring") || n.contains("outlet") { return "bolt.fill" }
        if n.contains("carpet") || n.contains("wood") || n.contains("furniture") || n.contains("cabinet") { return "hammer.fill" }
        if n.contains("paint") || n.contains("colour") || n.contains("color") { return "paintbrush.fill" }
        if n.contains("hvac") || n.contains("heating") || n.contains("cooling") || n.contains("air") || n.contains("furnace") { return "snowflake" }
        if n.contains("moving") || n.contains("movers") || n.contains("reloc") || n.contains("hauling") { return "shippingbox.fill" }
        if n.contains("pet") || n.contains("dog") || n.contains("cat") || n.contains("animal") { return "pawprint.fill" }
        if n.contains("window") || n.contains("glass") { return "square.grid.2x2.fill" }
        if n.contains("renovat") || n.contains("remodel") || n.contains("construct") { return "building.2.fill" }
        if n.contains("roof") || n.contains("gutter") { return "house.fill" }
        if n.contains("lock") || n.contains("security") || n.contains("key") { return "lock.fill" }
        if n.contains("pest") || n.contains("extermin") || n.contains("insect") { return "ant.fill" }
        return "wrench.and.screwdriver.fill"
    }
}

// MARK: - ServiceCard (list)
struct ServiceCard: View {
    let service: Service

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            ZStack {
                LinearGradient(colors: [Color.brandGreen, Color(red: 0.26, green: 0.63, blue: 0.28)],
                               startPoint: .topLeading, endPoint: .bottomTrailing)
                    .frame(width: 52, height: 52)
                    .cornerRadius(13)
                Text(String(service.title.prefix(1)).uppercased())
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(.white)
            }
            VStack(alignment: .leading, spacing: 3) {
                Text(service.title)
                    .font(.system(size: 15, weight: .bold))
                    .lineLimit(1)
                if let name = service.provider_name, !name.isEmpty {
                    Label(name, systemImage: "person.fill")
                        .font(.system(size: 12)).foregroundColor(.gray500).lineLimit(1)
                }
                if let loc = service.location, !loc.isEmpty {
                    Label(loc, systemImage: "mappin.circle.fill")
                        .font(.system(size: 12)).foregroundColor(.gray500).lineLimit(1)
                }
                if let desc = service.description, !desc.isEmpty {
                    Text(desc).font(.system(size: 12)).foregroundColor(.secondary).lineLimit(2)
                        .padding(.top, 2)
                }
            }
            Spacer(minLength: 4)
            Text("CAD \(Int(service.price))/hr")
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(.brandGreen)
                .padding(.horizontal, 8).padding(.vertical, 5)
                .background(Color.brandLight)
                .cornerRadius(8)
        }
        .padding(14)
        .background(Color(.systemBackground))
        .cornerRadius(14)
        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
    }
}

// MARK: - ServiceCardCompact (horizontal scroll)
struct ServiceCardCompact: View {
    let service: Service

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ZStack {
                Color.brandLight.frame(width: 40, height: 40).cornerRadius(10)
                Image(systemName: "wrench.and.screwdriver.fill").font(.system(size: 18)).foregroundColor(.brandGreen)
            }
            Text(service.title).font(.system(size: 14, weight: .bold)).lineLimit(2)
            if let name = service.provider_name, !name.isEmpty {
                Text(name).font(.system(size: 11)).foregroundColor(.gray500).lineLimit(1)
            }
            Spacer()
            Text("CAD \(Int(service.price))/hr")
                .font(.system(size: 11, weight: .bold)).foregroundColor(.brandGreen)
                .padding(.horizontal, 7).padding(.vertical, 3)
                .background(Color.brandLight).cornerRadius(6)
        }
        .padding(14)
        .frame(width: 170, height: 150)
        .background(Color(.systemBackground))
        .cornerRadius(14)
        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
    }
}

// MARK: - StatCard
struct StatCard: View {
    let title: String
    let value: String
    let icon: String
    var tint: Color = .brandGreen

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 10).fill(tint.opacity(0.12)).frame(width: 44, height: 44)
                Image(systemName: icon).font(.system(size: 18)).foregroundColor(tint)
            }
            VStack(alignment: .leading, spacing: 1) {
                Text(value).font(.system(size: 22, weight: .heavy))
                Text(title).font(.system(size: 12)).foregroundColor(.secondary)
            }
            Spacer()
        }
        .padding(16)
        .background(Color(.systemBackground))
        .cornerRadius(14)
        .shadow(color: .black.opacity(0.05), radius: 3, x: 0, y: 1)
    }
}

// MARK: - StatusBadge
struct StatusBadge: View {
    let status: String
    var body: some View {
        Text(status.uppercased())
            .font(.system(size: 10, weight: .bold))
            .tracking(0.5)
            .padding(.horizontal, 8).padding(.vertical, 3)
            .background(bgColor).foregroundColor(fgColor)
            .cornerRadius(5)
    }
    private var bgColor: Color {
        switch status.lowercased() {
        case "paid", "confirmed", "active": return Color(red: 0.882, green: 0.961, blue: 0.882)
        case "pending": return Color(red: 1, green: 0.973, blue: 0.882)
        case "completed": return Color(red: 0.882, green: 0.945, blue: 0.992)
        default: return Color(.systemGray6)
        }
    }
    private var fgColor: Color {
        switch status.lowercased() {
        case "paid", "confirmed", "active": return .brandGreen
        case "pending": return .orange
        case "completed": return .blue
        case "inactive", "cancelled": return .red
        default: return .secondary
        }
    }
}
