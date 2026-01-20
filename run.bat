@echo off
chcp 65001 >nul
cls

echo ================================================================
echo              VOID RUNNER - BUILD SCRIPT
echo                    Windows Edition
echo ================================================================
echo.

:: Repertoire du projet
set PROJECT_DIR=%~dp0
set SRC_DIR=%PROJECT_DIR%src
set BIN_DIR=%PROJECT_DIR%bin
set RESOURCES_DIR=%PROJECT_DIR%resources

:: Creer le dossier bin s'il n'existe pas
if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"

:: Creer les dossiers de ressources
if not exist "%RESOURCES_DIR%\images" mkdir "%RESOURCES_DIR%\images"
if not exist "%RESOURCES_DIR%\sprites" mkdir "%RESOURCES_DIR%\sprites"
if not exist "%RESOURCES_DIR%\sounds" mkdir "%RESOURCES_DIR%\sounds"

echo [INFO] Compilation du projet...
echo.

:: Aller dans le repertoire du projet
cd /d "%PROJECT_DIR%"

:: Compiler tous les fichiers Java (lister tous les fichiers)
dir /s /b "%SRC_DIR%\*.java" > sources.txt
javac -encoding UTF-8 -d "%BIN_DIR%" -sourcepath "%SRC_DIR%" @sources.txt

if %ERRORLEVEL% EQU 0 (
    del sources.txt
    echo.
    echo [OK] Compilation reussie !
    echo.
    echo [INFO] Lancement de VOID RUNNER...
    echo.
    
    :: Executer depuis la racine du projet pour les ressources
    java -cp "%BIN_DIR%" Main
) else (
    del sources.txt 2>nul
    echo.
    echo [ERREUR] Erreur de compilation !
    echo Verifiez que Java JDK est installe et dans le PATH
    pause
    exit /b 1
)

pause
