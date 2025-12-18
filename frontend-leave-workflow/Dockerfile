# Multi-stage build for Angular Frontend
FROM node:18-alpine AS build
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the application
ARG API_BASE_URL=http://localhost:8080/api
ENV API_BASE_URL=${API_BASE_URL}
RUN npm run build -- --configuration production

# Production stage with nginx
FROM nginx:alpine
COPY --from=build /app/dist/frontend-leave-workflow /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80 || exit 1

CMD ["nginx", "-g", "daemon off;"]

