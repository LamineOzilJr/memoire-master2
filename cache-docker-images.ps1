# Quick Image Cache Script
# This script pre-downloads all base images needed for building
# Usage: .\cache-docker-images.ps1

Write-Host "🐳 Pre-caching Docker Base Images" -ForegroundColor Green
Write-Host "This downloads images needed for building (~2-3 GB)" -ForegroundColor Yellow
Write-Host "Estimated time: 5-15 minutes depending on internet speed" -ForegroundColor Yellow
Write-Host ""

$images = @(
    @{ name = "Maven"; image = "maven:3.9.4-eclipse-temurin-17" },
    @{ name = "Node.js"; image = "node:18-alpine" },
    @{ name = "Nginx"; image = "nginx:alpine" },
    @{ name = "MySQL"; image = "mysql:8.0" },
    @{ name = "Eclipse Temurin JRE"; image = "eclipse-temurin:17-jre-alpine" }
)

$successCount = 0
$failCount = 0

foreach ($item in $images) {
    Write-Host "Pulling $($item.name): $($item.image)" -ForegroundColor Cyan
    docker pull $item.image

    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ Success`n" -ForegroundColor Green
        $successCount++
    } else {
        Write-Host "  ❌ Failed`n" -ForegroundColor Red
        $failCount++
    }
}

Write-Host "=================================================="
Write-Host "Results: $successCount successful, $failCount failed" -ForegroundColor Cyan
Write-Host "=================================================="
Write-Host ""

if ($failCount -eq 0) {
    Write-Host "✅ All images cached successfully!" -ForegroundColor Green
    Write-Host "You can now build without waiting for image downloads." -ForegroundColor Green
} else {
    Write-Host "⚠️  Some images failed to download." -ForegroundColor Yellow
    Write-Host "Docker will attempt to pull these during the build process." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "To build your containers now, run:" -ForegroundColor Cyan
Write-Host "  docker-compose up -d" -ForegroundColor Yellow

