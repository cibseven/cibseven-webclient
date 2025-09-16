@echo off
REM Universal script to create production release for bpm-sdk, cib-common-components, or frontend
REM Usage: 
REM   bump-npm-release.bat bpm-sdk
REM   bump-npm-release.bat cib-common-components
REM   bump-npm-release.bat frontend

setlocal enabledelayedexpansion

REM Check if package name is provided
if "%1"=="" (
    echo Please specify package: bpm-sdk, cib-common-components, or frontend
    echo Usage: %0 [package-name]
    exit /b 1
)

set "PACKAGE=%1"

REM Validate package name
if not "%PACKAGE%"=="bpm-sdk" if not "%PACKAGE%"=="cib-common-components" if not "%PACKAGE%"=="frontend" (
    echo Invalid package: %PACKAGE%
    echo Valid packages: bpm-sdk, cib-common-components, frontend
    exit /b 1
)

echo Creating production release for %PACKAGE%...

REM Navigate to project root if not already there
if not exist "pom.xml" cd ..

REM Ensure we're on main and up to date
git checkout main
git fetch origin
git pull origin main

REM Navigate to package directory
cd "%PACKAGE%"

REM Check if package.json exists
if not exist "package.json" (
    echo Error: package.json not found in %PACKAGE% directory
    exit /b 1
)

REM Get current version and extract production version (remove -dev.x)
for /f "tokens=*" %%i in ('node -p "require('./package.json').version"') do set CURRENT_VERSION=%%i
for /f "tokens=1 delims=-" %%i in ("%CURRENT_VERSION%") do set PROD_VERSION=%%i

echo Current version: %CURRENT_VERSION%
echo Production version: %PROD_VERSION%

REM Create release branch
set "BRANCH_NAME=release-%PACKAGE%-%PROD_VERSION%"
git checkout -b "%BRANCH_NAME%"

REM 1. Bump to production version (removes -dev.x)
npm version --no-git-tag-version %PROD_VERSION%
git add package.json
git commit -m "release: %PACKAGE% %PROD_VERSION%"

REM 2. Push commits and tags
git push origin "%BRANCH_NAME%"

echo Package: %PACKAGE%
echo Released version: %PROD_VERSION%
echo Created branch: %BRANCH_NAME%
echo Ready to create PR for release!
pause