package com.downloadc.downloadc.api;

import com.downloadc.downloadc.model.MoodleConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;


//MoodleApiClient for the Moodle Web Service API.

//Every request also requires:
//wstoken    : The login token we got from AuthService
//moodlewsrestformat=json : Tells Moodle to respond in JSON

public class MoodleApiClient {


    // Sends HTTP requests over the internet for API call
    private final HttpClient httpClient;

    //ObjectMapper converts raw Json text into a java object
    private final ObjectMapper objectMapper;

    //MoodleConfig holds the user's credentials and token
    // We store a reference here so every API call can access the token automatically.

    private final MoodleConfig config;

//Moodle exposes all of its functonality through a SINLGE URL endpoint
// wsfunction=core_webservice_get_site_info    :    Get site info + loggedin user
// wsfunction=core_enrol_get_users_courses   :    Get all courses the user is in
// wsfunction=mod_resource_get_resources_by_courses : Get downloadable files

    private static final String WS_ENDPOINT = "/webservice/rest/server.php";

//Constructor

    public MoodleApiClient(MoodleConfig config) {
        this.config = config;

        // Trust all SSL certificates (bypasses invalid certificate issues)
        try {

            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {

                        // Accepts any certificate authority

                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }


                        public void checkClientTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                            //
                        }


                        public void checkServerTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {

                        }
                    }
            };

            // Configure SSL context
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            // Build HTTP client with custom SSL
            this.httpClient = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();

            // ObjectMapper needs no special configuration — plain default setup works.
            this.objectMapper = new ObjectMapper();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to initialize MoodleApiClient — SSL setup error: " + e.getMessage(), e
            );
        }
    }

    // Call any Moodle function
    public JsonNode callFunction(String functionName, String extraParams) throws Exception {

        // Ensures user is logged in
        if (config.getToken() == null) {
            throw new Exception(
                    "Not logged in! You must call AuthService.getToken() " +
                            "and store the result with config.setToken() before making API calls."
            );
        }

        // Assemble the full URL step by step
        String url = config.getBaseUrl()   // "https://lms.nust.edu.pk/portal"
                + WS_ENDPOINT              // + "/webservice/rest/server.php"
                + "?wstoken="    + config.getToken()
                + "&moodlewsrestformat=json"
                + "&wsfunction=" + functionName;

        // Only append extra parameters if the caller actually provided any
        if (extraParams != null && !extraParams.isEmpty()) {
            url += "&" + extraParams;
        }

        System.out.println("[MoodleApiClient] Calling function: " + functionName);

        // Create GET request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        //Send the request and wait for the response.
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("[MoodleApiClient] Response received. HTTP Status: "
                + response.statusCode());

        // Parse JSON response
        return objectMapper.readTree(response.body());
    }

    // Overloaded method
    public JsonNode callFunction(String functionName) throws Exception {
        return callFunction(functionName, "");
    }
}