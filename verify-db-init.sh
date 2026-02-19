#!/bin/bash
# Quick verification script for database initialization
# Run this after starting your application in staging mode

echo "🔍 Database Initialization Verification Script"
echo "================================================"
echo ""

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo "❌ psql not found. Please install PostgreSQL client tools."
    echo "   Install with: brew install postgresql"
    exit 1
fi

# Prompt for database connection details
echo "Enter your database connection details:"
read -p "Host (e.g., your-db.region.neon.tech): " DB_HOST
read -p "Port (default 5432): " DB_PORT
DB_PORT=${DB_PORT:-5432}
read -p "Database name: " DB_NAME
read -p "Username: " DB_USER
read -sp "Password: " DB_PASS
echo ""
echo ""

# Connection string
export PGPASSWORD="$DB_PASS"

echo "🔗 Connecting to database..."
echo ""

# Test connection
if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1" > /dev/null 2>&1; then
    echo "❌ Failed to connect to database. Please check your credentials."
    exit 1
fi

echo "✅ Connected successfully!"
echo ""

# Run verification queries
echo "📊 Checking data initialization..."
echo ""

echo "1. Flyway Migrations:"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;"
echo ""

echo "2. Activities Count:"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT COUNT(*) as total,
       COUNT(CASE WHEN enabled = true THEN 1 END) as enabled,
       COUNT(CASE WHEN enabled = false THEN 1 END) as disabled
FROM activities;"
echo ""

echo "3. Activities List:"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT id, code, name, enabled
FROM activities
ORDER BY name;"
echo ""

echo "4. Users Count:"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT role, COUNT(*) as count
FROM users
GROUP BY role
ORDER BY role;"
echo ""

echo "5. Admin Profiles:"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT ap.id, ap.business_name, ap.city, u.email
FROM admin_profiles ap
JOIN users u ON ap.user_id = u.id
ORDER BY ap.id;"
echo ""

echo "6. Expense Categories Count:"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT ap.business_name, ec.type, COUNT(*) as count
FROM expense_categories ec
JOIN admin_profiles ap ON ec.admin_profile_id = ap.id
GROUP BY ap.business_name, ec.type
ORDER BY ap.business_name, ec.type;"
echo ""

echo "7. Services Count:"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT COUNT(*) as total,
       COUNT(CASE WHEN availability = true THEN 1 END) as available,
       COUNT(CASE WHEN availability = false THEN 1 END) as unavailable
FROM services;"
echo ""

echo "8. Services List:"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT id, name, city, contact_number, availability
FROM services
ORDER BY id;"
echo ""

echo "9. Service Resources Count:"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT s.name as service_name, COUNT(sr.id) as resources_count
FROM services s
LEFT JOIN service_resources sr ON sr.service_id = s.id
GROUP BY s.name
ORDER BY s.name;"
echo ""

echo "10. Resource Slot Configs:"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT sr.name as resource_name,
       rsc.opening_time,
       rsc.closing_time,
       rsc.slot_duration_minutes,
       rsc.base_price,
       rsc.enabled
FROM resource_slot_configs rsc
JOIN service_resources sr ON rsc.resource_id = sr.id
ORDER BY sr.id;"
echo ""

echo "✅ Verification Complete!"
echo ""
echo "Expected values:"
echo "  - Activities: 14"
echo "  - Users: At least 3 (1 manager + 2 admins)"
echo "  - Admin Profiles: 2 (Mumbai + Vellore)"
echo "  - Expense Categories: 30 (15 per admin)"
echo "  - Services: 2 (Mumbai + Vellore)"
echo "  - Resources: 5 (2 Mumbai + 3 Vellore)"
echo ""

# Cleanup
unset PGPASSWORD

