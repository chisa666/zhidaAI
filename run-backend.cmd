@echo off
setlocal
set "TEMP=D:\Environment\temp"
set "TMP=D:\Environment\tmp"
set "SPRING_PROFILES_ACTIVE=ollama"
set "OLLAMA_BASE_URL=http://127.0.0.1:11434"
set "OLLAMA_MODEL=qwen3:1.7b"
set "VECTOR_STORE_TYPE=pgvector"
set "JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=D:\Environment\java-tmp -Xmx768m -XX:+UseSerialGC"
if not exist "%TEMP%" mkdir "%TEMP%"
if not exist "%TMP%" mkdir "%TMP%"
if not exist "D:\Environment\java-tmp" mkdir "D:\Environment\java-tmp"
"D:\New Folder\bin\java.exe" -jar "%~dp0target\zhida-ai-0.0.1-SNAPSHOT.jar"
endlocal
