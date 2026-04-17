#!/usr/bin/env bash
# ============================================================
# DuckyTool - Run Script (Linux / macOS)
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src"
BIN_DIR="$SCRIPT_DIR/build"

# ── Colours ──────────────────────────────────────────────────
GREEN="\033[32m"; YELLOW="\033[33m"; RED="\033[31m"; RESET="\033[0m"; BOLD="\033[1m"

# ── Check Java ───────────────────────────────────────────────
if ! command -v java &>/dev/null; then
  echo -e "${RED}[ERROR] java not found. Please install Java 11 or later.${RESET}"
  exit 1
fi

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
if [[ "$JAVA_VER" -lt 11 ]]; then
  echo -e "${YELLOW}[WARN] Java $JAVA_VER detected. Java 11+ recommended.${RESET}"
fi

# ── Compile ───────────────────────────────────────────────────
echo -e "${BOLD}Compiling DuckyTool...${RESET}"
mkdir -p "$BIN_DIR"

if javac -source 11 -target 11 -d "$BIN_DIR" \
    "$SRC_DIR/KeyMapper.java" \
    "$SRC_DIR/FileHandler.java" \
    "$SRC_DIR/Encoder.java" \
    "$SRC_DIR/Decoder.java" \
    "$SRC_DIR/Main.java"; then
  echo -e "${GREEN}✓ Compilation successful.${RESET}"
else
  echo -e "${RED}[ERROR] Compilation failed.${RESET}"
  exit 1
fi

# ── Run ───────────────────────────────────────────────────────
echo ""
if [[ $# -eq 0 ]]; then
  # Interactive mode
  java -cp "$BIN_DIR" Main
elif [[ $# -eq 3 ]]; then
  # CLI mode: encode/decode
  java -cp "$BIN_DIR" Main "$1" "$2" "$3"
else
  echo "Usage:"
  echo "  ./run.sh                              (interactive menu)"
  echo "  ./run.sh encode samples/input.txt  samples/output.bin"
  echo "  ./run.sh decode samples/output.bin samples/decoded.txt"
  exit 1
fi
