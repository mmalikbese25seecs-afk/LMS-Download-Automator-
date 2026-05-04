package com.downloadc.downloadc.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Runs before every controller to enforce session requirements and redirect unauthenticated requests.
@Component
public class SessionFilter extends OncePerRequestFilter {

    @Autowired
    private SessionManager sessionManager;

    // Requests to these paths bypass the session check entirely.
    private static final String[] PUBLIC_PATHS = {
            "/index.html", "/api/auth/login", "/api/auth/status",
            "/css/", "/js/", "/favicon", ".png", ".ico", ".jpg"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Pass login, static assets, and auth-status calls straight through.
        for (String pub : PUBLIC_PATHS) {
            if (path.contains(pub)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // Return 401 JSON for unauthenticated API calls, the frontend handles the redirect.
        if (path.startsWith("/api/") && !sessionManager.isLoggedIn()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Session expired. Please log in again.\"}");
            return;
        }

        // Redirect directly-typed HTML URLs to the login page when there's no active session.
        if ((path.endsWith(".html") || path.equals("/"))
                && !path.equals("/index.html")
                && !sessionManager.isLoggedIn()) {
            response.sendRedirect("/index.html");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
