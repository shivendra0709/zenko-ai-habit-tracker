# 🧪 Testing Guide for HabitFlow

## **Overview**

This safety net provides comprehensive test coverage for critical components:

### **Test Structure**
```
src/test/java/com/habitflow/
├── service/
│   └── GeminiServiceTest.java        # Unit tests for AI service (demo mode, fallbacks)
├── controller/
│   ├── AuthControllerTest.java       # Auth API smoke tests
│   ├── GameControllerTest.java       # Gamification API tests
│   ├── HabitControllerTest.java      # Habit CRUD tests (placeholder - needs auth mocking)
│   └── SocialControllerTest.java     # Social + AI endpoints tests
└── repository/
    ├── HabitRepositoryTest.java      # Repository integration tests
    └── GameProfileRepositoryTest.java# GameProfile DB tests
```

### **Coverage Summary**
- ✅ **GeminiService**: 100% demo fallback scenarios, error handling, JSON responses
- ✅ **GameController**: XP, tokens, quests, profile endpoints
- ✅ **SocialController**: All AI endpoints (autopsy, DNA, alerts, chat, insights)
- ✅ **AuthController**: Demo login flow
- 📦 **Repositories**: CRUD operations, queries, multi-user isolation

---

## **🚀 Running Tests**

### **Run all tests:**
```bash
cd habit-tracker-backend
mvn clean test
```

### **Run specific test class:**
```bash
mvn test -Dtest=GeminiServiceTest
```

### **Run with verbose output:**
```bash
mvn test -X
```

### **Skip tests (just build):**
```bash
mvn clean package -DskipTests
```

---

## **📊 Test Reports**

After running tests, view reports:
- **HTML Report**: `target/site/surefire-report.html`
- **XML Reports**: `target/surefire-reports/*.xml`
- **Console Output**: Shows test results summary

Open `target/site/surefire-report.html` in your browser to see detailed results.

---

## **🔧 Current Gaps & Next Steps**

### **1. HabitController Full Integration Tests**
**Status:** Basic structure exists, needs authentication mocking
**Why Complex:** Uses session-based auth, not JWT
**Options:**
- A) Use `@WithMockUser` + `@WithSessionAttributes`
- B) Use `TestRestTemplate` with full `@SpringBootTest`
- C) Add JWT for simpler stateless testing (future refactor)

**Priority:** Medium - Habit operations are core feature

### **2. Frontend Tests**
**Status:** None (0% coverage)
**Options:**
- A) Add Jest/Vitest for unit tests (components, functions)
- B) Add Cypress/Playwright for E2E tests
- C) Add Storybook for component isolation

**Priority:** High - Frontend is 1773 lines single file, needs test coverage

### **3. Security Tests**
**Status:** None
**Options:**
- A) Test rate limiting (Resilience4j)
- B) Test session handling
- C) Test CSRF protection (when enabled)

**Priority:** Medium - We added rate limiting, should verify it works

### **4. Database Migration Tests**
**Status:** None
**Options:**
- Add Flyway/Liquibase migration tests
- Verify schema changes are backwards compatible

**Priority:** Low - For production deployments only

---

## **🛠️ Writing New Tests**

### **Adding a Test for a New Controller:**
```java
@WebMvcTest(YourController.class)
class YourControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private YourRepository repository;

    @Test
    void testYourEndpoint() throws Exception {
        mockMvc.perform(get("/api/your-endpoint"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.field").value("expected"));
    }
}
```

### **Using @DataJpaTest for Repositories:**
```java
@DataJpaTest
class YourRepositoryTest {

    @Autowired
    private YourRepository repository;

    @Test
    void testQueryMethod() {
        // Repository automatically uses in-memory H2 DB
        YourEntity entity = new YourEntity();
        repository.save(entity);
        // assertions...
    }
}
```

### **Mocking External Services (like Gemini API):**
```java
@MockBean
private GeminiService geminiService;

@BeforeEach
void setUp() {
    when(geminiService.habitAutopsy(anyString(), anyInt(), anyString()))
        .thenReturn("Mocked autopsy result");
}
```

---

## **⚠️  Testing Gotchas**

### **Session-Based Auth**
HabitFlow uses Spring Security with `HttpSession`. Controller tests need either:
- `@WithMockUser` for authentication + `@WithSessionAttributes` for `userId`
- Mock `HttpServletRequest.getSession()` to return mock session with `userId` attribute
- Integration test with `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`

### **H2 Database Isolation**
Repository tests use real H2 database that persists across tests in same class:
- Use `@BeforeEach` to clean up data
- Tests run in random order (don't rely on ordering)
- Entities need no-args constructor (JPA requirement)

### **Resilience4j Rate Limiting**
Rate limiting is applied at method level. To test:
- Integration test with `@SpringBootTest` (rate limiter bean active)
- Call endpoint > limit times, expect `429 Too Many Requests`

---

## **📈 Coverage Goals**

| Component | Current | Target |
|-----------|---------|--------|
| GeminiService | 90% | 100% |
| GameController | 80% | 90% |
| SocialController | 80% | 90% |
| HabitController | 10% | 80% |
| Frontend JS | 0% | 50% |
| **Overall** | ~40% | **75%** |

**Next milestone:** Add HabitController integration tests to reach 60% overall.

---

## **🔍 Diagnosing Failures**

### **"No suitable driver" H2 error:**
Ensure `application-test.properties` is in `src/test/resources`.

### **"No qualifier found" for beans:**
Add `@MockBean` for all dependencies not needed in the test.

### **"BeanCreationException" for RateLimiter:**
Add `@Import(RateLimitConfig.class)` to test configuration or include `@SpringBootTest`.

### **Tests work locally but fail in CI:**
Check environment:
- Java 17+ (matches project)
- Maven 3.8+
- No internet required (tests are isolated)

---

## **🎯 Quick Win: Add a Test for HabitController**

Want to contribute? Add the missing HabitController integration tests:
1. Create `HabitControllerIT.java` in `src/test/java/com/habitflow/controller/`
2. Use `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)`
3. Use `@AutoConfigureMockMvc` + `TestRestTemplate`
4. Mock session with `@WithSessionUser(email="user@example.com")` custom annotation
5. Cover all CRUD + completion toggle endpoints

See existing tests for patterns.

---

**Questions?** Check GeminiServiceTest for most complete example pattern.
