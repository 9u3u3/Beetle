import { EditorView, basicSetup } from "codemirror"
import { EditorState, StateEffect, StateField, Compartment } from "@codemirror/state"
import { oneDark } from "@codemirror/theme-one-dark"
import { autocompletion, completionKeymap, closeCompletion, startCompletion, completionStatus } from "@codemirror/autocomplete"
import { keymap, drawSelection } from "@codemirror/view"
import { linter, lintGutter, forceLinting, setDiagnostics } from "@codemirror/lint"
import { cpp } from "@codemirror/lang-cpp"
import { python } from "@codemirror/lang-python"
import { insertNewlineAndIndent, deleteCharBackward, undo, redo } from "@codemirror/commands"
import { openSearchPanel, closeSearchPanel } from "@codemirror/search"

const languageCompartment = new Compartment()
let currentLanguageId = 'cpp'; // Track current language for LSP

window.setLanguage = async function (slug, port = 8081) {
  console.log(`[LSP-JS] setLanguage called for ${slug} port: ${port}`);
  currentLanguageId = slug; // Update current language

  // Connect to LSP
  if (window.connectLsp) {
    window.connectLsp(port);
  }

  let langModule;
  try {
    if (slug === 'cpp') {
      langModule = cpp();
    } else if (slug === 'python') {
      langModule = python();
    } else {
      // Dynamic import
      const moduleUrl = `http://localhost:8080/plugins/${slug}/${slug}.js`;
      const module = await import(moduleUrl);

      // Check if default export is a function or the extension itself
      langModule = typeof module.default === 'function' ? module.default() : module.default;
    }

    if (window.view && langModule) {
      window.view.dispatch({
        effects: languageCompartment.reconfigure(langModule)
      })
      console.log(`[LSP-JS] Language reconfigured to ${slug}`);
    }
  } catch (e) {
    console.error(`[LSP-JS] Failed to load language ${slug}`, e);
  }
}

// Clear language support (for unsupported files)
window.clearLanguage = function () {
  console.log('[LSP-JS] Clearing language support (plaintext mode)');
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
  position: absolute;
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
`;
document.head.appendChild(style);

const completionListEl = document.createElement('div');
completionListEl.className = 'lsp-completion-list';
document.body.appendChild(completionListEl);

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
      console.log(`[LSP-JS] Cancelled request ${id}`);
    }
  }

  // Note: Diagnostics are handled via setDiagnostics() from @codemirror/lint
  // No custom StateEffect/StateField needed

  window.setFilePath = (path) => {
    const oldURI = URI;
    URI = `file://${path}`;
    console.log(`[LSP-JS] setFilePath: ${path}, oldURI=${oldURI}, newURI=${URI}`);

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
      console.log(`[LSP-JS] Sent didOpen for ${URI} with version ${window.lspVersion}`);
    }
  };

  window.setRootUri = (uri) => {
    rootURI = uri;
    console.log(`[LSP-JS] rootUri set to: ${rootURI}`);
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
          console.log(`[LSP-JS] Applying ${validDiagnostics.length} diagnostics (${errorCount} errors, ${warningCount} warnings)`);
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
      console.log('[LSP-JS] Hiding completion list (empty)');
      return;
    }

    console.log(`[LSP-JS] Showing completion list with ${window.completionItems.length} items`);

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
      // Use touchend to prevent keyboard dismissal on mobile
      el.ontouchend = (e) => {
        e.preventDefault();
        e.stopPropagation();
        window.completionIndex = index;
        window.applyCompletion();
        // Refocus editor to keep keyboard open
        view.focus();
      };
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
    // console.log("[LSP-JS] insertText called with: '" + text + "'"); // Silenced
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
      // console.log('[LSP-JS] Completion skipped: Not initialized');
      return null;
    }

    // console.log('[LSP-JS] Completion source triggered');

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
      console.log('[LSP-JS] Flushed pending didChange for completion');
    }

    try {
      // Store the request ID before sending
      const currentRequestId = messageId + 1; // sendRequest will use this ID
      pendingCompletionId = currentRequestId;

      // Log the completion request details
      console.log(`[LSP-JS] Completion request: uri=${URI}, line=${lineNumber}, char=${character}, triggerKind=${triggerKind}, triggerChar=${triggerCharacter || 'none'}, langId=${currentLanguageId}`);

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
        console.log('[LSP-JS] No completion items returned');
        return null
      }

      // console.log(`✨ Got ${items.length} completion items`);
      if (items.length > 0) {
        const firstFew = items.slice(0, 5).map(i => `${i.label}(${i.kind || '?'})`).join(', ');
        console.log(`[LSP-JS] First 5 items: ${firstFew}`);
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
      oneDark,
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
            // console.log('[LSP-JS] Content changed (user), calling onContentChanged');
            window.AndroidEditor.onContentChanged();
          }

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
              console.log(`[LSP-JS] Skipping didChange: document ${URI} not opened (opened: ${openedDocumentUri})`);
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
            console.log(`[LSP-JS] Sent didChange for ${URI} version ${window.lspVersion}`);
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

  window.isEditorReady = true

  const editorEl = document.querySelector('.cm-editor')
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
    console.log("[LSP-JS] Performed insertNewlineAndIndent");
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
          console.log(`[LSP-JS] Reconnect didOpen for ${URI}`);
        }
        return;
      }

      // First time connecting to this server - do full initialize
      isInitialized = false;

      // Log the initialize request for debugging
      console.log(`[LSP-JS] Sending initialize with rootUri: ${rootURI}, languageId: ${currentLanguageId}`);

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
        console.log(`[LSP-JS] ✅ Server initialized: ${result.serverInfo?.name || 'unknown'}, rootUri was: ${rootURI}`);
        console.log(`[LSP-JS] Server capabilities: ${JSON.stringify(result.capabilities?.completionProvider || {})}`);
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
          console.log(`[LSP-JS] Initial didOpen for ${URI}`);

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
                console.log(`[LSP-JS] Post-init sync: text changed during init, sent didChange v${window.lspVersion}`);
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
        console.log(`[LSP-JS] Received message (len=${event.data.length}): ${msgPreview}`);

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
              console.log(`[LSP-JS] Request ${msg.id} failed:`, msg.error);
              reject(msg.error);
            } else {
              console.log(`[LSP-JS] Request ${msg.id} succeeded`);
              resolve(msg.result);
            }
          } else {
            console.log(`[LSP-JS] Received response for unknown request ${msg.id}`);
          }
          return;
        }

        // Handle Server Requests (has both id and method - server is asking US something)
        if (msg.id !== undefined && msg.method !== undefined) {
          console.log(`[LSP-JS] Server request: ${msg.method} (id=${msg.id})`);

          // Respond to workspace/configuration requests
          if (msg.method === 'workspace/configuration') {
            // Send empty configuration response
            const response = {
              jsonrpc: '2.0',
              id: msg.id,
              result: msg.params.items.map(() => ({})) // Empty config for each item
            };
            ws.send(JSON.stringify(response));
            console.log(`[LSP-JS] Sent configuration response for id=${msg.id}`);
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
  console.log('[Editor] Font size set to:', size);
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
  console.log('[Editor] Bottom padding set to:', paddingPx);
};
