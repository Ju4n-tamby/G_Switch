#!/bin/bash

# ============================================
# Script de compilation et exécution
# VOID RUNNER - Navigate the Void
# ============================================

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║              ⚡ VOID RUNNER - BUILD SCRIPT ⚡                 ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Répertoire du projet
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_DIR/src"
BIN_DIR="$PROJECT_DIR/bin"
RESOURCES_DIR="$PROJECT_DIR/resources"

# Créer le dossier bin s'il n'existe pas
mkdir -p "$BIN_DIR"

# Créer les dossiers de ressources
mkdir -p "$RESOURCES_DIR/images"
mkdir -p "$RESOURCES_DIR/sprites"
mkdir -p "$RESOURCES_DIR/sounds"

echo "📁 Compilation du projet..."
echo ""

# Trouver tous les fichiers Java
JAVA_FILES=$(find "$SRC_DIR" -name "*.java")

# Compiler
javac -d "$BIN_DIR" -sourcepath "$SRC_DIR" $JAVA_FILES 2>&1

if [ $? -eq 0 ]; then
    echo "✅ Compilation réussie !"
    echo ""
    echo "🚀 Lancement de VOID RUNNER..."
    echo ""
    
    # Exécuter depuis la racine du projet pour les ressources
    cd "$PROJECT_DIR"
    java -cp "$BIN_DIR" Main
else
    echo "❌ Erreur de compilation !"
    exit 1
fi
