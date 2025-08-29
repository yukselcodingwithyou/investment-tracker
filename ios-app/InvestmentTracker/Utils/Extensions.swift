import Foundation

extension Formatter {
    static let turkishCurrency: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.locale = Locale(identifier: "tr_TR")
        formatter.numberStyle = .currency
        formatter.currencySymbol = "₺"
        formatter.maximumFractionDigits = 2
        return formatter
    }()
    
    static let turkishDecimal: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.locale = Locale(identifier: "tr_TR")
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 2
        return formatter
    }()
    
    static let percentage: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.numberStyle = .percent
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 2
        return formatter
    }()
}

extension Decimal {
    var formattedAsTurkishCurrency: String {
        return Formatter.turkishCurrency.string(for: self) ?? "₺0.00"
    }
    
    var formattedAsTurkishDecimal: String {
        return Formatter.turkishDecimal.string(for: self) ?? "0.00"
    }
    
    var formattedAsPercentage: String {
        return Formatter.percentage.string(for: self / 100) ?? "0.00%"
    }
}

extension Double {
    var formattedAsTurkishCurrency: String {
        return Formatter.turkishCurrency.string(for: self) ?? "₺0.00"
    }
    
    var formattedAsTurkishDecimal: String {
        return Formatter.turkishDecimal.string(for: self) ?? "0.00"
    }
    
    var formattedAsPercentage: String {
        return Formatter.percentage.string(for: self / 100) ?? "0.00%"
    }
}

// MARK: - Color Extensions for Semantic Colors
import SwiftUI

extension Color {
    static let gainGreen = Color.green
    static let lossRed = Color.red
    
    static func gainLossColor(for value: Decimal) -> Color {
        return value >= 0 ? .gainGreen : .lossRed
    }
    
    static func gainLossColor(for value: Double) -> Color {
        return value >= 0 ? .gainGreen : .lossRed
    }
}

// MARK: - Date Extensions
extension Date {
    var iso8601String: String {
        let formatter = ISO8601DateFormatter()
        return formatter.string(from: self)
    }
}

extension String {
    var iso8601Date: Date? {
        let formatter = ISO8601DateFormatter()
        return formatter.date(from: self)
    }
}