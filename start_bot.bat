@echo off
setlocal

REM === Ścieżka do JDK i bibliotek Selenium ===
set SRC=.
set LIB=lib
set CP=%SRC%;%LIB%\*

REM === Kompiluj main.java ===
echo [INFO] Kompiluję plik main.java...
javac -cp "%CP%" main.java

IF %ERRORLEVEL% NEQ 0 (
    echo [BŁĄD] Kompilacja nie powiodła się.
    pause
    exit /b 1
)

echo [INFO] Uruchamiam bota...
java -cp "%CP%" main

echo [INFO] Bot zakończył działanie.
pause
endlocal
