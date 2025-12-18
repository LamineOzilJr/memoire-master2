#!/bin/bash

# Docker Hub Push Script
# Usage: ./docker-hub-push.sh <docker-username>

if [ $# -eq 0 ]; then
    echo "Usage: ./docker-hub-push.sh <docker-username>"
    echo "Example: ./docker-hub-push.sh myusername"
    exit 1
fi

DOCKER_USER=$1
REGISTRY="${DOCKER_USER}"
TAG="latest"
VERSION="1.0.0"

echo "=================================================="
echo "🐳 Docker Hub Publishing Script"
echo "=================================================="
echo "Docker Hub Username: $DOCKER_USER"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker first.${NC}"
    exit 1
fi

# Check if user is logged in
if ! grep -q "\"auths\"" ~/.docker/config.json 2>/dev/null; then
    echo -e "${YELLOW}⚠️  You don't appear to be logged in to Docker Hub.${NC}"
    echo "Attempting to login..."
    docker login
fi

echo ""
echo -e "${YELLOW}Building Backend Image...${NC}"
docker build -t ${REGISTRY}/leaveworkflow-backend:${TAG} \
             -t ${REGISTRY}/leaveworkflow-backend:${VERSION} \
             ./backend-leaveworkflow

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Backend build failed${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Backend image built successfully${NC}"

echo ""
echo -e "${YELLOW}Building Frontend Image...${NC}"
docker build -t ${REGISTRY}/leaveworkflow-frontend:${TAG} \
             -t ${REGISTRY}/leaveworkflow-frontend:${VERSION} \
             --build-arg API_BASE_URL=http://api.yourdomain.com/api \
             ./frontend-leave-workflow

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Frontend build failed${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Frontend image built successfully${NC}"

echo ""
echo -e "${YELLOW}Pushing Backend Image to Docker Hub...${NC}"
docker push ${REGISTRY}/leaveworkflow-backend:${TAG}
docker push ${REGISTRY}/leaveworkflow-backend:${VERSION}

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Backend push failed${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Backend pushed successfully${NC}"

echo ""
echo -e "${YELLOW}Pushing Frontend Image to Docker Hub...${NC}"
docker push ${REGISTRY}/leaveworkflow-frontend:${TAG}
docker push ${REGISTRY}/leaveworkflow-frontend:${VERSION}

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Frontend push failed${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Frontend pushed successfully${NC}"

echo ""
echo "=================================================="
echo -e "${GREEN}✅ All images published to Docker Hub!${NC}"
echo "=================================================="
echo ""
echo "Repository URLs:"
echo "  Backend:  https://hub.docker.com/r/${DOCKER_USER}/leaveworkflow-backend"
echo "  Frontend: https://hub.docker.com/r/${DOCKER_USER}/leaveworkflow-frontend"
echo ""
echo "Pull commands:"
echo "  docker pull ${DOCKER_USER}/leaveworkflow-backend:${TAG}"
echo "  docker pull ${DOCKER_USER}/leaveworkflow-frontend:${TAG}"
echo ""

