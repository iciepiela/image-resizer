@echo off

:: Check if Docker is installed
where docker >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Docker is not installed. Please install Docker first.
    exit /b 1
)

:: Check if Docker Compose is installed
where docker-compose >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Docker Compose is not installed. Please install Docker Compose first.
    exit /b 1
)

:: Pull any updated images and build the services
echo Pulling images and building the services...
docker-compose pull
docker-compose build

:: Start the services
echo Starting services...
docker-compose up -w

:: Verify if the services are running
echo Checking running containers...
docker ps

:: Instructions for stopping the services
echo.
echo All services are running. To stop them, use: docker-compose down
pause
