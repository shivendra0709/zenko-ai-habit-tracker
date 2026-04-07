# 🚀 Deployment Guide - Zenko Habit Tracker

Deploy your AI-powered habit tracker to production!

---

## 📋 **Prerequisites**

- ✅ OpenRouter API key (already have: `sk-or-v1-e4fb5b95...`)
- ✅ GitHub account (for code repository)
- ✅ Cloud platform account (Railway, Render, or Fly.io)

---

## 🗄️ **Step 1: Database - PostgreSQL**

### **Option A: Railway/Managed PostgreSQL (Recommended)**

Both Railway and Render provide **free PostgreSQL instances**:

1. **Create a PostgreSQL database** on your chosen platform
2. Get connection details:
   ```
   Host: xxx.postgres.railway.app
   Port: 5432
   Database: railway
   Username: postgres (or provided)
   Password: xxx
   ```

3. Set these environment variables in your production app (see below)

---

## 🔧 **Step 2: Prepare Backend for Production**

Your backend is already configured with:
- ✅ `application-prod.properties` (Production profile)
- ✅ PostgreSQL support added to `pom.xml`
- ✅ CORS configuration
- ✅ Secure session cookies (HTTPS required)

---

## ☁️ **Step 3: Deploy to Railway (Easiest)**

### **3.1 Install Railway CLI**
```bash
npm i -g @railway/cli
# Or download from: https://railway.app/
```

### **3.2 Login**
```bash
railway login
```

### **3.3 Initialize Project**
```bash
cd "/Users/shivendrakumar/Desktop/Demo project/habit-tracker-backend"
railway init
# Follow prompts, select "Deploy a Dockerfile or Procfile"
```

### **3.4 Create PostgreSQL**
```bash
railway add
# Select PostgreSQL
```

### **3.5 Get Database URL**
```bash
railway variables
# Note: DATABASE_URL will be something like:
# postgresql://postgres:password@containers.us-west-1.xxx.railway.app:5432/railway
```

### **3.6 Set Environment Variables**
In Railway dashboard or via CLI:
```bash
railway variables set OPENROUTER_API_KEY=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
railway variables set DB_URL=jdbc:postgresql://<host>:<port>/<database>
railway variables set DB_USERNAME=postgres
railway variables set DB_PASSWORD=<password>
railway variables set CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
```

### **3.7 Deploy**
```bash
railway up
```

Your backend will be live at: `https://your-project.up.railway.app`

---

## 🎨 **Step 4: Deploy Frontend**

Your frontend is static (HTML/CSS/JS). Host it on:

### **Option A: Vercel (Easiest)**
```bash
cd "/Users/shivendrakumar/Desktop/Demo project/habit-tracker"
# Create vercel.json
echo '{
  "rewrites": [{ "source": "/api/(.*)", "destination": "https://your-backend.up.railway.app/api/$1" }]
}' > vercel.json
vercel --prod
```

### **Option B: Netlify**
1. Drag & drop `habit-tracker/` folder to netlify.com/drop
2. In Site settings → Build & Deploy → Environment:
   - Add `VITE_API_URL=https://your-backend.up.railway.app`
3. Update `app.js` to use that env var (or hardcode for now)

### **Option C: Railway Static**
```bash
cd "/Users/shivendrakumar/Desktop/Demo project"
railway add
# Select "Static Site"
# Upload habit-tracker/ folder
```

---

## 🔗 **Step 5: Connect Frontend to Backend**

In `habit-tracker/app.js`, update the API base URL:

Around line 56-57, change:
```javascript
const baseUrl = '/api';  // Relative path - will proxy via vercel.json
// OR for direct connection:
// const baseUrl = 'https://your-backend.up.railway.app/api';
```

If using Vercel with rewrites, keep relative path `/api`.

---

## 🔐 **Step 6: Google OAuth (Optional but Recommended)**

If you want Google sign-in working in production:

1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Find your OAuth 2.0 Client ID
3. Add **Authorized JavaScript origins**:
   - `https://your-frontend.vercel.app` (or your domain)
   - `https://your-frontend-domain.com`
4. Add **Authorized redirect URI**:
   - `https://your-backend.up.railway.app/login/oauth2/code/google`
   (Check your auth flow - Spring Security default)
5. Update `application-prod.properties`:
   ```properties
   google.client-id=YOUR_PROD_CLIENT_ID.apps.googleusercontent.com
   ```

---

## 🧪 **Step 7: Test Production**

1. **Frontend loads** ✅
2. **Can log in** (Demo or Google) ✅
3. **AI Coach works** ✅ (calls backend → OpenRouter)
4. **Create habits** ✅
5. **All features work** ✅

---

## 📝 **Environment Variables Summary**

### **Backend (Railway/Render)**
| Variable | Value | Required |
|----------|-------|----------|
| `OPENROUTER_API_KEY` | `sk-or-v1-...` | ✅ Yes |
| `DB_URL` | `jdbc:postgresql://...` | ✅ Yes |
| `DB_USERNAME` | `postgres` or provided | ✅ Yes |
| `DB_PASSWORD` | your-db-password | ✅ Yes |
| `CORS_ALLOWED_ORIGINS` | `https://your-frontend.com` | ✅ Yes |
| `PORT` | `8080` (auto) | optional |
| `google.client-id` | (if using Google OAuth) | optional |

### **Frontend (Vercel/Netlify)**
| Variable | Value | Required |
|----------|-------|----------|
| `VITE_API_URL` | `https://your-backend...` | optional (if using rewrites) |

---

## ⚠️ **Important Production Notes**

### **Security**
- ✅ Never commit `.env` or `application-prod.properties` with secrets
- ✅ Use platform's secret/variables management
- ✅ HTTPS enforced (platforms provide automatically)
- ✅ Secure cookies (`server.servlet.session.cookie.secure=true`)

### **Cost Management**
- **OpenRouter costs:** ~$0.002/1K tokens with GPT-3.5
- Set usage limits on OpenRouter dashboard: https://openrouter.ai/account/billing
- Consider adding rate limiting already in place (20 req/min)

### **Database**
- Railway PostgreSQL free tier: 1 GB, 1 connection
- Render PostgreSQL free tier: 1 GB, no persistent connections (sleep after 15 min)
- For production with real users, upgrade to paid tier (~$5-10/mo)

---

## 🆘 **Troubleshooting**

| Issue | Solution |
|-------|----------|
| 401 Unauthorized | Check session cookies, CORS config |
| Database connection error | Verify `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| 429 Rate limit | OpenRouter key limits - check usage |
| Google OAuth "origin not allowed" | Add your domain to Google Cloud Console |
| Frontend can't reach backend | Check CORS and `CORS_ALLOWED_ORIGINS` env var |
| App crashes on startup | Check logs: `railway logs` or platform dashboard |

---

## 📞 **Need Help?**

1. Check backend logs in Railway/Render dashboard
2. Test API endpoints directly: `https://your-backend.com/api/auth/user`
3. Verify OpenRouter key: https://openrouter.ai/keys
4. Test PostgreSQL connection: use platform's database console

---

## 🎯 **Quick Start Checklist**

- [ ] Push code to GitHub
- [ ] Sign up for Railway (or Render)
- [ ] Deploy backend (add PostgreSQL + set env vars)
- [ ] Deploy frontend (Vercel/Netlify)
- [ ] Configure domain names
- [ ] Test all features
- [ ] Set up monitoring (optional)
- [ ] Enable billing alerts (optional)

---

**Ready to deploy?** Pick a platform and I'll guide you through each step! 🚀

Which platform would you like to use?
- Railway (simplest)
- Render (good free tier)
- Fly.io (Docker)
- AWS (more complex)
