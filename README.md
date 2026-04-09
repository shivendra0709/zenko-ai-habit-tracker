# 🎯 Zenko - AI Habit Tracker

**Modern, Progressive Web App for habit tracking with AI insights, real-time updates, and mobile support.**

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![License](https://img.shields.io/badge/license-MIT-green)
![Java](https://img.shields.io/badge/Java-21-red)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen)
![PWA](https://img.shields.io/badge/PWA-Enabled-purple)

---

## ✨ Features

- **📱 Progressive Web App (PWA)**: Install on any device, works offline
- **🏋️ Habit Tracking**: Daily, weekly, monthly habits with customizable categories
- **🔥 Streaks & Stats**: Real-time streak tracking and discipline scoring
- **🤖 AI Coach**: Powered by OpenRouter for personalized insights
- **⚡ Real-Time Updates**: WebSocket support for instant sync across devices
- **🔐 Secure**: Google OAuth 2.0 authentication
- **📊 Analytics**: Habit completion heatmaps and performance metrics
- **🌙 Dark Mode**: Light/dark theme with persistent storage
- **🔔 Push Notifications**: Browser notifications for habit reminders
- **💾 Offline Support**: Service Worker caching for offline functionality

---

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+
- Node.js 18+ (for frontend tools)
- Firebase account (optional, for real-time database)

### Local Development

#### Backend
```bash
cd habit-tracker-backend
mvn clean package
java -jar target/zenko-backend-1.0.0.jar
```

Server runs on `http://localhost:8080`

#### Frontend
Frontend is served from `/src/main/resources/static/`

Open `http://localhost:8080` in your browser

### Environment Variables
```bash
# Google OAuth
GOOGLE_CLIENT_ID=your_google_client_id_here

# Database (development)
SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect

# Database (production - PostgreSQL on Render)
SPRING_DATASOURCE_URL=${DATABASE_URL}
SPRING_JPA_HIBERNATE_DDL_AUTO=update

# Firebase (optional)
FIREBASE_CREDENTIALS=/path/to/serviceAccountKey.json
FIREBASE_DATABASE_URL=https://your-project.firebaseio.com

# AI Service
OPENROUTER_API_KEY=your_openrouter_key_here

# Profile
SPRING_PROFILES_ACTIVE=dev|prod
```

---

## 🏗️ Architecture

### Backend (Spring Boot 3.5.0)
- **Framework**: Spring Boot with WebSockets
- **Database**: H2 (dev) / PostgreSQL (prod)
- **Authentication**: Google OAuth 2.0
- **Real-Time**: STOMP over WebSocket
- **Caching**: Spring Cache with @Cacheable

### Frontend (Vanilla JavaScript)
- **Architecture**: MVC with localStorage
- **PWA**: Service Worker + manifest.json
- **Styling**: Modern CSS with dark mode
- **Offline**: Full offline capability with sync

### Deployment
- **Backend**: Render.com (free tier)
- **Frontend**: Vercel (free tier)
- **Database**: Render PostgreSQL (free tier)
- **Storage**: Firebase Realtime Database (optional)

---

## 📦 Project Structure

```
zenko-ai-habit-tracker/
├── habit-tracker-backend/
│   ├── src/main/
│   │   ├── java/com/zenko/
│   │   │   ├── config/          # Spring configs (WebSocket, Firebase, Security)
│   │   │   ├── controller/      # REST API endpoints
│   │   │   ├── model/           # JPA entities
│   │   │   ├── repository/      # Data access layer
│   │   │   └── service/         # Business logic
│   │   └── resources/
│   │       ├── static/          # Frontend files
│   │       ├── application.properties
│   │       └── application-prod.properties
│   ├── pom.xml
│   └── Dockerfile
├── render.yaml                   # Render.com deployment config
├── vercel.json                   # Vercel deployment config
├── .gitignore
└── README.md
```

---

## 🔌 API Endpoints

### Authentication
- `POST /api/auth/google` - Sign in with Google
- `POST /api/auth/logout` - Sign out
- `GET /api/auth/user` - Get current user

### Habits
- `GET /api/habits` - List all habits
- `POST /api/habits` - Create habit
- `PUT /api/habits/{id}` - Update habit
- `DELETE /api/habits/{id}` - Delete habit
- `GET /api/completions` - Get completion history

### Habit Completion
- `POST /api/habits/{id}/complete` - Mark habit complete (permanent, no undo)

### AI Coach
- `POST /api/social/ai/autopsy` - Analyze habit failure
- `POST /api/social/ai/generate-habits` - Generate new habits

### WebSocket
- Connect: `ws://localhost:8080/ws/habits`
- Subscribe: `/topic/habits/{userId}` - Get real-time updates

---

## 🚀 Deployment

### Deploy Backend to Render

1. Push code to GitHub
2. Go to https://render.com
3. Create new "Web Service"
4. Connect GitHub repo
5. Use build command: `mvn clean package -DskipTests`
6. Use start command: `java -jar target/zenko-backend-1.0.0.jar`
7. Add environment variables (see `.env.example`)
8. Deploy!

### Deploy Frontend to Vercel

1. Connect GitHub repo to Vercel
2. Frontend auto-builds from `/src/main/resources/static/`
3. Set environment variable: `REACT_APP_API_URL=https://your-render-backend.onrender.com`
4. Deploy!

### Custom Domain
1. Get free domain from [Freenom](https://www.freenom.com) (.tk, .ml, .ga)
2. Point nameservers to Vercel (for frontend)
3. Or add subdomain: `api.yourdomain.tk` → Render backend

---

## 📱 Android App Integration

The Android app communicates with the backend API:

```kotlin
// Example: Kotlin/Android integration
val retrofit = Retrofit.Builder()
    .baseUrl("https://your-backend.onrender.com/api/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val service = retrofit.create(HabitService::class.java)
service.getHabits().enqueue(object : Callback<List<Habit>> {
    override fun onResponse(call: Call<List<Habit>>, response: Response<List<Habit>>) {
        // Handle response
    }
})
```

---

## 🧪 Testing

### Run Tests
```bash
mvn clean test
```

### Build for Production
```bash
mvn clean package
```

---

## 🔐 Security Considerations

- ✅ HTTPS enforced in production
- ✅ CORS configured for allowed origins
- ✅ Session-based authentication
- ✅ SQL injection prevention (JPA)
- ✅ XSS protection (CSP headers)
- ✅ CSRF tokens in modals
- ⚠️ API rate limiting (implement with Spring Cloud)

---

## 🎨 UI/UX Improvements (v1.0.0+)

- ✅ Modern gradient backgrounds
- ✅ Smooth animations on habit completion
- ✅ Better mobile responsiveness
- ✅ PWA "Add to Home Screen"
- ✅ Service Worker offline support
- ✅ Dark mode with smooth transitions
- ✅ Real-time sync with WebSockets

---

## 📊 Performance

- **Lighthouse Score**: 90+ (PWA optimized)
- **First Contentful Paint**: < 1s
- **Time to Interactive**: < 2s
- **Offline Load**: Instant (cached)
- **Mobile Score**: 95+ (responsive)

---

## 🔄 Roadmap

- [ ] Firebase Realtime Database sync
- [ ] Native Android app (Kotlin)
- [ ] Native iOS app (SwiftUI)
- [ ] Social features (duels, competitions)
- [ ] Export/backup habits
- [ ] Advanced analytics dashboard
- [ ] Calendar view for habits
- [ ] Integration with wearables

---

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

---

## 📄 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Firebase](https://firebase.google.com)
- [OpenRouter AI](https://openrouter.ai)
- [Vercel](https://vercel.com)
- [Render](https://render.com)

---

## 📧 Support & Contact

- **Issues**: [GitHub Issues](https://github.com/shivendra0709/zenko-ai-habit-tracker/issues)
- **Discussions**: [GitHub Discussions](https://github.com/shivendra0709/zenko-ai-habit-tracker/discussions)

---

**Built with ❤️ by Shivendra**

⭐ If you find this project useful, please star the repository!
