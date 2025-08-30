import Foundation
import Combine

@MainActor
class AuthService: ObservableObject {
    static let shared = AuthService()
    
    @Published var isAuthenticated = false
    @Published var currentUser: User?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let apiClient = APIClient.shared
    private let keychainService = KeychainService.shared
    
    private init() {
        checkAuthenticationStatus()
    }
    
    func checkAuthenticationStatus() {
        isAuthenticated = keychainService.getAccessToken() != nil
        if isAuthenticated {
            Task {
                await loadCurrentUser()
            }
        }
    }
    
    func signUp(name: String, email: String, password: String, confirmPassword: String) async {
        isLoading = true
        errorMessage = nil
        
        do {
            let request = SignUpRequest(
                name: name,
                email: email,
                password: password,
                confirmPassword: confirmPassword
            )
            
            let response: AuthResponse = try await apiClient.request(
                endpoint: "/auth/signup",
                method: .POST,
                body: request
            )
            
            await handleAuthResponse(response)
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
    
    func login(emailOrUsername: String, password: String) async {
        isLoading = true
        errorMessage = nil
        
        do {
            let request = LoginRequest(
                emailOrUsername: emailOrUsername,
                password: password
            )
            
            let response: AuthResponse = try await apiClient.request(
                endpoint: "/auth/login",
                method: .POST,
                body: request
            )
            
            await handleAuthResponse(response)
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
    
    func logout() {
        keychainService.clearTokens()
        currentUser = nil
        isAuthenticated = false
    }
    
    func handleOAuthSuccess(_ response: AuthResponse) async {
        await handleAuthResponse(response)
    }
    
    private func handleAuthResponse(_ response: AuthResponse) async {
        keychainService.storeTokens(
            accessToken: response.accessToken,
            refreshToken: response.refreshToken
        )
        currentUser = response.user
        isAuthenticated = true
    }
    
    private func loadCurrentUser() async {
        do {
            let user: User = try await apiClient.request(endpoint: "/auth/me", method: .GET)
            currentUser = user
        } catch {
            logout()
        }
    }
}