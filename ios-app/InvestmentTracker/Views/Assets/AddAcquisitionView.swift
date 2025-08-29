import SwiftUI

struct AddAcquisitionView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var assetType: AssetType = .preciousMetal
    @State private var assetSymbol = ""
    @State private var assetName = ""
    @State private var quantity = ""
    @State private var unitPrice = ""
    @State private var currency = "USD"
    @State private var fee = ""
    @State private var acquisitionDate = Date()
    @State private var notes = ""
    @State private var tags: [String] = []
    @State private var tagInput = ""
    
    @State private var isLoading = false
    @State private var errorMessage: String?
    
    var body: some View {
        NavigationView {
            Form {
                Section("Asset Information") {
                    Picker("Asset Type", selection: $assetType) {
                        ForEach(AssetType.allCases, id: \.self) { type in
                            Text(type.displayName).tag(type)
                        }
                    }
                    
                    TextField("Asset Symbol", text: $assetSymbol)
                        .autocapitalization(.allCharacters)
                    
                    TextField("Asset Name (Optional)", text: $assetName)
                }
                
                Section("Transaction Details") {
                    HStack {
                        TextField("Quantity", text: $quantity)
                            .keyboardType(.decimalPad)
                        Text("units")
                            .foregroundColor(.secondary)
                    }
                    
                    HStack {
                        TextField("Unit Price", text: $unitPrice)
                            .keyboardType(.decimalPad)
                        
                        TextField("Currency", text: $currency)
                            .frame(width: 60)
                    }
                    
                    if let unitPriceValue = Double(unitPrice),
                       let quantityValue = Double(quantity) {
                        HStack {
                            Text("TRY Estimate")
                                .foregroundColor(.secondary)
                            Spacer()
                            Text("₺\(unitPriceValue * quantityValue * 32.5, specifier: "%.2f")")
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    HStack {
                        TextField("Fee (Optional)", text: $fee)
                            .keyboardType(.decimalPad)
                        Text(currency)
                            .foregroundColor(.secondary)
                    }
                }
                
                Section("Date & Notes") {
                    DatePicker("Acquisition Date", selection: $acquisitionDate, displayedComponents: .date)
                    
                    TextField("Notes (Optional)", text: $notes, axis: .vertical)
                        .lineLimit(3...6)
                    
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Tags")
                            .font(.headline)
                        
                        if !tags.isEmpty {
                            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 8) {
                                ForEach(tags, id: \.self) { tag in
                                    HStack {
                                        Text(tag)
                                            .font(.caption)
                                        Button(action: {
                                            tags.removeAll { $0 == tag }
                                        }) {
                                            Image(systemName: "xmark.circle.fill")
                                                .font(.caption)
                                        }
                                    }
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color.blue.opacity(0.1))
                                    .foregroundColor(.blue)
                                    .cornerRadius(12)
                                }
                            }
                        }
                        
                        HStack {
                            TextField("Add tag", text: $tagInput)
                                .onSubmit {
                                    addTag()
                                }
                            Button("Add") {
                                addTag()
                            }
                            .disabled(tagInput.isEmpty)
                        }
                    }
                }
            }
            .navigationTitle("Add Acquisition")
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(true)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Save") {
                        Task {
                            await saveAcquisition()
                        }
                    }
                    .disabled(!isFormValid || isLoading)
                }
            }
            .alert("Error", isPresented: .constant(errorMessage != nil)) {
                Button("OK") {
                    errorMessage = nil
                }
            } message: {
                Text(errorMessage ?? "")
            }
        }
    }
    
    private var isFormValid: Bool {
        !assetSymbol.isEmpty &&
        !quantity.isEmpty &&
        !unitPrice.isEmpty &&
        Double(quantity) != nil &&
        Double(unitPrice) != nil
    }
    
    private func addTag() {
        let trimmed = tagInput.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty && !tags.contains(trimmed) {
            tags.append(trimmed)
            tagInput = ""
        }
    }
    
    private func saveAcquisition() async {
        isLoading = true
        errorMessage = nil
        
        guard let quantityValue = Decimal(string: quantity),
              let unitPriceValue = Decimal(string: unitPrice) else {
            errorMessage = "Invalid quantity or unit price"
            isLoading = false
            return
        }
        
        let request = AcquisitionRequest(
            assetType: assetType,
            assetSymbol: assetSymbol,
            assetName: assetName.isEmpty ? nil : assetName,
            quantity: quantityValue,
            unitPrice: unitPriceValue,
            currency: currency.isEmpty ? nil : currency,
            fee: fee.isEmpty ? nil : Decimal(string: fee),
            acquisitionDate: acquisitionDate,
            notes: notes.isEmpty ? nil : notes,
            tags: tags.isEmpty ? nil : tags
        )
        
        do {
            let _: AcquisitionRequest = try await APIClient.shared.request(
                endpoint: "/portfolio/acquisitions",
                method: .POST,
                body: request
            )
            
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
}

#Preview {
    AddAcquisitionView()
}