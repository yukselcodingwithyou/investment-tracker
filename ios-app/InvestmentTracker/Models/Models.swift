import Foundation

// MARK: - User Models
struct User: Codable, Identifiable {
    let id: String
    let name: String
    let email: String
    let emailVerified: Bool
    let providers: [OAuthProvider]
    let baseCurrency: String
    let timezone: String
    let createdAt: Date
}

enum OAuthProvider: String, Codable, CaseIterable {
    case google = "GOOGLE"
    case apple = "APPLE"
}

// MARK: - Authentication Models
struct AuthResponse: Codable {
    let accessToken: String
    let refreshToken: String
    let tokenType: String
    let user: User
}

struct LoginRequest: Codable {
    let emailOrUsername: String
    let password: String
}

struct SignUpRequest: Codable {
    let name: String
    let email: String
    let password: String
    let confirmPassword: String
}

// MARK: - Portfolio Models
struct PortfolioSummary: Codable {
    let totalValueTRY: Decimal
    let todayChangePercent: Decimal
    let totalUnrealizedPLTRY: Decimal
    let totalUnrealizedPLPercent: Decimal
    let status: String
    let estimatedProceedsTRY: Decimal
    let costBasisTRY: Decimal
    let unrealizedGainLossTRY: Decimal
    let unrealizedGainLossPercent: Decimal
    let fxInfluenceTRY: Decimal
}

struct Asset: Codable, Identifiable {
    let id: String
    let symbol: String
    let name: String
    let type: AssetType
    let currency: String
    
    // Portfolio-specific data
    let quantity: Decimal?
    let quantityUnit: String?
    let currentPrice: Decimal?
    let dayChangePercent: Decimal?
    let averageAcquisitionPrice: Decimal?
    let unrealizedPLTRY: Decimal?
    let unrealizedPLPercent: Decimal?
    let marketValueTRY: Decimal?
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
            return "FX"
        case .equity:
            return "Equity"
        case .fund:
            return "Fund"
        }
    }
}

struct AcquisitionRequest: Codable {
    let assetType: AssetType
    let assetSymbol: String
    let assetName: String?
    let quantity: Decimal
    let unitPrice: Decimal
    let currency: String?
    let fee: Decimal?
    let acquisitionDate: Date
    let notes: String?
    let tags: [String]?
}