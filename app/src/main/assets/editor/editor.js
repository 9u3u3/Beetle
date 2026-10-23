import { EditorView, basicSetup } from "codemirror"
import { EditorState, StateEffect, StateField, Compartment } from "@codemirror/state"
import { oneDark } from "@codemirror/theme-one-dark"
import { autocompletion, completionKeymap, closeCompletion, startCompletion, completionStatus, closeBrackets } from "@codemirror/autocomplete"
import { keymap, drawSelection, lineNumbers, highlightActiveLineGutter, highlightActiveLine } from "@codemirror/view"
import { linter, lintGutter, forceLinting, setDiagnostics } from "@codemirror/lint"
import { cpp } from "@codemirror/lang-cpp"
import { python } from "@codemirror/lang-python"
import { html } from "@codemirror/lang-html"
import { css } from "@codemirror/lang-css"
import { javascript } from "@codemirror/lang-javascript"
import { java } from "@codemirror/lang-java"
import { go } from "@codemirror/lang-go"
import { rust } from "@codemirror/lang-rust"
import { sql } from "@codemirror/lang-sql"
import { insertNewlineAndIndent, deleteCharBackward, undo, redo } from "@codemirror/commands"
import { openSearchPanel, closeSearchPanel } from "@codemirror/search"
import { bracketMatching } from "@codemirror/language"
import { indentUnit } from "@codemirror/language"
import { indentationMarkers } from "@replit/codemirror-indentation-markers"

// Compartments for dynamic settings
const languageCompartment = new Compartment()
const themeCompartment = new Compartment()
const lineWrapCompartment = new Compartment()
const lineNumbersCompartment = new Compartment()
const highlightActiveLineCompartment = new Compartment()
const bracketMatchingCompartment = new Compartment()
const closeBracketsCompartment = new Compartment()
const tabSizeCompartment = new Compartment()
const relativeLineNumbersCompartment = new Compartment()
const indentGuidesCompartment = new Compartment()

let currentLanguageId = 'cpp'; // Track current language for LSP
window.currentTheme = 'oneDark';

// Theme definitions for dynamic switching
const themes = {
  'oneDark': oneDark,
  'dark': EditorView.theme({
    "&": { backgroundColor: "#1e1e1e", color: "#d4d4d4" },
    ".cm-content": { caretColor: "#fff" },
    ".cm-cursor": { borderLeftColor: "#fff" },
    ".cm-gutters": { backgroundColor: "#252526", color: "#858585", border: "none" },
    ".cm-activeLineGutter": { backgroundColor: "#2a2a2a" },
    ".cm-activeLine": { backgroundColor: "#2a2a2a" },
    ".cm-selectionBackground": { backgroundColor: "#264f78" },
    "&.cm-focused .cm-selectionBackground": { backgroundColor: "#264f78" },
    ".cm-line": { padding: "0 4px" }
  }, { dark: true }),
  'monokai': EditorView.theme({
    "&": { backgroundColor: "#272822", color: "#f8f8f2" },
    ".cm-content": { caretColor: "#f8f8f0" },
    ".cm-cursor": { borderLeftColor: "#f8f8f0" },
    ".cm-gutters": { backgroundColor: "#272822", color: "#75715e", border: "none" },
    ".cm-activeLineGutter": { backgroundColor: "#3e3d32" },
    ".cm-activeLine": { backgroundColor: "#3e3d32" },
    ".cm-selectionBackground": { backgroundColor: "#49483e" },
    "&.cm-focused .cm-selectionBackground": { backgroundColor: "#49483e" }
  }, { dark: true }),
  'github': EditorView.theme({
    "&": { backgroundColor: "#0d1117", color: "#c9d1d9" },
    ".cm-content": { caretColor: "#58a6ff" },
    ".cm-cursor": { borderLeftColor: "#58a6ff" },
    ".cm-gutters": { backgroundColor: "#0d1117", color: "#8b949e", border: "none" },
    ".cm-activeLineGutter": { backgroundColor: "#161b22" },
    ".cm-activeLine": { backgroundColor: "#161b22" },
    ".cm-selectionBackground": { backgroundColor: "#264f78" },
    "&.cm-focused .cm-selectionBackground": { backgroundColor: "#264f78" }
  }, { dark: true }),
  'dracula': EditorView.theme({
    "&": { backgroundColor: "#282a36", color: "#f8f8f2" },
    ".cm-content": { caretColor: "#f8f8f2" },
    ".cm-cursor": { borderLeftColor: "#f8f8f2" },
    ".cm-gutters": { backgroundColor: "#282a36", color: "#6272a4", border: "none" },
    ".cm-activeLineGutter": { backgroundColor: "#44475a" },
    ".cm-activeLine": { backgroundColor: "#44475a" },
    ".cm-selectionBackground": { backgroundColor: "#44475a" },
    "&.cm-focused .cm-selectionBackground": { backgroundColor: "#44475a" }
  }, { dark: true }),
  'solarizedDark': EditorView.theme({
    "&": { backgroundColor: "#002b36", color: "#839496" },
    ".cm-content": { caretColor: "#839496" },
    ".cm-cursor": { borderLeftColor: "#839496" },
    ".cm-gutters": { backgroundColor: "#073642", color: "#586e75", border: "none" },
    ".cm-activeLineGutter": { backgroundColor: "#073642" },
    ".cm-activeLine": { backgroundColor: "#073642" },
    ".cm-selectionBackground": { backgroundColor: "#073642" },
    "&.cm-focused .cm-selectionBackground": { backgroundColor: "#073642" }
  }, { dark: true }),
  'nord': EditorView.theme({
    "&": { backgroundColor: "#2e3440", color: "#d8dee9" },
    ".cm-content": { caretColor: "#d8dee9" },
    ".cm-cursor": { borderLeftColor: "#d8dee9" },
    ".cm-gutters": { backgroundColor: "#2e3440", color: "#4c566a", border: "none" },
    ".cm-activeLineGutter": { backgroundColor: "#3b4252" },
    ".cm-activeLine": { backgroundColor: "#3b4252" },
    ".cm-selectionBackground": { backgroundColor: "#434c5e" },
    "&.cm-focused .cm-selectionBackground": { backgroundColor: "#434c5e" }
  }, { dark: true })
};

// Set editor theme function - exposed globally
window.setTheme = function (themeName) {
  const theme = themes[themeName];
  if (theme && window.view) {
    window.view.dispatch({
      effects: themeCompartment.reconfigure(theme)
    });
    window.currentTheme = themeName;

    // Extract and apply background color after theme change
    setTimeout(() => {
      const editorEl = document.querySelector('.cm-editor');
      if (editorEl) {
        const bg = window.getComputedStyle(editorEl).backgroundColor;
        document.body.style.backgroundColor = bg;
        // Also update scroll-spacer to match editor color
        const scrollSpacer = document.getElementById('scroll-spacer');
        if (scrollSpacer) {
          scrollSpacer.style.backgroundColor = bg;
        }
      }
    }, 50); // Small delay to ensure theme is fully applied

    return true;
  }
  console.warn('[Editor] Unknown theme or no view:', themeName);
  return false;
};

// Get available themes
window.getAvailableThemes = function () {
  return Object.keys(themes);
};

// Get current theme
window.getCurrentTheme = function () {
  return window.currentTheme;
};

// ========================================
// EDITOR SETTINGS APPLICATION
// ========================================

// Word wrap setting
window.setWordWrap = function (enabled) {
  if (!window.view) return false;

  window.view.dispatch({
    effects: lineWrapCompartment.reconfigure(enabled ? EditorView.lineWrapping : [])
  });
  return true;
};

// Line numbers setting
window.setLineNumbers = function (enabled) {
  if (!window.view) return false;

  window.view.dispatch({
    effects: lineNumbersCompartment.reconfigure(enabled ? [lineNumbers(), highlightActiveLineGutter()] : [])
  });
  return true;
};

// Tab size setting
window.setTabSize = function (size) {
  if (!window.view) return false;

  const spaces = ' '.repeat(size);
  window.view.dispatch({
    effects: tabSizeCompartment.reconfigure([
      EditorState.tabSize.of(size),
      indentUnit.of(spaces)
    ])
  });
  return true;
};

// Relative line numbers setting  
let relativeLineNumbersEnabled = false;
window.setRelativeLineNumbers = function (enabled) {
  if (!window.view) return false;

  relativeLineNumbersEnabled = enabled;

  if (enabled) {
    window.view.dispatch({
      effects: lineNumbersCompartment.reconfigure([
        lineNumbers({
          formatNumber: (lineNo, state) => {
            const cursorLine = state.doc.lineAt(state.selection.main.head).number;
            const distance = Math.abs(lineNo - cursorLine);
            return distance === 0 ? String(lineNo) : String(distance);
          }
        }),
        highlightActiveLineGutter()
      ])
    });
  } else {
    window.view.dispatch({
      effects: lineNumbersCompartment.reconfigure([lineNumbers(), highlightActiveLineGutter()])
    });
  }

  return true;
};

// Indent guides setting
window.setIndentGuides = function (enabled) {
  if (!window.view) return false;

  window.view.dispatch({
    effects: indentGuidesCompartment.reconfigure(enabled ? indentationMarkers() : [])
  });
  return true;
};

// Highlight active line setting
window.setHighlightActiveLine = function (enabled) {
  if (!window.view) return false;

  window.view.dispatch({
    effects: highlightActiveLineCompartment.reconfigure(enabled ? highlightActiveLine() : [])
  });
  return true;
};

// Bracket matching setting
window.setBracketMatching = function (enabled) {
  if (!window.view) return false;

  window.view.dispatch({
    effects: bracketMatchingCompartment.reconfigure(enabled ? bracketMatching() : [])
  });
  return true;
};

// Auto close brackets setting
window.setCloseBrackets = function (enabled) {
  if (!window.view) return false;

  window.view.dispatch({
    effects: closeBracketsCompartment.reconfigure(enabled ? closeBrackets() : [])
  });
  return true;
};

// Minimap (not natively supported in CM6, would need extension)
window.setMinimap = function (enabled) {
  // CM6 doesn't have built-in minimap - would need a third-party extension
  // For now, we just store the preference and log it
  return true;
};

window.setLanguage = async function (slug, port = 8081) {
  console.log(`[LSP-DEBUG] setLanguage called with slug=${slug}, port=${port}`);
  currentLanguageId = slug; // Update current language

  // Connect to LSP
  console.log(`[LSP-DEBUG] window.connectLsp exists: ${typeof window.connectLsp}`);
  if (window.connectLsp) {
    console.log(`[LSP-DEBUG] Calling connectLsp(${port})`);
    window.connectLsp(port);
  } else {
    console.error(`[LSP-DEBUG] ERROR: window.connectLsp is undefined!`);
  }

  let langModule;
  try {
    // The language packages are part of this bundle. A separately bundled module would carry its
    // own copy of @codemirror/state, and the editor cannot use it.
    const languages = {
      cpp: () => cpp(),
      python: () => python(),
      html: () => html(),
      css: () => css(),
      javascript: () => javascript({ typescript: true }),
      js: () => javascript({ typescript: true }),
      react: () => javascript({ jsx: true, typescript: true }),
      java: () => java(),
      go: () => go(),
      rust: () => rust(),
      sql: () => sql(),
    };
    const make = languages[slug];
    if (!make) throw new Error(`No language support for ${slug}`);
    langModule = make();

    if (window.view && langModule) {
      window.view.dispatch({
        effects: languageCompartment.reconfigure(langModule)
      })
    }
  } catch (e) {
    console.error(`[LSP-JS] Failed to load language ${slug}`, e);
  }
}

// Clear language support (for unsupported files)
window.clearLanguage = function () {
  currentLanguageId = 'plaintext';

  // Close any existing LSP connection
  if (window.ws) {
    try { window.ws.close(); } catch (e) { }
    window.ws = null;
  }

  // Reconfigure to empty (no syntax highlighting)
  if (window.view) {
    window.view.dispatch({
      effects: languageCompartment.reconfigure([])
    });
  }
}

// --- Global Error Handling for WEBVERR ---
window.onerror = function (message, source, lineno, colno, error) {
  console.error(`[WEBVERR] JS Error: ${message} at ${source}:${lineno}:${colno} `);
  if (error && error.stack) {
    console.error(`[WEBVERR] Stack: ${error.stack} `);
  }
};

window.onunhandledrejection = function (event) {
  console.error(`[WEBVERR] Unhandled Promise Rejection: ${event.reason} `);
};
// -----------------------------------------

window.code_ = ""
window.completionActive = false;
window.completionIndex = 0;
window.completionItems = [];

// Inject custom styles for completion list
const style = document.createElement('style');
style.textContent = `
  .lsp-completion-list {
  position: fixed;
  background: #1e1e1e;
  border: 1px solid #333;
  border-radius: 4px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
  z-index: 10000; /* High z-index */
  max-height: 200px;
  overflow-y: auto;
  min-width: 180px;
  max-width: calc(100vw - 20px); /* Don't overflow viewport */
  font-family: monospace;
  display: none;
}
  .lsp-completion-item {
  padding: 6px 10px; /* Better padding */
  cursor: pointer;
  color: #ccc;
  display: flex;
  align-items: center;
  font-size: 13px; /* Larger font */
  border-bottom: 1px solid #2a2a2a;
}
  .lsp-completion-item:last-child {
  border-bottom: none;
}
  .lsp-completion-item.selected {
  background: #005fcc; /* Blue selection */
  color: #fff;
}
  .lsp-completion-icon {
  margin-right: 10px;
  width: 16px;
  text-align: center;
  opacity: 0.8;
  font-weight: bold;
}
  .lsp-completion-detail {
  margin-left: auto;
  opacity: 0.6;
  font-size: 11px;
}
  /* Force diagnostic styles */
  .cm-lint-range-error {
  background-image: none!important;
  text-decoration: underline wavy #ff5555!important;
  text-decoration-skip-ink: none;
}
  .cm-lint-range-warning {
  background-image: none!important;
  text-decoration: underline wavy #ffb86c!important;
  text-decoration-skip-ink: none;
}
  .cm-lint-range-info {
  background-image: none!important;
  text-decoration: underline wavy #8be9fd!important;
  text-decoration-skip-ink: none;
}

/* CURSOR FIX - Single visible cursor only */
.cm-editor .cm-cursor {
    border-left: 2px solid #528bff !important;
    visibility: visible !important;
    display: block !important;
}
.cm-editor .cm-cursor-primary {
    border-left: 2px solid #528bff !important;
    visibility: visible !important;
}
/* Hide secondary/native cursors */
.cm-editor .cm-cursor-secondary,
.cm-editor .cm-secondaryCursor {
    display: none !important;
}
/* Hide native caret completely */
.cm-editor .cm-line {
    caret-color: transparent !important;
}
.cm-content {
    caret-color: transparent !important;
    -webkit-user-select: none !important;
    user-select: none !important;
}
/* Hide native selection handles (Android specific) */
* {
    -webkit-tap-highlight-color: transparent;
}
/* JS Status Bar CSS REMOVED - Using Kotlin BottomBar instead */

/* Documentation popup for completion item long-press */
.lsp-doc-popup {
  position: absolute;
  background: #252526;
  border: 1px solid #454545;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
  z-index: 10001; /* Above completion list */
  max-height: 200px;
  max-width: 280px;
  min-width: 150px;
  overflow-y: auto;
  padding: 8px 12px 10px 12px;
  font-family: monospace;
  font-size: 12px;
  color: #d4d4d4;
  line-height: 1.5;
  display: none;
  white-space: pre-wrap;
  word-wrap: break-word;
}
.lsp-doc-popup-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 6px;
}
.lsp-doc-popup-close {
  background: transparent;
  border: none;
  color: #808080;
  font-size: 16px;
  cursor: pointer;
  padding: 0 4px;
  margin: -4px -4px 0 8px;
  line-height: 1;
  flex-shrink: 0;
}
.lsp-doc-popup-close:hover {
  color: #fff;
}
.lsp-doc-popup-close:active {
  color: #ff5555;
}
.lsp-doc-popup code {
  background: #1e1e1e;
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 11px;
}
.lsp-doc-popup-title {
  color: #569cd6;
  font-weight: bold;
  font-size: 13px;
  flex: 1;
}
.lsp-doc-popup-empty {
  color: #808080;
  font-style: italic;
}
.lsp-doc-popup-content {
  margin-top: 6px;
}
`;
document.head.appendChild(style);

const completionListEl = document.createElement('div');
completionListEl.className = 'lsp-completion-list';
document.body.appendChild(completionListEl);

// Documentation popup element
const docPopupEl = document.createElement('div');
docPopupEl.className = 'lsp-doc-popup';
document.body.appendChild(docPopupEl);

// Show documentation popup for a completion item
function showDocPopup(item, targetElement) {
  if (!item) return;

  const doc = item.info || '';
  const detail = item.detail || '';

  // Build HTML with header containing title and close button
  let html = '<div class="lsp-doc-popup-header">';

  if (item.label) {
    html += `<span class="lsp-doc-popup-title">${escapeHtml(item.label)}</span>`;
  } else {
    html += '<span class="lsp-doc-popup-title">Documentation</span>';
  }

  // Close button (X)
  html += '<button class="lsp-doc-popup-close" id="doc-popup-close">✕</button>';
  html += '</div>';

  // Content area
  if (!doc && !detail) {
    html += '<span class="lsp-doc-popup-empty">No documentation available</span>';
  } else {
    html += '<div class="lsp-doc-popup-content">';
    if (detail) {
      html += `<code>${escapeHtml(detail)}</code><br><br>`;
    }
    if (doc) {
      // Basic markdown rendering for documentation
      html += escapeHtml(doc).replace(/`([^`]+)`/g, '<code>$1</code>');
    }
    html += '</div>';
  }

  docPopupEl.innerHTML = html;

  // Attach close button event listener
  const closeBtn = document.getElementById('doc-popup-close');
  if (closeBtn) {
    closeBtn.ontouchend = (e) => {
      e.preventDefault();
      e.stopPropagation();
      hideDocPopup();
    };
    closeBtn.onclick = (e) => {
      e.preventDefault();
      e.stopPropagation();
      hideDocPopup();
    };
  }

  // Position popup to the right of the completion list
  const listRect = completionListEl.getBoundingClientRect();
  const targetRect = targetElement.getBoundingClientRect();

  // Default: position to the right of the completion list
  let leftPos = listRect.right + 5;
  let topPos = targetRect.top;

  // If not enough space on the right, position to the left
  if (leftPos + 280 > window.innerWidth) {
    leftPos = listRect.left - 285;
    if (leftPos < 5) leftPos = 5; // Fallback if no space on left either
  }

  // Ensure popup doesn't go below viewport
  if (topPos + 200 > window.innerHeight) {
    topPos = window.innerHeight - 205;
  }

  docPopupEl.style.left = `${leftPos}px`;
  docPopupEl.style.top = `${topPos}px`;
  docPopupEl.style.display = 'block';

}

// Hide documentation popup
function hideDocPopup() {
  docPopupEl.style.display = 'none';
}

// Helper to escape HTML
function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// --- Debug HUD (DISABLED) ---
// (commented out for performance)
// const debugHudEl = ...

// --- JS Status Bar REMOVED - Using Kotlin BottomBar instead ---
// statusBarEl, updateStatusBar, updateLspStatus all removed.
// Kotlin BottomBar.kt is used for UI, EditorSession parses console.log([LSP-STATUS])


// Debug stats function (DISABLED)
window.updateDebugStats = function (stats) {
  // No-op: debugHudEl is disabled
};
// -----------------


function setupEditor() {
  let ws = null;
  let view = null

  let lastCompletionTime = 0
  let URI = "file:///main.cpp"
  let rootURI = "file:///"
  let pendingRequests = new Map()
  let messageId = 0

  let messageBuffer = '';
  let isInitialized = false;
  let initializationPromise = null;
  let pendingCompletionId = null; // Track in-flight completion request
  let openedDocumentUri = null; // Track which URI the server thinks is open

  // Function to send cancel request
  function sendCancelRequest(id) {
    if (ws.readyState === WebSocket.OPEN) {
      const cancel = {
        jsonrpc: "2.0",
        method: "$/cancelRequest",
        params: { id: id }
      };
      ws.send(JSON.stringify(cancel));
    }
  }

  // Note: Diagnostics are handled via setDiagnostics() from @codemirror/lint
  // No custom StateEffect/StateField needed

  window.setFilePath = (path) => {
    const oldURI = URI;
    URI = `file://${path}`;

    // If we haven't initialized yet, just updating URI is enough.
    // The onopen handler will use the new URI.
    if (isInitialized && oldURI !== URI && ws && ws.readyState === WebSocket.OPEN) {
      // Close old file if it was opened
      if (openedDocumentUri && openedDocumentUri === oldURI) {
        sendNotification(ws, 'textDocument/didClose', {
          textDocument: { uri: oldURI }
        });
        openedDocumentUri = null;
      }

      // Reset version counter when opening a new document
      window.lspVersion = 1;

      // Open new file
      sendNotification(ws, 'textDocument/didOpen', {
        textDocument: {
          uri: URI,
          languageId: currentLanguageId || 'cpp',
          version: window.lspVersion,
          text: view?.state.doc.toString() || ''
        }
      });
      openedDocumentUri = URI;
    }
  };

  window.setRootUri = (uri) => {
    rootURI = uri;
  }

  // Debounced diagnostics handling
  let pendingDiagnostics = null;
  let diagnosticsTimeout = null;

  function handleDiagnostics(params) {
    // Store the latest diagnostics and debounce processing
    pendingDiagnostics = params;

    if (diagnosticsTimeout) clearTimeout(diagnosticsTimeout);

    diagnosticsTimeout = setTimeout(() => {
      if (!view || !pendingDiagnostics) return;

      try {
        // Debug: Log raw diagnostics from LSP
        console.log('[LSP-RAW-DIAG] Received:', JSON.stringify(pendingDiagnostics.diagnostics?.slice(0, 3)));

        const diagnostics = []
        for (const d of pendingDiagnostics.diagnostics) {
          try {
            if (d.range.start.line < 0 || d.range.end.line < 0) continue
            if (d.range.start.line + 1 > view.state.doc.lines || d.range.end.line + 1 > view.state.doc.lines) continue;

            const startLine = view.state.doc.line(d.range.start.line + 1)
            const endLine = view.state.doc.line(d.range.end.line + 1)
            const startChar = Math.min(d.range.start.character, startLine.length)
            const endChar = Math.min(d.range.end.character, endLine.length)
            const from = startLine.from + startChar
            const to = endLine.from + endChar

            if (to < from) continue

            diagnostics.push({
              from: from,
              to: Math.max(from + 1, to),
              severity: d.severity === 1 ? 'error' :
                d.severity === 2 ? 'warning' : 'info',
              message: d.message || 'Unknown error',
              source: d.source || 'clangd'
            })
          } catch (e) {
            console.error('Error processing diagnostic:', e)
          }
        }

        // Use requestAnimationFrame to avoid blocking the main thread
        // This ensures diagnostics update doesn't interfere with user input
        requestAnimationFrame(() => {
          if (!view) return;

          // CRITICAL: Clamp diagnostics to valid document bounds
          // Document may have changed since diagnostics were calculated
          const docLen = view.state.doc.length;
          const validDiagnostics = diagnostics.map(d => ({
            ...d,
            from: Math.max(0, Math.min(d.from, docLen)),
            to: Math.max(0, Math.min(d.to, docLen))
          })).filter(d => d.from < docLen); // Only filter if completely out of bounds

          // Count errors and warnings for status bar
          let errorCount = 0;
          let warningCount = 0;
          validDiagnostics.forEach(d => {
            if (d.severity === 'error') errorCount++;
            else if (d.severity === 'warning') warningCount++;
          });
          window.currentErrorCount = errorCount;
          window.currentWarningCount = warningCount;
          window.cachedErrors = errorCount;
          window.cachedWarnings = warningCount;

          // Use CodeMirror's built-in setDiagnostics for proper linting display
          view.dispatch(setDiagnostics(view.state, validDiagnostics));

          // IMMEDIATELY send status update when diagnostics change (for bottom bar)
          const pos = view.state.selection.main.head;
          const line = view.state.doc.lineAt(pos);
          const col = pos - line.from;
          console.log(`[LSP-STATUS] line:${line.number} col:${col + 1} err:${errorCount} warn:${warningCount}`);

          // Send diagnostics to Kotlin bridge (throttled - only if 2+ seconds since last send)
          const now = Date.now();
          if (!window.lastDiagsSentTime || now - window.lastDiagsSentTime > 2000) {
            window.lastDiagsSentTime = now;
            const diagList = validDiagnostics.slice(0, 10).map(d => ({
              msg: d.message.substring(0, 100),
              line: view.state.doc.lineAt(d.from).number,
              col: d.from - view.state.doc.lineAt(d.from).from + 1,
              sev: d.severity
            }));
            console.log(`[LSP-DIAGS] ${JSON.stringify(diagList)}`);
          }
        });

        pendingDiagnostics = null;
      } catch (e) {
        console.error('Error in handleDiagnostics:', e)
      }
    }, 100); // 100ms debounce - faster diagnostics updates
  }

  function updateCompletionListUI() {
    if (window.completionItems.length === 0) {
      completionListEl.style.display = 'none';
      window.completionActive = false;
      return;
    }


    completionListEl.innerHTML = '';
    window.completionItems.forEach((item, index) => {
      const el = document.createElement('div');
      el.className = `lsp-completion-item ${index === window.completionIndex ? 'selected' : ''}`;

      const icon = item.type.charAt(0).toUpperCase();
      el.innerHTML = `
            <span class="lsp-completion-icon">${icon}</span>
            <span class="lsp-completion-label">${item.label}</span>
            <span class="lsp-completion-detail">${item.detail}</span>
        `;

      // Long-press detection for documentation popup
      let longPressTimer = null;
      let isLongPress = false;
      const LONG_PRESS_DURATION = 500; // 500ms for long press

      el.ontouchstart = (e) => {
        isLongPress = false;
        longPressTimer = setTimeout(() => {
          isLongPress = true;
          window.completionIndex = index;
          showDocPopup(item, el);
        }, LONG_PRESS_DURATION);
      };

      el.ontouchend = (e) => {
        e.preventDefault();
        e.stopPropagation();

        // Clear the long-press timer
        if (longPressTimer) {
          clearTimeout(longPressTimer);
          longPressTimer = null;
        }

        if (isLongPress) {
          // Long-press release: keep doc popup visible, don't apply completion
          // User will close it via X button
          isLongPress = false;
        } else {
          // Quick tap: apply completion
          hideDocPopup(); // Hide any open doc popup first
          window.completionIndex = index;
          window.applyCompletion();
          // Refocus editor to keep keyboard open
          view.focus();
        }
      };

      el.ontouchcancel = (e) => {
        // Touch was cancelled (e.g., scrolled away)
        if (longPressTimer) {
          clearTimeout(longPressTimer);
          longPressTimer = null;
        }
        isLongPress = false;
        // Don't hide doc popup on cancel - let user close via X
      };

      // Desktop click support (simple click = apply completion)
      el.onclick = (e) => {
        e.preventDefault();
        e.stopPropagation();
        window.completionIndex = index;
        window.applyCompletion();
        // Refocus editor to keep keyboard open
        view.focus();
      };
      completionListEl.appendChild(el);
    });

    // Position list at cursor with boundary checks
    const coords = view.coordsAtPos(view.state.selection.main.head);
    if (coords) {
      const viewportWidth = window.innerWidth;
      const listWidth = 200; // Approximate width

      // Calculate left position, ensuring it doesn't overflow right side
      let leftPos = coords.left;
      if (leftPos + listWidth > viewportWidth - 10) {
        leftPos = Math.max(10, viewportWidth - listWidth - 10);
      }

      completionListEl.style.left = `${leftPos}px`;
      completionListEl.style.top = `${coords.bottom + 5}px`;
      completionListEl.style.display = 'block';
      window.completionActive = true;

      // Scroll selected into view
      const selectedEl = completionListEl.children[window.completionIndex];
      if (selectedEl) {
        selectedEl.scrollIntoView({ block: 'nearest' });
      }
    }
  }

  window.navigateCompletion = function (direction) { // 'up' or 'down'
    if (!window.completionActive) return false;

    if (direction === 'up') {
      window.completionIndex = Math.max(0, window.completionIndex - 1);
    } else if (direction === 'down') {
      window.completionIndex = Math.min(window.completionItems.length - 1, window.completionIndex + 1);
    }
    updateCompletionListUI();
    return true;
  };

  // Function to hide completion list
  window.hideCompletion = function () {
    completionListEl.style.display = 'none';
    hideDocPopup(); // Also hide documentation popup
    window.completionActive = false;
    window.completionItems = [];
    window.completionIndex = 0;
  };

  window.applyCompletion = function () {
    if (!window.completionActive) return false;

    const item = window.completionItems[window.completionIndex];
    if (item) {
      const text = item.apply;
      const from = item.from; // We need to store 'from' in the item
      const to = view.state.selection.main.head; // Or use stored 'to' if available

      view.dispatch({
        changes: { from: from, to: to, insert: text },
        selection: { anchor: from + text.length }
      });

      // Hide and clear completion state
      window.hideCompletion();
      return true;
    }
    return false;
  };
  // ...
  window.insertText = function (text) {
    view.dispatch(view.state.replaceSelection(text));
    view.focus(); // Re-enabled focus

    // Manually trigger completion
    if (window.completionTimeout) clearTimeout(window.completionTimeout);
    window.completionTimeout = setTimeout(() => {
      startCompletion(view);
    }, 600);
  };

  async function lspCompletionSource(context) {
    // Wait for initialization
    if (!isInitialized) {
      return null;
    }


    const now = Date.now()

    // Always allow explicit completions
    if (!context.explicit) {
      // For automatic completions, check timing (debounce 300ms - faster)
      if (now - lastCompletionTime < 300) {
        return null
      }
    }

    lastCompletionTime = now;

    const line = context.state.doc.lineAt(context.pos)
    const lineNumber = line.number - 1
    const character = context.pos - line.from

    // CANCEL any pending completion request before starting a new one
    if (pendingCompletionId !== null) {
      sendCancelRequest(pendingCompletionId);
      pendingRequests.delete(pendingCompletionId); // Remove from pending
      pendingCompletionId = null;
    }

    // Determine trigger kind and character
    let triggerKind = 1; // Invoked
    let triggerCharacter = undefined;

    // Check if triggered by a character (basic check)
    const charBefore = context.state.sliceDoc(context.pos - 1, context.pos);
    if (['.', ':', '<', '"', '/', '@'].includes(charBefore)) {
      triggerKind = 2; // TriggerCharacter
      triggerCharacter = charBefore;
    }

    // Force flush pending didChange before requesting completion
    // This ensures the server has the latest text (e.g. "std::")
    if (window.didChangeTimeout) {
      clearTimeout(window.didChangeTimeout);
      window.didChangeTimeout = null;
      window.lspVersion++;
      sendNotification(ws, 'textDocument/didChange', {
        textDocument: {
          uri: URI,
          version: window.lspVersion
        },
        contentChanges: [{ text: context.state.doc.toString() }]
      });
    }

    try {
      // Store the request ID before sending
      const currentRequestId = messageId + 1; // sendRequest will use this ID
      pendingCompletionId = currentRequestId;

      // Log the completion request details

      const result = await sendRequest(ws, 'textDocument/completion', {
        textDocument: { uri: URI },
        position: { line: lineNumber, character },
        context: {
          triggerKind: triggerKind,
          triggerCharacter: triggerCharacter
        }
      }, 30000) // 30 second timeout for slow LSPs like typescript-language-server

      pendingCompletionId = null; // Clear on success

      if (!result) return null

      const items = result.items || result
      if (!Array.isArray(items) || items.length === 0) {
        return null
      }

      // console.log(`✨ Got ${items.length} completion items`);
      if (items.length > 0) {
        const firstFew = items.slice(0, 5).map(i => `${i.label}(${i.kind || '?'})`).join(', ');
      }

      const wordBefore = context.matchBefore(/[\w\-]*/) // Match words and hyphens (for CSS/HTML)
      const from = wordBefore ? wordBefore.from : context.pos;
      const prefix = wordBefore ? wordBefore.text.toLowerCase() : "";

      // Filter and sort items
      let filteredItems = items;

      if (prefix) {
        filteredItems = items.filter(item => {
          const label = item.label.toLowerCase();
          // Also check filterText if available
          const filterText = (item.filterText || item.label).toLowerCase();
          return label.includes(prefix) || filterText.includes(prefix);
        });

        // Sort by relevance: exact match > starts with > contains
        filteredItems.sort((a, b) => {
          const aLabel = (a.filterText || a.label).toLowerCase();
          const bLabel = (b.filterText || b.label).toLowerCase();

          const aStarts = aLabel.startsWith(prefix);
          const bStarts = bLabel.startsWith(prefix);

          if (aStarts && !bStarts) return -1;
          if (!aStarts && bStarts) return 1;

          return aLabel.localeCompare(bLabel);
        });
      }

      // Limit to 20 items to save memory
      window.completionItems = filteredItems.slice(0, 20).map(item => {
        const cleanLabel = item.label.replace(/^[•\s]+/, '').trim()
        let textToInsert = cleanLabel

        if (item.insertText) textToInsert = item.insertText
        else if (item.textEdit && item.textEdit.newText) textToInsert = item.textEdit.newText

        return {
          label: cleanLabel,
          type: item.kind === 3 ? 'function' :
            item.kind === 6 ? 'variable' :
              item.kind === 14 ? 'keyword' :
                item.kind === 5 ? 'class' :
                  item.kind === 7 ? 'class' :
                    item.kind === 8 ? 'interface' :
                      item.kind === 9 ? 'namespace' :
                        item.kind === 2 ? 'method' :
                          item.kind === 13 ? 'enum' : 'text',
          detail: item.detail || '',
          info: typeof item.documentation === 'string' ? item.documentation : item.documentation?.value || '',
          apply: textToInsert,
          from: from // Store start position
        }
      })

      window.completionIndex = 0;
      updateCompletionListUI();

      // Return null to prevent CodeMirror's default autocomplete
      return null;

    } catch (e) {
      console.error('Completion error:', e)
      return null
    }
  }

  // Note: No custom linter needed - we use setDiagnostics() directly from handleDiagnostics

  const startState = EditorState.create({
    doc: "",
    extensions: [
      basicSetup,
      themeCompartment.of(oneDark),
      // Dynamic settings compartments
      lineWrapCompartment.of(EditorView.lineWrapping), // Default: word wrap ON
      lineNumbersCompartment.of([lineNumbers(), highlightActiveLineGutter()]), // Default: ON
      highlightActiveLineCompartment.of(highlightActiveLine()), // Default: ON
      bracketMatchingCompartment.of(bracketMatching()), // Default: ON
      closeBracketsCompartment.of(closeBrackets()), // Default: ON
      tabSizeCompartment.of([EditorState.tabSize.of(4), indentUnit.of('    ')]), // Default: 4
      relativeLineNumbersCompartment.of([]), // Relative line numbers - off by default
      indentGuidesCompartment.of([]), // Indent guides - off by default
      drawSelection(), // Ensure custom cursor drawing
      languageCompartment.of(cpp()), // Default
      lintGutter(), // Gutter for diagnostic markers
      // EditorState.readOnly.of(true), // Makes the editor readOnly for the user (to block the default from popping up)
      EditorView.contentAttributes.of({
        inputmode: "none"
      }),
      // Autocompletion re-enabled after freeze fix
      autocompletion({
        override: [lspCompletionSource],
        activateOnTyping: true,
        defaultKeymap: true,
        maxRenderedOptions: 10,
        closeOnBlur: false,
        interactionToken: "lsp"
      }),
      keymap.of([
        ...completionKeymap,
        {
          key: `Escape`,
          run: closeCompletion
        }
      ]),
      EditorView.updateListener.of((update) => {
        if (update.docChanged) {
          window.code_ = update.state.doc.toString()

          // Notify Android about content change
          if (window.AndroidEditor && !window.isProgrammaticChange) {
            window.AndroidEditor.onContentChanged();
          }
          // Trigger auto-save timer
          triggerAutoSave();

          // Auto-hide completion if text no longer matches any suggestion
          if (window.completionActive && window.completionItems.length > 0) {
            // Get current word being typed
            const pos = update.state.selection.main.head;
            const firstItem = window.completionItems[0];
            if (firstItem && firstItem.from !== undefined) {
              const typedText = update.state.doc.sliceString(firstItem.from, pos);

              // Check if typed text matches any completion
              const hasMatch = window.completionItems.some(item =>
                item.label.toLowerCase().startsWith(typedText.toLowerCase())
              );

              // Check if typed text exactly equals a completion (word complete)
              const exactMatch = window.completionItems.some(item =>
                item.label.toLowerCase() === typedText.toLowerCase()
              );

              if (!hasMatch || (exactMatch && typedText.length > 0)) {
                // Hide if no prefix match OR if exact match (word is complete)
                window.hideCompletion();
              }
            }
          }
        }

        // Handle Completion UI on selection change
        if (update.selectionSet) {
          if (window.completionActive) {
            const coords = view.coordsAtPos(view.state.selection.main.head);
            if (coords) {
              const viewportWidth = window.innerWidth;
              const listWidth = 200;
              let leftPos = coords.left;
              if (leftPos + listWidth > viewportWidth - 10) {
                leftPos = Math.max(10, viewportWidth - listWidth - 10);
              }
              completionListEl.style.left = `${leftPos}px`;
              completionListEl.style.top = `${coords.bottom + 5}px`;
            } else {
              // Cursor off screen or invalid, hide
              window.hideCompletion();
            }
          }
        }

        // Update position immediately on every change
        const state = view.state;
        const pos = state.selection.main.head;
        const line = state.doc.lineAt(pos);
        const col = pos - line.from;

        // Count diagnostics (throttled to avoid performance hit)
        // Use forEachDiagnostic from @codemirror/lint to access diagnostics
        const now = Date.now();
        if (!window.lastDiagTime || now - window.lastDiagTime > 500) {
          let errors = 0;
          let warnings = 0;
          // Get diagnostics from the lint state - they're stored internally by setDiagnostics
          // We track our own counts from handleDiagnostics instead
          errors = window.currentErrorCount || 0;
          warnings = window.currentWarningCount || 0;
          window.cachedErrors = errors;
          window.cachedWarnings = warnings;
          window.lastDiagTime = now;
        }

        // Output for Kotlin bridge - THROTTLED to prevent flooding on rapid typing/backspace
        // Only send status if position changed AND at least 100ms since last update
        const statusKey = `${line.number}-${col}`;
        if (statusKey !== window.lastStatusKey && (!window.lastStatusTime || now - window.lastStatusTime > 100)) {
          window.lastStatusKey = statusKey;
          window.lastStatusTime = now;
          console.log(`[LSP-STATUS] line:${line.number} col:${col + 1} err:${window.cachedErrors || 0} warn:${window.cachedWarnings || 0}`);

          // --- BREADCRUMBS / SYMBOLS ---
          // Fetch document symbols if we have a valid connection
          if (ws && ws.readyState === WebSocket.OPEN && isInitialized && openedDocumentUri === URI) {
            // Debounce symbol request
            if (window.symbolTimeout) clearTimeout(window.symbolTimeout);
            window.symbolTimeout = setTimeout(() => {
              sendRequest(ws, 'textDocument/documentSymbol', {
                textDocument: { uri: URI }
              }).then(symbols => {
                if (!symbols || !Array.isArray(symbols)) {
                  return;
                }

                // Helper to normalize range
                function getRange(sym) {
                  if (sym.range) return sym.range;
                  if (sym.location && sym.location.range) return sym.location.range;
                  return null;
                }

                // Helper to find symbol at position
                function findSymbolAtPos(symbols, line, col) {
                  let found = [];

                  // Check if we have a flat list (SymbolInformation) or hierarchy (DocumentSymbol)
                  const isFlat = symbols.length > 0 && !symbols[0].children && symbols[0].location;

                  if (isFlat) {
                    // Handle flat list (SymbolInformation)
                    // Find all symbols containing the cursor
                    const matches = [];
                    for (const sym of symbols) {
                      const range = getRange(sym);
                      if (!range) continue;

                      const startLine = range.start.line;
                      const endLine = range.end.line;

                      if (line > startLine || (line === startLine && col >= range.start.character)) {
                        if (line < endLine || (line === endLine && col <= range.end.character)) {
                          matches.push(sym);
                        }
                      }
                    }

                    // Sort by range length (descending) to get hierarchy order? 
                    // Actually, we want Outer -> Inner.
                    // Outer starts earlier, or ends later.
                    // Let's sort by start line/char.
                    matches.sort((a, b) => {
                      const ra = getRange(a);
                      const rb = getRange(b);
                      if (ra.start.line !== rb.start.line) return ra.start.line - rb.start.line;
                      return ra.start.character - rb.start.character;
                    });

                    return matches.map(s => ({ name: s.name, kind: s.kind, detail: s.detail || s.containerName }));

                  } else {
                    // Handle hierarchy (DocumentSymbol)
                    for (const sym of symbols) {
                      const range = getRange(sym);
                      if (!range) continue;

                      const startLine = range.start.line;
                      const endLine = range.end.line;

                      if (line > startLine || (line === startLine && col >= range.start.character)) {
                        if (line < endLine || (line === endLine && col <= range.end.character)) {
                          found.push({ name: sym.name, kind: sym.kind, detail: sym.detail });
                          if (sym.children) {
                            found = found.concat(findSymbolAtPos(sym.children, line, col));
                          }
                          break; // Assuming non-overlapping siblings in hierarchy
                        }
                      }
                    }
                    return found;
                  }
                }             // Current cursor position (0-indexed)
                const cursorLine = line.number - 1;
                const cursorCol = col;

                const breadcrumbs = findSymbolAtPos(symbols, cursorLine, cursorCol);

                // Send to Android
                // Only send if changed
                const breadcrumbStr = JSON.stringify(breadcrumbs);
                if (window.lastBreadcrumbs !== breadcrumbStr) {
                  window.lastBreadcrumbs = breadcrumbStr;
                  console.log(`[LSP-SYMBOLS] ${breadcrumbStr}`);
                }

              }).catch(e => {
                console.error("Symbol fetch failed:", e);
              });
            }, 500); // 500ms debounce
          }
        }

        // Full Text Sync - Re-enabled with longer debounce
        // IMPORTANT: Only send didChange if document is properly opened with server
        if (update.docChanged && ws && ws.readyState === WebSocket.OPEN && view && isInitialized && openedDocumentUri === URI) {
          if (window.didChangeTimeout) clearTimeout(window.didChangeTimeout);

          window.didChangeTimeout = setTimeout(() => {
            // Double-check document is still open (might have changed during debounce)
            if (openedDocumentUri !== URI) {
              return;
            }

            window.lspVersion++;
            const payload = {
              textDocument: {
                uri: URI,
                version: window.lspVersion
              },
              contentChanges: [{ text: view.state.doc.toString() }]
            };

            sendNotification(ws, 'textDocument/didChange', payload);
          }, 100); // 100ms debounce - fastest LSP updates
        }
      })
    ]
  })

  view = new EditorView({
    state: startState,
    parent: document.getElementById("editor")
  })
  window.view = view; // Expose globally
  window.editor = view; // Alias for compatibility

  // The editor is only as tall as its text, so a tap on the empty area below a short (or empty)
  // file hit nothing: no focus, no keyboard, and typed spaces scrolled the page instead.
  // Treat a tap outside the editor as a tap below the last line.
  const scrollWrapper = document.getElementById('scroll-wrapper');
  if (scrollWrapper && !window.tapToFocusInstalled) {
    window.tapToFocusInstalled = true;
    scrollWrapper.addEventListener('click', (e) => {
      const v = window.view;
      if (!v || e.target.closest('.cm-editor')) return;
      v.focus();
      v.dispatch({ selection: { anchor: v.state.doc.length }, scrollIntoView: true });
    });
  }

  window.isEditorReady = true

  // Notify Android that CM6 is ready so loading screen can be hidden
  if (window.AndroidEditorReady && window.AndroidEditorReady.onReady) {
    try {
      window.AndroidEditorReady.onReady();
    } catch (e) {
      console.error('[Editor] Error calling onReady:', e);
    }
  } else {
  }

  const editorEl = document.querySelector('.cm-editor')

  // Dismiss completion dropdown when clicking on the editor (not on completion list)
  editorEl.addEventListener('mousedown', (e) => {
    // Check if click is on completion list or doc popup
    if (!completionListEl.contains(e.target) && !docPopupEl.contains(e.target)) {
      if (window.completionActive) {
        window.hideCompletion();
        hideDocPopup();
      }
    }
  });

  editorEl.addEventListener('touchstart', (e) => {
    // Check if touch is on completion list or doc popup
    if (!completionListEl.contains(e.target) && !docPopupEl.contains(e.target)) {
      if (window.completionActive) {
        window.hideCompletion();
        hideDocPopup();
      }
    }
  }, { passive: true });

  // --- Custom Touch Handling REMOVED to allow native selection ---
  /*
  let lastTouchY = 0;
  let lastTouchX = 0;
  let isScrolling = false;

  editorEl.addEventListener('touchstart', (e) => {
    if (e.touches.length !== 1) return;
    lastTouchY = e.touches[0].clientY;
    lastTouchX = e.touches[0].clientX;
    isScrolling = false;
  }, { passive: true });

  editorEl.addEventListener('touchmove', (e) => {
    if (e.touches.length !== 1) return;

    const clientY = e.touches[0].clientY;
    const clientX = e.touches[0].clientX;
    const deltaY = lastTouchY - clientY;
    const deltaX = lastTouchX - clientX;

    lastTouchY = clientY;
    lastTouchX = clientX;

    if (Math.abs(deltaY) > 2 || Math.abs(deltaX) > 2) {
      isScrolling = true;
    }
  }, { passive: true });

  editorEl.addEventListener('touchend', (e) => {
    if (isScrolling) return;
    e.preventDefault(); // This was blocking native selection!

    // Manual Cursor Placement
    const touch = e.changedTouches[0];
    const pos = view.posAtCoords({ x: touch.clientX, y: touch.clientY });

    if (pos !== null) {
      view.dispatch({
        selection: { anchor: pos, head: pos },
        scrollIntoView: true
      });
      view.focus();
    }
  }, { passive: false });
  */

  // Prevent context menu - REMOVED to allow native selection
  // editorEl.addEventListener('contextmenu', (e) => {
  //   e.preventDefault();
  //   return false;
  // });
  // -------------------------------------------
  const bg = window.getComputedStyle(editorEl).backgroundColor
  document.body.style.backgroundColor = bg
  // Also update scroll-spacer to match editor color
  const scrollSpacer = document.getElementById('scroll-spacer');
  if (scrollSpacer) {
    scrollSpacer.style.backgroundColor = bg;
  }

  window.insertText = function (text) {
    view.dispatch(view.state.replaceSelection(text));
    view.focus();

    // Close completion on space, punctuation, etc.
    if (text === ' ' || text === '\t' || text === ';' || text === ',' || text === ')' || text === '}') {
      if (window.completionActive) {
        completionListEl.style.display = 'none';
        window.completionActive = false;
      }
      return; // Don't trigger completion
    }

    // Only trigger completion on word characters
    if (/[a-zA-Z_:]/.test(text)) {
      if (window.completionTimeout) clearTimeout(window.completionTimeout);
      window.completionTimeout = setTimeout(() => {
        startCompletion(view);
      }, 600);
    }
  };

  // Simple backspace - deletes one character
  window.performBackspace = function () {
    // Close completion on backspace
    if (window.completionActive) {
      completionListEl.style.display = 'none';
      window.completionActive = false;
    }

    // Guard: don't try to delete at position 0 (causes freeze)
    const pos = view.state.selection.main.head;
    if (pos === 0) return;

    deleteCharBackward(view);
  };

  window.performEnter = function () {
    // Hide completion list if active
    if (window.completionActive) {
      completionListEl.style.display = 'none';
      window.completionActive = false;
    }

    // Use auto-indent for new lines
    insertNewlineAndIndent(view);
  };

  window.performCopy = function () {
    const selection = view.state.selection.main;
    const text = view.state.sliceDoc(selection.from, selection.to);
    navigator.clipboard.writeText(text).catch(e => console.error("Copy failed:", e));
  };

  window.performPaste = async function () {
    try {
      const text = await navigator.clipboard.readText();
      view.dispatch(view.state.replaceSelection(text));
      // view.focus();
    } catch (e) {
      console.error("Paste failed:", e);
    }
  };

  window.performCut = function () {
    const selection = view.state.selection.main;
    const text = view.state.sliceDoc(selection.from, selection.to);
    navigator.clipboard.writeText(text);
    view.dispatch({
      changes: { from: selection.from, to: selection.to }
    });
  };

  window.performUndo = function () {
    undo(view);
  };

  window.performRedo = function () {
    redo(view);
  };

  // ========================================
  // CURSOR NAVIGATION FUNCTIONS
  // ========================================

  // Move cursor up one line
  window.moveUp = function () {
    const pos = view.state.selection.main.head;
    const line = view.state.doc.lineAt(pos);

    if (line.number === 1) return; // Already at first line

    const prevLine = view.state.doc.line(line.number - 1);
    const colInLine = pos - line.from;
    const newPos = prevLine.from + Math.min(colInLine, prevLine.length);

    view.dispatch({
      selection: { anchor: newPos, head: newPos },
      scrollIntoView: true
    });
    view.focus();
  };

  // Move cursor down one line
  window.moveDown = function () {
    const pos = view.state.selection.main.head;
    const line = view.state.doc.lineAt(pos);

    if (line.number === view.state.doc.lines) return; // Already at last line

    const nextLine = view.state.doc.line(line.number + 1);
    const colInLine = pos - line.from;
    const newPos = nextLine.from + Math.min(colInLine, nextLine.length);

    view.dispatch({
      selection: { anchor: newPos, head: newPos },
      scrollIntoView: true
    });
    view.focus();
  };

  // Move cursor left one character
  window.moveLeft = function () {
    const pos = view.state.selection.main.head;
    if (pos === 0) return; // Already at start

    view.dispatch({
      selection: { anchor: pos - 1, head: pos - 1 },
      scrollIntoView: true
    });
    view.focus();
  };

  // Move cursor right one character
  window.moveRight = function () {
    const pos = view.state.selection.main.head;
    if (pos >= view.state.doc.length) return; // Already at end

    view.dispatch({
      selection: { anchor: pos + 1, head: pos + 1 },
      scrollIntoView: true
    });
    view.focus();
  };

  // Move cursor to start of line (Home)
  window.moveHome = function () {
    const pos = view.state.selection.main.head;
    const line = view.state.doc.lineAt(pos);

    view.dispatch({
      selection: { anchor: line.from, head: line.from },
      scrollIntoView: true
    });
    view.focus();
  };

  // Move cursor to end of line (End)
  window.moveEnd = function () {
    const pos = view.state.selection.main.head;
    const line = view.state.doc.lineAt(pos);

    view.dispatch({
      selection: { anchor: line.to, head: line.to },
      scrollIntoView: true
    });
    view.focus();
  };

  // Page Up - scroll up by viewport height and move cursor
  window.pageUp = function () {
    const scrollInfo = view.scrollDOM;
    const viewportHeight = scrollInfo.clientHeight;

    // Calculate how many lines fit in viewport
    const lineHeight = view.defaultLineHeight;
    const linesPerPage = Math.floor(viewportHeight / lineHeight);

    const pos = view.state.selection.main.head;
    const currentLine = view.state.doc.lineAt(pos);
    const targetLineNum = Math.max(1, currentLine.number - linesPerPage);
    const targetLine = view.state.doc.line(targetLineNum);

    const colInLine = pos - currentLine.from;
    const newPos = targetLine.from + Math.min(colInLine, targetLine.length);

    view.dispatch({
      selection: { anchor: newPos, head: newPos },
      scrollIntoView: true
    });
    view.focus();
  };

  // Page Down - scroll down by viewport height and move cursor
  window.pageDown = function () {
    const scrollInfo = view.scrollDOM;
    const viewportHeight = scrollInfo.clientHeight;

    // Calculate how many lines fit in viewport
    const lineHeight = view.defaultLineHeight;
    const linesPerPage = Math.floor(viewportHeight / lineHeight);

    const pos = view.state.selection.main.head;
    const currentLine = view.state.doc.lineAt(pos);
    const targetLineNum = Math.min(view.state.doc.lines, currentLine.number + linesPerPage);
    const targetLine = view.state.doc.line(targetLineNum);

    const colInLine = pos - currentLine.from;
    const newPos = targetLine.from + Math.min(colInLine, targetLine.length);

    view.dispatch({
      selection: { anchor: newPos, head: newPos },
      scrollIntoView: true
    });
    view.focus();
  };

  // ========================================
  // SEARCH & REPLACE FUNCTIONS
  // ========================================

  // Store search state
  window.searchState = {
    query: '',
    matches: [],
    currentIndex: -1,
    caseSensitive: false,
    wholeWord: false,
    useRegex: false
  };

  // Search in document and highlight matches
  window.searchInDocument = function (query, caseSensitive = false, wholeWord = false, useRegex = false) {
    window.searchState.query = query;
    window.searchState.caseSensitive = caseSensitive;
    window.searchState.wholeWord = wholeWord;
    window.searchState.useRegex = useRegex;
    window.searchState.matches = [];
    window.searchState.currentIndex = -1;

    if (!query || query.length === 0) {
      window.clearSearch();
      return { count: 0, current: 0 };
    }

    const docText = view.state.doc.toString();
    let matches = [];

    try {
      let searchRegex;
      if (useRegex) {
        searchRegex = new RegExp(query, caseSensitive ? 'g' : 'gi');
      } else {
        // Escape special regex characters
        const escaped = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const pattern = wholeWord ? `\\b${escaped}\\b` : escaped;
        searchRegex = new RegExp(pattern, caseSensitive ? 'g' : 'gi');
      }

      let match;
      while ((match = searchRegex.exec(docText)) !== null) {
        matches.push({ from: match.index, to: match.index + match[0].length });
      }
    } catch (e) {
      console.error('[Search] Invalid regex:', e);
      return { count: 0, current: 0, error: e.message };
    }

    window.searchState.matches = matches;

    // Highlight all matches by adding decorations
    if (matches.length > 0) {
      window.searchState.currentIndex = 0;
      // Move to first match
      view.dispatch({
        selection: { anchor: matches[0].from, head: matches[0].to },
        scrollIntoView: true
      });
    }

    console.log(`[Search] Found ${matches.length} matches for "${query}"`);
    return { count: matches.length, current: matches.length > 0 ? 1 : 0 };
  };

  // Go to next match
  window.findNext = function () {
    const { matches, currentIndex } = window.searchState;
    if (matches.length === 0) return { count: 0, current: 0 };

    const nextIndex = (currentIndex + 1) % matches.length;
    window.searchState.currentIndex = nextIndex;

    const match = matches[nextIndex];
    view.dispatch({
      selection: { anchor: match.from, head: match.to },
      scrollIntoView: true
    });

    return { count: matches.length, current: nextIndex + 1 };
  };

  // Go to previous match
  window.findPrevious = function () {
    const { matches, currentIndex } = window.searchState;
    if (matches.length === 0) return { count: 0, current: 0 };

    const prevIndex = (currentIndex - 1 + matches.length) % matches.length;
    window.searchState.currentIndex = prevIndex;

    const match = matches[prevIndex];
    view.dispatch({
      selection: { anchor: match.from, head: match.to },
      scrollIntoView: true
    });

    return { count: matches.length, current: prevIndex + 1 };
  };

  // Replace current match
  window.replaceInDocument = function (replaceText) {
    const { matches, currentIndex, query } = window.searchState;
    if (matches.length === 0 || currentIndex < 0) return { count: 0, current: 0 };

    const match = matches[currentIndex];

    // Replace the current selection
    view.dispatch({
      changes: { from: match.from, to: match.to, insert: replaceText }
    });

    // Re-search to update matches after replacement
    setTimeout(() => {
      window.searchInDocument(query, window.searchState.caseSensitive, window.searchState.wholeWord, window.searchState.useRegex);
    }, 50);

    return { count: matches.length - 1, current: Math.min(currentIndex + 1, matches.length - 1) };
  };

  // Replace all matches
  window.replaceAllInDocument = function (replaceText) {
    const { matches, query } = window.searchState;
    if (matches.length === 0) return { replaced: 0 };

    // Build changes array in reverse order to maintain correct positions
    const changes = matches.map(m => ({
      from: m.from,
      to: m.to,
      insert: replaceText
    })).reverse();

    view.dispatch({ changes });

    const replacedCount = matches.length;
    window.clearSearch();

    console.log(`[Search] Replaced ${replacedCount} occurrences`);
    return { replaced: replacedCount };
  };

  // Clear search highlights
  window.clearSearch = function () {
    window.searchState = {
      query: '',
      matches: [],
      currentIndex: -1,
      caseSensitive: false,
      wholeWord: false,
      useRegex: false
    };
    console.log('[Search] Cleared');
  };

  // Get current match count (for UI updates)
  window.getMatchCount = function () {
    return {
      count: window.searchState.matches.length,
      current: window.searchState.currentIndex + 1
    };
  };

  // ========================================
  // CM6 BUILT-IN SEARCH PANEL (More Reliable)
  // ========================================

  // Toggle CM6's built-in search panel
  window.toggleSearchPanel = function () {
    openSearchPanel(view);
    return true;
  };

  window.openSearch = function () {
    openSearchPanel(view);
    return true;
  };

  window.closeSearch = function () {
    closeSearchPanel(view);
    return true;
  };

  // --- Helper to send requests ---
  function sendRequest(socket, method, params, timeout = 5000) {
    return new Promise((resolve, reject) => {
      if (socket.readyState !== WebSocket.OPEN) {
        reject(new Error('WebSocket not open'));
        return;
      }

      const id = ++messageId;
      const request = {
        jsonrpc: "2.0",
        id: id,
        method: method,
        params: params
      };

      const timer = setTimeout(() => {
        if (pendingRequests.has(id)) {
          pendingRequests.delete(id);
          reject(new Error(`Request ${method} timed out`));
        }
      }, timeout);

      pendingRequests.set(id, { resolve, reject, timer });
      socket.send(JSON.stringify(request));
    });
  }

  function sendNotification(socket, method, params) {
    if (socket.readyState === WebSocket.OPEN) {
      const notification = {
        jsonrpc: "2.0",
        method: method,
        params: params
      };
      socket.send(JSON.stringify(notification));
    }
  }

  // --- Connect LSP ---
  // This function is now internal to setupEditor or called via setLanguage
  // But since we need it globally accessible for setLanguage, we should expose it or define it outside?
  // Actually, setLanguage is defined outside setupEditor, but it needs access to 'editor' (which is 'view').
  // And 'connectLsp' needs access to 'ws' which is inside setupEditor.
  // This structure is a bit messy. 
  // Let's make connectLsp a global function or attached to window so setLanguage can call it.

  let intentionalClose = false;
  let reconnectPort = 8081;

  // Track which ports have already been initialized (server keeps running)
  const initializedPorts = new Set();

  window.connectLsp = function (port) {
    // Clear initialization cache for this port — server may have been restarted
    initializedPorts.delete(port);

    // Clear any pending reconnect attempts
    reconnectPort = port;

    if (ws) {
      console.log("Closing existing WebSocket connection...");
      intentionalClose = true; // Don't reconnect on this close
      try { ws.close(); } catch (e) { }
      ws = null;
    }

    const socketUrl = `ws://localhost:${port}/clsp`;
    console.log(`Connecting to LSP at ${socketUrl}`);

    try {
      ws = new WebSocket(socketUrl);
    } catch (e) {
      console.error("WebSocket creation failed:", e);
      setTimeout(() => window.connectLsp(port), 3000);
      return;
    }

    ws.onopen = () => {
      console.log("Connected to LSP server");
      intentionalClose = false; // Reset flag on successful connection

      // Check if this server is already initialized
      if (initializedPorts.has(port)) {
        console.log(`Server on port ${port} already initialized, skipping initialize`);
        isInitialized = true;

        // Just send didOpen for the current file
        if (URI) {
          window.lspVersion = 1; // Reset version
          sendNotification(ws, 'textDocument/didOpen', {
            textDocument: {
              uri: URI,
              languageId: currentLanguageId || 'cpp',
              version: window.lspVersion,
              text: view?.state.doc.toString() || ''
            }
          });
          openedDocumentUri = URI;
        }
        return;
      }

      // First time connecting to this server - do full initialize
      isInitialized = false;

      // Log the initialize request for debugging

      initializationPromise = sendRequest(ws, 'initialize', {
        processId: null,
        rootUri: rootURI,
        // Workspace folders help tsserver understand project structure
        workspaceFolders: rootURI ? [{ uri: rootURI, name: 'project' }] : null,
        capabilities: {
          workspace: {
            workspaceFolders: true,
            configuration: true
          },
          textDocument: {
            completion: {
              completionItem: {
                snippetSupport: false,
                documentationFormat: ['plaintext', 'markdown'],
                labelDetailsSupport: true
              },
              contextSupport: true,
              completionItemKind: {
                valueSet: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25]
              }
            },
            publishDiagnostics: {
              relatedInformation: true
            },
            hover: {
              contentFormat: ['plaintext', 'markdown']
            },
            synchronization: {
              willSave: false,
              didSave: true,
              willSaveWaitUntil: false
            }
          }
        },
        // Initialization options for typescript-language-server
        // These help tsserver create a proper inferred project
        initializationOptions: {
          hostInfo: 'bananaspaghetti-editor',
          preferences: {
            includeCompletionsForModuleExports: true,
            includeCompletionsWithInsertText: true,
            includeCompletionsWithSnippetText: false,
            includeAutomaticOptionalChainCompletions: true,
            includeCompletionsForImportStatements: true,
            allowIncompleteCompletions: true,
            useLabelDetailsInCompletionEntries: true
          },
          // Tell tsserver to use modern JavaScript defaults
          tsserver: {
            logDirectory: '/tmp',
            logVerbosity: 'off'
          }
        }
      }, 15000).then((result) => {
        isInitialized = true;
        initializedPorts.add(port); // Mark as initialized
        sendNotification(ws, 'initialized', {});

        // Send didOpen if we have a file
        if (URI) {
          window.lspVersion = 1; // Reset version for new document
          const currentText = view?.state.doc.toString() || '';
          sendNotification(ws, 'textDocument/didOpen', {
            textDocument: {
              uri: URI,
              languageId: currentLanguageId || 'cpp',
              version: window.lspVersion,
              text: currentText
            }
          });
          openedDocumentUri = URI;

          // If user was typing during initialization, the document may have changed
          // by the time didOpen was sent. Schedule a sync to ensure LSP has latest text.
          // This fixes the race condition where user types before LSP is ready.
          setTimeout(() => {
            if (ws && ws.readyState === WebSocket.OPEN && openedDocumentUri === URI && view) {
              const latestText = view.state.doc.toString();
              // Only sync if text actually changed since didOpen
              if (latestText !== currentText) {
                window.lspVersion++;
                sendNotification(ws, 'textDocument/didChange', {
                  textDocument: { uri: URI, version: window.lspVersion },
                  contentChanges: [{ text: latestText }]
                });
              }
            }
          }, 200); // 200ms after didOpen to catch any typing during init
        }
      }).catch(e => {
        console.error("LSP Init failed:", JSON.stringify(e));
        // If server says "already initialized", mark it and retry
        if (e && e.code === -32600 && e.message && e.message.includes('already initialized')) {
          console.log("Server already initialized, marking and retrying...");
          initializedPorts.add(port);
          isInitialized = true;
          // Send didOpen
          if (URI) {
            sendNotification(ws, 'textDocument/didOpen', {
              textDocument: {
                uri: URI,
                languageId: currentLanguageId || 'cpp',
                version: 1,
                text: view?.state.doc.toString() || ''
              }
            });
          }
        }
      });
    };

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);

        // Log all incoming messages for debugging
        const msgPreview = event.data.length > 200 ? event.data.substring(0, 200) + '...' : event.data;

        // IMPORTANT: Distinguish between:
        // 1. Response to OUR request: has id, has result OR error, NO method
        // 2. Request FROM server: has id, has method, no result
        // 3. Notification from server: has method, NO id

        // Handle Responses to our requests (must have result or error, and no method)
        if (msg.id !== undefined && (msg.result !== undefined || msg.error !== undefined) && msg.method === undefined) {
          if (pendingRequests.has(msg.id)) {
            const { resolve, reject, timer } = pendingRequests.get(msg.id);
            clearTimeout(timer);
            pendingRequests.delete(msg.id);
            if (msg.error) {
              reject(msg.error);
            } else {
              resolve(msg.result);
            }
          } else {
          }
          return;
        }

        // Handle Server Requests (has both id and method - server is asking US something)
        if (msg.id !== undefined && msg.method !== undefined) {

          // Respond to workspace/configuration requests
          if (msg.method === 'workspace/configuration') {
            // Send empty configuration response
            const response = {
              jsonrpc: '2.0',
              id: msg.id,
              result: msg.params.items.map(() => ({})) // Empty config for each item
            };
            ws.send(JSON.stringify(response));
          } else {
            // For other server requests, send an error (not supported)
            const response = {
              jsonrpc: '2.0',
              id: msg.id,
              error: { code: -32601, message: 'Method not supported' }
            };
            ws.send(JSON.stringify(response));
          }
          return;
        }

        // Handle Notifications (has method but no id)
        if (msg.method === "textDocument/publishDiagnostics") {
          handleDiagnostics(msg.params);
        }
      } catch (e) {
        console.error("Error parsing LSP message:", e);
      }
    };

    ws.onerror = (error) => {
      console.error("WebSocket error:", error);
    };

    ws.onclose = (event) => {
      console.log("WebSocket closed:", event.code, event.reason);

      // Only reconnect if it was not an intentional close
      if (!intentionalClose) {
        setTimeout(() => {
          console.log("Attempting to reconnect...");
          window.connectLsp(reconnectPort);
        }, 3000);
      }
      intentionalClose = false; // Reset for next time
    };
  };

  // Initial connection (default)
  // window.connectLsp(8081); 

  window.performFind = function () {
    openSearchPanel(view);
  };

  window.closeFindPanel = function () {
    closeSearchPanel(view);
  };

  window.performSelectAll = function () {
    view.dispatch({
      selection: { anchor: 0, head: view.state.doc.length }
    });
  };

} // End of setupEditor

setupEditor();

// Runtime font size change for CodeMirror
// Uses CSS injection for dynamic font size updates
window.setFontSize = function (size) {
  const existingStyle = document.getElementById('editor-font-size-style');
  if (existingStyle) {
    existingStyle.remove();
  }

  const fontStyle = document.createElement('style');
  fontStyle.id = 'editor-font-size-style';
  fontStyle.textContent = `
    .cm-content, .cm-line, .cm-gutterElement {
      font-size: ${size}px !important;
      line-height: 1.4 !important;
    }
    .cm-gutters {
      font-size: ${size}px !important;
    }
  `;
  document.head.appendChild(fontStyle);
};

// Dynamic bottom padding for when bottom bar is expanded
// This allows scrolling past the last line when bottom bar covers it
window.setBottomPadding = function (paddingPx) {
  const existingStyle = document.getElementById('editor-bottom-padding-style');
  if (existingStyle) {
    existingStyle.remove();
  }

  const paddingStyle = document.createElement('style');
  paddingStyle.id = 'editor-bottom-padding-style';
  paddingStyle.textContent = `
    .cm-scroller {
      padding-bottom: ${paddingPx}px !important;
    }
  `;
  document.head.appendChild(paddingStyle);
};

// Auto-save functionality
let autoSaveTimer = null;
let autoSaveEnabled = false;
let autoSaveDelay = 3000; // Default 3 seconds

window.setAutoSave = function (enabled, delaySeconds) {
  autoSaveEnabled = enabled;
  if (delaySeconds) autoSaveDelay = delaySeconds * 1000;

  if (!enabled && autoSaveTimer) {
    clearTimeout(autoSaveTimer);
    autoSaveTimer = null;
  }
  return true;
};

function triggerAutoSave() {
  if (!autoSaveEnabled || !window.view) return;

  if (autoSaveTimer) clearTimeout(autoSaveTimer);

  autoSaveTimer = setTimeout(() => {
    if (window.AndroidInterface && window.AndroidInterface.saveFile) {
      window.AndroidInterface.saveFile();
    }
  }, autoSaveDelay);
}

