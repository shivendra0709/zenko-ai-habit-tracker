# 🔐 Environment Variables Setup Guide

## Overview
The `.env` file contains all configuration needed for the app. It's **not committed to git** for security ⚠️

---

## 📝 Step-by-Step Setup

### 1. **Database Configuration** (DATABASE_URL)

#### For Local Development (H2 - No Setup Needed)
```bash
DATABASE_URL=jdbc:h2:mem:testdb
SPRING_PROFILES_ACTIVE=dev
```

#### For Render PostgreSQL (Production)
1. Go to https://render.com/dashboard
2. Click "New" → "PostgreSQL"
3. Name: `zenko-db`
4. After creation, copy the **Internal Database URL**:
   - Format: `postgresql://user:password@host:5432/dbname`
   - Example: `postgresql://zenko_user:abc123xyz@dpg-example.render.com:5432/zenko_db`
5. Paste into `.env`:
   ```
   DATABASE_URL=postgresql://user:password@db.instance.render.com:5432/zenko_db
   ```

---

### 2. **Google OAuth Configuration**

#### Get Google Client ID & Secret:
1. Go to https://console.cloud.google.com
2. Create a new project (or select existing)
3. Left sidebar → "APIs & Services" → "OAuth consent screen"
   - Set up OAuth consent (choose "External")
   - Add app name, user support email, developer contact
4. Left sidebar → "Credentials"
5. Click "Create Credentials" → "OAuth client ID"
6. Choose: **Web application**
7. Add Authorized redirect URIs:
   ```
   http://localhost:8080/login/oauth2/code/google
   https://zenko-backend.onrender.com/login/oauth2/code/google
   ```
8. Copy the **Client ID** and **Client Secret**
9. Update `.env`:
   ```
   GOOGLE_CLIENT_ID=123456789-abc123def456ghi789.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=GOCSPX-abcdefghijklmnopqrst
   GOOGLE_REDIRECT_URI=https://zenko-backend.onrender.com/login/oauth2/code/google
   ```

---

### 3. **OpenRouter API Key** (Free AI Model Access)

#### Get Free API Key:
1. Go to https://openrouter.ai
2. Click "Sign in" → "Continue with Google"
3. Complete signup
4. Go to https://openrouter.ai/keys
5. Click "Create Key"
6. Copy the key (starts with `sk-or-v1-`)
7. Update `.env`:
   ```
   OPENROUTER_API_KEY=sk-or-v1-your_actual_key_here
   ```

#### Using OpenRouter (Optional - AI Features):
- **Default model**: `openai/gpt-3.5-turbo` (cheap, ~$0.002/1K tokens)
- **Free models**: Have rate limits, but free
- **Override in code** if needed:
  ```java
  String model = "anthropic/claude-3-haiku"; // Use Claude instead
  ```

---

### 4. **Firebase Configuration** (Optional - Real-Time Sync)

#### Setup Firebase:
1. Go to https://firebase.google.com
2. Click "Go to console"
3. Create new project → Name: "Zenko Habit Tracker"
4. Accept analytics (optional)
5. Wait for project creation (2-3 minutes)
6. Left sidebar → "Project Settings" (⚙️ icon)
7. Tab: "Service Accounts"
8. Click "Generate new private key"
9. A JSON file downloads (keep it safe ⚠️)
10. Open the JSON and copy these values to `.env`:
    ```
    FIREBASE_PROJECT_ID=zenko-habit-tracker
    FIREBASE_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQE...\n-----END PRIVATE KEY-----\n
    FIREBASE_CLIENT_EMAIL=firebase-adminsdk-xxx@zenko-habit-tracker.iam.gserviceaccount.com
    FIREBASE_DATABASE_URL=https://zenko-habit-tracker.firebaseio.com
    ```

⚠️ **IMPORTANT**: Never commit this JSON file or the `.env` file to git!

---

### 5. **Frontend Configuration**

#### Local Development:
```
REACT_APP_API_URL=http://localhost:8080/api
REACT_APP_ENV=development
```

#### Production (After Backend Deploy):
```
REACT_APP_API_URL=https://zenko-backend.onrender.com/api
REACT_APP_ENV=production
```

---

### 6. **CORS Configuration**

Update allowed origins to match your deployed domain:

```
CORS_ALLOWED_ORIGINS=https://zenko-ai-habit-tracker.vercel.app,https://zenko.tk,http://localhost:3000
```

---

## 🚀 For Render Deployment

### Don't set `.env` in Render!
Instead, add environment variables in Render Dashboard:

1. Go to https://render.com/dashboard
2. Select your web service (`zenko-backend`)
3. Click "Environment"
4. Add each variable individually:
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `DATABASE_URL` = (automatically provided by Render)
   - `GOOGLE_CLIENT_ID` = your value
   - `GOOGLE_CLIENT_SECRET` = your value
   - `OPENROUTER_API_KEY` = your key
   - etc.

✅ **Never upload `.env` file itself to Render** — Render will auto-inject these.

---

## 🚀 For Vercel Frontend Deployment

1. Go to https://vercel.com/dashboard
2. Select your project
3. Go to "Settings" → "Environment Variables"
4. Add:
   - `REACT_APP_API_URL` = `https://zenko-backend.onrender.com/api`
   - `REACT_APP_ENV` = `production`

---

## ✅ Quick Checklist

Before deploying, make sure you have:

- [ ] Database URL (PostgreSQL or H2)
- [ ] Google OAuth Client ID & Secret
- [ ] OpenRouter API Key (free)
- [ ] Firebase config (optional but recommended)
- [ ] Frontend API URL pointing to backend
- [ ] CORS origins configured
- [ ] `.env` file is in `.gitignore` ✅ (already set)
- [ ] Sensitive values NOT committed to git

---

## 🔒 Security Best Practices

1. **Local Development**:
   - Use `.env.local` for local-only secrets
   - Keep OpenRouter key with limited quota

2. **Production (Render/Vercel)**:
   - Add secrets via dashboard (never in `.env` files)
   - Use restricted API keys (limit to specific endpoints)
   - Rotate keys periodically

3. **Deploy Process**:
   ```bash
   # Local: .env with test values
   git commit .env.example (safe to share)
   git push (never push .env!)
   
   # Render: Add values in dashboard
   # Vercel: Add values in dashboard
   ```

---

## 📝 Environment File Template

Save this as reference for all needed variables:

```env
SPRING_PROFILES_ACTIVE=prod
SPRING_JPA_HIBERNATE_DDL_AUTO=update
DATABASE_URL=postgresql://user:password@host:5432/zenko_db
JAVA_TOOL_OPTIONS=-Xmx512m
GOOGLE_CLIENT_ID=xxxxx
GOOGLE_CLIENT_SECRET=xxxxx
OPENROUTER_API_KEY=sk-or-v1-xxxxx
FIREBASE_PROJECT_ID=xxxxx
Firebase_PRIVATE_KEY=xxxxx
FIREBASE_CLIENT_EMAIL=xxxxx
FIREBASE_DATABASE_URL=xxxxx
REACT_APP_API_URL=https://zenko-backend.onrender.com/api
REACT_APP_ENV=production
CORS_ALLOWED_ORIGINS=https://zenko-ai-habit-tracker.vercel.app
```

---

## 🆘 Troubleshooting

### ❌ "Cannot find .env file"
- Ensure `.env` is in project root (`/Desktop/Demo project/.env`)
- Spring Boot reads from root automatically

### ❌ "Invalid database URL"
- Format: `postgresql://user:password@hostname:5432/database`
- For Render: Copy "Internal Database URL" from Render dashboard

### ❌ "Google OAuth fails"
- Check redirect URI matches exactly (including `https://`)
- Add both `http://localhost:8080` and `https://zenko-backend.onrender.com` to Google Console

### ❌ "OpenRouter API returns 401"
- Verify API key starts with `sk-or-v1-`
- Regenerate key at https://openrouter.ai/keys if needed

---

Ready to deploy? 🚀 Follow [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
