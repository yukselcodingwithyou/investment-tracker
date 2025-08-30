import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authService: AuthService
    @StateObject private var oauthService = OAuthService.shared
    @Binding var showingSignUp: Bool
    
    @State private var emailOrUsername = ""
    @State private var password = ""
    @State private var showingForgotPassword = false
    @State private var oauthError: String?
    
    var body: some View {
        ScrollView {
            VStack(spacing: 32) {
                Spacer()
                
                // Header
                VStack(spacing: 16) {
                    Text("Welcome Back")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                    
                    Text("Log in to your Investment Tracker")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                
                // Form
                VStack(spacing: 16) {
                    VStack(alignment: .leading, spacing: 8) {
                        TextField("Email or Username", text: $emailOrUsername)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .textContentType(.emailAddress)
                            .autocapitalization(.none)
                            .keyboardType(.emailAddress)
                    }
                    
                    VStack(alignment: .leading, spacing: 8) {
                        SecureField("Password", text: $password)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .textContentType(.password)
                        
                        HStack {
                            Spacer()
                            Button("Forgot Password?") {
                                showingForgotPassword = true
                            }
                            .font(.caption)
                            .foregroundColor(.blue)
                        }
                    }
                }
                
                // Login Button
                Button(action: {
                    Task {
                        await authService.login(
                            emailOrUsername: emailOrUsername,
                            password: password
                        )
                    }
                }) {
                    HStack {
                        if authService.isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .scaleEffect(0.8)
                        }
                        Text("Login")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                .disabled(authService.isLoading || emailOrUsername.isEmpty || password.isEmpty)
                
                // Divider
                HStack {
                    Rectangle()
                        .frame(height: 1)
                        .foregroundColor(.gray.opacity(0.3))
                    
                    Text("Or log in with")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 16)
                    
                    Rectangle()
                        .frame(height: 1)
                        .foregroundColor(.gray.opacity(0.3))
                }
                
                // Social Login Buttons
                VStack(spacing: 12) {
                    Button(action: {
                        Task {
                            do {
                                let authResponse = try await oauthService.signInWithGoogle()
                                await authService.handleOAuthSuccess(authResponse)
                            } catch {
                                oauthError = error.localizedDescription
                            }
                        }
                    }) {
                        HStack {
                            Image(systemName: "globe")
                                .foregroundColor(.black)
                            Text("Continue with Google")
                                .foregroundColor(.black)
                                .fontWeight(.medium)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(Color.white)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                        )
                        .cornerRadius(10)
                    }
                    
                    Button(action: {
                        Task {
                            do {
                                let authResponse = try await oauthService.signInWithApple()
                                await authService.handleOAuthSuccess(authResponse)
                            } catch {
                                oauthError = error.localizedDescription
                            }
                        }
                    }) {
                        HStack {
                            Image(systemName: "applelogo")
                                .foregroundColor(.white)
                            Text("Continue with Apple")
                                .foregroundColor(.white)
                                .fontWeight(.medium)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(Color.black)
                        .cornerRadius(10)
                    }
                }
                
                Spacer()
                
                // Sign Up Link
                HStack {
                    Text("Don't have an account?")
                        .foregroundColor(.secondary)
                    Button("Sign Up") {
                        showingSignUp = true
                    }
                    .foregroundColor(.blue)
                    .fontWeight(.medium)
                }
            }
            .padding(.horizontal, 24)
        }
        .navigationBarHidden(true)
        .alert("Error", isPresented: .constant(authService.errorMessage != nil || oauthError != nil)) {
            Button("OK") {
                authService.errorMessage = nil
                oauthError = nil
            }
        } message: {
            Text(authService.errorMessage ?? oauthError ?? "")
        }
        .sheet(isPresented: $showingForgotPassword) {
            ForgotPasswordView()
        }
    }
}

#Preview {
    LoginView(showingSignUp: .constant(false))
        .environmentObject(AuthService.shared)
}