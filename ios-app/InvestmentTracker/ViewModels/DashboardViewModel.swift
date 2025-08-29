import Foundation
import Combine

@MainActor
class DashboardViewModel: ObservableObject {
    @Published var portfolioSummary: PortfolioSummary?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let apiClient = APIClient.shared
    
    func loadPortfolioSummary() async {
        isLoading = true
        errorMessage = nil
        
        do {
            let summary: PortfolioSummary = try await apiClient.request(
                endpoint: "/portfolio/summary",
                method: .GET
            )
            portfolioSummary = summary
        } catch {
            errorMessage = error.localizedDescription
            // For demo purposes, use mock data if API fails
            portfolioSummary = createMockPortfolioSummary()
        }
        
        isLoading = false
    }
    
    private func createMockPortfolioSummary() -> PortfolioSummary {
        return PortfolioSummary(
            totalValueTRY: 100000.00,
            todayChangePercent: 2.5,
            totalUnrealizedPLTRY: 5000.00,
            totalUnrealizedPLPercent: 5.26,
            status: "UP",
            estimatedProceedsTRY: 95000.00,
            costBasisTRY: 90000.00,
            unrealizedGainLossTRY: 5000.00,
            unrealizedGainLossPercent: 5.56,
            fxInfluenceTRY: 1000.00
        )
    }
}