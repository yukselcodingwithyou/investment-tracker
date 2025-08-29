import SwiftUI

struct DashboardView: View {
    @StateObject private var viewModel = DashboardViewModel()
    @State private var showingAddAcquisition = false
    
    var body: some View {
        NavigationView {
            ScrollView {
                LazyVStack(spacing: 20) {
                    if viewModel.portfolioSummary == nil {
                        // Empty State
                        emptyStateView
                    } else {
                        // Populated State
                        populatedStateView
                    }
                }
                .padding()
            }
            .navigationTitle("Dashboard")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        // Settings action
                    }) {
                        Image(systemName: "gearshape")
                    }
                }
            }
            .refreshable {
                await viewModel.loadPortfolioSummary()
            }
            .sheet(isPresented: $showingAddAcquisition) {
                AddAcquisitionView()
            }
        }
        .task {
            await viewModel.loadPortfolioSummary()
        }
    }
    
    private var emptyStateView: some View {
        VStack(spacing: 24) {
            Spacer()
            
            VStack(spacing: 16) {
                Image(systemName: "plus.circle")
                    .font(.system(size: 60))
                    .foregroundColor(.blue)
                
                VStack(spacing: 8) {
                    Text("No acquisitions yet")
                        .font(.title2)
                        .fontWeight(.semibold)
                    
                    Text("Tap 'Add Acquisition' to get started.")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
            }
            .padding(32)
            .background(Color.gray.opacity(0.1))
            .cornerRadius(16)
            
            Spacer()
            
            // Sticky CTA Button
            Button(action: {
                showingAddAcquisition = true
            }) {
                HStack {
                    Image(systemName: "plus")
                    Text("Add Acquisition")
                        .fontWeight(.semibold)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .background(Color.blue)
                .foregroundColor(.white)
                .cornerRadius(10)
            }
        }
    }
    
    private var populatedStateView: some View {
        VStack(spacing: 20) {
            if let summary = viewModel.portfolioSummary {
                // Portfolio Summary Card
                portfolioSummaryCard(summary)
                
                // If Liquidated Now Card
                liquidationCard(summary)
                
                // Portfolio Value Chart
                portfolioChartCard
                
                // Allocation Chart
                allocationChartCard
                
                // Top Movers
                topMoversCard
            }
        }
    }
    
    private func portfolioSummaryCard(_ summary: PortfolioSummary) -> some View {
        VStack(spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Total Portfolio Value")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    Text("₺\(summary.totalValueTRY, specifier: "%.2f")")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                    
                    HStack {
                        Text("Today's Change: \(summary.todayChangePercent, specifier: "%.2f")%")
                            .font(.caption)
                        
                        Text("Total P/L: ₺\(summary.totalUnrealizedPLTRY, specifier: "%.2f") (\(summary.totalUnrealizedPLPercent, specifier: "%.2f")%)")
                            .font(.caption)
                    }
                    .foregroundColor(summary.totalUnrealizedPLTRY >= 0 ? .green : .red)
                }
                
                Spacer()
                
                VStack {
                    Text(summary.status)
                        .font(.caption)
                        .fontWeight(.semibold)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 4)
                        .background(summary.status == "UP" ? Color.green : Color.red)
                        .foregroundColor(.white)
                        .cornerRadius(20)
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(16)
        .shadow(radius: 2)
    }
    
    private func liquidationCard(_ summary: PortfolioSummary) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("If liquidated now")
                .font(.headline)
                .fontWeight(.semibold)
            
            HStack {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text("Estimated proceeds")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Spacer()
                        Text("₺\(summary.estimatedProceedsTRY, specifier: "%.2f")")
                            .font(.subheadline)
                            .fontWeight(.medium)
                    }
                    
                    HStack {
                        Text("Cost Basis")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Spacer()
                        Text("₺\(summary.costBasisTRY, specifier: "%.2f")")
                            .font(.subheadline)
                            .fontWeight(.medium)
                    }
                    
                    HStack {
                        Text("Unrealized Gain/Loss")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Spacer()
                        Text("₺\(summary.unrealizedGainLossTRY, specifier: "%.2f") (\(summary.unrealizedGainLossPercent, specifier: "%.2f")%)")
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(summary.unrealizedGainLossTRY >= 0 ? .green : .red)
                    }
                    
                    if summary.fxInfluenceTRY != 0 {
                        HStack {
                            Text("FX Influence")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text("₺\(summary.fxInfluenceTRY, specifier: "%.2f")")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(16)
        .shadow(radius: 2)
    }
    
    private var portfolioChartCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Portfolio Value")
                .font(.headline)
                .fontWeight(.semibold)
            
            // Chart placeholder
            Rectangle()
                .frame(height: 200)
                .foregroundColor(Color.blue.opacity(0.1))
                .cornerRadius(8)
                .overlay(
                    Text("Chart Placeholder")
                        .foregroundColor(.blue)
                )
            
            // Time period selector
            HStack {
                ForEach(["30D", "90D"], id: \.self) { period in
                    Button(period) {
                        // Handle period selection
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(period == "30D" ? Color.blue : Color.clear)
                    .foregroundColor(period == "30D" ? .white : .blue)
                    .cornerRadius(20)
                }
                Spacer()
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(16)
        .shadow(radius: 2)
    }
    
    private var allocationChartCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Allocation")
                .font(.headline)
                .fontWeight(.semibold)
            
            // Pie chart placeholder
            Circle()
                .frame(width: 150, height: 150)
                .foregroundColor(Color.orange.opacity(0.3))
                .overlay(
                    Text("Pie Chart\nPlaceholder")
                        .multilineTextAlignment(.center)
                        .foregroundColor(.orange)
                )
            
            // Legend placeholder
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Circle()
                        .frame(width: 12, height: 12)
                        .foregroundColor(.blue)
                    Text("Precious Metals")
                        .font(.caption)
                }
                HStack {
                    Circle()
                        .frame(width: 12, height: 12)
                        .foregroundColor(.green)
                    Text("Equities")
                        .font(.caption)
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(16)
        .shadow(radius: 2)
    }
    
    private var topMoversCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Top 5 Movers Today")
                .font(.headline)
                .fontWeight(.semibold)
            
            VStack(spacing: 8) {
                ForEach(1...3, id: \.self) { index in
                    HStack {
                        Text("Asset \(index)")
                            .font(.subheadline)
                        Spacer()
                        Text("+₺12.34")
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(.green)
                    }
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(16)
        .shadow(radius: 2)
    }
}

#Preview {
    DashboardView()
        .environmentObject(AuthService.shared)
}