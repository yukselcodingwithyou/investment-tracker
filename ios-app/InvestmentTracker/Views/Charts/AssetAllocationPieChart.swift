import SwiftUI
import Charts

struct AssetAllocationPieChart: View {
    let data: [AssetAllocationPoint]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Asset Allocation")
                .font(.headline)
                .fontWeight(.semibold)
            
            if data.isEmpty {
                Circle()
                    .frame(width: 150, height: 150)
                    .foregroundColor(Color.orange.opacity(0.3))
                    .overlay(
                        Text("No allocation\ndata")
                            .multilineTextAlignment(.center)
                            .foregroundColor(.orange)
                    )
            } else {
                HStack {
                    // Pie Chart
                    Chart(data) { allocation in
                        SectorMark(
                            angle: .value("Percentage", allocation.percentage),
                            innerRadius: .ratio(0.4),
                            angularInset: 1.5
                        )
                        .foregroundStyle(colorForAssetType(allocation.assetType))
                        .opacity(0.8)
                    }
                    .frame(width: 150, height: 150)
                    
                    Spacer()
                    
                    // Legend
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(data) { allocation in
                            HStack(spacing: 8) {
                                Circle()
                                    .fill(colorForAssetType(allocation.assetType))
                                    .frame(width: 12, height: 12)
                                
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(allocation.assetName)
                                        .font(.caption)
                                        .fontWeight(.medium)
                                    
                                    Text("\(allocation.percentage, specifier: "%.1f")%")
                                        .font(.caption2)
                                        .foregroundColor(.secondary)
                                }
                                
                                Spacer()
                                
                                Text("₺\(allocation.value, specifier: "%.0f")")
                                    .font(.caption)
                                    .fontWeight(.semibold)
                            }
                        }
                    }
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 2, x: 0, y: 1)
    }
    
    private func colorForAssetType(_ type: String) -> Color {
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

#Preview {
    AssetAllocationPieChart(data: [
        AssetAllocationPoint(assetType: "EQUITY", assetName: "Stocks", value: 5000, percentage: 50, color: "#0066CC"),
        AssetAllocationPoint(assetType: "FX", assetName: "Forex", value: 3000, percentage: 30, color: "#00AA00"),
        AssetAllocationPoint(assetType: "PRECIOUS_METAL", assetName: "Gold", value: 2000, percentage: 20, color: "#FF8800")
    ])
    .padding()
}