@echo off
setlocal

set "PROJECT_DIR=%~dp0"
set "MAVEN_HOME=D:\Environment\apache-maven-3.9.16"
set "MAVEN_REPO=D:\Environment\apache-maven-3.9.16\repo"
set "NPM_CACHE=D:\Environment\npm-cache"
set "ENV_ROOT=D:\Environment"
set "TEMP=%ENV_ROOT%\temp"
set "TMP=%ENV_ROOT%\tmp"
set "MAVEN_OPTS=-Djava.io.tmpdir=%ENV_ROOT%\maven-tmp"

if not exist "%TEMP%" mkdir "%TEMP%"
if not exist "%TMP%" mkdir "%TMP%"
if not exist "%ENV_ROOT%\maven-tmp" mkdir "%ENV_ROOT%\maven-tmp"

echo [1/2] Downloading Maven dependencies to %MAVEN_REPO%
call "%MAVEN_HOME%\bin\mvn.cmd" -s "%MAVEN_HOME%\conf\settings.xml" -Dmaven.repo.local="%MAVEN_REPO%" -f "%PROJECT_DIR%pom.xml" dependency:go-offline
if errorlevel 1 (
  echo Maven dependency download failed.
  exit /b 1
)

echo [2/2] Downloading npm dependencies to %NPM_CACHE%
pushd "%PROJECT_DIR%frontend"
call npm install --cache "%NPM_CACHE%"
set "NPM_EXIT=%ERRORLEVEL%"
popd
if not "%NPM_EXIT%"=="0" (
  echo npm dependency download failed.
  exit /b %NPM_EXIT%
)

echo All dependencies downloaded successfully.
endlocal
