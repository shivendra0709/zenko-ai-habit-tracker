# ✅ Production-Ready Setup Complete!

Your Zenko Habit Tracker is now configured for production deployment!

---

## 📦 **What's Been Prepared**

### **Backend (Spring Boot)**
- ✅ PostgreSQL support added (`pom.xml`)
- ✅ Production profile: `application-prod.properties`
- ✅ Development profile: `application-dev.properties` (H2)
- ✅ CORS configuration (`CorsConfig.java`)
- ✅ Dockerfile (multi-stage build)
- ✅ docker-compose.yml (local PostgreSQL + backend)
- ✅ Environment variable configuration

### **Frontend (Static)**
- ✅ All API calls use relative paths (`/api/...`) → works on any domain
- ✅ CORS enabled for your production domain
- ✅ Updated AI branding (OpenRouter GPT-3.5)

### **Deployment Tools**
- ✅ `deploy.sh` - Interactive deployment helper
- ✅ `DEPLOYMENT.md` - Comprehensive deployment guide

---

## 🗄️ **Database**

**Local:** H2 file-based (already working)
**Production:** PostgreSQL (required)

### **Test PostgreSQL Locally (Optional)**
```bash
cd "/Users/shivendrakumar/Desktop/Demo project"
docker-compose up -d
```
Backend will automatically connect to PostgreSQL at `jdbc:postgresql://postgres:5432/zenko`

---

## 🚀 **Quick Deploy to Railway (Recommended)**

### **1. Push to GitHub**
```bash
cd "/Users/shivendrakumar/Desktop/Demo project"
git add .
git commit -m "Prepare for production deployment"
git remote add origin https://github.com/yourusername/zenko-habit-tracker.git
git push -u origin main
```

### **2. Deploy Backend to Railway**
```bash
cd habit-tracker-backend
railway login
railway init
railway add  # select PostgreSQL
railway variables set OPENROUTER_API_KEY=sk-or-v1-e4fb5b95...
railway variables set CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app
railway up
```

Backend URL will be: `https://your-project.up.railway.app`

### **3. Deploy Frontend to Vercel**
```bash
cd habit-tracker
vercel --prod
```
Or drag & drop `habit-tracker/` folder to https://vercel.com/drop

Frontend URL: `https://your-project.vercel.app`

### **4. Configure CORS**
In Railway dashboard or via CLI:
```bash
railway variables set CORS_ALLOWED_ORIGINS=https://your-project.vercel.app
```

---

## 📝 **Environment Variables Reference**

### **Backend (Required)**
| Variable | Description | Example |
|----------|-------------|---------|
| `OPENROUTER_API_KEY` | OpenRouter API key | `sk-or-v1-...` |
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://host:5432/db` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `your-password` |
| `SPRING_PROFILES_ACTIVE` | `prod` for production | `prod` |
| `CORS_ALLOWED_ORIGINS` | Frontend URLs (comma-separated) | `https://app.com,https://staging.com` |

### **Backend (Optional)**
| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | `8080` |
| `google.client-id` | Google OAuth client ID | (dev client ID) |
| `openrouter.model` | AI model to use | `openai/gpt-3.5-turbo` |

---

## 🧪 **Test Your Deployment**

After deploying:

### **1. Test Backend Health**
```bash
curl https://your-backend.com/actuator/health
```
Should return: `{"status":"UP"}`

### **2. Test AI Endpoint**
```bash
curl -X POST https://your-backend.com/api/auth/demo -c /tmp/c.txt
curl -X POST https://your-backend.com/api/social/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello","userName":"Test"}' \
  -b /tmp/c.txt
```
Should return: `{"aiGenerated":true,"reply":"..."}`

### **3. Open Frontend**
Visit: `https://your-frontend.com`
- Try "Try Demo" → should see AI Coach with real responses
- All features (habits, gamification) should work

---

## 🔐 **Security Checklist for Production**

- ✅ **Database:** PostgreSQL with strong password
- ✅ **API Keys:** Stored in environment variables (not in code)
- ✅ **HTTPS:** Enabled automatically by Railway/Render
- ✅ **CORS:** Restricted to your frontend domain(s)
- ✅ **Session cookies:** Secure, SameSite=Lax
- ✅ **Rate limiting:** 20 requests/min on AI endpoints
- ✅ **SQL Injection:** Protected by Spring JPA

**Optional hardening:**
- Enable CSRF protection (Spring Security)
- Add rate limiting to all endpoints
- Set up monitoring/alerting
- Enable database connection pooling tuning

---

## 💰 **Estimated Costs**

| Service | Cost |
|---------|------|
| **OpenRouter AI** | ~$0.002/1K tokens (GPT-3.5) |
| **Railway** | Free tier → $5-10/mo for always-on |
| **Render** | Free PostgreSQL (256MB) → $7/mo for web service |
| **Vercel Frontend** | Free |
| **Total (small app)** | **~$5-15/month** |

---

## 🆘 **Troubleshooting**

| Problem | Solution |
|---------|----------|
| 401 errors | Check CORS, verify session cookies, check backend URL |
| DB connection failed | Verify `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| AI returns demo mode | `OPENROUTER_API_KEY` not set or invalid |
| 429 rate limit | Too many requests, check OpenRouter usage |
| App crashes | View logs in Railway/Render dashboard |
| Google OAuth fails | Add your domain to Google Cloud Console |

---

## 📚 **Resources**

- **OpenRouter Dashboard:** https://openrouter.ai/
- **Railway Docs:** https://docs.railway.app/
- **Render Docs:** https://render.com/docs
- **Spring Boot Production:** https://spring.io/guides/production-ready/

---

## 🎉 **You're Ready to Deploy!**

1. Push code to GitHub
2. Choose a platform (Railway recommended)
3. Follow the steps in `DEPLOYMENT.md`
4. Test everything
5. Share your app with the world! 🌍

**Need help?** Check `DEPLOYMENT.md` for detailed platform-specific instructions.

---

**Status:** ✅ Production-ready | ✅ Database migrated | ✅ Docker support | ✅ CORS configured | ✅ Security settings
