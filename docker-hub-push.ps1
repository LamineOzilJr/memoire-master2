# Docker Hub Push Script for Windows PowerShell
# Usage: .\docker-hub-push.ps1 -DockerUsername "myusername"

param(
    [Parameter(Mandatory=$true)]
    [string]$DockerUsername
)

$TAG = "latest"
$VERSION = "1.0.0"

Write-Host "=================================================="
Write-Host "🐳 Docker Hub Publishing Script (Windows)" -ForegroundColor Green
Write-Host "=================================================="
Write-Host "Docker Hub Username: $DockerUsername"
Write-Host ""

# Check if docker is running
try {
    docker info > $null 2>&1
} catch {
    Write-Host "❌ Docker is not running. Please start Docker first." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Building Backend Image..." -ForegroundColor Yellow
docker build -t $DockerUsername/leaveworkflow-backend:$TAG `
             -t $DockerUsername/leaveworkflow-backend:$VERSION `
             ./backend-leaveworkflow

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Backend build failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Backend image built successfully" -ForegroundColor Green

Write-Host ""
Write-Host "Building Frontend Image..." -ForegroundColor Yellow
docker build -t $DockerUsername/leaveworkflow-frontend:$TAG `
             -t $DockerUsername/leaveworkflow-frontend:$VERSION `
             --build-arg API_BASE_URL=http://api.yourdomain.com/api `
             ./frontend-leave-workflow

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Frontend build failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Frontend image built successfully" -ForegroundColor Green

Write-Host ""
Write-Host "Pushing Backend Image to Docker Hub..." -ForegroundColor Yellow
docker push $DockerUsername/leaveworkflow-backend:$TAG
docker push $DockerUsername/leaveworkflow-backend:$VERSION

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Backend push failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Backend pushed successfully" -ForegroundColor Green

Write-Host ""
Write-Host "Pushing Frontend Image to Docker Hub..." -ForegroundColor Yellow
docker push $DockerUsername/leaveworkflow-frontend:$TAG
docker push $DockerUsername/leaveworkflow-frontend:$VERSION

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Frontend push failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Frontend pushed successfully" -ForegroundColor Green

Write-Host ""
Write-Host "=================================================="
Write-Host "✅ All images published to Docker Hub!" -ForegroundColor Green
Write-Host "=================================================="
Write-Host ""
Write-Host "Repository URLs:"
Write-Host "  Backend:  https://hub.docker.com/r/$DockerUsername/leaveworkflow-backend"
Write-Host "  Frontend: https://hub.docker.com/r/$DockerUsername/leaveworkflow-frontend"
Write-Host ""
Write-Host "Pull commands:"
Write-Host "  docker pull $DockerUsername/leaveworkflow-backend:$TAG"
Write-Host "  docker pull $DockerUsername/leaveworkflow-frontend:$TAG"
Write-Host ""

