@echo off
REM Universal script to bump dev version for bpm-sdk, cib-common-components, or frontend
REM Usage: 
REM   bump-npm-dev.bat bmp-sdk
REM   bump-npm-dev.bat cib-common-components
REM   bump-npm-dev.bat frontend

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

echo Bumping %PACKAGE% dev version...

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

REM Create new branch with timestamp
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "YY=%dt:~2,2%" & set "YYYY=%dt:~0,4%" & set "MM=%dt:~4,2%" & set "DD=%dt:~6,2%"
set "HH=%dt:~8,2%" & set "Min=%dt:~10,2%" & set "Sec=%dt:~12,2%"
set "BRANCH_NAME=bump-%PACKAGE%-version-%YYYY%%MM%%DD%-%HH%%Min%%Sec%"

git checkout -b "%BRANCH_NAME%"

REM Bump prerelease version
npm version prerelease --no-git-tag-version

REM Get the new version
for /f "tokens=*" %%i in ('node -p "require('./package.json').version"') do set NEW_VERSION=%%i

REM Commit changes
git add package.json
git commit -m "bump: increase %PACKAGE% version to %NEW_VERSION%"

REM Push branch
git push origin "%BRANCH_NAME%"

echo Package: %PACKAGE%
echo Created branch: %BRANCH_NAME%
echo Bumped to version: %NEW_VERSION%
echo Ready to create PR!
pause