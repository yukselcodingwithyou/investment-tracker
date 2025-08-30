import Foundation
import AuthenticationServices

struct OAuthLoginRequest: Codable {
    let token: String
    let nonce: String?
}

@MainActor
class OAuthService: ObservableObject {
    static let shared = OAuthService()
    
    private init() {}
    
    func signInWithGoogle() async throws -> AuthResponse {
        // For now, throw an error indicating Google Sign-In is not implemented
        // In a real implementation, this would use GoogleSignIn framework
        throw NSError(domain: "OAuthService", code: 1, userInfo: [NSLocalizedDescriptionKey: "Google Sign-In not yet implemented. Please configure GoogleSignIn framework."])
    }
    
    func signInWithApple() async throws -> AuthResponse {
        // For now, throw an error indicating Apple Sign-In is not implemented  
        // In a real implementation, this would use ASAuthorizationAppleIDProvider
        throw NSError(domain: "OAuthService", code: 2, userInfo: [NSLocalizedDescriptionKey: "Apple Sign-In not yet implemented. Please configure Sign in with Apple capability."])
    }
    
    private func performOAuthLogin(token: String, provider: OAuthProvider, nonce: String? = nil) async throws -> AuthResponse {
        let request = OAuthLoginRequest(token: token, nonce: nonce)
        let endpoint = provider == .google ? "/auth/oauth/google" : "/auth/oauth/apple"
        
        let authResponse: AuthResponse = try await APIClient.shared.request(
            endpoint: endpoint,
            method: .POST,
            body: request
        )
        
        // Store tokens
        KeychainService.shared.storeTokens(
            accessToken: authResponse.accessToken,
            refreshToken: authResponse.refreshToken
        )
        
        return authResponse
    }
}