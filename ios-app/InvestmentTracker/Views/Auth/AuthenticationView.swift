import SwiftUI

struct AuthenticationView: View {
    @State private var showingSignUp = false
    
    var body: some View {
        NavigationStack {
            if showingSignUp {
                SignUpView(showingSignUp: $showingSignUp)
            } else {
                LoginView(showingSignUp: $showingSignUp)
            }
        }
    }
}

#Preview {
    AuthenticationView()
        .environmentObject(AuthService.shared)
}