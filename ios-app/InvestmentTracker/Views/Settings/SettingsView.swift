import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var authService: AuthService
    @State private var showingLogoutAlert = false
    
    var body: some View {
        NavigationView {
            List {
                Section("General") {
                    NavigationLink(destination: Text("Currency Settings")) {
                        HStack {
                            Image(systemName: "banknote")
                                .foregroundColor(.blue)
                                .frame(width: 24)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Base Currency")
                                Text("TRY")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    
                    NavigationLink(destination: Text("Timezone Settings")) {
                        HStack {
                            Image(systemName: "clock")
                                .foregroundColor(.blue)
                                .frame(width: 24)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Timezone")
                                Text("Europe/Istanbul")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }
                
                Section("Data") {
                    NavigationLink(destination: Text("Import CSV")) {
                        HStack {
                            Image(systemName: "square.and.arrow.down")
                                .foregroundColor(.green)
                                .frame(width: 24)
                            Text("Import from CSV")
                        }
                    }
                    
                    NavigationLink(destination: Text("Export CSV")) {
                        HStack {
                            Image(systemName: "square.and.arrow.up")
                                .foregroundColor(.orange)
                                .frame(width: 24)
                            Text("Export to CSV")
                        }
                    }
                    
                    NavigationLink(destination: Text("Backup")) {
                        HStack {
                            Image(systemName: "externaldrive")
                                .foregroundColor(.purple)
                                .frame(width: 24)
                            Text("Backup")
                        }
                    }
                }
                
                Section("Legal") {
                    NavigationLink(destination: Text("Privacy Policy")) {
                        HStack {
                            Image(systemName: "hand.raised")
                                .foregroundColor(.gray)
                                .frame(width: 24)
                            Text("Privacy Policy")
                        }
                    }
                    
                    NavigationLink(destination: Text("Terms of Service")) {
                        HStack {
                            Image(systemName: "doc.text")
                                .foregroundColor(.gray)
                                .frame(width: 24)
                            Text("Terms of Service")
                        }
                    }
                    
                    NavigationLink(destination: Text("Disclaimers")) {
                        HStack {
                            Image(systemName: "exclamationmark.triangle")
                                .foregroundColor(.gray)
                                .frame(width: 24)
                            Text("Disclaimers")
                        }
                    }
                }
                
                Section("Account") {
                    if let user = authService.currentUser {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(user.name)
                                .font(.headline)
                            Text(user.email)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 4)
                    }
                    
                    Button(action: {
                        showingLogoutAlert = true
                    }) {
                        HStack {
                            Image(systemName: "arrow.right.square")
                                .foregroundColor(.red)
                                .frame(width: 24)
                            Text("Logout")
                                .foregroundColor(.red)
                        }
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.large)
            .alert("Logout", isPresented: $showingLogoutAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Logout", role: .destructive) {
                    authService.logout()
                }
            } message: {
                Text("Are you sure you want to logout?")
            }
        }
    }
}

#Preview {
    SettingsView()
        .environmentObject(AuthService.shared)
}