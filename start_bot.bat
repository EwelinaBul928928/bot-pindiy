@echo off
setlocal

REM 
set SRC=.
set LIB=lib
set CP=%SRC%;%LIB%\*

REM 
echo [INFO] Kompiluję plik main.java...
javac -cp "%CP%" main.java

IF %ERRORLEVEL% NEQ 0 (
    echo [BŁĄD] Kompilacja nie powiodła się.
    pause
    exit /b 1
)

echo [INFO] Uruchamiam bota
java -cp "%CP%" main

echo [INFO] Bot zakończył działanie.
pause
endlocal
