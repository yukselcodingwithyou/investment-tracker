# iOS App Completion Prompt

## 🎯 Objective
Complete all missing iOS SwiftUI implementations for the Investment Tracker application to achieve production readiness.

## 📱 Required Implementations

### 1. Refresh Token Implementation

**Task:** Implement automatic token refresh functionality

**File to modify:**
- `ios-app/InvestmentTracker/Services/APIClient.swift`

**Implementation:**
```swift
private func tryRefreshToken() async -> Bool {
    guard let refreshToken = keychainService.getRefreshToken() else {
        return false
    }
    
    do {
        let refreshRequest = RefreshTokenRequest(refreshToken: refreshToken)
        let response: AuthResponse = try await request(
            endpoint: "/auth/refresh",
            method: .POST,
            body: refreshRequest
        )
        
        // Save new tokens
        keychainService.saveTokens(
            accessToken: response.accessToken,
            refreshToken: response.refreshToken
        )
        
        return true
    } catch {
        // Refresh failed, clear tokens
        keychainService.clearTokens()
        
        // Notify app to redirect to login
        DispatchQueue.main.async {
            NotificationCenter.default.post(name: .authenticationExpired, object: nil)
        }
        
        return false
    }
}
```

**New models to create:**
```swift
// Models/Auth/RefreshTokenRequest.swift
struct RefreshTokenRequest: Codable {
    let refreshToken: String
}

// Add to AuthResponse.swift
extension Notification.Name {
    static let authenticationExpired = Notification.Name("authenticationExpired")
}
```

### 2. OAuth Implementation (Google & Apple Sign-In)

**Files to create:**
- `ios-app/InvestmentTracker/Services/OAuthService.swift`
- `ios-app/InvestmentTracker/Models/Auth/OAuthProvider.swift`

**Dependencies to add to Xcode project:**
- GoogleSignIn SDK
- AuthenticationServices (Apple Sign In)

**OAuthService implementation:**
```swift
import Foundation
import GoogleSignIn
import AuthenticationServices

@MainActor
class OAuthService: NSObject, ObservableObject {
    static let shared = OAuthService()
    
    private override init() {
        super.init()
        configureGoogleSignIn()
    }
    
    private func configureGoogleSignIn() {
        guard let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
              let plist = NSDictionary(contentsOfFile: path),
              let clientId = plist["CLIENT_ID"] as? String else {
            fatalError("GoogleService-Info.plist not found or CLIENT_ID missing")
        }
        
        if let config = GIDConfiguration(clientID: clientId) {
            GIDSignIn.sharedInstance.configuration = config
        }
    }
    
    func signInWithGoogle() async throws -> AuthResponse {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootViewController = windowScene.windows.first?.rootViewController else {
            throw OAuthError.noRootViewController
        }
        
        do {
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController)
            let idToken = result.user.idToken?.tokenString
            
            guard let token = idToken else {
                throw OAuthError.noIDToken
            }
            
            // Send token to backend for verification
            let oauthRequest = OAuthLoginRequest(
                token: token,
                provider: "GOOGLE"
            )
            
            let response: AuthResponse = try await APIClient.shared.request(
                endpoint: "/auth/oauth/google",
                method: .POST,
                body: oauthRequest
            )
            
            return response
            
        } catch {
            throw OAuthError.googleSignInFailed(error)
        }
    }
    
    func signInWithApple() async throws -> AuthResponse {
        return try await withCheckedThrowingContinuation { continuation in
            let request = ASAuthorizationAppleIDProvider().createRequest()
            request.requestedScopes = [.fullName, .email]
            
            let authController = ASAuthorizationController(authorizationRequests: [request])
            authController.delegate = AppleSignInDelegate { result in
                continuation.resume(with: result)
            }
            authController.presentationContextProvider = AppleSignInPresentationContextProvider()
            authController.performRequests()
        }
    }
}

// Apple Sign In Delegate
class AppleSignInDelegate: NSObject, ASAuthorizationControllerDelegate {
    private let completion: (Result<AuthResponse, Error>) -> Void
    
    init(completion: @escaping (Result<AuthResponse, Error>) -> Void) {
        self.completion = completion
    }
    
    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let identityToken = appleIDCredential.identityToken,
              let tokenString = String(data: identityToken, encoding: .utf8) else {
            completion(.failure(OAuthError.noIDToken))
            return
        }
        
        Task {
            do {
                let oauthRequest = OAuthLoginRequest(
                    token: tokenString,
                    provider: "APPLE"
                )
                
                let response: AuthResponse = try await APIClient.shared.request(
                    endpoint: "/auth/oauth/apple",
                    method: .POST,
                    body: oauthRequest
                )
                
                completion(.success(response))
            } catch {
                completion(.failure(error))
            }
        }
    }
    
    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        completion(.failure(OAuthError.appleSignInFailed(error)))
    }
}

class AppleSignInPresentationContextProvider: NSObject, ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        return UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow } ?? UIWindow()
    }
}

enum OAuthError: Error, LocalizedError {
    case noRootViewController
    case noIDToken
    case googleSignInFailed(Error)
    case appleSignInFailed(Error)
    
    var errorDescription: String? {
        switch self {
        case .noRootViewController:
            return "No root view controller found"
        case .noIDToken:
            return "No ID token received"
        case .googleSignInFailed(let error):
            return "Google Sign In failed: \(error.localizedDescription)"
        case .appleSignInFailed(let error):
            return "Apple Sign In failed: \(error.localizedDescription)"
        }
    }
}
```

**Update SignUpView and LoginView:**
```swift
// In SignUpView.swift and LoginView.swift, replace TODO comments:

Button(action: {
    Task {
        do {
            let authResponse = try await OAuthService.shared.signInWithGoogle()
            await authViewModel.handleOAuthResponse(authResponse)
        } catch {
            // Handle error
            print("Google Sign In error: \(error)")
        }
    }
}) {
    // Button content
}

Button(action: {
    Task {
        do {
            let authResponse = try await OAuthService.shared.signInWithApple()
            await authViewModel.handleOAuthResponse(authResponse)
        } catch {
            // Handle error
            print("Apple Sign In error: \(error)")
        }
    }
}) {
    // Button content
}
```

### 3. Forgot Password Implementation

**File to modify:**
- `ios-app/InvestmentTracker/Views/Auth/ForgotPasswordView.swift`

**Implementation:**
```swift
private func sendResetEmail() {
    isLoading = true
    
    Task {
        do {
            let _: String = try await APIClient.shared.request(
                endpoint: "/auth/forgot-password?email=\(email)",
                method: .POST
            )
            
            await MainActor.run {
                isLoading = false
                showSuccessMessage = true
                message = "Password reset instructions have been sent to your email."
            }
            
        } catch {
            await MainActor.run {
                isLoading = false
                message = error.localizedDescription
            }
        }
    }
}
```

### 4. Portfolio Service Implementation

**Files to create:**
- `ios-app/InvestmentTracker/Services/PortfolioService.swift`
- `ios-app/InvestmentTracker/Models/Portfolio/PortfolioSummary.swift`
- `ios-app/InvestmentTracker/Models/Portfolio/AcquisitionLot.swift`

**PortfolioService implementation:**
```swift
import Foundation

@MainActor
class PortfolioService: ObservableObject {
    static let shared = PortfolioService()
    
    @Published var portfolioSummary: PortfolioSummary?
    @Published var acquisitions: [AcquisitionLot] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private init() {}
    
    func fetchPortfolioSummary() async {
        isLoading = true
        errorMessage = nil
        
        do {
            let summary: PortfolioSummary = try await APIClient.shared.request(
                endpoint: "/portfolio/summary",
                method: .GET
            )
            
            portfolioSummary = summary
            
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
    
    func addAcquisition(_ acquisition: AcquisitionRequest) async throws -> AcquisitionLot {
        let newAcquisition: AcquisitionLot = try await APIClient.shared.request(
            endpoint: "/portfolio/acquisitions",
            method: .POST,
            body: acquisition
        )
        
        acquisitions.append(newAcquisition)
        await fetchPortfolioSummary() // Refresh summary
        
        return newAcquisition
    }
    
    func refreshData() async {
        await fetchPortfolioSummary()
    }
}

// Models
struct PortfolioSummary: Codable {
    let totalValueTRY: Decimal
    let todayChangePercent: Decimal?
    let totalUnrealizedPLTRY: Decimal
    let totalUnrealizedPLPercent: Decimal
    let status: String
    let estimatedProceedsTRY: Decimal
    let costBasisTRY: Decimal
    let unrealizedGainLossTRY: Decimal
    let unrealizedGainLossPercent: Decimal
    let fxInfluenceTRY: Decimal?
}

struct AcquisitionLot: Codable, Identifiable {
    let id: String
    let userId: String
    let assetId: String
    let quantity: Decimal
    let unitPrice: Decimal
    let currency: String
    let fee: Decimal
    let acquisitionDate: Date
    let notes: String?
    let tags: [String]?
    let createdAt: Date
    let updatedAt: Date
}

struct AcquisitionRequest: Codable {
    let assetId: String
    let quantity: Decimal
    let unitPrice: Decimal
    let currency: String?
    let fee: Decimal?
    let acquisitionDate: Date
    let notes: String?
    let tags: [String]?
}
```

### 5. Asset Management Service

**Files to create:**
- `ios-app/InvestmentTracker/Services/AssetService.swift`
- `ios-app/InvestmentTracker/Models/Asset/Asset.swift`

**AssetService implementation:**
```swift
import Foundation

@MainActor
class AssetService: ObservableObject {
    static let shared = AssetService()
    
    @Published var assets: [Asset] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private init() {}
    
    func searchAssets(query: String = "", type: AssetType? = nil, page: Int = 0) async {
        isLoading = true
        errorMessage = nil
        
        var endpoint = "/assets?page=\(page)&size=20"
        if !query.isEmpty {
            endpoint += "&search=\(query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")"
        }
        if let type = type {
            endpoint += "&type=\(type.rawValue)"
        }
        
        do {
            let response: PagedResponse<Asset> = try await APIClient.shared.request(
                endpoint: endpoint,
                method: .GET
            )
            
            if page == 0 {
                assets = response.content
            } else {
                assets.append(contentsOf: response.content)
            }
            
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
    
    func getAssetDetails(id: String) async throws -> AssetDetail {
        return try await APIClient.shared.request(
            endpoint: "/assets/\(id)",
            method: .GET
        )
    }
}

// Models
struct Asset: Codable, Identifiable {
    let id: String
    let symbol: String
    let name: String
    let type: AssetType
    let currency: String
}

struct AssetDetail: Codable {
    let id: String
    let symbol: String
    let name: String
    let type: AssetType
    let currency: String
    let currentPrice: Decimal?
    let priceHistory: [PricePoint]?
    let description: String?
}

struct PricePoint: Codable {
    let date: Date
    let price: Decimal
}

struct PagedResponse<T: Codable>: Codable {
    let content: [T]
    let totalElements: Int
    let totalPages: Int
    let size: Int
    let number: Int
}

enum AssetType: String, Codable, CaseIterable {
    case preciousMetal = "PRECIOUS_METAL"
    case fx = "FX"
    case equity = "EQUITY"
    case fund = "FUND"
    
    var displayName: String {
        switch self {
        case .preciousMetal:
            return "Precious Metal"
        case .fx:
            return "Foreign Exchange"
        case .equity:
            return "Equity"
        case .fund:
            return "Fund"
        }
    }
}
```

### 6. Add Acquisition View

**Files to create:**
- `ios-app/InvestmentTracker/Views/Portfolio/AddAcquisitionView.swift`

**Implementation:**
```swift
import SwiftUI

struct AddAcquisitionView: View {
    @StateObject private var portfolioService = PortfolioService.shared
    @StateObject private var assetService = AssetService.shared
    @Environment(\.dismiss) private var dismiss
    
    @State private var selectedAsset: Asset?
    @State private var quantity = ""
    @State private var unitPrice = ""
    @State private var currency = "USD"
    @State private var fee = ""
    @State private var acquisitionDate = Date()
    @State private var notes = ""
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showingAssetPicker = false
    
    let currencies = ["USD", "EUR", "TRY", "GBP", "JPY"]
    
    var body: some View {
        NavigationView {
            Form {
                Section("Asset") {
                    Button(action: {
                        showingAssetPicker = true
                    }) {
                        HStack {
                            Text("Select Asset")
                            Spacer()
                            if let asset = selectedAsset {
                                Text(asset.symbol)
                                    .foregroundColor(.secondary)
                            } else {
                                Text("Choose...")
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }
                
                Section("Transaction Details") {
                    TextField("Quantity", text: $quantity)
                        .keyboardType(.decimalPad)
                    
                    TextField("Unit Price", text: $unitPrice)
                        .keyboardType(.decimalPad)
                    
                    Picker("Currency", selection: $currency) {
                        ForEach(currencies, id: \.self) { currency in
                            Text(currency).tag(currency)
                        }
                    }
                    
                    TextField("Fee (Optional)", text: $fee)
                        .keyboardType(.decimalPad)
                    
                    DatePicker("Acquisition Date", selection: $acquisitionDate, displayedComponents: .date)
                    
                    TextField("Notes (Optional)", text: $notes, axis: .vertical)
                        .lineLimit(3...6)
                }
                
                if let errorMessage = errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle("Add Acquisition")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        Task {
                            await saveAcquisition()
                        }
                    }
                    .disabled(isLoading || selectedAsset == nil || quantity.isEmpty || unitPrice.isEmpty)
                }
            }
            .sheet(isPresented: $showingAssetPicker) {
                AssetPickerView(selectedAsset: $selectedAsset)
            }
        }
    }
    
    private func saveAcquisition() async {
        guard let asset = selectedAsset,
              let quantityDecimal = Decimal(string: quantity),
              let unitPriceDecimal = Decimal(string: unitPrice) else {
            errorMessage = "Please fill in all required fields with valid numbers"
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        let request = AcquisitionRequest(
            assetId: asset.id,
            quantity: quantityDecimal,
            unitPrice: unitPriceDecimal,
            currency: currency,
            fee: fee.isEmpty ? nil : Decimal(string: fee),
            acquisitionDate: acquisitionDate,
            notes: notes.isEmpty ? nil : notes,
            tags: nil
        )
        
        do {
            _ = try await portfolioService.addAcquisition(request)
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
}

struct AssetPickerView: View {
    @StateObject private var assetService = AssetService.shared
    @Binding var selectedAsset: Asset?
    @Environment(\.dismiss) private var dismiss
    @State private var searchText = ""
    
    var body: some View {
        NavigationView {
            List(assetService.assets) { asset in
                Button(action: {
                    selectedAsset = asset
                    dismiss()
                }) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(asset.symbol)
                            .font(.headline)
                        Text(asset.name)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(asset.type.displayName)
                            .font(.caption2)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(4)
                    }
                }
                .buttonStyle(PlainButtonStyle())
            }
            .searchable(text: $searchText)
            .navigationTitle("Select Asset")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .task {
                await assetService.searchAssets()
            }
            .onChange(of: searchText) { _, newValue in
                Task {
                    await assetService.searchAssets(query: newValue)
                }
            }
        }
    }
}
```

### 7. Chart Service Implementation

**Files to create:**
- `ios-app/InvestmentTracker/Services/ChartService.swift`
- `ios-app/InvestmentTracker/Views/Portfolio/PortfolioChartView.swift`

**Dependencies to add:**
- Charts framework (iOS 16+) or SwiftUI Charts

**ChartService implementation:**
```swift
import Foundation
import Charts

@MainActor
class ChartService: ObservableObject {
    static let shared = ChartService()
    
    @Published var portfolioHistory: [PortfolioHistoryPoint] = []
    @Published var assetAllocation: [AssetAllocationPoint] = []
    @Published var isLoading = false
    
    private init() {}
    
    func fetchPortfolioHistory(period: TimePeriod = .oneMonth) async {
        isLoading = true
        
        do {
            let history: [PortfolioHistoryPoint] = try await APIClient.shared.request(
                endpoint: "/portfolio/history?period=\(period.rawValue)",
                method: .GET
            )
            
            portfolioHistory = history
            
        } catch {
            print("Error fetching portfolio history: \(error)")
        }
        
        isLoading = false
    }
    
    func fetchAssetAllocation() async {
        do {
            let allocation: [AssetAllocationPoint] = try await APIClient.shared.request(
                endpoint: "/portfolio/allocation",
                method: .GET
            )
            
            assetAllocation = allocation
            
        } catch {
            print("Error fetching asset allocation: \(error)")
        }
    }
}

struct PortfolioHistoryPoint: Codable, Identifiable {
    let id = UUID()
    let date: Date
    let value: Decimal
    
    private enum CodingKeys: String, CodingKey {
        case date, value
    }
}

struct AssetAllocationPoint: Codable, Identifiable {
    let id = UUID()
    let assetSymbol: String
    let assetName: String
    let value: Decimal
    let percentage: Decimal
    
    private enum CodingKeys: String, CodingKey {
        case assetSymbol, assetName, value, percentage
    }
}

enum TimePeriod: String, CaseIterable {
    case oneWeek = "1W"
    case oneMonth = "1M"
    case threeMonths = "3M"
    case sixMonths = "6M"
    case oneYear = "1Y"
    case all = "ALL"
}
```

**Portfolio Chart View:**
```swift
import SwiftUI
import Charts

struct PortfolioChartView: View {
    @StateObject private var chartService = ChartService.shared
    @State private var selectedPeriod: TimePeriod = .oneMonth
    
    var body: some View {
        VStack(spacing: 16) {
            // Period Selector
            Picker("Time Period", selection: $selectedPeriod) {
                ForEach(TimePeriod.allCases, id: \.self) { period in
                    Text(period.rawValue).tag(period)
                }
            }
            .pickerStyle(SegmentedPickerStyle())
            
            // Portfolio Value Chart
            if chartService.isLoading {
                ProgressView()
                    .frame(height: 200)
            } else {
                Chart(chartService.portfolioHistory) { point in
                    LineMark(
                        x: .value("Date", point.date),
                        y: .value("Value", point.value)
                    )
                    .foregroundStyle(.blue)
                }
                .frame(height: 200)
                .chartYAxis {
                    AxisMarks(position: .leading)
                }
                .chartXAxis {
                    AxisMarks(values: .stride(by: .day, count: 7)) { _ in
                        AxisTick()
                        AxisValueLabel(format: .dateTime.month().day())
                    }
                }
            }
            
            // Asset Allocation Chart
            Chart(chartService.assetAllocation, id: \.id) { point in
                SectorMark(
                    angle: .value("Percentage", point.percentage),
                    innerRadius: .ratio(0.5),
                    outerRadius: .ratio(1.0)
                )
                .foregroundStyle(by: .value("Asset", point.assetSymbol))
            }
            .frame(height: 200)
            .chartLegend(position: .bottom, alignment: .center)
        }
        .padding()
        .task {
            await chartService.fetchPortfolioHistory(period: selectedPeriod)
            await chartService.fetchAssetAllocation()
        }
        .onChange(of: selectedPeriod) { _, newPeriod in
            Task {
                await chartService.fetchPortfolioHistory(period: newPeriod)
            }
        }
    }
}
```

### 8. Update Navigation and Integration

**Update DashboardView to include Add Acquisition:**
```swift
// In DashboardView.swift, replace the TODO button:
Button(action: {
    showingAddAcquisition = true
}) {
    Label("Add Acquisition", systemImage: "plus")
}
.sheet(isPresented: $showingAddAcquisition) {
    AddAcquisitionView()
}
```

**Update AuthViewModel to handle OAuth:**
```swift
// Add to AuthViewModel.swift:
func handleOAuthResponse(_ response: AuthResponse) async {
    await MainActor.run {
        self.currentUser = response.user
        self.isAuthenticated = true
    }
    
    // Save tokens
    KeychainService.shared.saveTokens(
        accessToken: response.accessToken,
        refreshToken: response.refreshToken
    )
}
```

## 📦 Dependencies to Add

**Add to iOS project:**
1. GoogleSignIn SDK via Swift Package Manager:
   - `https://github.com/google/GoogleSignIn-iOS`

2. Enable Apple Sign In capability in Xcode

3. Add GoogleService-Info.plist to project

## ⚙️ Configuration

**Add to Info.plist:**
```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLName</key>
        <string>REVERSED_CLIENT_ID</string>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>YOUR_REVERSED_CLIENT_ID</string>
        </array>
    </dict>
</array>
```

**Add to AppDelegate or App.swift:**
```swift
import GoogleSignIn

// In application(_:didFinishLaunchingWithOptions:) or App init
GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: "YOUR_CLIENT_ID")
```

## 🧪 Testing Requirements

**Create test files:**
- `OAuthServiceTests.swift`
- `PortfolioServiceTests.swift`
- `AssetServiceTests.swift`
- `APIClientTests.swift`
- `ChartServiceTests.swift`

## 🎯 Success Criteria

After implementing these changes:
- [ ] All TODO comments resolved
- [ ] OAuth login functional for Google and Apple
- [ ] Token refresh working automatically
- [ ] Forgot password flow complete
- [ ] Add acquisition form functional
- [ ] Portfolio charts displaying real data
- [ ] Asset search and selection working
- [ ] Comprehensive test coverage
- [ ] Smooth navigation between screens
- [ ] Error handling and loading states implemented