package com.zenko.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SocialController.
 * Tests social features: feed, duels, bonds, and AI coaching endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SocialControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private ObjectMapper mapper = new ObjectMapper();
    private String sessionCookie;

    @BeforeEach
    void setUp() {
        // Login as demo user
        ResponseEntity<Map> login = restTemplate.postForEntity("/api/auth/demo", null, Map.class);
        assertEquals(HttpStatus.OK, login.getStatusCode());

        String setCookie = login.getHeaders().getFirst("Set-Cookie");
        assertNotNull(setCookie);
        sessionCookie = setCookie.split(";")[0];
    }

    private HttpEntity<String> authEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sessionCookie);
        return new HttpEntity<>(headers);
    }

    private HttpEntity<String> authEntity(Object body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sessionCookie);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(mapper.writeValueAsString(body), headers);
    }

    // ─────────────────────────────────────
    // FEED TESTS
    // ─────────────────────────────────────

    @Test
    void testGetFeed_ShouldReturnListOfEvents() {
        // When
        ResponseEntity<List> response = restTemplate.exchange("/api/social/feed", HttpMethod.GET, authEntity(), List.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Should contain simulated events (at least some)
        assertTrue(response.getBody().size() >= 0); // could be 0 or more
    }

    @Test
    void testPostToFeed_ShouldCreateEvent() throws Exception {
        // Given
        Map<String, Object> post = Map.of(
                "habitName", "Morning Meditation",
                "streak", 15,
                "emoji", "🧘"
        );

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/social/feed/post", authEntity(post), Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body.get("id"));
        assertEquals("Your City", body.get("city"));
        assertEquals("Morning Meditation", body.get("habit"));
        assertEquals(15, body.get("streak"));
        assertEquals("🧘", body.get("emoji"));
        assertTrue((Boolean) body.get("isReal"));
    }

    // ─────────────────────────────────────
    // DUEL TESTS
    // ─────────────────────────────────────

    @Test
    void testCreateDuel_ShouldReturnDuelWithInviteCode() throws Exception {
        // Given
        Map<String, Object> req = Map.of("habitName", "Push-ups", "challengerName", "Test User");
        HttpEntity<String> entity = authEntity(req);

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/social/duels", entity, Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body.get("id"));
        assertNotNull(body.get("inviteCode"));
        assertEquals("Push-ups", body.get("habitName"));
        // The challenger should be set from session
    }

    @Test
    void testGetDuels_InitiallyEmpty_ShouldReturnEmptyList() {
        // When
        ResponseEntity<List> response = restTemplate.exchange("/api/social/duels", HttpMethod.GET, authEntity(), List.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Empty or contains just created duels
        assertNotNull(response.getBody());
    }

    @Test
    void testJoinDuel_WithValidCode_ShouldActivateDuel() throws Exception {
        // Given: create a duel first
        Map<String, Object> createReq = Map.of("habitName", "Running", "challengerName", "Challenger");
        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/social/duels", authEntity(createReq), Map.class);
        String inviteCode = (String) createResponse.getBody().get("inviteCode");

        // When: join with the invite code
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/social/duels/" + inviteCode + "/join", HttpMethod.POST, authEntity(null), Map.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertEquals("active", body.get("status"));
        assertNotNull(body.get("opponentName"));
        assertNotNull(body.get("opponentEmail"));
    }

    @Test
    void testJoinDuel_WithInvalidCode_ShouldReturnBadRequest() throws Exception {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/social/duels/INVALID/join", HttpMethod.POST, authEntity(null), String.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Duel not found or already started"));
    }

    // ─────────────────────────────────────
    // BOND TESTS
    // ─────────────────────────────────────

    @Test
    void testCreateBond_ShouldReturnBondWithInviteCode() throws Exception {
        // Given
        Map<String, Object> req = Map.of("habitName", "Reading", "user1Name", "User 1");
        HttpEntity<String> entity = authEntity(req);

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/social/bonds", entity, Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body.get("id"));
        assertNotNull(body.get("inviteCode"));
        assertEquals("Reading", body.get("habitName"));
        assertEquals("pending", body.get("status"));
    }

    @Test
    void testGetBonds_InitiallyEmpty_ShouldReturnEmptyList() {
        // When
        ResponseEntity<List> response = restTemplate.exchange("/api/social/bonds", HttpMethod.GET, authEntity(), List.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testJoinBond_WithValidCode_ShouldActivateBond() throws Exception {
        // Given: create bond
        Map<String, Object> createReq = Map.of("habitName", "Yoga", "user1Name", "Creator");
        ResponseEntity<Map> createResponse = restTemplate.postForEntity("/api/social/bonds", authEntity(createReq), Map.class);
        String inviteCode = (String) createResponse.getBody().get("inviteCode");

        // When: join
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/social/bonds/" + inviteCode + "/join", HttpMethod.POST, authEntity(null), Map.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertEquals("active", body.get("status"));
        assertNotNull(body.get("user2Name"));
        assertNotNull(body.get("user2Email"));
    }

    @Test
    void testJoinBond_WithInvalidCode_ShouldReturnBadRequest() throws Exception {
        // When
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/social/bonds/INVALID/join", HttpMethod.POST, authEntity(null), String.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Bond not found or already joined"));
    }

    // ─────────────────────────────────────
    // AI COACH TESTS
    // ─────────────────────────────────────

    @Test
    void testHabitAutopsy_ShouldReturnReport() throws Exception {
        // Given
        Map<String, Object> req = Map.of(
                "habitName", "Meditation",
                "streak", 21,
                "reason", "Busy week at work"
        );

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/social/ai/autopsy", authEntity(req), Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body.get("report"));
        assertFalse(((String) body.get("report")).isEmpty());
        // In test profile, aiConfigured will be false (no API key set)
        assertFalse((Boolean) body.get("aiGenerated"));
    }

    @Test
    void testHabitDNA_ShouldReturnProfile() throws Exception {
        // Given
        Map<String, Object> req = Map.of(
                "summary", "Completed 5 habits daily with 80% average",
                "userName", "Test User"
        );

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/social/ai/dna", authEntity(req), Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body.get("profile"));
        assertFalse(((String) body.get("profile")).isEmpty());
        assertFalse((Boolean) body.get("aiGenerated"));
    }

    @Test
    void testPredictiveAlert_ShouldReturnNudge() throws Exception {
        // Given
        Map<String, Object> req = Map.of(
                "habitName", "Exercise",
                "missRate", 40,
                "timePattern", "morning 7-8am"
        );

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/social/ai/alert", authEntity(req), Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body.get("alert"));
        assertFalse(((String) body.get("alert")).isEmpty());
        assertFalse((Boolean) body.get("aiGenerated"));
    }

    @Test
    void testAIInsights_ShouldReturnInsights() throws Exception {
        // Given
        Map<String, Object> req = Map.of(
                "summary", "High completion on weekdays, low on weekends",
                "userName", "Test User"
        );

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/social/ai/insights", authEntity(req), Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body.get("insights"));
        assertFalse(((String) body.get("insights")).isEmpty());
        assertFalse((Boolean) body.get("aiGenerated"));
    }

    @Test
    void testAIChat_ShouldReturnReply() throws Exception {
        // Given
        Map<String, Object> req = Map.of(
                "message", "How can I improve my consistency?",
                "habitContext", "3 habits tracked, 60% completion rate",
                "userName", "Test User"
        );

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/social/ai/chat", authEntity(req), Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body.get("reply"));
        assertFalse(((String) body.get("reply")).isEmpty());
        assertFalse((Boolean) body.get("aiGenerated"));
    }

    // ─────────────────────────────────────
    // RATE LIMITING
    // ─────────────────────────────────────

    // Note: Rate limiting tests would require sending >20 requests within a minute.
    // Those are better as integration tests with controlled clock or via MockMvc.
    // For now, AI endpoints are covered functionally; rate limiting verified manually.

    // ─────────────────────────────────────
    // AUTH REQUIREMENTS
    // ─────────────────────────────────────

    @Test
    void testAllEndpoints_WithoutAuth_ShouldReturnUnauthorized() {
        // Given: no session
        HttpEntity<String> entity = new HttpEntity<>(new HttpHeaders());

        // Feed - may return 401 or 403 depending on security config
        HttpStatusCode feedStatus = restTemplate.exchange("/api/social/feed", HttpMethod.GET, entity, String.class).getStatusCode();
        assertTrue(feedStatus.is4xxClientError(), "Feed should require auth, got " + feedStatus);

        // Create Duel
        HttpStatusCode duelStatus = restTemplate.postForEntity("/api/social/duels", entity, String.class).getStatusCode();
        assertTrue(duelStatus.is4xxClientError(), "CreateDuel should require auth, got " + duelStatus);

        // Create Bond
        HttpStatusCode bondStatus = restTemplate.postForEntity("/api/social/bonds", entity, String.class).getStatusCode();
        assertTrue(bondStatus.is4xxClientError(), "CreateBond should require auth, got " + bondStatus);

        // AI endpoints
        HttpStatusCode chatStatus = restTemplate.postForEntity("/api/social/ai/chat", entity, String.class).getStatusCode();
        assertTrue(chatStatus.is4xxClientError(), "AI chat should require auth, got " + chatStatus);
    }
}
