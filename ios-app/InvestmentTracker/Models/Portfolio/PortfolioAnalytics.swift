import Foundation

struct PortfolioHistoryPoint: Codable, Identifiable {
    let id = UUID()
    let date: Date
    let value: Double
    let change: Double
    let changePercent: Double
    
    private enum CodingKeys: String, CodingKey {
        case date, value, change, changePercent
    }
}

struct AssetAllocationPoint: Codable, Identifiable {
    let id = UUID()
    let assetType: String
    let assetName: String
    let value: Double
    let percentage: Double
    let color: String
    
    private enum CodingKeys: String, CodingKey {
        case assetType, assetName, value, percentage, color
    }
}

struct TopMover: Codable, Identifiable {
    let id = UUID()
    let assetId: String
    let assetSymbol: String
    let assetName: String
    let currentPrice: Double
    let change: Double
    let changePercent: Double
    let value: Double
    let direction: String
    
    private enum CodingKeys: String, CodingKey {
        case assetId, assetSymbol, assetName, currentPrice, change, changePercent, value, direction
    }
}

struct PortfolioAnalytics: Codable {
    let portfolioHistory: [PortfolioHistoryPoint]
    let assetAllocation: [AssetAllocationPoint]
    let topMovers: [TopMover]
    let totalReturn: Double?
    let totalReturnPercent: Double?
    let volatility: Double?
    let sharpeRatio: Double?
    let maxDrawdown: Double?
}