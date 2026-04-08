package com.zenko.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for AuthController.
 * Tests demo login, user retrieval, logout, and Google OAuth flow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    private Long createdUserId; // track for cleanup if needed

    @Test
    void testDemoLogin_ShouldCreateOrReturnUserAndSetSession() {
        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/demo", null, Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("id"));
        assertTrue(body.containsKey("email"));
        assertTrue(body.containsKey("name"));
        assertEquals("demo@habitflow.app", body.get("email"));
        assertEquals("Demo User", body.get("name"));
        assertTrue(body.containsKey("provider"));
        assertEquals("google", body.get("provider"));

        // Session cookie should be set
        assertTrue(response.getHeaders().getFirst("Set-Cookie") != null);
    }

    @Test
    void testDemoLogin_SecondLogin_ShouldReturnSameUser() {
        // Given: first login
        ResponseEntity<Map> first = restTemplate.postForEntity("/api/auth/demo", null, Map.class);
        Long firstId = ((Number) first.getBody().get("id")).longValue();

        // When: second login (should get same user)
        ResponseEntity<Map> second = restTemplate.postForEntity("/api/auth/demo", null, Map.class);
        Long secondId = ((Number) second.getBody().get("id")).longValue();

        // Then
        assertEquals(firstId, secondId);
    }

    @Test
    void testGetUser_WithValidSession_ShouldReturnUser() {
        // Given: login first
        ResponseEntity<Map> login = restTemplate.postForEntity("/api/auth/demo", null, Map.class);
        String cookie = login.getHeaders().getFirst("Set-Cookie");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", cookie.split(";")[0]);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange("/api/auth/user", HttpMethod.GET, entity, Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().containsKey("email"));
        assertTrue(response.getBody().containsKey("name"));
    }

    @Test
    void testGetUser_WithoutSession_ShouldReturnUnauthorized() {
        // Given: no session
        HttpEntity<String> entity = new HttpEntity<>(new HttpHeaders());

        // When
        ResponseEntity<String> response = restTemplate.exchange("/api/auth/user", HttpMethod.GET, entity, String.class);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testLogout_ShouldInvalidateSession() {
        // Given: login
        ResponseEntity<Map> login = restTemplate.postForEntity("/api/auth/demo", null, Map.class);
        String cookie = login.getHeaders().getFirst("Set-Cookie");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", cookie.split(";")[0]);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // When: logout (handled by Spring Security logout filter)
        ResponseEntity<String> logoutResponse = restTemplate.exchange(
                "/api/auth/logout", HttpMethod.POST, entity, String.class
        );

        // Then
        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());
        // Body is empty - Spring Security handles logout without body

        // Subsequent request should be unauthorized
        ResponseEntity<String> userResponse = restTemplate.exchange(
                "/api/auth/user", HttpMethod.GET, entity, String.class
        );
        assertEquals(HttpStatus.UNAUTHORIZED, userResponse.getStatusCode());
    }

    @Test
    void testGoogleLogin_WithoutToken_ShouldReturnBadRequest() {
        // Given: empty body
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/google", entity, String.class);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Missing token"));
    }

    // Note: Full Google login test with token validation would require mocking the external Google API call.
    // That can be done with MockRestServiceServer or @MockBean RestTemplate, but requires more setup.
    // The demo login and session management are the critical paths that are fully tested here.
}
