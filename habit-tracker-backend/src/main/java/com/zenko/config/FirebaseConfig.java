package com.zenko.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Firebase configuration for real-time database and authentication.
 * 
 * Set the following environment variable:
 * FIREBASE_CREDENTIALS: Path to your Firebase JSON service account key
 * FIREBASE_DATABASE_URL: URL of your Firebase Realtime Database
 */
@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @Value("${firebase.database.url:}")
    private String databaseUrl;

    /**
     * Initialize Firebase Admin SDK if credentials are provided.
     */
    @Bean
    public FirebaseApp initializeFirebase() {
        try {
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                // If credentials not configured, log warning and skip Firebase initialization
                if (credentialsPath == null || credentialsPath.isEmpty()) {
                    logger.warn("Firebase credentials not configured. Firebase features are disabled. " +
                            "Set FIREBASE_CREDENTIALS environment variable to enable.");
                    return null;
                }

                GoogleCredentials credentials = GoogleCredentials.fromStream(
                    getClass().getResourceAsStream(credentialsPath));

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .setDatabaseUrl(databaseUrl)
                        .build();

                FirebaseApp.initializeApp(options);
                logger.info("Firebase Admin SDK initialized successfully");
            }
        } catch (IOException e) {
            logger.warn("Firebase initialization failed: " + e.getMessage() + 
                    " Firebase features are disabled. Configure FIREBASE_CREDENTIALS to enable.");
        }

        return null;
    }

    /**
     * Get Firebase Database instance.
     */
    @Bean
    public FirebaseDatabase firebaseDatabase() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return FirebaseDatabase.getInstance();
            }
        } catch (Exception e) {
            logger.warn("Firebase Database not available: " + e.getMessage());
        }
        return null;
    }
}
