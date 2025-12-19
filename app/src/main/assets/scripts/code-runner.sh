#!/bin/bash
# code-runner.sh - Unified code runner for BananaSpaghetti
# Handles code execution and web serving for 15+ languages

# === ENVIRONMENT SETUP (silent) ===
export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin:$PREFIX/local/bin:$PATH
export HOME=/home
export TERM=xterm-256color

# === ARGUMENT PARSING ===
FILE="$1"

if [ -z "$FILE" ]; then
    echo "Usage: code-runner.sh <file_path>"
    exit 1
fi

# === EXTRACT FILE INFO ===
FILENAME=$(basename "$FILE")
EXT="${FILENAME##*.}"
NAME="${FILENAME%.*}"
DIR=$(dirname "$FILE")

# Change to file directory for relative imports
cd "$DIR" 2>/dev/null || cd /

# === DEPENDENCY CHECK FUNCTION ===
# Outputs MISSING_PKG:package_name if command not found
# Returns exit code 100 for missing package
check_pkg() {
    local cmd="$1"
    local pkg="$2"
    
    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "MISSING_PKG:$pkg"
        echo "! Missing: $pkg (command: $cmd)"
        exit 100
    fi
}

# === COMPILE AND RUN HELPER ===
compile_and_run() {
    local compiler="$1"
    local flags="$2"
    local src="$3"
    local out="$4"
    
    echo "> $compiler $src $flags"
    if $compiler "$src" $flags -o "$out" 2>&1; then
        "./$out"
        local exit_code=$?
        rm -f "$out" 2>/dev/null
        return $exit_code
    else
        echo "X Compilation failed"
        return 1
    fi
}

# === MAIN RUNNER ===
case "$EXT" in
    # === PYTHON ===
    py)
        check_pkg python3 python3
        echo "> python3 $FILENAME"
        python3 "$FILE"
        ;;
    
    # === JAVASCRIPT (Node.js) ===
    js)
        check_pkg node nodejs
        echo "> node $FILENAME"
        node "$FILE"
        ;;
    
    # === TYPESCRIPT ===
    ts)
        check_pkg npx nodejs
        echo "> npx ts-node $FILENAME"
        npx ts-node "$FILE"
        ;;
    
    # === C ===
    c)
        check_pkg gcc gcc
        compile_and_run gcc "" "$FILE" "$NAME"
        ;;
    
    # === C++ ===
    cpp|cc|cxx)
        check_pkg g++ g++
        compile_and_run g++ "" "$FILE" "$NAME"
        ;;
    
    # === JAVA ===
    java)
        check_pkg javac openjdk17
        echo "> javac $FILENAME"
        if javac "$FILE" 2>&1; then
            echo "> java $NAME"
            java "$NAME"
            # Cleanup class file
            rm -f "$NAME.class" 2>/dev/null
        else
            echo "X Compilation failed"
        fi
        ;;
    
    # === GO ===
    go)
        check_pkg go go
        echo "> go run $FILENAME"
        go run "$FILE"
        ;;
    
    # === RUST ===
    rs)
        check_pkg rustc rust
        compile_and_run rustc "" "$FILE" "$NAME"
        ;;
    
    # === SHELL ===
    sh|bash)
        echo "> bash $FILENAME"
        bash "$FILE"
        ;;
    
    # === RUBY ===
    rb)
        check_pkg ruby ruby
        echo "> ruby $FILENAME"
        ruby "$FILE"
        ;;
    
    # === PHP ===
    php)
        check_pkg php php
        echo "> php $FILENAME"
        php "$FILE"
        ;;
    
    # === LUA ===
    lua)
        check_pkg lua lua5.4
        echo "> lua $FILENAME"
        lua "$FILE"
        ;;
    
    # === PERL ===
    pl)
        check_pkg perl perl
        echo "> perl $FILENAME"
        perl "$FILE"
        ;;
    
    # === KOTLIN SCRIPT ===
    kts)
        check_pkg kotlin kotlin
        echo "> kotlin $FILENAME"
        kotlin "$FILE"
        ;;
    
    # === SWIFT ===
    swift)
        check_pkg swift swift
        echo "> swift $FILENAME"
        swift "$FILE"
        ;;
    
    # === HTML - Check for React/Vite project first, then fall back to HTTP server ===
    html|htm)
        check_pkg python3 python3
        
        # Look for package.json in current or parent directories (React/Vite project detection)
        PKG_DIR="$DIR"
        FOUND_PKG=0
        for i in 1 2 3 4 5; do
            if [ -f "$PKG_DIR/package.json" ]; then
                FOUND_PKG=1
                break
            fi
            PKG_DIR=$(dirname "$PKG_DIR")
        done
        
        # If package.json found, check if it's a React/Vite/Next project
        if [ $FOUND_PKG -eq 1 ]; then
            PKG_FILE="$PKG_DIR/package.json"
            
            # Check for React/Vite/Next indicators using multiple greps (busybox compatible)
            IS_REACT_PROJECT=0
            if grep -q '"react"' "$PKG_FILE" 2>/dev/null; then
                IS_REACT_PROJECT=1
            elif grep -q '"vite"' "$PKG_FILE" 2>/dev/null; then
                IS_REACT_PROJECT=1
            elif grep -q '"next"' "$PKG_FILE" 2>/dev/null; then
                IS_REACT_PROJECT=1
            elif grep -q '"react-scripts"' "$PKG_FILE" 2>/dev/null; then
                IS_REACT_PROJECT=1
            fi
            
            if [ $IS_REACT_PROJECT -eq 1 ]; then
                echo "> Detected React/Vite project, running dev server..."
                
                check_pkg node nodejs
                cd "$PKG_DIR"
                
                # Check if node_modules exists
                if [ ! -d "node_modules" ]; then
                    echo "> Installing dependencies (npm install)..."
                    npm install
                fi
                
                # Detect project type and run appropriate command
                if grep -q '"vite"' package.json 2>/dev/null; then
                    echo "> Vite project detected"
                    echo "SERVER_URL:http://localhost:5173"
                    npm run dev
                elif grep -q '"react-scripts"' package.json 2>/dev/null; then
                    echo "> Create React App detected"
                    echo "SERVER_URL:http://localhost:3000"
                    npm start
                elif grep -q '"next"' package.json 2>/dev/null; then
                    echo "> Next.js project detected"
                    echo "SERVER_URL:http://localhost:3000"
                    npm run dev
                else
                    echo "> Running npm run dev"
                    echo "SERVER_URL:http://localhost:3000"
                    npm run dev 2>/dev/null || npm start
                fi
                exit $?
            fi
        fi
        
        # Fall back to simple HTTP server for vanilla HTML
        PORT=5500
        # Find available port
        while netstat -tuln 2>/dev/null | grep -q ":$PORT "; do
            PORT=$((PORT + 1))
            if [ $PORT -gt 5510 ]; then
                echo "! Could not find available port"
                exit 1
            fi
        done
        
        echo "> Starting HTTP server on port $PORT"
        echo "SERVER_URL:http://localhost:$PORT"
        echo "SERVER_FILE:$FILENAME"
        
        # Run server - will keep running until killed
        python3 -m http.server $PORT
        ;;
    
    # === REACT / JSX / TSX - Detect and run dev server ===
    jsx|tsx)
        check_pkg node nodejs
        
        # Look for package.json in current or parent directories
        PKG_DIR="$DIR"
        FOUND_PKG=0
        for i in 1 2 3 4 5; do
            if [ -f "$PKG_DIR/package.json" ]; then
                FOUND_PKG=1
                break
            fi
            PKG_DIR=$(dirname "$PKG_DIR")
        done
        
        if [ $FOUND_PKG -eq 0 ]; then
            echo "! No package.json found. Run 'npm init' first."
            exit 1
        fi
        
        cd "$PKG_DIR"
        
        # Check if node_modules exists
        if [ ! -d "node_modules" ]; then
            echo "> Installing dependencies (npm install)..."
            npm install
        fi
        
        # Detect project type and run appropriate command
        if grep -q '"vite"' package.json 2>/dev/null; then
            echo "> Vite project detected"
            echo "SERVER_URL:http://localhost:5173"
            npm run dev
        elif grep -q '"react-scripts"' package.json 2>/dev/null; then
            echo "> Create React App detected"
            echo "SERVER_URL:http://localhost:3000"
            npm start
        elif grep -q '"next"' package.json 2>/dev/null; then
            echo "> Next.js project detected"
            echo "SERVER_URL:http://localhost:3000"
            npm run dev
        else
            echo "> Running npm start"
            echo "SERVER_URL:http://localhost:3000"
            npm start
        fi
        ;;
    
    # === CSS/SCSS - Open in browser context ===
    css|scss|sass)
        echo "i CSS files cannot be run directly."
        echo "  Open an HTML file that includes this CSS."
        ;;
    
    # === JSON - Validate ===
    json)
        check_pkg python3 python3
        echo "> Validating JSON..."
        python3 -c "import json; json.load(open('$FILE')); print('OK Valid JSON')" 2>&1 || echo "X Invalid JSON"
        ;;
    
    # === MARKDOWN - No execution ===
    md)
        echo "i Markdown files cannot be executed."
        ;;
    
    # === UNKNOWN ===
    *)
        echo "! No runner for .$EXT files"
        exit 1
        ;;
esac
