import SwiftUI

struct HistoryView: View {
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Text("Import & History Sources")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .padding(.top)
                
                VStack(spacing: 16) {
                    Text("Track your investment history by importing data from CSV and Excel files. Keep a complete record of all your transactions.")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                    
                    Button(action: {
                        // TODO: Implement file import
                    }) {
                        HStack {
                            Image(systemName: "square.and.arrow.down")
                            Text("Import from File")
                                .fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                    }
                    .padding(.horizontal)
                    
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Image(systemName: "info.circle")
                                .foregroundColor(.blue)
                            Text("Supported Formats")
                                .font(.headline)
                                .fontWeight(.semibold)
                        }
                        
                        Text("We support importing from CSV and Excel files with acquisition data, disposals, dividends, fees, and taxes. These are standard formats used by most brokers and investment platforms.")
                            .font(.body)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .background(Color.blue.opacity(0.1))
                    .cornerRadius(12)
                    .padding(.horizontal)
                    
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Last Import")
                                    .font(.headline)
                                    .fontWeight(.semibold)
                                
                                Text("Imported 100 acquisitions")
                                    .font(.body)
                                    .foregroundColor(.secondary)
                                
                                Text("January 1, 2024")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            
                            Spacer()
                            
                            Image(systemName: "chevron.right")
                                .foregroundColor(.gray)
                        }
                    }
                    .padding()
                    .background(Color.white)
                    .cornerRadius(12)
                    .shadow(radius: 1)
                    .padding(.horizontal)
                }
                
                Spacer()
            }
            .navigationTitle("History")
            .navigationBarTitleDisplayMode(.large)
        }
    }
}

#Preview {
    HistoryView()
}