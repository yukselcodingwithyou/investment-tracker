import Foundation
import SwiftUI

@MainActor
class ChartService: ObservableObject {
    static let shared = ChartService()
    
    @Published var portfolioHistory: [PortfolioHistoryPoint] = []
    @Published var assetAllocation: [AssetAllocationPoint] = []
    @Published var topMovers: [TopMover] = []
    @Published var analytics: PortfolioAnalytics?
    @Published var selectedPeriod: String = "30D"
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()
    
    private init() {}
    
    func fetchPortfolioHistory(period: String = "30D") async {
        isLoading = true
        errorMessage = nil
        selectedPeriod = period
        
        do {
            let history: [PortfolioHistoryPoint] = try await APIClient.shared.request(
                endpoint: "/portfolio/history?period=\(period)",
                method: .GET
            )
            
            portfolioHistory = history.sorted { $0.date < $1.date }
            
        } catch {
            errorMessage = "Failed to load portfolio history: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    func fetchAssetAllocation() async {
        isLoading = true
        errorMessage = nil
        
        do {
            let allocation: [AssetAllocationPoint] = try await APIClient.shared.request(
                endpoint: "/portfolio/allocation",
                method: .GET
            )
            
            assetAllocation = allocation
            
        } catch {
            errorMessage = "Failed to load asset allocation: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    func fetchTopMovers(limit: Int = 5) async {
        isLoading = true
        errorMessage = nil
        
        do {
            let movers: [TopMover] = try await APIClient.shared.request(
                endpoint: "/portfolio/top-movers?limit=\(limit)",
                method: .GET
            )
            
            topMovers = movers
            
        } catch {
            errorMessage = "Failed to load top movers: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    func fetchComprehensiveAnalytics(period: String = "30D") async {
        isLoading = true
        errorMessage = nil
        selectedPeriod = period
        
        do {
            let analyticsData: PortfolioAnalytics = try await APIClient.shared.request(
                endpoint: "/portfolio/analytics?period=\(period)",
                method: .GET
            )
            
            analytics = analyticsData
            portfolioHistory = analyticsData.portfolioHistory.sorted { $0.date < $1.date }
            assetAllocation = analyticsData.assetAllocation
            topMovers = analyticsData.topMovers
            
        } catch {
            errorMessage = "Failed to load analytics: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    func formatValue(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "TRY"
        formatter.maximumFractionDigits = 2
        return formatter.string(from: NSNumber(value: value)) ?? "₺0.00"
    }
    
    func formatPercentage(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .percent
        formatter.maximumFractionDigits = 2
        return formatter.string(from: NSNumber(value: value / 100)) ?? "0%"
    }
    
    func colorForAssetType(_ type: String) -> Color {
        switch type.uppercased() {
        case "EQUITY":
            return .blue
        case "FX":
            return .green
        case "PRECIOUS_METAL":
            return .orange
        case "FUND":
            return .purple
        default:
            return .gray
        }
    }
}