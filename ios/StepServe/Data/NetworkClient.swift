import Foundation

// Change to your machine's IP when testing on a physical device
// Simulator: http://localhost:8000/api/v1/
// Physical device: http://192.168.x.x:8000/api/v1/
// Production: https://youruser.pythonanywhere.com/api/v1/
let API_BASE_URL = "http://10.0.0.168:8000/api/v1"

enum APIResult<T> {
    case success(T)
    case failure(String)
}

final class NetworkClient {
    static let shared = NetworkClient()
    private let session: URLSession
    private let decoder: JSONDecoder

    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        session = URLSession(configuration: config)
        decoder = JSONDecoder()
    }

    // MARK: - Core request
    func request<T: Decodable>(
        path: String,
        method: String = "GET",
        body: (any Encodable)? = nil,
        authenticated: Bool = true
    ) async -> APIResult<T> {
        guard let url = URL(string: "\(API_BASE_URL)\(path)") else {
            return .failure("Invalid URL")
        }

        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")

        if authenticated, let token = TokenManager.shared.token {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let body = body {
            do {
                req.httpBody = try JSONEncoder().encode(body)
            } catch {
                return .failure("Encoding error: \(error.localizedDescription)")
            }
        }

        do {
            let (data, response) = try await session.data(for: req)
            guard let http = response as? HTTPURLResponse else {
                return .failure("No HTTP response")
            }

            if (200..<300).contains(http.statusCode) {
                do {
                    let decoded = try decoder.decode(T.self, from: data)
                    return .success(decoded)
                } catch {
                    return .failure("Decode error: \(error.localizedDescription)")
                }
            } else {
                if let apiErr = try? decoder.decode(APIError.self, from: data) {
                    return .failure(apiErr.detail)
                }
                return .failure("HTTP \(http.statusCode)")
            }
        } catch {
            return .failure(error.localizedDescription)
        }
    }

    // MARK: - Multipart upload
    func upload(path: String, fileData: Data, fileName: String, mimeType: String) async -> APIResult<ProviderUpload> {
        guard let url = URL(string: "\(API_BASE_URL)\(path)") else {
            return .failure("Invalid URL")
        }
        let boundary = UUID().uuidString
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        if let token = TokenManager.shared.token {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        var body = Data()
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"\(fileName)\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: \(mimeType)\r\n\r\n".data(using: .utf8)!)
        body.append(fileData)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)
        req.httpBody = body

        do {
            let (data, response) = try await session.data(for: req)
            guard let http = response as? HTTPURLResponse else { return .failure("No HTTP response") }
            if (200..<300).contains(http.statusCode) {
                if let decoded = try? decoder.decode(ProviderUpload.self, from: data) {
                    return .success(decoded)
                }
                return .failure("Decode error")
            } else {
                if let apiErr = try? decoder.decode(APIError.self, from: data) {
                    return .failure(apiErr.detail)
                }
                return .failure("HTTP \(http.statusCode)")
            }
        } catch {
            return .failure(error.localizedDescription)
        }
    }
}
