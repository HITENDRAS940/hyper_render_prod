# Gmail API Setup Instructions

## Overview

The Gmail API integration supports two modes:
1. **Local Development**: Uses `credentials.json` file with interactive OAuth flow
2. **Production (Render)**: Uses environment variables with stored refresh token

---

## 🔧 LOCAL DEVELOPMENT SETUP

### Step 1: Get credentials.json from Google Cloud Console

1. Go to **Google Cloud Console**: https://console.cloud.google.com
2. Select your project (or create new one)
3. Enable **Gmail API**:
   - Go to "APIs & Services" > "Library"
   - Search for "Gmail API"
   - Click "Enable"

4. Create **OAuth Client ID**:
   - Go to "APIs & Services" > "Credentials"
   - Click "+ CREATE CREDENTIALS" > "OAuth client ID"
   - Application type: **Desktop app** (IMPORTANT!)
   - Name: "Hyper Backend"
   - Click "Create"

5. Download the JSON file and save as:
   ```
   src/main/resources/credentials.json
   ```

### Step 2: First Run (Get Refresh Token)

1. Start your application:
   ```bash
   ./mvnw spring-boot:run
   ```

2. A browser will open for OAuth authorization

3. Login with: **gethyperindia@gmail.com**

4. Grant the required permissions

5. **IMPORTANT**: Check the console logs for the refresh token:
   ```
   =======================================================
   IMPORTANT: Save this refresh token for production use!
   GMAIL_REFRESH_TOKEN=1//0abc123...xyz
   =======================================================
   ```

6. **Copy and save this refresh token** - you'll need it for Render!

---

## 🚀 PRODUCTION DEPLOYMENT (Render)

### Step 1: Get Your Credentials

From your `credentials.json` file, extract:
- `client_id` → GMAIL_CLIENT_ID
- `client_secret` → GMAIL_CLIENT_SECRET

From the local development run (Step 2 above):
- The logged refresh token → GMAIL_REFRESH_TOKEN

### Step 2: Set Environment Variables on Render

In your Render dashboard, add these environment variables:

| Variable | Value |
|----------|-------|
| `GMAIL_CLIENT_ID` | `622776222056-xxx.apps.googleusercontent.com` |
| `GMAIL_CLIENT_SECRET` | `GOCSPX-xxx` |
| `GMAIL_REFRESH_TOKEN` | `1//0abc123...` (from local run) |
| `GMAIL_FROM_EMAIL` | `gethyperindia@gmail.com` |

### Step 3: Deploy

That's it! Your app will use the environment variables in production.

**No files needed on Render** - everything is in environment variables.

---

## 📋 How It Works

### Local Development
```
credentials.json exists → Interactive OAuth flow → Saves tokens to .tokens/
```

### Production (Render)
```
Environment variables set → Uses refresh token directly → No files needed
```

---

## ⚠️ Important Notes

### Security
- ✅ `credentials.json` is in `.gitignore` - never committed
- ✅ `.tokens/` is in `.gitignore` - never committed
- ✅ Refresh tokens are stored as env vars on Render (secure)

### Refresh Token Expiration
- Refresh tokens are **long-lived** (usually don't expire)
- If you revoke access in Google Account, you'll need a new token
- If token expires, run locally again to get a new one

### Gmail Limits
- **500 emails/day** (free Gmail account)
- Resets every 24 hours

---

## 🔍 Troubleshooting

### "credentials.json not found" (Local)
- Make sure file is at: `src/main/resources/credentials.json`
- Run `./mvnw clean compile` to copy to target/classes

### "Invalid refresh token" (Production)
1. Run the app locally to get a new refresh token
2. Update `GMAIL_REFRESH_TOKEN` on Render

### "Port 8080 already in use" (Local OAuth)
```bash
# Kill the process using port 8080
lsof -ti:8080 | xargs kill -9
```

### "This app isn't verified" (First OAuth)
- Click "Advanced" → "Go to Hyper Backend (unsafe)"
- This is normal for development apps

---

## 📁 Files

### Committed to Git:
- `GmailService.java` - Service with dual-mode support
- `application.properties` - Gmail config placeholders

### NOT Committed (in .gitignore):
- `credentials.json` - OAuth client credentials
- `.tokens/` - Stored access tokens

---

## ✅ Checklist

### For Local Development:
- [ ] `credentials.json` in `src/main/resources/`
- [ ] OAuth client type is "Desktop app"
- [ ] Ran app locally and completed OAuth flow
- [ ] Saved the refresh token from logs

### For Production (Render):
- [ ] `GMAIL_CLIENT_ID` set
- [ ] `GMAIL_CLIENT_SECRET` set
- [ ] `GMAIL_REFRESH_TOKEN` set
- [ ] `GMAIL_FROM_EMAIL` set (optional, defaults to gethyperindia@gmail.com)
