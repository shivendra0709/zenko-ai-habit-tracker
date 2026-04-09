# 🎯 Render Configuration Guide for Zenko

## Quick Fill-In Guide

### ✅ Already Correct
- **Name**: `zenko-backend`
- **Region**: Singapore (or your preferred region)
- **Instance Type**: Free
- **Repository**: `https://github.com/shivendra0709/zenko-ai-habit-tracker`
- **Branch**: `main`
- **Git Credentials**: Your email ✅

---

## ⚠️ Fields That Need Changes

### 1. **Root Directory** (Important!)
**Set this to**: `habit-tracker-backend`

Why? Your project is a monorepo - the backend code is inside `habit-tracker-backend/` folder.

---

### 2. **Build Command** (CRITICAL)
Look for a field labeled "Build Command" and set it to:
```
cd habit-tracker-backend && mvn clean package -DskipTests
```

**Note**: This might be under "Build Settings" or similar, NOT "Dockerfile Path"

---

### 3. **Start Command** (CRITICAL)
Look for a field labeled "Start Command" and set it to:
```
java -jar habit-tracker-backend/target/zenko-backend-1.0.0.jar
```

---

### 4. **Runtime** (CRITICAL)
You MUST select:
- **Runtime**: `Java 21` (NOT Docker)

If you're using **render.yaml**, Render will read from there. Otherwise, use the form fields.

---

### 5. **Health Check Path** (Good to set)
Set this to:
```
/api/habits
```

This tells Render to check if your app is healthy by calling this endpoint.

---

### 6. **Environment Variables**

Click "Add Environment Variable" and add these:

| Key | Value |
|-----|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `JAVA_TOOL_OPTIONS` | `-Xmx512m` |
| `GOOGLE_CLIENT_ID` | Your Google OAuth Client ID |
| `OPENROUTER_API_KEY` | Your OpenRouter API key |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` |

⚠️ **DATABASE_URL**: Render will automatically provide this if you created a PostgreSQL database in Step 3.2

---

## 📋 Form Layout (What Render Actually Shows)

When you're on the settings page, look for these sections:

### Build & Deploy Section
```
✅ Repository: https://github.com/shivendra0709/zenko-ai-habit-tracker
✅ Branch: main
🔴 Root Directory: [LEAVE BLANK or set to "habit-tracker-backend"]
```

### If NOT Using render.yaml (Recommended for clarity)
You should see:
```
Build Command: cd habit-tracker-backend && mvn clean package -DskipTests
Start Command: java -jar habit-tracker-backend/target/zenko-backend-1.0.0.jar
```

### If Using render.yaml (Already configured)
Render will read these values from your `render.yaml` file automatically. You just need to:
1. Set **Root Directory**: Leave empty or `habit-tracker-backend`
2. Render will ignore the form fields and use render.yaml instead

---

## 🚨 IMPORTANT: Fix Your Dockerfile Path Field

The field showing:
```
Dockerfile Path
java -jar target/zenko-backend-1.0.0.jar
```

This is **WRONG**. This field should contain a path to a Dockerfile (like `Dockerfile` or `habit-tracker-backend/Dockerfile`).

**Since you're deploying Java (not Docker):**
- Leave "Dockerfile Path" empty or just put `.`
- Make sure **Runtime** is set to **Java 21**
- Use **Build Command** and **Start Command** instead

---

## 🔧 Step-by-Step Setup

### If You See These Options:

**✅ GOOD (Use this approach):**
```
Runtime: Java 21
Build Command: [dropdown] ← Choose this
Start Command: [dropdown] ← Choose this
```

**❌ BAD (Avoid this):**
```
Runtime: Docker
Dockerfile: [text field]
```

---

## ✅ Correct Final Configuration

```
Name: zenko-backend
Runtime: ⭐ Java 21
Region: Singapore
Instance: Free

Build Command: cd habit-tracker-backend && mvn clean package -DskipTests
Start Command: java -jar habit-tracker-backend/target/zenko-backend-1.0.0.jar

Health Check Path: /api/habits
Health Check Protocol: HTTP

Environment Variables:
  SPRING_PROFILES_ACTIVE=prod
  JAVA_TOOL_OPTIONS=-Xmx512m
  GOOGLE_CLIENT_ID=<your_value>
  OPENROUTER_API_KEY=<your_value>
  SPRING_JPA_HIBERNATE_DDL_AUTO=update
  DATABASE_URL=<auto-filled by Render>

Auto-Deploy: On
Root Directory: habit-tracker-backend (optional)
```

---

## 🎯 If You're Stuck

**There are TWO ways to deploy on Render:**

### Option 1️⃣: Using render.yaml (Current)
- Render reads `render.yaml` from your repo
- You don't need to fill in Build/Start commands in the form
- Just connect the repo and let Render read the config

### Option 2️⃣: Using Web UI Form (Simpler)
- Select **Java 21 Runtime** (NOT Docker)
- Fill in Build Command and Start Command in the form
- Ignore Dockerfile fields
- Delete or rename `render.yaml` so Render uses form fields

**Recommended**: Stick with **Option 1** (render.yaml) - it's already set up!

---

## 📱 Environment Variables - Where to Get Values

| Variable | Where to Get |
|----------|-------------|
| `GOOGLE_CLIENT_ID` | Google Cloud Console → OAuth 2.0 credentials |
| `OPENROUTER_API_KEY` | OpenRouter.ai → API Keys section |
| `DATABASE_URL` | Render PostgreSQL → Connection string |

---

## 🔍 Checklist Before Clicking "Save"

- [ ] Runtime is `Java 21` (NOT Docker)
- [ ] Build Command includes `cd habit-tracker-backend`
- [ ] Start Command uses `habit-tracker-backend/target/zenko-backend-1.0.0.jar`
- [ ] Root Directory is `habit-tracker-backend` (or empty if using render.yaml)
- [ ] Health Check Path is `/api/habits`
- [ ] Environment variables are set
- [ ] No Docker/Dockerfile fields are filled
- [ ] Branch is `main`

---

## 🚀 After You Save

1. Click "Save Changes"
2. Render will trigger a new deploy
3. Watch the **Logs** tab for build progress
4. Wait 5-10 minutes
5. Should see ✅ "Deploy successful"
6. Test at `https://zenko-backend.onrender.com/api/habits`

---

## ❌ If Deployment Still Fails

Common issues:

| Error | Fix |
|-------|-----|
| "Dockerfile not found" | Use Java 21 Runtime, NOT Docker |
| "JAR not found" | Build Command MUST include `cd habit-tracker-backend` |
| "Maven not found" | Render Java Runtime auto-includes Maven |
| "Port already in use" | Render handles this automatically |

---

**Which approach are you using - render.yaml or Web Form? Let me know and I'll give more specific guidance!**
