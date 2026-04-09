# 🚀 GitHub Setup & Deployment Guide for Zenko

## Step 1: Create GitHub Repository

### Option A: Using GitHub CLI (Recommended)
```bash
# Install GitHub CLI if you haven't: https://cli.github.com

gh auth login  # Authenticate with GitHub
gh repo create zenko-ai-habit-tracker --public --source=. --remote=origin --push
```

### Option B: Using GitHub Web Interface
1. Go to https://github.com/new
2. Enter repository name: `zenko-ai-habit-tracker`
3. Description: `AI-Powered Habit Tracker with Real-Time Updates and PWA Support`
4. Choose **Public**
5. Click **Create repository**
6. Copy the HTTPS URL

Then run:
```bash
cd "/Users/shivendrakumar/Desktop/Demo project"
git remote add origin https://github.com/shivendra0709/zenko-ai-habit-tracker.git
git branch -M main
git push -u origin main
```

---

## Step 2: Set Up Firebase (Optional but Recommended)

1. Go to https://firebase.google.com
2. Click "Create project"
3. Name it "Zenko Habit Tracker"
4. Accept analytics
5. In Project Settings → Service Accounts → Generate new private key
6. Save as `serviceAccountKey.json` (⚠️ Do NOT commit this!)

---

## Step 3: Deploy Backend to Render

### 3.1 Create Render Account
- Go to https://render.com
- Sign up with GitHub

### 3.2 Create PostgreSQL Database
1. Click "New" → "PostgreSQL"
2. Name: `zenko-db`
3. Region: Same as server (e.g., Oregon)
4. PostgreSQL Version: 15
5. Click "Create Database"
6. Copy the **Internal Database URL** (you'll need it)

### 3.3 Deploy Backend
1. Click "New" → "Web Service"
2. Connect your GitHub repository
3. Fill in:
   - **Name**: `zenko-backend`
   - **Runtime**: `Java 21`
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/zenko-backend-1.0.0.jar`
4. Add Environment Variables:
   ```
   SPRING_PROFILES_ACTIVE=prod
   SPRING_JPA_HIBERNATE_DDL_AUTO=update
   # DATABASE_URL will be auto-provided if using Render DB
   JAVA_TOOL_OPTIONS=-Xmx512m
   GOOGLE_CLIENT_ID=<your_google_client_id>
   OPENROUTER_API_KEY=<your_openrouter_key>
   ```
5. Click "Create Web Service"
6. Wait for deployment (5-10 minutes)
7. Copy the URL (e.g., `https://zenko-backend.onrender.com`)

---

## Step 4: Deploy Frontend to Vercel

### 4.1 Create Vercel Account
- Go to https://vercel.com
- Sign up with GitHub

### 4.2 Deploy Frontend
1. Click "Add New" → "Project"
2. Select your `zenko-ai-habit-tracker` repository
3. Configuration:
   - **Framework Preset**: Static Site
   - **Build Command**: Leave blank
   - **Output Directory**: `habit-tracker-backend/src/main/resources/static`
4. Environment Variables:
   ```
   REACT_APP_API_URL=https://zenko-backend.onrender.com
   ```
5. Click "Deploy"
6. Wait for deployment (30 seconds - 2 minutes)
7. Get your Vercel URL (e.g., `zenko-ai-habit-tracker.vercel.app`)

---

## Step 5: Get Free Domain (Optional)

### Option A: Free Subdomain from Vercel/Render
- Vercel gives you: `zenko-ai-habit-tracker.vercel.app`
- Use this as your production domain

### Option B: Free Domain from Freenom
1. Go to https://www.freenom.com
2. Find a domain: Try `zenko.tk`, `zenko.ml`, or `zenkoapp.ga`
3. Register (free for 12 months)
4. Go to Management Tools → Nameservers
5. Point to Vercel nameservers (provided by Vercel)
6. In Vercel, add custom domain in Project Settings

---

## Step 6: Update API URL

After deployment, update your frontend to use the production API:

**Option 1: Environment Variable (Recommended)**
- Vercel automatically uses the `REACT_APP_API_URL` env var

**Option 2: Hardcode in app.js**
```javascript
// In habit-tracker-backend/src/main/resources/static/app.js
const API_BASE = window.location.hostname === 'localhost' 
  ? '/api'
  : 'https://zenko-backend.onrender.com/api';
```

---

## Step 7: Enable CORS for Production

Update `HabitController.java` or create `CorsConfig.java`:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("https://zenko-ai-habit-tracker.vercel.app",
                           "https://yourdomain.tk")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

## 🔗 Deployed URLs

After completing above steps:

- **Frontend**: `https://zenko-ai-habit-tracker.vercel.app`
- **Backend**: `https://zenko-backend.onrender.com`
- **Custom Domain**: `https://zenko.tk` (if using Freenom)

---

## 🧪 Testing Production Deployment

```bash
# Test backend is running
curl https://zenko-backend.onrender.com/api/habits

# Test frontend loads
open https://zenko-ai-habit-tracker.vercel.app

# Monitor Render logs
# In Render dashboard → zenko-backend → Logs tab
```

---

## 📱 Environment-Specific Configuration

### Development (Local)
```
SPRING_PROFILES_ACTIVE=dev
Database: H2 (in-memory)
CORS: http://localhost:8080
```

### Production (Render/Vercel)
```
SPRING_PROFILES_ACTIVE=prod
Database: PostgreSQL (Render)
CORS: https://zenko-ai-habit-tracker.vercel.app
SSL: ✅ Automatic via Render/Vercel
```

---

## 🔐 Security Checklist

- [ ] Firebase credentials NOT in git (use `.env` only)
- [ ] Google OAuth credentials in Render secrets
- [ ] Database connections use secure URLs
- [ ] CORS restricted to your domain
- [ ] SSL/TLS enabled (automatic on both platforms)
- [ ] No `console.log()` of sensitive data in production
- [ ] API keys stored in environment variables

---

## ❌ Troubleshooting

### "CORS error" when app loads
- Check `REACT_APP_API_URL` in Vercel environment
- Verify CORS config in Spring Boot

### "Database connection error"
- Check `DATABASE_URL` in Render
- Verify PostgreSQL is running

### "Service Worker not working"
- Check `sw.js` is accessible at `https://yourdomain.com/sw.js`
- Verify manifest.json is served with correct headers

### "Static files not loading"
- Check `vercel.json` rewrites configuration
- Ensure files are in `habit-tracker-backend/src/main/resources/static/`

---

## 📞 Support Resources

- **Render docs**: https://render.com/docs
- **Vercel docs**: https://vercel.com/docs
- **Firebase docs**: https://firebase.google.com/docs
- **Spring Boot**: https://spring.io/projects/spring-boot

---

## 🎉 You're Live!

Your Zenko app is now deployed to production! 🚀

Next steps:
1. Share the URL with friends
2. Monitor performance in Render/Vercel dashboards
3. Collect user feedback
4. Plan mobile app release

---

**Happy tracking! 🎯**
