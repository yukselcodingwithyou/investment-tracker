# Deployment & Configuration Completion Prompt

## 🎯 Objective
Establish production-ready deployment and configuration infrastructure for the Investment Tracker application across Backend, iOS, and Android platforms.

## 🐳 Backend Deployment Implementation

### 1. Docker Configuration

**Create `backend/Dockerfile`:**
```dockerfile
FROM openjdk:17-jdk-slim as builder

WORKDIR /app
COPY gradle gradle
COPY build.gradle settings.gradle gradlew ./
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew build -x test

FROM openjdk:17-jre-slim

RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Create `docker-compose.yml`:**
```yaml
version: '3.8'

services:
  investment-tracker-backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - MONGODB_URI=mongodb://mongodb:27017/investment_tracker
      - REDIS_URL=redis://redis:6379
      - EXTERNAL_PRICE_API_URL=${PRICE_API_URL}
      - EXTERNAL_PRICE_API_KEY=${PRICE_API_KEY}
      - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
      - APPLE_CLIENT_ID=${APPLE_CLIENT_ID}
      - APPLE_CLIENT_SECRET=${APPLE_CLIENT_SECRET}
      - MAIL_HOST=${MAIL_HOST}
      - MAIL_USERNAME=${MAIL_USERNAME}
      - MAIL_PASSWORD=${MAIL_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - mongodb
      - redis
    networks:
      - investment-tracker-network
    restart: unless-stopped

  mongodb:
    image: mongo:6.0
    ports:
      - "27017:27017"
    environment:
      - MONGO_INITDB_ROOT_USERNAME=admin
      - MONGO_INITDB_ROOT_PASSWORD=${MONGO_PASSWORD}
      - MONGO_INITDB_DATABASE=investment_tracker
    volumes:
      - mongodb_data:/data/db
      - ./scripts/mongo-init.js:/docker-entrypoint-initdb.d/mongo-init.js:ro
    networks:
      - investment-tracker-network
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    networks:
      - investment-tracker-network
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - investment-tracker-backend
    networks:
      - investment-tracker-network
    restart: unless-stopped

volumes:
  mongodb_data:
  redis_data:

networks:
  investment-tracker-network:
    driver: bridge
```

### 2. Production Configuration

**Create `backend/src/main/resources/application-prod.yml`:**
```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  profiles:
    active: prod
  
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/investment_tracker}
      auto-index-creation: false
  
  data:
    redis:
      url: ${REDIS_URL:redis://localhost:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
  
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid,profile,email
          apple:
            client-id: ${APPLE_CLIENT_ID}
            client-secret: ${APPLE_CLIENT_SECRET}

  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          ssl:
            trust: ${MAIL_HOST:smtp.gmail.com}

logging:
  level:
    com.yuksel.investmenttracker: INFO
    org.springframework.security: INFO
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /var/log/investment-tracker/application.log

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  health:
    redis:
      enabled: true
    mongo:
      enabled: true

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000 # 24 hours
    refresh-expiration: 604800000 # 7 days
  
  cors:
    allowed-origins: ${ALLOWED_ORIGINS:https://investmenttracker.com,https://app.investmenttracker.com}
    allowed-methods: GET,POST,PUT,DELETE,OPTIONS
    allowed-headers: "*"
    allow-credentials: true
  
  rate-limiting:
    enabled: true
    requests-per-minute: 100
    
  mail:
    from: ${MAIL_FROM:noreply@investmenttracker.com}
  
  frontend:
    url: ${FRONTEND_URL:https://app.investmenttracker.com}

external:
  price-api:
    url: ${EXTERNAL_PRICE_API_URL}
    key: ${EXTERNAL_PRICE_API_KEY}
    timeout: 5000
    retry-attempts: 3
```

**Create `backend/src/main/resources/application-docker.yml`:**
```yaml
spring:
  profiles:
    include: prod
  
  data:
    mongodb:
      uri: mongodb://mongodb:27017/investment_tracker
    redis:
      url: redis://redis:6379

logging:
  file:
    name: /app/logs/application.log
```

### 3. Kubernetes Deployment

**Create `k8s/namespace.yaml`:**
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: investment-tracker
```

**Create `k8s/configmap.yaml`:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: investment-tracker-config
  namespace: investment-tracker
data:
  SPRING_PROFILES_ACTIVE: "prod,k8s"
  MONGODB_URI: "mongodb://mongodb-service:27017/investment_tracker"
  REDIS_URL: "redis://redis-service:6379"
  FRONTEND_URL: "https://app.investmenttracker.com"
```

**Create `k8s/secret.yaml`:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: investment-tracker-secrets
  namespace: investment-tracker
type: Opaque
data:
  JWT_SECRET: # base64 encoded
  MONGO_PASSWORD: # base64 encoded
  REDIS_PASSWORD: # base64 encoded
  GOOGLE_CLIENT_SECRET: # base64 encoded
  APPLE_CLIENT_SECRET: # base64 encoded
  MAIL_PASSWORD: # base64 encoded
  EXTERNAL_PRICE_API_KEY: # base64 encoded
```

**Create `k8s/deployment.yaml`:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: investment-tracker-backend
  namespace: investment-tracker
spec:
  replicas: 3
  selector:
    matchLabels:
      app: investment-tracker-backend
  template:
    metadata:
      labels:
        app: investment-tracker-backend
    spec:
      containers:
      - name: investment-tracker-backend
        image: your-registry/investment-tracker-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          valueFrom:
            configMapKeyRef:
              name: investment-tracker-config
              key: SPRING_PROFILES_ACTIVE
        - name: MONGODB_URI
          valueFrom:
            configMapKeyRef:
              name: investment-tracker-config
              key: MONGODB_URI
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: investment-tracker-secrets
              key: JWT_SECRET
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

**Create `k8s/service.yaml`:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: investment-tracker-backend-service
  namespace: investment-tracker
spec:
  selector:
    app: investment-tracker-backend
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080
  type: ClusterIP
```

**Create `k8s/ingress.yaml`:**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: investment-tracker-ingress
  namespace: investment-tracker
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
spec:
  tls:
  - hosts:
    - api.investmenttracker.com
    secretName: investment-tracker-tls
  rules:
  - host: api.investmenttracker.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: investment-tracker-backend-service
            port:
              number: 80
```

### 4. Monitoring and Logging

**Create `k8s/monitoring/prometheus-config.yaml`:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: investment-tracker
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
    scrape_configs:
    - job_name: 'investment-tracker'
      kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
          - investment-tracker
      relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
```

**Create `docker-compose.monitoring.yml`:**
```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
    networks:
      - investment-tracker-network

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana-dashboards:/var/lib/grafana/dashboards
    networks:
      - investment-tracker-network

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    networks:
      - investment-tracker-network

  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch
    networks:
      - investment-tracker-network

volumes:
  prometheus_data:
  grafana_data:
  elasticsearch_data:

networks:
  investment-tracker-network:
    external: true
```

## 📱 iOS Deployment Configuration

### 1. Build Configuration

**Create `ios-app/InvestmentTracker/Configuration/Config.plist`:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>API_BASE_URL</key>
    <string>https://api.investmenttracker.com</string>
    <key>GOOGLE_CLIENT_ID</key>
    <string>$(GOOGLE_CLIENT_ID)</string>
    <key>APPLE_CLIENT_ID</key>
    <string>$(APPLE_CLIENT_ID)</string>
    <key>SENTRY_DSN</key>
    <string>$(SENTRY_DSN)</string>
    <key>ANALYTICS_ENABLED</key>
    <true/>
</dict>
</plist>
```

**Create environment-specific configurations:**

`ios-app/InvestmentTracker/Configuration/Config-Debug.plist`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>API_BASE_URL</key>
    <string>http://localhost:8080/api</string>
    <key>ANALYTICS_ENABLED</key>
    <false/>
</dict>
</plist>
```

`ios-app/InvestmentTracker/Configuration/Config-Release.plist`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>API_BASE_URL</key>
    <string>https://api.investmenttracker.com</string>
    <key>ANALYTICS_ENABLED</key>
    <true/>
</dict>
</plist>
```

**Create configuration manager:**
```swift
// Configuration/ConfigurationManager.swift
import Foundation

struct ConfigurationManager {
    static let shared = ConfigurationManager()
    
    private let bundle = Bundle.main
    
    var apiBaseURL: String {
        return getValue(for: "API_BASE_URL") ?? "https://api.investmenttracker.com"
    }
    
    var googleClientId: String {
        return getValue(for: "GOOGLE_CLIENT_ID") ?? ""
    }
    
    var appleClientId: String {
        return getValue(for: "APPLE_CLIENT_ID") ?? ""
    }
    
    var sentryDSN: String {
        return getValue(for: "SENTRY_DSN") ?? ""
    }
    
    var isAnalyticsEnabled: Bool {
        return getBoolValue(for: "ANALYTICS_ENABLED") ?? true
    }
    
    private func getValue(for key: String) -> String? {
        return bundle.object(forInfoDictionaryKey: key) as? String
    }
    
    private func getBoolValue(for key: String) -> Bool? {
        return bundle.object(forInfoDictionaryKey: key) as? Bool
    }
}
```

### 2. CI/CD for iOS

**Create `.github/workflows/ios-deploy.yml`:**
```yaml
name: iOS Build and Deploy

on:
  push:
    branches: [main]
    paths: ['ios-app/**']

jobs:
  build-and-deploy:
    runs-on: macos-latest
    
    steps:
    - name: Checkout
      uses: actions/checkout@v3
    
    - name: Setup Xcode
      uses: maxim-lobanov/setup-xcode@v1
      with:
        xcode-version: '15.0'
    
    - name: Install Apple Certificate
      uses: apple-actions/import-codesign-certs@v1
      with:
        p12-file-base64: ${{ secrets.APPLE_CERTIFICATE_P12 }}
        p12-password: ${{ secrets.APPLE_CERTIFICATE_PASSWORD }}
    
    - name: Install Provisioning Profile
      uses: apple-actions/download-provisioning-profiles@v1
      with:
        bundle-id: com.yuksel.investmenttracker
        issuer-id: ${{ secrets.APPSTORE_ISSUER_ID }}
        api-key-id: ${{ secrets.APPSTORE_API_KEY_ID }}
        api-private-key: ${{ secrets.APPSTORE_API_PRIVATE_KEY }}
    
    - name: Install Dependencies
      run: |
        cd ios-app
        pod install
    
    - name: Build Archive
      run: |
        cd ios-app
        xcodebuild -workspace InvestmentTracker.xcworkspace \
          -scheme InvestmentTracker \
          -configuration Release \
          -destination generic/platform=iOS \
          -archivePath $PWD/build/InvestmentTracker.xcarchive \
          GOOGLE_CLIENT_ID=${{ secrets.GOOGLE_CLIENT_ID }} \
          APPLE_CLIENT_ID=${{ secrets.APPLE_CLIENT_ID }} \
          SENTRY_DSN=${{ secrets.SENTRY_DSN }} \
          archive
    
    - name: Export IPA
      run: |
        cd ios-app
        xcodebuild -exportArchive \
          -archivePath $PWD/build/InvestmentTracker.xcarchive \
          -exportOptionsPlist ExportOptions.plist \
          -exportPath $PWD/build
    
    - name: Upload to TestFlight
      uses: apple-actions/upload-testflight-build@v1
      with:
        app-path: ios-app/build/InvestmentTracker.ipa
        issuer-id: ${{ secrets.APPSTORE_ISSUER_ID }}
        api-key-id: ${{ secrets.APPSTORE_API_KEY_ID }}
        api-private-key: ${{ secrets.APPSTORE_API_PRIVATE_KEY }}
    
    - name: Upload to App Store
      if: github.ref == 'refs/heads/main'
      uses: apple-actions/upload-testflight-build@v1
      with:
        app-path: ios-app/build/InvestmentTracker.ipa
        issuer-id: ${{ secrets.APPSTORE_ISSUER_ID }}
        api-key-id: ${{ secrets.APPSTORE_API_KEY_ID }}
        api-private-key: ${{ secrets.APPSTORE_API_PRIVATE_KEY }}
        submit-for-review: true
```

**Create `ios-app/ExportOptions.plist`:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>app-store</string>
    <key>teamID</key>
    <string>YOUR_TEAM_ID</string>
    <key>uploadBitcode</key>
    <false/>
    <key>uploadSymbols</key>
    <true/>
    <key>compileBitcode</key>
    <false/>
</dict>
</plist>
```

### 3. iOS Crash Reporting and Analytics

**Add Sentry for crash reporting:**
```swift
// AppDelegate.swift or App.swift
import Sentry

func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
    SentrySDK.start { options in
        options.dsn = ConfigurationManager.shared.sentryDSN
        options.debug = false
        options.environment = "production"
    }
    return true
}
```

## 🤖 Android Deployment Configuration

### 1. Build Configuration

**Update `android-app/app/build.gradle`:**
```kotlin
android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.yuksel.investmenttracker"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
        
        buildConfigField "String", "API_BASE_URL", "\"https://api.investmenttracker.com\""
        buildConfigField "boolean", "ANALYTICS_ENABLED", "true"
    }
    
    signingConfigs {
        release {
            storeFile file("../keystore/release.keystore")
            storePassword System.getenv("KEYSTORE_PASSWORD") ?: project.findProperty("KEYSTORE_PASSWORD")
            keyAlias System.getenv("KEY_ALIAS") ?: project.findProperty("KEY_ALIAS")
            keyPassword System.getenv("KEY_PASSWORD") ?: project.findProperty("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        debug {
            buildConfigField "String", "API_BASE_URL", "\"http://10.0.2.2:8080/api\""
            buildConfigField "boolean", "ANALYTICS_ENABLED", "false"
            applicationIdSuffix ".debug"
            versionNameSuffix "-debug"
            debuggable true
        }
        
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release
            
            buildConfigField "String", "API_BASE_URL", "\"https://api.investmenttracker.com\""
            buildConfigField "boolean", "ANALYTICS_ENABLED", "true"
        }
        
        staging {
            initWith debug
            buildConfigField "String", "API_BASE_URL", "\"https://staging-api.investmenttracker.com\""
            buildConfigField "boolean", "ANALYTICS_ENABLED", "true"
            applicationIdSuffix ".staging"
            versionNameSuffix "-staging"
            debuggable false
        }
    }
    
    flavorDimensions += "environment"
    productFlavors {
        production {
            dimension "environment"
        }
        
        development {
            dimension "environment"
            applicationIdSuffix ".dev"
        }
    }
}

dependencies {
    // Crash reporting
    implementation 'io.sentry:sentry-android:6.32.0'
    
    // Analytics
    implementation 'com.google.firebase:firebase-analytics:21.5.0'
    implementation 'com.google.firebase:firebase-crashlytics:18.6.0'
    
    // Performance monitoring
    implementation 'com.google.firebase:firebase-perf:20.5.0'
}

apply plugin: 'com.google.gms.google-services'
apply plugin: 'com.google.firebase.crashlytics'
apply plugin: 'com.google.firebase.firebase-perf'
```

### 2. CI/CD for Android

**Create `.github/workflows/android-deploy.yml`:**
```yaml
name: Android Build and Deploy

on:
  push:
    branches: [main]
    paths: ['android-app/**']

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout
      uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Setup Android SDK
      uses: android-actions/setup-android@v2
    
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
    
    - name: Decode Keystore
      env:
        ENCODED_STRING: ${{ secrets.KEYSTORE_BASE64 }}
      run: |
        echo $ENCODED_STRING | base64 -di > android-app/keystore/release.keystore
    
    - name: Build Release APK
      env:
        KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
        KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
      run: |
        cd android-app
        ./gradlew assembleRelease
    
    - name: Build Release AAB
      env:
        KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
        KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
      run: |
        cd android-app
        ./gradlew bundleRelease
    
    - name: Upload to Play Console
      uses: r0adkll/upload-google-play@v1
      with:
        serviceAccountJsonPlainText: ${{ secrets.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON }}
        packageName: com.yuksel.investmenttracker
        releaseFiles: android-app/app/build/outputs/bundle/release/app-release.aab
        track: beta
        status: completed
        inAppUpdatePriority: 2
    
    - name: Upload APK Artifact
      uses: actions/upload-artifact@v3
      with:
        name: app-release
        path: android-app/app/build/outputs/apk/release/app-release.apk
```

### 3. ProGuard Configuration

**Create `android-app/app/proguard-rules.pro`:**
```pro
# Keep data classes
-keep class com.yuksel.investmenttracker.data.model.** { *; }
-keep class com.yuksel.investmenttracker.dto.** { *; }

# Keep Retrofit interfaces
-keep interface com.yuksel.investmenttracker.data.network.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep Navigation
-keep class androidx.navigation.** { *; }

# Keep OAuth libraries
-keep class com.google.android.gms.** { *; }
-keep class com.willowtreeapps.signinwithapplebutton.** { *; }
```

## 🔒 Security Configuration

### 1. Secrets Management

**Create `scripts/setup-secrets.sh`:**
```bash
#!/bin/bash

# Setup script for production secrets
set -euo pipefail

# Check if running on appropriate environment
if [[ "${ENVIRONMENT:-}" != "production" ]]; then
    echo "This script should only run in production environment"
    exit 1
fi

# Generate JWT secret if not provided
if [[ -z "${JWT_SECRET:-}" ]]; then
    echo "Generating JWT secret..."
    export JWT_SECRET=$(openssl rand -base64 64)
fi

# Generate strong passwords
if [[ -z "${MONGO_PASSWORD:-}" ]]; then
    export MONGO_PASSWORD=$(openssl rand -base64 32)
fi

if [[ -z "${REDIS_PASSWORD:-}" ]]; then
    export REDIS_PASSWORD=$(openssl rand -base64 32)
fi

# Create Kubernetes secrets
kubectl create secret generic investment-tracker-secrets \
    --namespace=investment-tracker \
    --from-literal=JWT_SECRET="${JWT_SECRET}" \
    --from-literal=MONGO_PASSWORD="${MONGO_PASSWORD}" \
    --from-literal=REDIS_PASSWORD="${REDIS_PASSWORD}" \
    --from-literal=GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET}" \
    --from-literal=APPLE_CLIENT_SECRET="${APPLE_CLIENT_SECRET}" \
    --from-literal=MAIL_PASSWORD="${MAIL_PASSWORD}" \
    --from-literal=EXTERNAL_PRICE_API_KEY="${EXTERNAL_PRICE_API_KEY}" \
    --dry-run=client -o yaml | kubectl apply -f -

echo "Secrets configured successfully"
```

### 2. SSL/TLS Configuration

**Create `nginx/nginx.conf`:**
```nginx
upstream backend {
    server investment-tracker-backend:8080;
}

server {
    listen 80;
    server_name api.investmenttracker.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.investmenttracker.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    # Security headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload";
    add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';";

    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    limit_req zone=api burst=20 nodelay;

    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }

    location /actuator/health {
        proxy_pass http://backend;
        access_log off;
    }
}
```

## 📊 Monitoring and Observability

### 1. Application Monitoring

**Add monitoring dependencies to backend:**
```gradle
dependencies {
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.sentry:sentry-spring-boot-starter:6.32.0'
}
```

**Configure monitoring in `application-prod.yml`:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,httptrace
  endpoint:
    health:
      show-details: when-authorized
    metrics:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    web:
      server:
        request:
          autotime:
            enabled: true

sentry:
  dsn: ${SENTRY_DSN}
  environment: production
  release: ${APP_VERSION:unknown}
  traces-sample-rate: 0.1
```

### 2. Log Aggregation

**Create `monitoring/filebeat.yml`:**
```yaml
filebeat.inputs:
- type: container
  paths:
    - '/var/lib/docker/containers/*/*.log'
  processors:
  - add_docker_metadata:
      host: "unix:///var/run/docker.sock"

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "investment-tracker-logs-%{+yyyy.MM.dd}"

logging.level: info
logging.to_files: true
```

## 🚀 Deployment Scripts

### 1. Production Deployment Script

**Create `scripts/deploy-production.sh`:**
```bash
#!/bin/bash
set -euo pipefail

ENVIRONMENT="production"
NAMESPACE="investment-tracker"
IMAGE_TAG="${1:-latest}"

echo "Deploying Investment Tracker to ${ENVIRONMENT} environment"

# Build and push Docker image
echo "Building Docker image..."
docker build -t "your-registry/investment-tracker-backend:${IMAGE_TAG}" ./backend
docker push "your-registry/investment-tracker-backend:${IMAGE_TAG}"

# Update Kubernetes deployment
echo "Updating Kubernetes deployment..."
kubectl set image deployment/investment-tracker-backend \
    investment-tracker-backend="your-registry/investment-tracker-backend:${IMAGE_TAG}" \
    --namespace="${NAMESPACE}"

# Wait for rollout to complete
echo "Waiting for deployment to complete..."
kubectl rollout status deployment/investment-tracker-backend --namespace="${NAMESPACE}" --timeout=300s

# Run health check
echo "Running health check..."
kubectl run health-check --image=curlimages/curl --rm -i --restart=Never \
    --namespace="${NAMESPACE}" \
    -- curl -f http://investment-tracker-backend-service/actuator/health

echo "Deployment completed successfully!"
```

### 2. Database Migration Script

**Create `scripts/migrate-database.sh`:**
```bash
#!/bin/bash
set -euo pipefail

MONGODB_URI="${MONGODB_URI:-mongodb://localhost:27017/investment_tracker}"

echo "Running database migrations..."

# Create indexes
mongosh "${MONGODB_URI}" --eval "
    db.users.createIndex({ email: 1 }, { unique: true });
    db.acquisition_lots.createIndex({ userId: 1, assetId: 1 });
    db.price_snapshots.createIndex({ assetId: 1, asOf: -1 });
    db.auth_tokens.createIndex({ userId: 1 });
    db.auth_tokens.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 });
    db.password_reset_tokens.createIndex({ token: 1 }, { unique: true });
    db.password_reset_tokens.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 });
"

echo "Database migrations completed!"
```

## 🎯 Deployment Success Criteria

After implementing the deployment infrastructure:

### Backend Deployment
- [ ] Docker containerization complete
- [ ] Kubernetes manifests configured
- [ ] Production configuration secure
- [ ] SSL/TLS certificates configured
- [ ] Rate limiting implemented
- [ ] Health checks functional
- [ ] Monitoring and logging operational

### iOS Deployment
- [ ] Build configurations for all environments
- [ ] CI/CD pipeline functional
- [ ] Code signing automated
- [ ] TestFlight deployment working
- [ ] App Store deployment automated
- [ ] Crash reporting configured

### Android Deployment
- [ ] Build variants configured
- [ ] CI/CD pipeline functional
- [ ] APK/AAB signing automated
- [ ] Play Console deployment working
- [ ] ProGuard configuration optimized
- [ ] Crash reporting configured

### Security & Monitoring
- [ ] Secrets management implemented
- [ ] Security headers configured
- [ ] Monitoring dashboards operational
- [ ] Log aggregation functional
- [ ] Alert system configured
- [ ] Backup and recovery procedures documented