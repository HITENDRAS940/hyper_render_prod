#!/bin/bash
# Quick script to run the application with Swagger UI enabled

echo "╔══════════════════════════════════════════════════════════════════════════════╗"
echo "║                  🚀 Starting Application with Swagger UI 🚀                   ║"
echo "╚══════════════════════════════════════════════════════════════════════════════╝"
echo ""

# Check if in correct directory
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: Not in the correct directory. Please cd to backendHyper first."
    exit 1
fi

echo "📋 Step 1: Cleaning previous build..."
./mvnw clean

echo ""
echo "📋 Step 2: Rebuilding with Swagger UI dependencies..."
./mvnw package -DskipTests

if [ $? -ne 0 ]; then
    echo ""
    echo "❌ Build failed! Please check the errors above."
    exit 1
fi

echo ""
echo "✅ Build successful!"
echo ""
echo "📋 Step 3: Starting application with DEV profile..."
echo ""
echo "⚙️  Configuration:"
echo "   • Profile: dev"
echo "   • Port: 8080"
echo "   • Swagger UI: ENABLED"
echo ""
echo "🌐 Swagger UI will be available at:"
echo "   • http://localhost:8080/swagger-ui"
echo "   • http://localhost:8080/swagger-ui/index.html"
echo ""
echo "📄 OpenAPI JSON spec:"
echo "   • http://localhost:8080/v3/api-docs"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Starting application... Press Ctrl+C to stop"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Set profile and run
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run

