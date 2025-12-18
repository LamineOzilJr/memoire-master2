# Docker Build Error Quick Fix Script
# Usage: .\fix-docker-build.ps1

Write-Host "=================================================="
Write-Host "🔧 Docker Build Error Fix Script" -ForegroundColor Green
Write-Host "=================================================="
Write-Host ""

# Check if running as admin
$isAdmin = [bool]([System.Security.Principal.WindowsIdentity]::GetCurrent().groups -match "S-1-5-32-544")
if (-not $isAdmin) {
    Write-Host "⚠️  Some operations require Administrator privileges" -ForegroundColor Yellow
    Write-Host "Consider running this script as Administrator for best results" -ForegroundColor Yellow
    Write-Host ""
}

# Step 1: Check Docker
Write-Host "Step 1: Checking Docker installation..." -ForegroundColor Cyan
try {
    $dockerVersion = docker --version
    Write-Host "✅ Docker found: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker is not installed or not in PATH" -ForegroundColor Red
    exit 1
}

# Step 2: Check Docker daemon
Write-Host ""
Write-Host "Step 2: Checking Docker daemon..." -ForegroundColor Cyan
try {
    docker info > $null 2>&1
    Write-Host "✅ Docker daemon is running" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker daemon is not running" -ForegroundColor Red
    Write-Host "   Please start Docker Desktop or Docker service" -ForegroundColor Yellow
    exit 1
}

# Step 3: Reset authentication
Write-Host ""
Write-Host "Step 3: Resetting Docker authentication..." -ForegroundColor Cyan
docker logout 2>&1 | Out-Null
Write-Host "✅ Logged out from Docker Hub" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Please log in to Docker Hub:" -ForegroundColor Yellow
docker login
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Docker login failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Successfully logged in" -ForegroundColor Green

# Step 4: Clean Docker cache
Write-Host ""
Write-Host "Step 4: Cleaning Docker cache..." -ForegroundColor Cyan
Write-Host "   This may take a moment..." -ForegroundColor Gray
docker system prune -a -f 2>&1 | Out-Null
Write-Host "✅ Docker cache cleaned" -ForegroundColor Green

# Step 5: Pre-cache base images
Write-Host ""
Write-Host "Step 5: Pre-caching base images..." -ForegroundColor Cyan
Write-Host "   This will download ~2-3 GB (may take 5-15 minutes)" -ForegroundColor Yellow
Write-Host ""

$images = @(
    "maven:3.9.4-eclipse-temurin-17",
    "node:18-alpine",
    "nginx:alpine",
    "mysql:8.0",
    "eclipse-temurin:17-jre-alpine"
)

$failedImages = @()
foreach ($image in $images) {
    Write-Host "   Pulling $image..." -ForegroundColor Gray
    docker pull $image
    if ($LASTEXITCODE -ne 0) {
        $failedImages += $image
        Write-Host "   ⚠️  Failed to pull $image" -ForegroundColor Yellow
    } else {
        Write-Host "   ✅ $image" -ForegroundColor Green
    }
}

# Step 6: Restart Docker service
Write-Host ""
Write-Host "Step 6: Restarting Docker service..." -ForegroundColor Cyan
if ($isAdmin) {
    try {
        Restart-Service docker -Force -ErrorAction Stop
        Start-Sleep -Seconds 10
        Write-Host "✅ Docker service restarted" -ForegroundColor Green
    } catch {
        Write-Host "⚠️  Could not restart Docker service" -ForegroundColor Yellow
        Write-Host "   Please restart Docker Desktop manually" -ForegroundColor Yellow
    }
} else {
    Write-Host "⚠️  Skipping service restart (requires admin)" -ForegroundColor Yellow
    Write-Host "   Please restart Docker Desktop manually" -ForegroundColor Yellow
}

# Step 7: Ready to build
Write-Host ""
Write-Host "=================================================="
Write-Host "✅ Docker is ready for building!" -ForegroundColor Green
Write-Host "=================================================="
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Navigate to your project root:"
Write-Host "     cd C:\xampp\htdocs\MesProjets\memoire-master2"
Write-Host ""
Write-Host "  2. Try building backend:"
Write-Host "     docker build -t leaveworkflow-backend:latest ./backend-leaveworkflow"
Write-Host ""
Write-Host "  3. Try building frontend:"
Write-Host "     docker build -t leaveworkflow-frontend:latest --build-arg API_BASE_URL=http://localhost:8080/api ./frontend-leave-workflow"
Write-Host ""
Write-Host "  4. If builds succeed, run all services:"
Write-Host "     docker-compose up -d"
Write-Host ""

if ($failedImages.Count -gt 0) {
    Write-Host "⚠️  Warning: Some images failed to download:" -ForegroundColor Yellow
    foreach ($img in $failedImages) {
        Write-Host "   - $img" -ForegroundColor Yellow
    }
    Write-Host "   Docker will attempt to pull these during build." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "For more help, see: DOCKER_ERROR_TROUBLESHOOTING.md" -ForegroundColor Cyan
Write-Host ""

