package com.zenko.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenko.model.AppUser;
import com.zenko.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AppUserRepository userRepo;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Verify Google ID token → find/create user → establish Spring Security session.
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String idToken = body.get("token");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing token"));
        }

        try {
            // Verify token via Google's public tokeninfo endpoint
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid Google token"));
            }

            JsonNode info = mapper.readTree(resp.getBody());
            String googleId = info.get("sub").asText();
            String email    = info.get("email").asText();
            String name     = info.has("name")    ? info.get("name").asText()    : email;
            String avatar   = info.has("picture") ? info.get("picture").asText() : "";

            // Find or create user in H2 database
            AppUser user = userRepo.findByGoogleId(googleId).orElseGet(() -> {
                AppUser u = new AppUser();
                u.setGoogleId(googleId);
                u.setEmail(email);
                u.setName(name);
                u.setAvatar(avatar);
                return userRepo.save(u);
            });

            // Always sync latest profile info from Google
            user.setName(name);
            user.setAvatar(avatar);
            userRepo.save(user);

            // ── Set Spring Security context so /api/** routes see an authenticated user ──
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    email, null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            // Persist security context into the HTTP session
            HttpSession session = request.getSession(true);
            session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext
            );
            // Also store our custom userId and userEmail for the habit controllers
            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", email);
            session.setAttribute("userName", name);

            return ResponseEntity.ok(buildUserResponse(user));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Auth failed: " + e.getMessage()));
        }
    }

    /**
     * Demo login — creates/finds a demo user and establishes a full Spring Security session.
     */
    @PostMapping("/demo")
    public ResponseEntity<?> demoLogin(HttpServletRequest request) {
        // Find or create demo user
        AppUser user = userRepo.findByGoogleId("demo-user-001").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setGoogleId("demo-user-001");
            u.setEmail("demo@habitflow.app");
            u.setName("Demo User");
            u.setAvatar("");
            return userRepo.save(u);
        });

        String email = user.getEmail();

        // Establish full Spring Security context (same as Google login)
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                email, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = request.getSession(true);
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            securityContext
        );
        session.setAttribute("userId", user.getId());
        session.setAttribute("userEmail", email);
        session.setAttribute("userName", user.getName());

        return ResponseEntity.ok(buildUserResponse(user));
    }

    /**
     * Return current logged-in user (checks session userId).
     */
    @GetMapping("/user")
    public ResponseEntity<?> getUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        return userRepo.findById(userId)
            .map(u -> ResponseEntity.ok(buildUserResponse(u)))
            .orElse(ResponseEntity.status(401).body(null));
    }

    /**
     * Sign out — clear Spring Security context and invalidate session.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Signed out successfully"));
    }

    private Map<String, Object> buildUserResponse(AppUser u) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", u.getId());
        map.put("name", u.getName());
        map.put("email", u.getEmail());
        map.put("avatar", u.getAvatar());
        map.put("provider", "google");
        return map;
    }
}
