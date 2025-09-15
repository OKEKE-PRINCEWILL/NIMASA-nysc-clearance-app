
package com.example.NIMASA.NYSC.Clearance.Form.config;

import com.example.NIMASA.NYSC.Clearance.Form.SecurityService.CustomUserDetailsService;
import com.example.NIMASA.NYSC.Clearance.Form.SecurityService.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class JWTFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final ApplicationContext context;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String accessToken = null;
        String username = null;

        // ONLY look for access tokens (not refresh tokens)
        // Priority: Authorization header first, then cookie
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        } else {
            // Check for access token in cookie
            accessToken = extractAccessTokenFromCookie(request);
        }

        // Extract username and validate token type
        if (accessToken != null) {
            try {
                username = jwtService.extractUsername(accessToken);

                // CRITICAL: Verify this is an access token, not a refresh token
                String tokenType = jwtService.extractTokenType(accessToken);
                if (!"access".equals(tokenType)) {
                    // Someone is trying to use a refresh token as an access token
                    setErrorResponse(response, "Invalid token type");
                    return;
                }
            } catch (Exception e) {
                // Token is malformed or expired
                // Don't set error here - just continue without authentication
                // Frontend will handle 401 and refresh token automatically
            }
        }

        // Authenticate if we have a valid username and no existing authentication
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = context.getBean(CustomUserDetailsService.class).loadUserByUsername(username);

                // Validate access token specifically
                if (jwtService.validateAccessToken(accessToken, userDetails)) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                // User not found or other authentication error
                // Continue without setting authentication
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract access token from cookies
     */
    private String extractAccessTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Set error response for invalid token types
     */
    private void setErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"%s\"}", message));
    }

    /**
     * Skip JWT filtering for certain endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip authentication for public endpoints
        return path.startsWith("/api/unified-auth/login") ||
                path.startsWith("/api/unified-auth/refresh") ||
                path.startsWith("/api/unified-auth/bootstrap") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs") ||
                path.equals("/") ||
                request.getMethod().equals("OPTIONS"); // CORS preflight
    }
}
