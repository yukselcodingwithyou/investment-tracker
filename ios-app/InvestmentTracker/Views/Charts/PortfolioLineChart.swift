import SwiftUI
import Charts

struct PortfolioLineChart: View {
    let data: [PortfolioHistoryPoint]
    @State private var selectedPeriod: String = "30D"
    
    private let periods = ["7D", "30D", "90D", "1Y", "ALL"]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Portfolio Value")
                    .font(.headline)
                    .fontWeight(.semibold)
                
                Spacer()
                
                // Period selector
                Picker("Period", selection: $selectedPeriod) {
                    ForEach(periods, id: \.self) { period in
                        Text(period).tag(period)
                    }
                }
                .pickerStyle(SegmentedPickerStyle())
                .frame(width: 200)
            }
            
            if data.isEmpty {
                Rectangle()
                    .frame(height: 200)
                    .foregroundColor(Color.blue.opacity(0.1))
                    .cornerRadius(8)
                    .overlay(
                        Text("No chart data available")
                            .foregroundColor(.secondary)
                    )
            } else {
                Chart(data) { point in
                    LineMark(
                        x: .value("Date", point.date),
                        y: .value("Value", point.value)
                    )
                    .foregroundStyle(.blue)
                    .lineStyle(StrokeStyle(lineWidth: 2))
                    
                    AreaMark(
                        x: .value("Date", point.date),
                        y: .value("Value", point.value)
                    )
                    .foregroundStyle(
                        LinearGradient(
                            colors: [.blue.opacity(0.3), .blue.opacity(0.1)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                }
                .frame(height: 200)
                .chartXAxis {
                    AxisMarks(values: .stride(by: .day, count: 7)) { _ in
                        AxisValueLabel(format: .dateTime.month(.abbreviated).day())
                        AxisGridLine()
                    }
                }
                .chartYAxis {
                    AxisMarks { _ in
                        AxisValueLabel()
                        AxisGridLine()
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

#Preview {
    PortfolioLineChart(data: [
        PortfolioHistoryPoint(date: Date().addingTimeInterval(-86400 * 30), value: 10000, change: 0, changePercent: 0),
        PortfolioHistoryPoint(date: Date().addingTimeInterval(-86400 * 20), value: 10500, change: 500, changePercent: 5),
        PortfolioHistoryPoint(date: Date().addingTimeInterval(-86400 * 10), value: 9800, change: -700, changePercent: -6.67),
        PortfolioHistoryPoint(date: Date(), value: 11200, change: 1400, changePercent: 14.29)
    ])
    .padding()
}