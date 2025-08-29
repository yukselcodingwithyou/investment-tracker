# Investment Tracker

A production-quality Investment Tracker application with Spring Boot backend, iOS SwiftUI, and Android Jetpack Compose clients.

## Features

### Core Functionality
- **Acquisitions-only tracking** (no in-app trading)
- User records purchases; app shows mark-to-market P/L in TRY
- UP/DOWN status and full history
- Read-only imported disposals/dividends/fees/taxes support
- Turkish locale (tr_TR) for currency/number formatting
- Base currency: TRY

### Authentication
- Email/password authentication
- Social login (Google, Apple) - framework ready
- JWT token management with secure storage
- Refresh token handling

### Portfolio Management
- Portfolio summary with P/L calculations
- Asset management and tracking
- Historical data import via CSV
- Multi-asset support (Precious Metals, FX, Equity, Funds)

## Architecture

### Backend (Spring Boot 3 + Java 17)
- **Framework**: Spring Boot 3.2.1 with Java 17
- **Database**: MongoDB with indexes
- **Security**: Spring Security + JWT + OAuth2
- **API**: RESTful APIs with OpenAPI documentation
- **Architecture**: Controller → Service → Repository → Domain
- **Validation**: Jakarta Validation with RFC7807 Problem+JSON errors
- **Caching**: Caffeine for price data
- **Testing**: Unit tests, @DataMongoTest, Testcontainers

### iOS App (SwiftUI + MVVM)
- **Target**: iOS 17+, Swift 5.9
- **Architecture**: MVVM with dependency injection
- **UI**: SwiftUI with NavigationStack
- **Networking**: URLSession with async/await
- **Storage**: Keychain for secure token storage
- **Locale**: Turkish (tr_TR) currency formatting
- **Testing**: ViewModel unit tests, UI snapshot tests

### Android App (Kotlin + Jetpack Compose)
- **Target**: Android API 24+, Kotlin 1.9
- **Architecture**: MVVM with Hilt DI
- **UI**: Jetpack Compose with Material 3
- **Networking**: Retrofit + OkHttp + Kotlinx Serialization
- **Storage**: EncryptedSharedPreferences
- **Locale**: Turkish (tr_TR) formatting
- **Testing**: ViewModel unit tests with Turbine

## Getting Started

### Prerequisites
- **Backend**: Java 17, MongoDB
- **iOS**: Xcode 15+, iOS 17+ device/simulator  
- **Android**: Android Studio, Android 7.0+ device/emulator

### Backend Setup
```bash
cd backend
./gradlew bootRun
```

Access:
- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/v3/api-docs

### iOS Setup
1. Open `ios-app/InvestmentTracker.xcodeproj` in Xcode
2. Configure development team
3. Build and run

### Android Setup
1. Open `android-app` in Android Studio
2. Sync project
3. Build and run

## API Endpoints

### Authentication
- `POST /api/auth/signup` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/oauth/google` - Google OAuth
- `POST /api/auth/oauth/apple` - Apple OAuth
- `GET /api/auth/me` - Current user info

### Portfolio
- `GET /api/portfolio/summary` - Portfolio summary
- `POST /api/portfolio/acquisitions` - Add acquisition
- `GET /api/assets` - List assets with search/filter
- `GET /api/assets/{id}` - Asset details
- `POST /api/imports/csv` - Import CSV data

## Data Models

### Core Entities
```
User: id, name, email, providers, baseCurrency, timezone
Asset: id, symbol, name, type, currency
AcquisitionLot: userId, assetId, quantity, unitPrice, fee, date
PriceSnapshot: assetId, price, currency, timestamp
ImportedEvent: userId, assetId, type, date, amount
```

### Asset Types
- `PRECIOUS_METAL` - Gold, Silver, etc.
- `FX` - Currency pairs (USD/TRY, EUR/USD)
- `EQUITY` - Individual stocks
- `FUND` - Mutual funds, ETFs

## Security

### Authentication Flow
1. User authenticates via email/password or OAuth
2. Server returns JWT access token + refresh token
3. Client stores tokens securely (Keychain/EncryptedSharedPreferences)
4. Access token included in Authorization header
5. Auto-refresh on 401 responses

### Password Security
- Argon2id hashing (BCrypt fallback)
- Minimum 8 character passwords
- OAuth2 token validation for Google/Apple

## UI/UX Guidelines

### Design Principles
- **Colors**: Neutral background, semantic gains (green) / losses (red)
- **Typography**: Large numeric emphasis, decimal-aligned lists
- **Accessibility**: 44pt/48dp minimum tap targets, VoiceOver support
- **Responsiveness**: Pull-to-refresh, graceful loading/error states

### Navigation
- **Bottom tabs**: Dashboard, Assets, History, Settings
- **Consistent**: Same structure across iOS/Android
- **Deep linking**: Asset details, acquisition forms

## Development Status

### ✅ Completed
- Backend API foundation with auth and portfolio endpoints
- iOS SwiftUI app with complete authentication and main screens
- Android Jetpack Compose app with auth and navigation
- Turkish locale support and currency formatting
- Secure token management
- OpenAPI documentation

### 🚧 In Progress
- CSV import/export functionality
- Price data management and charts
- Social login integration
- Comprehensive testing

### 📋 Planned
- Real-time price updates
- Push notifications
- Advanced portfolio analytics
- Multi-currency support expansion

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes with tests
4. Submit a pull request

## License

This project is licensed under the MIT License.