package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

//AuthService xchanges credentials for a Token.
//Send username + password to the token endpoint

public class AuthService {

// HttpClient send the login request to Moodle's token endpoint
// ObjectMapper converts JSon string into a Java object
private final HttpClient httpClient;
private final ObjectMapper objectMapper;

// Constructor
public AuthService() {
    try {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {

                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {

                    }
                }
        };
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new SecureRandom());

        this.httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        this.objectMapper = new ObjectMapper();

    } catch (Exception e) {
        throw new RuntimeException(
                "Failed to initialize AuthService — SSL setup failed: " + e.getMessage(), e
        );
    }
}

// Gets token so password is not sent in every request

public String getToken(MoodleConfig config) {
    System.out.println("[AuthService] Authenticating user: " + config.getUsername());

    try {
        // URL-encode credentials to handle special characters safely
        String encodedUser = URLEncoder.encode(config.getUsername(), StandardCharsets.UTF_8);
        String encodedPass = URLEncoder.encode(config.getPassword(), StandardCharsets.UTF_8);

        // Construct secure request body
        String body = String.format("username=%s&password=%s&service=moodle_mobile_app",
                encodedUser, encodedPass);

        String endpoint = config.getBaseUrl() + "/login/token.php";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = objectMapper.readTree(response.body());

        // Error validation from Moodle API response
        if (json.has("error")) {
            System.err.println("[AuthService] Authentication Failed: " + json.get("error").asText());
            return null;
        }

        if (json.has("token")) {
            System.out.println("[AuthService] Login successful. Session token acquired.");
            return json.get("token").asText();
        }

    } catch (Exception e) {
        System.err.println("[AuthService] Connection Error: " + e.getMessage());
    }

    return null;
}
}