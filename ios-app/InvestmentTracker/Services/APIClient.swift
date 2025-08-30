import Foundation

enum APIError: Error, LocalizedError {
    case invalidURL
    case noData
    case decodingError(Error)
    case networkError(Error)
    case serverError(Int, String)
    case unauthorized
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .noData:
            return "No data received"
        case .decodingError(let error):
            return "Data parsing error: \(error.localizedDescription)"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        case .serverError(let code, let message):
            return "Server error (\(code)): \(message)"
        case .unauthorized:
            return "Authentication required"
        }
    }
}

enum HTTPMethod: String {
    case GET = "GET"
    case POST = "POST"
    case PUT = "PUT"
    case DELETE = "DELETE"
}

@MainActor
class APIClient: ObservableObject {
    static let shared = APIClient()
    
    private let baseURL = "http://localhost:8080/api"
    private let keychainService = KeychainService.shared
    
    private init() {}
    
    func request<T: Codable>(
        endpoint: String,
        method: HTTPMethod,
        body: Codable? = nil
    ) async throws -> T {
        guard let url = URL(string: baseURL + endpoint) else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = method.rawValue
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // Add authorization header if available
        if let accessToken = keychainService.getAccessToken() {
            request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        }
        
        // Add body for POST/PUT requests
        if let body = body {
            do {
                let encoder = JSONEncoder()
                encoder.dateEncodingStrategy = .iso8601
                request.httpBody = try encoder.encode(body)
            } catch {
                throw APIError.decodingError(error)
            }
        }
        
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.networkError(URLError(.badServerResponse))
            }
            
            switch httpResponse.statusCode {
            case 200...299:
                break
            case 401:
                // Try to refresh token
                if await tryRefreshToken() {
                    // Retry the request with new token
                    return try await self.request(endpoint: endpoint, method: method, body: body)
                } else {
                    throw APIError.unauthorized
                }
            default:
                let errorMessage = String(data: data, encoding: .utf8) ?? "Unknown error"
                throw APIError.serverError(httpResponse.statusCode, errorMessage)
            }
            
            let decoder = JSONDecoder()
            decoder.dateDecodingStrategy = .iso8601
            
            do {
                return try decoder.decode(T.self, from: data)
            } catch {
                throw APIError.decodingError(error)
            }
            
        } catch {
            if error is APIError {
                throw error
            } else {
                throw APIError.networkError(error)
            }
        }
    }
    
    private func tryRefreshToken() async -> Bool {
        guard let refreshToken = keychainService.getRefreshToken() else {
            return false
        }
        
        do {
            // Create refresh token request
            let refreshRequest = RefreshTokenRequest(refreshToken: refreshToken)
            
            // Make request without authorization header (since we're refreshing)
            guard let url = URL(string: baseURL + "/auth/refresh") else {
                return false
            }
            
            var request = URLRequest(url: url)
            request.httpMethod = HTTPMethod.POST.rawValue
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            
            let encoder = JSONEncoder()
            request.httpBody = try encoder.encode(refreshRequest)
            
            let (data, response) = try await URLSession.shared.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse,
                  httpResponse.statusCode == 200 else {
                // Refresh failed, clear tokens
                keychainService.clearTokens()
                return false
            }
            
            let decoder = JSONDecoder()
            let authResponse: AuthResponse = try decoder.decode(AuthResponse.self, from: data)
            
            // Store new tokens
            keychainService.storeTokens(
                accessToken: authResponse.accessToken,
                refreshToken: authResponse.refreshToken
            )
            
            return true
            
        } catch {
            // Refresh failed, clear tokens
            keychainService.clearTokens()
            return false
        }
    }
}