@echo off
setlocal
cd /d "%~dp0"
set "GRADLE_USER_HOME=%CD%\.gradle\user-home"
if exist "D:\Java\bin\java.exe" set "JAVA_HOME=D:\Java"
call "%~dp0gradlew.bat" runClient --console=plain
if errorlevel 1 pause
