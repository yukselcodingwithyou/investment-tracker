import SwiftUI

struct TopMoversView: View {
    let topMovers: [TopMover]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Top Movers")
                .font(.headline)
                .fontWeight(.semibold)
            
            if topMovers.isEmpty {
                VStack {
                    Image(systemName: "chart.line.uptrend.xyaxis")
                        .font(.largeTitle)
                        .foregroundColor(.secondary)
                    
                    Text("No movers data available")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, minHeight: 100)
            } else {
                LazyVStack(spacing: 12) {
                    ForEach(topMovers) { mover in
                        TopMoverRow(mover: mover)
                    }
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 2, x: 0, y: 1)
    }
}

struct TopMoverRow: View {
    let mover: TopMover
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(mover.assetSymbol)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                
                Text(mover.assetName)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            VStack(alignment: .trailing, spacing: 4) {
                Text("₺\(mover.value, specifier: "%.2f")")
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                HStack(spacing: 4) {
                    Image(systemName: mover.direction == "UP" ? "arrow.up.right" : "arrow.down.right")
                        .font(.caption)
                        .foregroundColor(mover.direction == "UP" ? .green : .red)
                    
                    Text("\(mover.changePercent, specifier: "%.2f")%")
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(mover.direction == "UP" ? .green : .red)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    TopMoversView(topMovers: [
        TopMover(assetId: "1", assetSymbol: "AAPL", assetName: "Apple Inc.", currentPrice: 150.0, change: 5.0, changePercent: 3.45, value: 1500.0, direction: "UP"),
        TopMover(assetId: "2", assetSymbol: "GOOGL", assetName: "Alphabet Inc.", currentPrice: 2500.0, change: -25.0, changePercent: -1.0, value: 2500.0, direction: "DOWN"),
        TopMover(assetId: "3", assetSymbol: "TSLA", assetName: "Tesla Inc.", currentPrice: 800.0, change: 40.0, changePercent: 5.26, value: 800.0, direction: "UP")
    ])
    .padding()
}