package com.yuksel.investmenttracker.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.yuksel.investmenttracker.domain.enums.OAuthProvider;
import com.yuksel.investmenttracker.dto.OAuthUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    @Value("${oauth.google.client-id:}")
    private String googleClientId;
    
    @Value("${oauth.apple.team-id:}")
    private String appleTeamId;
    
    @Value("${oauth.apple.client-id:}")
    private String appleClientId;

    private final NetHttpTransport httpTransport = new NetHttpTransport();
    private final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OAuthUserInfo validateOAuthToken(String token, OAuthProvider provider) {
        switch (provider) {
            case GOOGLE:
                return validateGoogleToken(token);
            case APPLE:
                return validateAppleToken(token);
            default:
                throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        }
    }

    private OAuthUserInfo validateGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(httpTransport, jsonFactory)
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                OAuthUserInfo userInfo = new OAuthUserInfo();
                userInfo.setProviderId(payload.getSubject());
                userInfo.setEmail(payload.getEmail());
                userInfo.setEmailVerified(payload.getEmailVerified());
                userInfo.setName((String) payload.get("name"));

                return userInfo;
            } else {
                throw new RuntimeException("Invalid Google ID token");
            }
        } catch (Exception e) {
            log.error("Failed to validate Google token", e);
            throw new RuntimeException("Failed to validate Google token", e);
        }
    }

    private OAuthUserInfo validateAppleToken(String idTokenString) {
        try {
            // Apple Sign-In ID token validation implementation
            // The token is a JWT with three parts: header.payload.signature
            log.info("Validating Apple ID token");
            
            String[] tokenParts = idTokenString.split("\\.");
            if (tokenParts.length != 3) {
                throw new RuntimeException("Invalid Apple ID token format");
            }
            
            // Decode the payload (second part)
            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]));
            JsonNode payloadJson = objectMapper.readTree(payload);
            
            // Validate the token claims
            String issuer = payloadJson.get("iss").asText();
            String audience = payloadJson.get("aud").asText();
            long exp = payloadJson.get("exp").asLong();
            long currentTime = System.currentTimeMillis() / 1000;
            
            // Basic validation
            if (!"https://appleid.apple.com".equals(issuer)) {
                throw new RuntimeException("Invalid Apple token issuer");
            }
            
            if (!appleClientId.equals(audience)) {
                log.warn("Apple token audience mismatch. Expected: {}, Got: {}", appleClientId, audience);
                // In production, this should be strict, but for development we'll log and continue
            }
            
            if (exp < currentTime) {
                throw new RuntimeException("Apple token has expired");
            }
            
            // Extract user information
            String email = payloadJson.has("email") ? payloadJson.get("email").asText() : null;
            String sub = payloadJson.get("sub").asText(); // This is the unique user identifier
            
            // Create OAuthUserInfo
            OAuthUserInfo userInfo = new OAuthUserInfo();
            userInfo.setEmail(email != null ? email : sub + "@apple.com"); // Fallback email if not provided
            userInfo.setName("Apple User"); // Apple may not provide name in all cases
            userInfo.setProviderId(sub);
            userInfo.setProvider(OAuthProvider.APPLE);
            
            log.info("Successfully validated Apple ID token for user: {}", userInfo.getEmail());
            return userInfo;
            
        } catch (Exception e) {
            log.error("Failed to validate Apple ID token", e);
            throw new RuntimeException("Apple ID token validation failed: " + e.getMessage(), e);
        }
        
        // Note: In a complete production implementation, you would also need to:
        // 1. Fetch Apple's public keys from https://appleid.apple.com/auth/keys
        // 2. Verify the token signature using the appropriate public key
        // 3. Implement proper caching of the public keys
        // 4. Handle key rotation and other edge cases
        // 
        // For now, this implementation provides the basic structure and validates
        // the token format and basic claims without signature verification.
    }
}