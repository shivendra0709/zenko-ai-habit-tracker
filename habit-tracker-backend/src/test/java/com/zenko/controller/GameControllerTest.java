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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for GameController.
 * Tests gamification features: XP, levels, streak tokens, quests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GameControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private ObjectMapper mapper = new ObjectMapper();
    private String sessionCookie;

    @BeforeEach
    void setUp() {
        // Login as demo user to establish session
        ResponseEntity<Map> login = restTemplate.postForEntity("/api/auth/demo", null, Map.class);
        assertEquals(HttpStatus.OK, login.getStatusCode());

        // Extract session cookie
        String setCookie = login.getHeaders().getFirst("Set-Cookie");
        assertNotNull(setCookie);
        sessionCookie = setCookie.split(";")[0];
    }

    private HttpEntity<String> authEntity() {
        return authEntity(null);
    }

    private HttpEntity<String> authEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sessionCookie);
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return new HttpEntity<>(body, headers);
    }

    @Test
    void testGetProfile_ShouldReturnInitialProfile() {
        // When
        ResponseEntity<Map> response = restTemplate.exchange("/api/game/profile", HttpMethod.GET, authEntity(), Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        // Initial values
        assertEquals(0, ((Number) body.get("xp")).intValue());
        assertEquals(1, ((Number) body.get("level")).intValue());
        assertEquals(0, ((Number) body.get("levelProgress")).intValue());
        assertEquals(3, ((Number) body.get("streakTokens")).intValue()); // starting tokens
        assertEquals(0, ((Number) body.get("questsCompleted")).intValue());
        assertEquals(0, ((Number) body.get("healthXp")).intValue());
        assertEquals(0, ((Number) body.get("mindXp")).intValue());
        assertEquals(0, ((Number) body.get("wealthXp")).intValue());
        assertEquals(1, ((Number) body.get("healthLevel")).intValue());
        assertEquals(1, ((Number) body.get("mindLevel")).intValue());
        assertEquals(1, ((Number) body.get("wealthLevel")).intValue());
        assertNotNull(body.get("activeQuests"));
        assertNotNull(body.get("completedQuestIds"));
        assertFalse((Boolean) body.get("aiConfigured")); // false since no API key in test
    }

    @Test
    void testAddXp_ShouldIncrementXpAndReturnNewValues() throws Exception {
        // Given
        Map<String, Object> req = Map.of("amount", 50, "category", "Health");
        HttpEntity<String> entity = authEntity(mapper.writeValueAsString(req));

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/game/add-xp", entity, Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertEquals(50, ((Number) body.get("xp")).intValue());
        // Level thresholds: 0-99=1, 100-299=2
        // So at 50 XP, level should still be 1
        assertEquals(1, ((Number) body.get("level")).intValue());
    }

    @Test
    void testAddXp_HealthCategory_ShouldIncreaseHealthXp() throws Exception {
        // Given
        Map<String, Object> req = Map.of("amount", 100, "category", "Health");
        HttpEntity<String> entity = authEntity(mapper.writeValueAsString(req));

        // When
        restTemplate.postForEntity("/api/game/add-xp", entity, Map.class);

        // Then
        ResponseEntity<Map> profileResponse = restTemplate.exchange("/api/game/profile", HttpMethod.GET, authEntity(), Map.class);
        assertEquals(100, ((Number) profileResponse.getBody().get("healthXp")).intValue());
    }

    @Test
    void testAddXp_MindCategory_ShouldIncreaseMindXp() throws Exception {
        // Given
        Map<String, Object> req = Map.of("amount", 150, "category", "Mind");
        HttpEntity<String> entity = authEntity(mapper.writeValueAsString(req));

        // When
        restTemplate.postForEntity("/api/game/add-xp", entity, Map.class);

        // Then
        ResponseEntity<Map> profileResponse = restTemplate.exchange("/api/game/profile", HttpMethod.GET, authEntity(), Map.class);
        assertEquals(150, ((Number) profileResponse.getBody().get("mindXp")).intValue());
    }

    @Test
    void testAddXp_WealthCategory_ShouldIncreaseWealthXp() throws Exception {
        // Given
        Map<String, Object> req = Map.of("amount", 80, "category", "Wealth");
        HttpEntity<String> entity = authEntity(mapper.writeValueAsString(req));

        // When
        restTemplate.postForEntity("/api/game/add-xp", entity, Map.class);

        // Then
        ResponseEntity<Map> profileResponse = restTemplate.exchange("/api/game/profile", HttpMethod.GET, authEntity(), Map.class);
        assertEquals(80, ((Number) profileResponse.getBody().get("wealthXp")).intValue());
    }

    @Test
    void testUseToken_ShouldDecreaseStreakTokens() throws Exception {
        // Given: initial profile has 3 tokens
        ResponseEntity<Map> profileBefore = restTemplate.exchange("/api/game/profile", HttpMethod.GET, authEntity(), Map.class);
        int initialTokens = ((Number) profileBefore.getBody().get("streakTokens")).intValue();
        assertTrue(initialTokens > 0);

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/game/use-token", authEntity(), Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(initialTokens - 1, ((Number) response.getBody().get("streakTokens")).intValue());
    }

    @Test
    void testUseToken_WhenNoTokens_ShouldReturnBadRequest() throws Exception {
        // Given: use tokens until none left
        while (true) {
            ResponseEntity<Map> resp = restTemplate.postForEntity("/api/game/use-token", authEntity(), Map.class);
            if (resp.getStatusCode() != HttpStatus.OK) break;
        }

        // When: try to use another token
        ResponseEntity<String> response = restTemplate.postForEntity("/api/game/use-token", authEntity(), String.class);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("No freeze tokens left"));
    }

    @Test
    void testEarnToken_ShouldIncreaseStreakTokens() throws Exception {
        // Given: current token count
        ResponseEntity<Map> before = restTemplate.exchange("/api/game/profile", HttpMethod.GET, authEntity(), Map.class);
        int initialTokens = ((Number) before.getBody().get("streakTokens")).intValue();

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/game/earn-token", authEntity(), Map.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(initialTokens + 1, ((Number) response.getBody().get("streakTokens")).intValue());
    }

    @Test
    void testEarnToken_WhenMaxTokens_ShouldReturnBadRequest() throws Exception {
        // Given: earn tokens until max (5)
        while (true) {
            ResponseEntity<Map> resp = restTemplate.postForEntity("/api/game/earn-token", authEntity(), Map.class);
            if (resp.getStatusCode() != HttpStatus.OK) break;
        }

        // When: try to earn another
        ResponseEntity<String> response = restTemplate.postForEntity("/api/game/earn-token", authEntity(), String.class);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Token limit reached"));
    }

    @Test
    void testGetQuests_ShouldReturnQuestsList() {
        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/game/quests?habitsSummary=general habits", HttpMethod.GET, authEntity(), Map.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("quests"));

        // Quests should be a JSON array string (stored in profile activeQuestsJson)
        String questsJson = (String) body.get("quests");
        assertTrue(questsJson.startsWith("["));
        assertTrue(questsJson.endsWith("]"));

        // Parse to verify structure at least has array
        try {
            com.fasterxml.jackson.databind.JsonNode parsed = mapper.readTree(questsJson);
            assertTrue(parsed.isArray());
        } catch (Exception e) {
            fail("Quests should be valid JSON array");
        }
    }

    @Test
    void testCompleteQuest_ShouldAwardXpAndMarkCompleted() throws Exception {
        // Given: get quests to populate active quests
        ResponseEntity<Map> questsResponse = restTemplate.exchange(
                "/api/game/quests", HttpMethod.GET, authEntity(), Map.class
        );
        String questsJson = (String) questsResponse.getBody().get("quests");

        // Parse first quest ID
        int questId = mapper.readTree(questsJson).get(0).get("id").asInt();

        // When: complete the quest
        Map<String, Object> req = Map.of("xpReward", 100);
        HttpEntity<String> entity = authEntity(mapper.writeValueAsString(req));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/game/complete-quest/" + questId, entity, Map.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertTrue(((Number) body.get("xp")).intValue() >= 100);
        assertEquals(1, ((Number) body.get("questsCompleted")).intValue());

        // Verify quest marked as completed in profile
        ResponseEntity<Map> profileAfter = restTemplate.exchange("/api/game/profile", HttpMethod.GET, authEntity(), Map.class);
        String completedIds = (String) profileAfter.getBody().get("completedQuestIds");
        assertTrue(completedIds.contains(String.valueOf(questId)));
    }

    @Test
    void testCompleteQuest_WithoutAuth_ShouldReturnUnauthorized() {
        // Given: no session
        HttpEntity<String> entity = new HttpEntity<>(new HttpHeaders());

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/game/complete-quest/1", entity, String.class
        );

        // Then: should be a 4xx error (401 or 403)
        assertTrue(response.getStatusCode().is4xxClientError());
    }
}
