# 🛡️ Security Fixes Applied

## ✅ Completed (Phase 1)

### 1. Gemini API Key Moved to Environment Variable (CRITICAL)
- **Files Changed:**
  - `application.properties` - Removed hardcoded API key
  - `GeminiService.java` - Updated to read from `GEMINI_API_KEY` env var
  - Created `.env.example` with setup instructions

- **Before:** API key was exposed in source code
- **After:** Must be set via environment variable
- **Backward Compatible:** Still works with `gemini.api.key` property for development

### 2. Rate Limiting Added to AI Endpoints
- **Files Changed:**
  - `pom.xml` - Added Resilience4j dependency
  - `RateLimitConfig.java` - Configuration (20 req/min general, 10 req/min AI endpoints)
  - `SocialController.java` - Added `@RateLimiter(name="aiEndpoints")` to all `/api/social/ai/*` endpoints

- **Protection:** Prevents API abuse and DoS on Gemini API calls
- **Configurable:** Easily adjust limits in `RateLimitConfig.java`

### 3. Gitignore Created
- **File Added:** `.gitignore`
- **Protects:** `.env`, IDE files, build artifacts, H2 database files, secrets

## 📋 Recommended Next Steps (Phase 2)

### 1. Testing (Medium Priority)
- [ ] Add unit tests for `GeminiService` (test demo fallbacks, error handling)
- [ ] Add integration tests for REST controllers ( mocking session)
- [ ] Add frontend tests (Jest/Vitest) for critical user flows

### 2. CSRF Protection (Production Hardening)
Current state: CSRF disabled for API endpoints (convenient for development).
**Option A:** Keep disabled, rely on:
  - SameSite=Lax cookies
  - HTTPS enforcement in production
  - Short session timeout

**Option B:** Enable CSRF with token-based approach:
  - Update `SecurityConfig.java` to enable CSRF
  - Add CSRF token cookie that frontend can read
  - Update frontend JS to include token in API calls

**Recommendation:** Evaluate risk tolerance. For a public-facing production app, enable CSRF.

### 3. JWT + Refresh Tokens (Future Consideration)
Stateless auth would:
  - Remove server-side session storage
  - Better scalability (horizontal scaling without sticky sessions)
  - Simplify microservices architecture

**But:** Requires significant refactoring (frontend token storage, refresh logic, logout handling).
**Timeline:** Consider for production v2.0.

### 4. Production Configuration
- [ ] Move `application.properties` to external config (outside JAR)
- [ ] Add HTTPS enforcement (Spring Security `requiresChannel().anyRequest().requiresSecure()`)
- [ ] Set secure, HttpOnly cookies for session
- [ ] Add security headers (HSTS, CSP, X-Frame-Options)
- [ ] Configure CORS more restrictively (specific allowed origins)

## 🚀 How to Test the Changes

1. **Set Environment Variable:**
   ```bash
   export GEMINI_API_KEY="your-key-here"
   ```

2. **Build and Run:**
   ```bash
   cd habit-tracker-backend
   mvn clean package
   mvn spring-boot:run
   ```

3. **Verify:**
   - Check logs for "Gemini AI features enabled"
   - AI Coach section should show "AI Configured: true"
   - API rate limiting headers in response (optional: add logging)

4. **Test Rate Limiting:**
   ```bash
   # Should succeed
   curl -X POST http://localhost:8080/api/social/ai/chat \
     -H "Content-Type: application/json" \
     -d '{"message":"test"}' \
     -c cookies.txt -b cookies.txt

   # Repeat 11 times quickly → should get 429 Too Many Requests on 11th
   ```

## ⚠️  Important Notes

- **Gemini API Key:** The key previously in code has been **deactivated**. You must get a new key from https://aistudio.google.com/app/apikey and set `GEMINI_API_KEY`.
- **Demo Mode:** Without API key, AI features use canned responses (still functional).
- **Rate Limits:** Currently set to 10 AI requests/minute. Adjust in `RateLimitConfig.java` based on your needs and Gemini quota.

## 📞 Questions?

- Rate limiting works out of the box, no additional setup needed.
- To disable rate limiting in development, comment out `@RateLimiter` annotations or set very high limits.
- For production deployment, ensure environment variable is set in your hosting platform.
