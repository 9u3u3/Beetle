// Imports
import { EditorView, basicSetup } from "codemirror"
import { EditorState, StateEffect, StateField } from "@codemirror/state"
import { oneDark } from "@codemirror/theme-one-dark"
import { autocompletion, completionKeymap, closeCompletion, startCompletion, completionStatus } from "@codemirror/autocomplete"
import { keymap } from "@codemirror/view"
import { linter, lintGutter, forceLinting } from "@codemirror/lint"
import { cpp } from "@codemirror/lang-cpp"
import { insertNewlineAndIndent, deleteCharBackward, undo, redo } from "@codemirror/commands"
import { openSearchPanel, closeSearchPanel } from "@codemirror/search"

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
    box-shadow: 0 4px 6px rgba(0,0,0,0.3);
    z-index: 1000;
    max-height: 200px;
    overflow-y: auto;
    width: 250px;
    font-family: monospace;
    display: none;
  }
  .lsp-completion-item {
    padding: 4px 8px;
    cursor: pointer;
    color: #ccc;
    display: flex;
    align-items: center;
    font-size: 12px;
  }
  .lsp-completion-item.selected {
    background: #2c313a;
    color: #fff;
  }
  .lsp-completion-icon {
    margin-right: 8px;
    width: 14px;
    text-align: center;
    opacity: 0.7;
  }
  .lsp-completion-detail {
    margin-left: auto;
    opacity: 0.5;
    font-size: 10px;
  }
  /* Force diagnostic styles */
  .cm-lint-range-error {
    background-image: none !important;
    text-decoration: underline wavy #ff5555 !important;
    text-decoration-skip-ink: none;
  }
  .cm-lint-range-warning {
    background-image: none !important;
    text-decoration: underline wavy #ffb86c !important;
    text-decoration-skip-ink: none;
  }
  .cm-lint-range-info {
    background-image: none !important;
    text-decoration: underline wavy #8be9fd !important;
    text-decoration-skip-ink: none;
  }
`;
document.head.appendChild(style);

const completionListEl = document.createElement('div');
completionListEl.className = 'lsp-completion-list';
document.body.appendChild(completionListEl);


function setupEditor() {
  const ws = new WebSocket("ws://localhost:8081/clsp")
  let view = null

  let lastCompletionTime = 0
  let URI = "file:///main.cpp"
  let rootURI = "file:///"
  let pendingRequests = new Map()
  let messageId = 0

  let messageBuffer = '';
  let isInitialized = false;
  let initializationPromise = null;

  // StateEffect for updating diagnostics
  const setDiagnosticsEffect = StateEffect.define()

  // StateField to store diagnostics
  const diagnosticsField = StateField.define({
    create() {
      return []
    },
    update(diagnostics, tr) {
      for (let effect of tr.effects) {
        if (effect.is(setDiagnosticsEffect)) {
          return effect.value
        }
      }
      return diagnostics
    }
  })

  window.setFilePath = (path) => {
    // console.log(`[LSP-JS] Setting file path to: ${path}`);
    const oldURI = URI;
    URI = `file://${path}`;

    // If we haven't initialized yet, just updating URI is enough.
    // The onopen handler will use the new URI.
    if (isInitialized && oldURI !== URI && ws.readyState === WebSocket.OPEN) {
      // Close old file
      sendNotification(ws, 'textDocument/didClose', {
        textDocument: { uri: oldURI }
      });

      // Open new file
      sendNotification(ws, 'textDocument/didOpen', {
        textDocument: {
          uri: URI,
          languageId: 'cpp',
          version: 1,
          text: view?.state.doc.toString() || ''
        }
      });
    }
  };

  const setRootUri = (uri) => { rootURI = uri }

  function handleDiagnostics(params) {
    if (!view) return
    try {
      const diagnostics = []
      for (const d of params.diagnostics) {
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
          // console.log(`[LSP-JS] Diagnostic mapped: ${d.message} at ${from}-${to}`);
        } catch (e) {
          console.error('Error processing diagnostic:', e)
        }
      }

      // console.log(`📊 ${diagnostics.length} diagnostics`);

      // Update diagnostics using StateEffect
      view.dispatch({
        effects: setDiagnosticsEffect.of(diagnostics)
      })

      // Force linter to update
      forceLinting(view)
    } catch (e) {
      console.error('Error in handleDiagnostics:', e)
    }
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
      el.onclick = () => {
        window.completionIndex = index;
        window.applyCompletion();
      };
      completionListEl.appendChild(el);
    });

    // Position list at cursor
    const coords = view.coordsAtPos(view.state.selection.main.head);
    if (coords) {
      completionListEl.style.left = `${coords.left}px`;
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

      completionListEl.style.display = 'none';
      window.completionActive = false;
      return true;
    }
    return false;
  };

  async function lspCompletionSource(context) {
    // Wait for initialization
    if (!isInitialized) {
      console.log('[LSP-JS] Completion skipped: Not initialized');
      return null;
    }

    console.log('[LSP-JS] Completion source triggered');

    const now = Date.now()

    // Always allow explicit completions
    if (!context.explicit) {
      // For automatic completions, check timing (debounce 300ms)
      if (now - lastCompletionTime < 300) {
        return null
      }
    }

    lastCompletionTime = now;

    const line = context.state.doc.lineAt(context.pos)
    const lineNumber = line.number - 1
    const character = context.pos - line.from

    // Force flush pending didChange before requesting completion
    // This ensures the server has the latest text (e.g. "std::")
    if (window.didChangeTimeout) {
      clearTimeout(window.didChangeTimeout);
      window.didChangeTimeout = null;
      sendNotification(ws, 'textDocument/didChange', {
        textDocument: {
          uri: URI,
          version: Date.now()
        },
        contentChanges: [{ text: context.state.doc.toString() }]
      });
      console.log('[LSP-JS] Flushed pending didChange for completion');
    }

    try {
      const result = await sendRequest(ws, 'textDocument/completion', {
        textDocument: { uri: URI },
        position: { line: lineNumber, character },
        context: {
          triggerKind: context.explicit ? 1 : 2,
        }
      }, 8000)

      if (!result) return null

      const items = result.items || result
      if (!Array.isArray(items) || items.length === 0) {
        console.log('[LSP-JS] No completion items returned');
        return null
      }

      console.log(`✨ Got ${items.length} completion items`);
      if (items.length > 0) {
        const firstFew = items.slice(0, 5).map(i => i.label).join(', ');
        console.log(`[LSP-JS] Suggestions: ${firstFew}...`);
      }

      const wordBefore = context.matchBefore(/\w*/)
      const from = wordBefore ? wordBefore.from : context.pos;

      window.completionItems = items.slice(0, 50).map(item => {
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

  // Linter function that reads from StateField
  const lspLinter = linter((view) => {
    return view.state.field(diagnosticsField)
  })

  const startState = EditorState.create({
    doc: "",
    extensions: [
      basicSetup,
      oneDark,
      cpp(),
      diagnosticsField, // Add StateField
      lspLinter,
      lintGutter(),
      // EditorState.readOnly.of(true), // Makes the editor readOnly for the user (to block the default from popping up)
      EditorView.contentAttributes.of({
        inputmode: "none"
      }),
      autocompletion({
        override: [lspCompletionSource],
        activateOnTyping: true,
        defaultKeymap: true,
        maxRenderedOptions: 50,
        closeOnBlur: false,
        interactionToken: "lsp" // explicit token to help debugging
      }),
      keymap.of([
        ...completionKeymap,
        {
          key: `Escape`,
          run: closeCompletion
        }
      ]),
      EditorView.updateListener.of((update) => {
        if (update.docChanged) window.code_ = update.state.doc.toString()

        // Throttle status updates (max once every 500ms)
        const now = Date.now();
        if (!window.lastStatusTime || now - window.lastStatusTime > 500) {
          // Calculate status data only when needed
          const state = view.state;
          const pos = state.selection.main.head;
          const line = state.doc.lineAt(pos);
          const col = pos - line.from;

          // Count diagnostics
          let errors = 0;
          let warnings = 0;
          const diags = state.field(diagnosticsField);
          diags.forEach(d => {
            if (d.severity === 'error') errors++;
            if (d.severity === 'warning') warnings++;
          });

          const statusMsg = `[LSP-STATUS] line:${line.number} col:${col + 1} err:${errors} warn:${warnings}`;
          if (window.lastLspStatus !== statusMsg) {
            console.log(statusMsg);
            window.lastLspStatus = statusMsg;
            window.lastStatusTime = now;
          }
        }

        // Debounce didChange (200ms)
        if (update.docChanged && ws.readyState === WebSocket.OPEN && view && isInitialized) {
          if (window.didChangeTimeout) clearTimeout(window.didChangeTimeout);
          window.didChangeTimeout = setTimeout(() => {
            // Skip if document is too large to prevent freezing
            const docText = view.state.doc.toString();
            if (docText.length > 50000) {
              console.warn('[LSP-JS] Document too large, skipping didChange');
              return;
            }
            sendNotification(ws, 'textDocument/didChange', {
              textDocument: {
                uri: URI,
                version: Date.now()
              },
              contentChanges: [{ text: docText }]
            })
          }, 1000); // Increased to 1 second
        }
      })
    ]
  })

  view = new EditorView({
    state: startState,
    parent: document.body
  })

  window.editor = view
  window.isEditorReady = true

  const editorEl = document.querySelector('.cm-editor')
  const bg = window.getComputedStyle(editorEl).backgroundColor
  document.body.style.backgroundColor = bg

  window.insertText = function (text) {
    // console.log(`[LSP-JS] insertText called with: '${text}'`);
    view.dispatch(view.state.replaceSelection(text));
    // view.focus(); // Removed to prevent potential layout thrashing during rapid typing

    // Manually trigger completion since inputmode="none" disables default behavior
    if (window.completionTimeout) clearTimeout(window.completionTimeout);
    window.completionTimeout = setTimeout(() => {
      startCompletion(view);
    }, 300);
  };

  window.performBackspace = function () {
    deleteCharBackward(view);
    // view.focus();
  };

  window.performEnter = function () {
    insertNewlineAndIndent(view);
    // view.focus();
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

  window.performSelectAll = function () {
    view.dispatch({
      selection: { anchor: 0, head: view.state.doc.length }
    });
  };

  window.performFind = function () {
    openSearchPanel(view);
  };

  window.closeFindPanel = function () {
    closeSearchPanel(view);
  };

  function parseJSONMessages(text) {
    const messages = [];
    let depth = 0;
    let inString = false;
    let escape = false;
    let startIdx = -1;
    let i = 0;

    while (i < text.length) {
      const char = text[i];

      if (escape) {
        escape = false;
        i++;
        continue;
      }

      if (char === '\\' && inString) {
        escape = true;
        i++;
        continue;
      }

      if (char === '"' && !escape) {
        inString = !inString;
        i++;
        continue;
      }

      if (inString) {
        i++;
        continue;
      }

      if (char === '{') {
        if (depth === 0) startIdx = i;
        depth++;
      } else if (char === '}') {
        depth--;
        if (depth === 0 && startIdx !== -1) {
          const jsonStr = text.substring(startIdx, i + 1);
          try {
            const parsed = JSON.parse(jsonStr);
            messages.push(parsed);
          } catch (e) {
            console.error('Failed to parse JSON:', jsonStr.substring(0, 100));
          }
          startIdx = -1;
        }
      }

      i++;
    }

    return {
      messages,
      remaining: startIdx !== -1 ? text.substring(startIdx) : ''
    };
  }

  ws.onopen = () => {
    console.log('✓ [LSP-JS] WebSocket Connected');
    // console.log('[LSP-STATUS] line:1 col:1 err:0 warn:0'); // Reset status

    initializationPromise = sendRequest(ws, 'initialize', {
      processId: null,
      rootUri: rootURI,
      capabilities: {
        textDocument: {
          completion: {
            completionItem: {
              snippetSupport: false,
              documentationFormat: ['plaintext', 'markdown']
            },
            contextSupport: true
          },
          publishDiagnostics: {},
          hover: {
            contentFormat: ['plaintext']
          }
        }
      }
    }, 15000).then((result) => {
      console.log('✅ Server initialized:', result.serverInfo?.name);
      isInitialized = true;

      sendNotification(ws, 'initialized', {});

      sendNotification(ws, 'textDocument/didOpen', {
        textDocument: {
          uri: URI,
          languageId: 'cpp',
          version: 1,
          text: view?.state.doc.toString() || ''
        }
      });

      // Request initial diagnostics
      console.log('[LSP-JS] Requesting initial diagnostics');
      // Some servers might need a change notification to trigger diagnostics if open didn't
      // But usually didOpen is enough. We can try a fake change if needed.

      return result;
    }).catch(err => {
      console.error('Initialize error:', err);
      isInitialized = false;
      throw err;
    });
  };

  ws.onerror = (error) => {
    console.error('❌ [LSP-JS] WebSocket error:', error);
    isInitialized = false;
  }

  ws.onclose = (event) => {
    console.log(`[LSP-JS] WebSocket closed: ${event.code} - ${event.reason}`);
    isInitialized = false;
  }

  ws.onmessage = (event) => {
    try {
      const data = event.data.toString();
      // console.log('[LSP-JS] RX RAW:', data.substring(0, 100)); // Log raw data snippet
      messageBuffer += data;
      messageBuffer = messageBuffer.replace(/Content-Length:\s*\d+\s*/g, '');

      const { messages, remaining } = parseJSONMessages(messageBuffer);
      messageBuffer = remaining;

      for (const message of messages) {
        // console.log('[LSP-JS] RX JSON:', JSON.stringify(message).substring(0, 150)); // Log parsed JSON

        if (message.id !== undefined && pendingRequests.has(message.id)) {
          const { resolve, reject, timeoutId } = pendingRequests.get(message.id);
          clearTimeout(timeoutId);
          pendingRequests.delete(message.id);

          if (message.error) {
            console.error('❌ [LSP-JS] LSP Error:', message.error);
            reject(message.error);
          } else {
            // console.log(`✓ [LSP-JS] Response for request ${message.id}`);
            resolve(message.result);
          }
        }

        if (message.method === 'textDocument/publishDiagnostics') {
          // console.log(`[LSP-JS] Diagnostics received for ${message.params.uri}`);
          handleDiagnostics(message.params);
        }
      }
    } catch (e) {
      console.error('Error processing WebSocket message:', e);
    }
  };

  function sendRequest(ws, method, params, timeout = 10000) {
    return new Promise((resolve, reject) => {
      if (ws.readyState !== WebSocket.OPEN) {
        reject(new Error('WebSocket not open'));
        return;
      }

      const id = messageId++;
      const message = {
        jsonrpc: '2.0',
        id,
        method,
        params
      };

      const timeoutId = setTimeout(() => {
        if (pendingRequests.has(id)) {
          pendingRequests.delete(id);
          console.error(`⏱️ Request ${id} (${method}) timed out`);
          reject(new Error(`Request timeout: ${method}`));
        }
      }, timeout);

      pendingRequests.set(id, { resolve, reject, timeoutId });

      // Cleanup old requests if too many are pending (prevent memory leak)
      if (pendingRequests.size > 20) {
        const oldestId = Math.min(...pendingRequests.keys());
        const oldest = pendingRequests.get(oldestId);
        if (oldest) {
          clearTimeout(oldest.timeoutId);
          pendingRequests.delete(oldestId);
          console.warn(`[LSP-JS] Cleaned up old request ${oldestId}`);
        }
      }

      // console.log(`→ [LSP-JS] Request ${id}: ${method}`, JSON.stringify(params).substring(0, 200));
      ws.send(JSON.stringify(message));
    });
  }

  function sendNotification(ws, method, params) {
    if (ws.readyState !== WebSocket.OPEN) {
      console.warn('Cannot send notification, WebSocket not open');
      return;
    }

    const message = {
      jsonrpc: '2.0',
      method,
      params
    };

    ws.send(JSON.stringify(message));
  }
}

setupEditor();
