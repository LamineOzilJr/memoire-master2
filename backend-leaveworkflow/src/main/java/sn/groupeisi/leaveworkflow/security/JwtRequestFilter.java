package sn.groupeisi.leaveworkflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sn.groupeisi.leaveworkflow.service.UserService;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = null;
        String username = null;

        System.out.println("=== JWT Filter - " + request.getRequestURI() + " ===");
        System.out.println("Authorization Header: " + (header != null ? "Present" : "Missing"));

        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
            System.out.println("Token extracted (first 20 chars): " + token.substring(0, Math.min(20, token.length())) + "...");
            try {
                username = jwtTokenProvider.getUserNameFromJwt(token);
                System.out.println("Username from token: " + username);
            } catch (Exception e) {
                System.err.println("ERROR extracting username: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("Loading user: " + username);
            try {
                UserDetails userDetails = userService.loadUserByUsername(username);
                System.out.println("User loaded successfully");

                boolean isValid = jwtTokenProvider.validateToken(token);
                System.out.println("Token valid: " + isValid);

                if (isValid) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("✓ Authentication SUCCESS for: " + username);
                } else {
                    System.err.println("✗ Token validation FAILED");
                }
            } catch (Exception e) {
                System.err.println("ERROR during authentication: " + e.getMessage());
                e.printStackTrace();
            }
        }

        chain.doFilter(request, response);
    }
}