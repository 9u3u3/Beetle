#!/bin/bash
# code-runner.sh - Unified code runner for BananaSpaghetti
# Handles both code execution AND web serving
# Supports 15+ languages with package dependency checking

# === LOGGING ===
LOG_TAG="[CodeRunner]"
log() { echo "$LOG_TAG $1"; }
log_debug() { echo "$LOG_TAG [DEBUG] $1"; }

# === ENVIRONMENT SETUP (silent) ===
export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin:$PREFIX/local/bin:$PATH
export HOME=/home
export TERM=xterm-256color

# === ARGUMENT PARSING ===
FILE="$1"

if [ -z "$FILE" ]; then
    echo "Usage: code-runner.sh <file_path>"
    echo "Supported: py, js, ts, c, cpp, cc, java, go, rs, sh, rb, php, lua, pl, html, htm, jsx, tsx"
    exit 1
fi

log_debug "Input file: $FILE"

# === EXTRACT FILE INFO ===
FILENAME=$(basename "$FILE")
EXT="${FILENAME##*.}"
NAME="${FILENAME%.*}"
DIR=$(dirname "$FILE")

log_debug "Extension: $EXT, Name: $NAME, Dir: $DIR"

# Change to file directory for relative imports
cd "$DIR" 2>/dev/null || {
    log "⚠ Could not cd to $DIR"
    cd /
}

# === DEPENDENCY CHECK FUNCTION ===
# Outputs MISSING_PKG:package_name if command not found
# Returns exit code 100 for missing package
check_pkg() {
    local cmd="$1"
    local pkg="$2"
    
    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "MISSING_PKG:$pkg"
        log "⚠ Missing: $pkg (command: $cmd)"
        exit 100
    fi
    log_debug "Found: $cmd"
}

# === COMPILE AND RUN HELPER ===
compile_and_run() {
    local compiler="$1"
    local flags="$2"
    local src="$3"
    local out="$4"
    
    log "▶ $compiler $src $flags"
    if $compiler "$src" $flags -o "$out" 2>&1; then
        log "▶ Running ./$out"
        "./$out"
        local exit_code=$?
        # Cleanup compiled binary
        rm -f "$out" 2>/dev/null
        return $exit_code
    else
        log "✗ Compilation failed"
        return 1
    fi
}

# === MAIN RUNNER ===
case "$EXT" in
    # === PYTHON ===
    py)
        check_pkg python3 python3
        log "▶ python3 $FILENAME"
        python3 "$FILE"
        ;;
    
    # === JAVASCRIPT (Node.js) ===
    js)
        check_pkg node nodejs
        log "▶ node $FILENAME"
        node "$FILE"
        ;;
    
    # === TYPESCRIPT ===
    ts)
        check_pkg npx nodejs
        log "▶ npx ts-node $FILENAME"
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
        log "▶ javac $FILENAME"
        if javac "$FILE" 2>&1; then
            log "▶ java $NAME"
            java "$NAME"
            # Cleanup class file
            rm -f "$NAME.class" 2>/dev/null
        else
            log "✗ Compilation failed"
        fi
        ;;
    
    # === GO ===
    go)
        check_pkg go go
        log "▶ go run $FILENAME"
        go run "$FILE"
        ;;
    
    # === RUST ===
    rs)
        check_pkg rustc rust
        compile_and_run rustc "" "$FILE" "$NAME"
        ;;
    
    # === SHELL ===
    sh|bash)
        log "▶ bash $FILENAME"
        bash "$FILE"
        ;;
    
    # === RUBY ===
    rb)
        check_pkg ruby ruby
        log "▶ ruby $FILENAME"
        ruby "$FILE"
        ;;
    
    # === PHP ===
    php)
        check_pkg php php
        log "▶ php $FILENAME"
        php "$FILE"
        ;;
    
    # === LUA ===
    lua)
        check_pkg lua lua5.4
        log "▶ lua $FILENAME"
        lua "$FILE"
        ;;
    
    # === PERL ===
    pl)
        check_pkg perl perl
        log "▶ perl $FILENAME"
        perl "$FILE"
        ;;
    
    # === KOTLIN SCRIPT ===
    kts)
        check_pkg kotlin kotlin
        log "▶ kotlin $FILENAME"
        kotlin "$FILE"
        ;;
    
    # === SWIFT ===
    swift)
        check_pkg swift swift
        log "▶ swift $FILENAME"
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
            log_debug "Found package.json at: $PKG_FILE"
            
            # Check for React/Vite/Next indicators using multiple greps (busybox compatible)
            IS_REACT_PROJECT=0
            if grep -q '"react"' "$PKG_FILE" 2>/dev/null; then
                log_debug "Found 'react' in package.json"
                IS_REACT_PROJECT=1
            elif grep -q '"vite"' "$PKG_FILE" 2>/dev/null; then
                log_debug "Found 'vite' in package.json"
                IS_REACT_PROJECT=1
            elif grep -q '"next"' "$PKG_FILE" 2>/dev/null; then
                log_debug "Found 'next' in package.json"
                IS_REACT_PROJECT=1
            elif grep -q '"react-scripts"' "$PKG_FILE" 2>/dev/null; then
                log_debug "Found 'react-scripts' in package.json"
                IS_REACT_PROJECT=1
            fi
            
            if [ $IS_REACT_PROJECT -eq 1 ]; then
                log "▶ Detected React/Vite project, running dev server..."
                
                check_pkg node nodejs
                cd "$PKG_DIR"
                log_debug "Changed to project dir: $PKG_DIR"
                
                # Check if node_modules exists
                if [ ! -d "node_modules" ]; then
                    log "▶ Installing dependencies (npm install)..."
                    npm install
                fi
                
                # Detect project type and run appropriate command
                if grep -q '"vite"' package.json 2>/dev/null; then
                    log "▶ Vite project detected"
                    echo "SERVER_URL:http://localhost:5173"
                    npm run dev
                elif grep -q '"react-scripts"' package.json 2>/dev/null; then
                    log "▶ Create React App detected"
                    echo "SERVER_URL:http://localhost:3000"
                    npm start
                elif grep -q '"next"' package.json 2>/dev/null; then
                    log "▶ Next.js project detected"
                    echo "SERVER_URL:http://localhost:3000"
                    npm run dev
                else
                    log "▶ Running npm run dev"
                    echo "SERVER_URL:http://localhost:3000"
                    npm run dev 2>/dev/null || npm start
                fi
                exit $?
            else
                log_debug "package.json found but no React/Vite/Next detected"
            fi
        else
            log_debug "No package.json found in parent directories"
        fi
        
        # Fall back to simple HTTP server for vanilla HTML
        PORT=5500
        # Find available port
        while netstat -tuln 2>/dev/null | grep -q ":$PORT "; do
            PORT=$((PORT + 1))
            if [ $PORT -gt 5510 ]; then
                log "⚠ Could not find available port"
                exit 1
            fi
        done
        
        log "▶ Starting HTTP server on port $PORT"
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
            log "⚠ No package.json found. Run 'npm init' first."
            exit 1
        fi
        
        cd "$PKG_DIR"
        log_debug "Found package.json in: $PKG_DIR"
        
        # Check if node_modules exists
        if [ ! -d "node_modules" ]; then
            log "▶ Installing dependencies (npm install)..."
            npm install
        fi
        
        # Detect project type and run appropriate command
        if grep -q '"vite"' package.json 2>/dev/null; then
            log "▶ Vite project detected"
            echo "SERVER_URL:http://localhost:5173"
            npm run dev
        elif grep -q '"react-scripts"' package.json 2>/dev/null; then
            log "▶ Create React App detected"
            echo "SERVER_URL:http://localhost:3000"
            npm start
        elif grep -q '"next"' package.json 2>/dev/null; then
            log "▶ Next.js project detected"
            echo "SERVER_URL:http://localhost:3000"
            npm run dev
        else
            log "▶ Running npm start"
            echo "SERVER_URL:http://localhost:3000"
            npm start
        fi
        ;;
    
    # === CSS/SCSS - Open in browser context ===
    css|scss|sass)
        log "ℹ CSS files cannot be run directly."
        log "  Open an HTML file that includes this CSS."
        ;;
    
    # === JSON - Validate ===
    json)
        check_pkg python3 python3
        log "▶ Validating JSON..."
        python3 -c "import json; json.load(open('$FILE')); print('✓ Valid JSON')" 2>&1 || log "✗ Invalid JSON"
        ;;
    
    # === MARKDOWN - No execution ===
    md)
        log "ℹ Markdown files cannot be executed."
        ;;
    
    # === UNKNOWN ===
    *)
        log "⚠ No runner configured for .$EXT files"
        log "Supported: py, js, ts, c, cpp, cc, java, go, rs, sh, rb, php, lua, pl, html, jsx, tsx"
        exit 1
        ;;
esac

exit_code=$?
log_debug "Exit code: $exit_code"
exit $exit_code
