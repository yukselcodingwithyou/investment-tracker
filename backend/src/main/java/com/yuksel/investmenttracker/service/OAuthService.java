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

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    @Value("${oauth.google.client-id:}")
    private String googleClientId;

    private final NetHttpTransport httpTransport = new NetHttpTransport();
    private final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

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
        // TODO: Implement Apple ID token validation
        // This requires JWT token parsing and Apple's public key verification
        // For now, throw a runtime exception indicating it's not implemented
        log.warn("Apple ID token validation not yet fully implemented");
        throw new RuntimeException("Apple ID token validation not yet implemented");
    }
}