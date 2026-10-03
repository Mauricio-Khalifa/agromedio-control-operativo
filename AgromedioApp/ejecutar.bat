@echo off
rem =========================================================
rem  Sistema de Control Operativo AGROMEDIO
rem  Inicio rapido de la aplicacion (requiere Java 17 o superior)
rem =========================================================
cd /d "%~dp0"

if not exist "dist\AgromedioApp.jar" (
    echo No se encontro dist\AgromedioApp.jar
    echo Ejecute primero: "C:\Program Files\NetBeans-21\netbeans\extide\ant\bin\ant.bat" jar
    pause
    exit /b 1
)

start "" "javaw" -Dfile.encoding=UTF-8 -jar "dist\AgromedioApp.jar"
if errorlevel 1 (
    echo Error al iniciar la aplicacion. Verifique que Java este instalado.
    pause
    exit /b 1
)
