@echo off
rem Guess Market client launcher (Windows, Java 25).
rem   run-client.bat                                   -> connects to http://localhost:8080/guess-market
rem   run-client.bat http://HOST:8080/guess-market     -> connects to another server
cd /d "%~dp0"
if "%~1"=="" (
    java --enable-native-access=ALL-UNNAMED -cp "guess-market-client.jar;lib\*" ui.Launcher
) else (
    java --enable-native-access=ALL-UNNAMED -Dguessmarket.url=%~1 -cp "guess-market-client.jar;lib\*" ui.Launcher
)
if errorlevel 1 pause