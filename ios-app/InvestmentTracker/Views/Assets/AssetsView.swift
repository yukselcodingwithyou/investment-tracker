import SwiftUI

struct AssetsView: View {
    @State private var searchText = ""
    @State private var selectedFilter: AssetType? = nil
    @State private var showingAddAcquisition = false
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.gray)
                    TextField("Search assets...", text: $searchText)
                }
                .padding()
                .background(Color.gray.opacity(0.1))
                .cornerRadius(10)
                .padding(.horizontal)
                .padding(.top)
                
                // Filter chips
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        filterChip(title: "All", filter: nil)
                        ForEach(AssetType.allCases, id: \.self) { type in
                            filterChip(title: type.displayName, filter: type)
                        }
                    }
                    .padding(.horizontal)
                }
                .padding(.vertical)
                
                // Assets list
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(1...5, id: \.self) { index in
                            assetRow(index: index)
                        }
                    }
                    .padding()
                }
            }
            .navigationTitle("Investment Tracker")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        showingAddAcquisition = true
                    }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showingAddAcquisition) {
                AddAcquisitionView()
            }
        }
    }
    
    private func filterChip(title: String, filter: AssetType?) -> some View {
        Button(action: {
            selectedFilter = filter
        }) {
            Text(title)
                .font(.caption)
                .fontWeight(.medium)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(selectedFilter == filter ? Color.blue : Color.gray.opacity(0.2))
                .foregroundColor(selectedFilter == filter ? .white : .primary)
                .cornerRadius(20)
        }
    }
    
    private func assetRow(index: Int) -> some View {
        HStack(spacing: 16) {
            // Asset icon
            Circle()
                .frame(width: 40, height: 40)
                .foregroundColor(.blue.opacity(0.2))
                .overlay(
                    Text("XAU")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.blue)
                )
            
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text("XAU/USD")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    Spacer()
                    Text("$2,345.67")
                        .font(.subheadline)
                        .fontWeight(.medium)
                }
                
                HStack {
                    Text("2.5 oz")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("+0.53%")
                        .font(.caption)
                        .foregroundColor(.green)
                }
                
                HStack {
                    Text("Avg. Acq. Price: $2,300.00")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                }
                
                HStack {
                    Text("Unrealized P/L")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("₺1,250.00")
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.green)
                }
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(radius: 1)
    }
}

#Preview {
    AssetsView()
}