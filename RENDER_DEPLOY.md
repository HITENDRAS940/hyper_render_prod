# 🚀 Deploying Hyper Backend to Render

Complete step-by-step guide for deploying your Spring Boot backend to Render.

---

## 📋 Prerequisites

Before deploying, make sure you have:

1. ✅ GitHub repository with your code pushed
2. ✅ Render account (https://render.com - free tier available)
3. ✅ Gmail refresh token (run locally first - see GMAIL_SETUP.md)
4. ✅ Razorpay credentials
5. ✅ Cloudinary credentials
6. ✅ Google OAuth client ID (for user authentication)

---

## 🔧 Step 1: Prepare Your Code

### 1.1 Make sure these files are in your repo:

```
✅ pom.xml
✅ mvnw (Maven wrapper)
✅ src/main/resources/application.properties
✅ src/main/resources/application-prod.properties
✅ .gitignore (excluding credentials.json, .tokens/, .env)
```

### 1.2 Verify .gitignore excludes sensitive files:

```gitignore
credentials.json
.tokens/
.env
```

### 1.3 Push to GitHub:

```bash
git add .
git commit -m "Prepare for Render deployment"
git push origin main
```

---

## 🗄️ Step 2: Create PostgreSQL Database on Render

1. Go to https://dashboard.render.com
2. Click **"New +"** → **"PostgreSQL"**
3. Configure:
   - **Name**: `hyper-db`
   - **Database**: `hyper_booking`
   - **User**: `hyper_user`
   - **Region**: Choose closest to your users (e.g., Singapore for India)
   - **Plan**: Free (or paid for production)
4. Click **"Create Database"**
5. Wait for database to be ready (takes 1-2 minutes)
6. **Copy the connection details** - you'll need them later:
   - Internal Database URL
   - External Database URL
   - Username
   - Password

---

## 🌐 Step 3: Create Web Service on Render

1. Go to https://dashboard.render.com
2. Click **"New +"** → **"Web Service"**
3. Connect your GitHub repository
4. Configure the service:

### Basic Settings:
| Setting | Value |
|---------|-------|
| **Name** | `hyper-backend` |
| **Region** | Same as database |
| **Branch** | `main` |
| **Runtime** | `Java` |
| **Build Command** | `./mvnw -DskipTests clean package` |
| **Start Command** | `java -Dserver.port=$PORT -jar target/Hyper_backend-0.0.1-SNAPSHOT.jar` |
| **Plan** | Free (or paid for production) |

### Instance Type:
- Free: 512 MB RAM (good for testing)
- Starter: 512 MB RAM (no sleep, $7/month)
- Standard: 2 GB RAM (recommended for production, $25/month)

---

## 🔐 Step 4: Set Environment Variables

In the Render dashboard, go to your web service → **Environment** tab.

Add these environment variables:

### Required Variables:

| Variable | Value | Notes |
|----------|-------|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Use production profile |
| `DATABASE_URL` | `jdbc:postgresql://...` | From Step 2 (Internal URL) |
| `DATABASE_USERNAME` | `hyper_user` | From Step 2 |
| `DATABASE_PASSWORD` | `(password)` | From Step 2 |
| `JWT_SECRET` | `(generate 32+ char string)` | Use: `openssl rand -base64 32` |

### Gmail API (for sending emails):

| Variable | Value |
|----------|-------|
| `GMAIL_CLIENT_ID` | `622776222056-xxx.apps.googleusercontent.com` |
| `GMAIL_CLIENT_SECRET` | `GOCSPX-xxx` |
| `GMAIL_REFRESH_TOKEN` | `1//0abc...` (from local OAuth flow) |
| `GMAIL_FROM_EMAIL` | `gethyperindia@gmail.com` |

### OAuth (for user login):

| Variable | Value |
|----------|-------|
| `GOOGLE_CLIENT_ID` | Your Google OAuth client ID |
| `APPLE_BUNDLE_ID` | Your Apple app bundle ID |

### Cloudinary (for image uploads):

| Variable | Value |
|----------|-------|
| `CLOUDINARY_CLOUD_NAME` | Your cloud name |
| `CLOUDINARY_API_KEY` | Your API key |
| `CLOUDINARY_API_SECRET` | Your API secret |

### Razorpay (for payments):

| Variable | Value |
|----------|-------|
| `RAZORPAY_KEY_ID` | Your Razorpay key ID |
| `RAZORPAY_KEY_SECRET` | Your Razorpay secret |
| `RAZORPAY_ENVIRONMENT` | `live` (or `test`) |
| `RAZORPAY_WEBHOOK_SECRET` | Your webhook secret |

### Pricing:

| Variable | Value |
|----------|-------|
| `PRICING_TAX_RATE` | `0.18` |
| `PRICING_CONVENIENCE_FEE_RATE` | `0.02` |
| `PRICING_CONVENIENCE_FEE_FIXED` | `0` |

---

## 🚀 Step 5: Deploy

1. Click **"Create Web Service"**
2. Render will:
   - Clone your repository
   - Run the build command
   - Start your application
3. Wait for deployment (takes 3-5 minutes for first build)
4. Check the logs for any errors

---

## ✅ Step 6: Verify Deployment

### Check Health:
```bash
curl https://hyper-backend.onrender.com/actuator/health
```

### Check Swagger UI:
```
https://hyper-backend.onrender.com/swagger-ui.html
```

### Test an endpoint:
```bash
curl https://hyper-backend.onrender.com/api/services
```

---

## 🔍 Troubleshooting

### Build Fails

**Check logs for:**
- Missing dependencies → Check pom.xml
- Java version mismatch → Render uses Java 17 by default
- Test failures → We use `-DskipTests` to avoid this

**Common fixes:**
```bash
# Make sure mvnw is executable
chmod +x mvnw
git add mvnw
git commit -m "Make mvnw executable"
git push
```

### Application Crashes on Start

**Check:**
- Database connection → Verify DATABASE_URL format
- Missing env vars → Check all required variables are set
- Port binding → Make sure using `$PORT` env var

**Database URL format:**
```
jdbc:postgresql://hostname:5432/database_name
```
NOT the `postgres://` format.

### Gmail Not Working

1. Make sure you ran locally first to get refresh token
2. Verify all 4 Gmail env vars are set
3. Check logs for "Gmail Service initialized successfully"

### Database Migration Fails

Check Flyway logs. If you have existing data:
```properties
# In application-prod.properties
spring.flyway.baseline-on-migrate=true
```

---

## 📊 Monitoring

### View Logs:
- Go to your service → **Logs** tab
- Use filters to find errors

### Metrics:
- Go to your service → **Metrics** tab
- Monitor CPU, Memory, Response times

### Alerts:
- Set up alerts for downtime
- Configure notification channels (email, Slack)

---

## 🔄 Auto-Deploy

Render automatically deploys when you push to GitHub:

```bash
git add .
git commit -m "Update feature X"
git push origin main
# Render auto-deploys in 2-3 minutes
```

To disable auto-deploy:
- Go to service **Settings** → Turn off "Auto-Deploy"

---

## 💰 Cost Estimation (Monthly)

| Component | Free Tier | Production |
|-----------|-----------|------------|
| Web Service | $0 (sleeps after 15min) | $7-25 |
| PostgreSQL | $0 (90 days, then expires) | $7-20 |
| **Total** | $0 | $14-45 |

**Free tier limitations:**
- Service sleeps after 15 minutes of inactivity
- Cold start takes 30-60 seconds
- Database expires after 90 days

---

## 🔐 Security Checklist

- [ ] JWT_SECRET is unique and secure (32+ characters)
- [ ] All secrets are in environment variables (not in code)
- [ ] HTTPS is enabled (automatic on Render)
- [ ] Database is not publicly accessible
- [ ] credentials.json is NOT in your repository

---

## 📝 Quick Reference

### Your Render URLs:
```
Web Service: https://hyper-backend.onrender.com
Database: (internal only)
```

### Useful Commands:

```bash
# View local logs
./mvnw spring-boot:run

# Build for production
./mvnw -DskipTests clean package

# Test the JAR locally
java -jar target/Hyper_backend-0.0.1-SNAPSHOT.jar
```

### Environment Variables Summary:

```bash
# Required (12 variables)
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://...
DATABASE_USERNAME=hyper_user
DATABASE_PASSWORD=xxx
JWT_SECRET=xxx
GMAIL_CLIENT_ID=xxx
GMAIL_CLIENT_SECRET=xxx
GMAIL_REFRESH_TOKEN=xxx
GMAIL_FROM_EMAIL=gethyperindia@gmail.com
GOOGLE_CLIENT_ID=xxx
CLOUDINARY_CLOUD_NAME=xxx
CLOUDINARY_API_KEY=xxx
CLOUDINARY_API_SECRET=xxx
RAZORPAY_KEY_ID=xxx
RAZORPAY_KEY_SECRET=xxx
RAZORPAY_ENVIRONMENT=live
RAZORPAY_WEBHOOK_SECRET=xxx
```

---

## ✅ Deployment Complete!

Your Hyper Backend is now live on Render! 🎉

**Next steps:**
1. Configure your mobile app to use the Render URL
2. Set up Razorpay webhooks to point to your Render URL
3. Monitor logs for any issues
4. Consider upgrading to paid tier for production

---

## 📚 Additional Resources

- [Render Java Documentation](https://render.com/docs/deploy-java)
- [Spring Boot on Render](https://render.com/docs/deploy-spring-boot)
- [Render Environment Variables](https://render.com/docs/environment-variables)
- [Render PostgreSQL](https://render.com/docs/databases)
