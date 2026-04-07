#!/bin/bash

# 🚀 Zenko Habit Tracker - Deployment Helper Script
# This script helps you deploy to Railway, Render, or Fly.io

set -e

echo "================================"
echo "🚀 Zenko Deployment Helper"
echo "================================"
echo ""

# Check prerequisites
command -v git >/dev/null 2>&1 || { echo "❌ git is required. Install it first."; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "⚠️  docker not found. Docker is optional but recommended."; }

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Step 1:${NC} Ensure your code is committed to Git"
echo "   git add . && git commit -m 'Prepare for deployment'"
echo ""

echo -e "${YELLOW}Step 2:${NC} Create a GitHub repository (if not done)"
echo "   Go to https://github.com/new and create a repo"
echo "   Then: git remote add origin https://github.com/yourusername/your-repo.git"
echo "   git push -u origin main"
echo ""

echo -e "${YELLOW}Step 3:${NC} Choose deployment platform"
echo ""
echo "   1) Railway (Recommended - Easiest)"
echo "   2) Render"
echo "   3) Fly.io"
echo "   4) Local Docker only"
echo ""
read -p "Enter your choice (1-4): " choice

case $choice in
  1)
    echo ""
    echo "📦 Deploying to Railway..."
    echo ""
    echo "Prerequisites:"
    echo "  - Install Railway CLI: npm i -g @railway/cli"
    echo "  - Run: railway login"
    echo ""
    echo "Manual steps:"
    echo "  cd habit-tracker-backend"
    echo "  railway init"
    echo "  railway add  (select PostgreSQL)"
    echo "  railway variables set OPENROUTER_API_KEY=your-key"
    echo "  railway variables set CORS_ALLOWED_ORIGINS=https://your-frontend.com"
    echo "  railway up"
    echo ""
    echo "Then deploy frontend to Vercel/Netlify with your backend URL."
    ;;
  2)
    echo ""
    echo "📦 Deploying to Render..."
    echo ""
    echo "1. Go to https://render.com"
    echo "2. Create a Web Service from your GitHub repo"
    echo "3. Build Command: mvn clean package -DskipTests"
    echo "4. Start Command: java -jar target/*.jar"
    echo "5. Add PostgreSQL database"
    echo "6. Set Environment Variables:"
    echo "   - SPRING_PROFILES_ACTIVE=prod"
    echo "   - DB_URL=jdbc:postgresql://..."
    echo "   - DB_USERNAME=..."
    echo "   - DB_PASSWORD=..."
    echo "   - OPENROUTER_API_KEY=..."
    echo "   - CORS_ALLOWED_ORIGINS=https://your-frontend.com"
    echo ""
    ;;
  3)
    echo ""
    echo "📦 Deploying to Fly.io..."
    echo ""
    echo "1. Install flyctl: https://fly.io/docs/hands-on/install-flyctl/"
    echo "2. fly auth login"
    echo "3. fly launch (in habit-tracker-backend/)"
    echo "4. fly postgres create (for database)"
    echo "5. Attach database: fly postgres attach --app your-app-name"
    echo "6. Set secrets: fly secrets set OPENROUTER_API_KEY=..."
    echo "7. fly deploy"
    echo ""
    ;;
  4)
    echo ""
    echo "🐳 Starting locally with Docker Compose..."
    echo ""
    if [ -f "docker-compose.yml" ]; then
      docker-compose up -d
      echo ""
      echo -e "${GREEN}✅ Services started!${NC}"
      echo "   Backend: http://localhost:8080"
      echo "   PostgreSQL: localhost:5432"
      echo ""
      echo "To stop: docker-compose down"
    else
      echo "❌ docker-compose.yml not found!"
    fi
    ;;
  *)
    echo "Invalid choice"
    exit 1
    ;;
esac

echo ""
echo "================================"
echo "📚 Next Steps:"
echo "================================"
echo "1. Deploy frontend to Vercel/Netlify"
echo "2. Set CORS_ALLOWED_ORIGINS to your frontend URL"
echo "3. Test all features"
echo "4. Set up monitoring/alerts on your platform"
echo ""
echo "📖 See DEPLOYMENT.md for detailed instructions"
echo ""
