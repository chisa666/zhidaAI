@echo off
setlocal
set "TEMP=D:\Environment\temp"
set "TMP=D:\Environment\tmp"
set "npm_config_cache=D:\Environment\npm-cache"
cd /d "%~dp0frontend"
npm run dev
endlocal
