import SwiftUI

// MARK: - Brand Colors
extension Color {
    static let brandGreen    = Color(red: 0.18, green: 0.49, blue: 0.20)  // #2e7d32
    static let brandGreen700 = Color(red: 0.22, green: 0.56, blue: 0.24)  // #388e3c
    static let brandLight    = Color(red: 0.945, green: 0.973, blue: 0.945) // #f1f8f1
}

// MARK: - PrimaryButton
struct PrimaryButton: View {
    let title: String
    let action: () -> Void
    var isLoading: Bool = false
    var disabled: Bool = false

    var body: some View {
        Button(action: action) {
            HStack {
                if isLoading { ProgressView().tint(.white).padding(.trailing, 4) }
                Text(title).fontWeight(.semibold)
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(disabled || isLoading ? Color.gray.opacity(0.4) : Color.brandGreen)
            .foregroundColor(.white)
            .cornerRadius(12)
        }
        .disabled(disabled || isLoading)
    }
}

// MARK: - Banners
struct SuccessBanner: View {
    let message: String
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "checkmark.circle.fill").foregroundColor(.green)
            Text(message).font(.subheadline).foregroundColor(.green)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.green.opacity(0.1))
        .cornerRadius(10)
    }
}

struct ErrorBanner: View {
    let message: String
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "xmark.circle.fill").foregroundColor(.red)
            Text(message).font(.subheadline).foregroundColor(.red)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.red.opacity(0.1))
        .cornerRadius(10)
    }
}

// MARK: - LoadingView
struct LoadingView: View {
    var body: some View {
        VStack { ProgressView().scaleEffect(1.5).padding() }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - EmptyState
struct EmptyState: View {
    let icon: String
    let title: String
    let message: String
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon).font(.system(size: 48)).foregroundColor(.gray.opacity(0.5))
            Text(title).font(.headline)
            Text(message).font(.subheadline).foregroundColor(.secondary).multilineTextAlignment(.center)
        }
        .padding(32)
        .frame(maxWidth: .infinity)
    }
}

// MARK: - SectionHeader
struct SectionHeader: View {
    let title: String
    var body: some View {
        Text(title)
            .font(.headline)
            .fontWeight(.bold)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.vertical, 4)
    }
}

// MARK: - ServiceCard
struct ServiceCard: View {
    let service: Service
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Image(systemName: "music.note").foregroundColor(.brandGreen)
                Text(service.title).font(.subheadline).fontWeight(.semibold).lineLimit(1)
                Spacer()
                Text("CAD \(service.price, specifier: "%.0f")/hr")
                    .font(.caption).fontWeight(.bold).foregroundColor(.brandGreen)
            }
            if let location = service.location, !location.isEmpty {
                Label(location, systemImage: "mappin.circle").font(.caption).foregroundColor(.secondary)
            }
            if let name = service.provider_name, !name.isEmpty {
                Label(name, systemImage: "person.circle").font(.caption).foregroundColor(.secondary)
            }
        }
        .padding(14)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
    }
}

// MARK: - CategoryChip
struct CategoryChip: View {
    let category: Category
    let selected: Bool
    let onTap: () -> Void
    var body: some View {
        Button(action: onTap) {
            Text(category.name)
                .font(.caption).fontWeight(.medium)
                .padding(.horizontal, 14).padding(.vertical, 7)
                .background(selected ? Color.brandGreen : Color(.systemGray5))
                .foregroundColor(selected ? .white : .primary)
                .cornerRadius(20)
        }
    }
}

// MARK: - StatCard
struct StatCard: View {
    let title: String
    let value: String
    let icon: String
    var body: some View {
        VStack(spacing: 6) {
            Image(systemName: icon).font(.title2).foregroundColor(.brandGreen)
            Text(value).font(.title3).fontWeight(.bold)
            Text(title).font(.caption).foregroundColor(.secondary).multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 3, x: 0, y: 1)
    }
}

// MARK: - StatusBadge
struct StatusBadge: View {
    let status: String
    var body: some View {
        Text(status.uppercased())
            .font(.caption2).fontWeight(.bold)
            .padding(.horizontal, 8).padding(.vertical, 3)
            .background(color(for: status))
            .foregroundColor(.white)
            .cornerRadius(6)
    }
    private func color(for s: String) -> Color {
        switch s.lowercased() {
        case "paid", "confirmed", "completed": return .brandGreen
        case "pending": return .orange
        case "cancelled": return .red
        default: return .gray
        }
    }
}

// MARK: - BackButton helper
struct BackButton: View {
    @Environment(\.dismiss) var dismiss
    var body: some View {
        Button(action: { dismiss() }) {
            Image(systemName: "chevron.left").fontWeight(.semibold)
        }
    }
}
