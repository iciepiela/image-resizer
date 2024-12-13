#!/bin/bash

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Pull any updated images and build the services
echo "Pulling images and building the services..."
docker-compose pull
docker-compose build

# Start the services
echo "Starting services..."
docker-compose up -w

# Verify if the services are running
echo "Checking running containers..."
docker ps

# Instructions for stopping the services
echo ""
echo "All services are running. To stop them, use: docker-compose down"
