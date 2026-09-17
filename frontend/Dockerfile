# syntax=docker/dockerfile:1

# ---- Build stage: produce the static Angular bundle ----
FROM node:22-alpine AS build
WORKDIR /app

# Install deps against the lockfile first for layer caching.
COPY package.json package-lock.json ./
RUN npm ci

# Build the production bundle -> dist/frontend/browser
COPY . .
RUN npm run build

# ---- Runtime stage: serve the SPA + reverse-proxy /api to the backend ----
FROM nginx:alpine AS runtime

# Our SPA + API-proxy server config.
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Static assets built above.
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
