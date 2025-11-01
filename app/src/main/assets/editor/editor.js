//// Imports
//import { EditorView, basicSetup } from "codemirror"
//import { EditorState, StateEffect, StateField } from "@codemirror/state"
//import { oneDark } from "@codemirror/theme-one-dark"
//import { autocompletion, completionKeymap, closeCompletion, startCompletion, completionStatus } from "@codemirror/autocomplete"
//import { keymap } from "@codemirror/view"
//import { linter, setDiagnostics, lintGutter } from "@codemirror/lint"
//import { cpp } from "@codemirror/lang-cpp"
//import { insertNewlineAndIndent, deleteCharBackward } from "@codemirror/commands"
//
//window.code_ = ""
//
//function setupEditor() {
//  const ws = new WebSocket("ws://localhost:8081/clsp")
//  let view = null
//
//  let lastCompletionTime = 0
//  let URI = "file:///main.cpp"
//  let rootURI = "file:///"
//  let pendingRequests = new Map()
//  let messageId = 0
//
//  let messageBuffer = '';
//
//  const setFileUri = (uri) => {URI = uri}
//  const setRootUri = (uri) => {rootURI = uri}
//
//  function handleDiagnostics(params) {
//    if(!view) return
//    try {
//      const diagnostics = []
//      for(const d of params.diagnostics) {
//        try {
//          if(d.range.start.line < 0 || d.range.end.line < 0) continue
//          if(d.range.start.line + 1 > view.state.doc.lines || d.range.end.line + 1 > view.state.doc.lines) continue;
//
//          const startLine = view.state.doc.line(d.range.start.line + 1)
//          const endLine = view.state.doc.line(d.range.end.line + 1)
//          const startChar = Math.min(d.range.start.character, startLine.length)
//          const endChar = Math.min(d.range.end.character, endLine.length)
//          const from = startLine.from + startChar
//          const to = endLine.from + endChar
//
//          if(to < from) continue
//
//          diagnostics.push({
//            from: from,
//            to: Math.max(from + 1, to),
//            severity: d.severity === 1 ? 'error' :
//                      d.severity === 2 ? 'warning' : 'info',
//            message: d.message || 'Unknown error',
//            source: d.source || 'clangd'
//          })
//        } catch(e) {
//          console.error('Error processing diagnostic:', e)
//        }
//      }
//
//      console.log(`📊 ${diagnostics.length} diagnostics`);
//
//      // Use setDiagnostics to apply diagnostics
//      view.dispatch(setDiagnostics(view.state, diagnostics))
//    } catch(e) {
//      console.error('Error in handleDiagnostics:', e)
//    }
//  }
//
//  async function lspCompletionSource(context) {
//    const now = Date.now()
//
//    // Always allow explicit completions
//    if(!context.explicit) {
//      // For automatic completions, check timing
//      if(now - lastCompletionTime < 200) {
//        return null
//      }
//    }
//
//    lastCompletionTime = now;
//
//    const line = context.state.doc.lineAt(context.pos)
//    const lineNumber = line.number - 1
//    const character = context.pos - line.from
//
//    try {
//      const result = await sendRequest(ws, 'textDocument/completion', {
//        textDocument: {uri: URI},
//        position: {line: lineNumber, character},
//        context: {
//          triggerKind: context.explicit ? 1 : 2,
//        }
//      }, 5000)
//
//      if(!result) {
//        return null
//      }
//
//      const items = result.items || result
//      if(!Array.isArray(items) || items.length === 0) {
//        return null
//      }
//
//      console.log(`✨ Got ${items.length} completion items`)
//
//      const options = items.slice(0, 50).map(item => {
//        const cleanLabel = item.label.replace(/^[•\s]+/, '').trim()
//        let textToInsert = cleanLabel
//
//        if(item.insertText) textToInsert = item.insertText
//        else if(item.textEdit && item.textEdit.newText) textToInsert = item.textEdit.newText
//
//        return {
//          label: cleanLabel,
//          type: item.kind === 3 ? 'function' :
//                item.kind === 6 ? 'variable' :
//                item.kind === 14 ? 'keyword' :
//                item.kind === 5 ? 'class' :
//                item.kind === 7 ? 'class' :
//                item.kind === 8 ? 'interface' :
//                item.kind === 9 ? 'namespace' :
//                item.kind === 2 ? 'method' :
//                item.kind === 13 ? 'enum' : 'text',
//          detail: item.detail || '',
//          info: typeof item.documentation === 'string' ? item.documentation : item.documentation?.value || '',
//          apply: textToInsert
//        }
//      })
//
//      const wordBefore = context.matchBefore(/\w*/)
//      return {
//        from: wordBefore ? wordBefore.from : context.pos,
//        options,
//        validFor: /^\w*$/
//      }
//    } catch(e) {
//      console.error('Completion error:', e)
//      return null
//    }
//  }
//
//  const startState = EditorState.create({
//    doc: "",
//    extensions: [
//      basicSetup,
//      oneDark,
//      cpp(),
//      linter(() => []), // Empty linter - diagnostics are set via setDiagnostics
//      lintGutter(), // THIS IS THE KEY! Must include lintGutter for squiggly lines
//      autocompletion({
//        override: [lspCompletionSource],
//        activateOnTyping: true,
//        defaultKeymap: true,
//        maxRenderedOptions: 50,
//        closeOnBlur: false
//      }),
//      keymap.of([
//        ...completionKeymap,
//        {
//          key: `Escape`,
//          run: closeCompletion
//        }
//      ]),
//      EditorView.updateListener.of((update) => {
//        if(update.docChanged) window.code_ = update.state.doc.toString()
//
//        if(update.docChanged && ws.readyState === WebSocket.OPEN && view) {
//          sendNotification(ws, 'textDocument/didChange', {
//            textDocument: {
//              uri: URI,
//              version: Date.now()
//            },
//            contentChanges: [{text: update.state.doc.toString()}]
//          })
//        }
//      })
//    ]
//  })
//
//  view = new EditorView({
//    state: startState,
//    parent: document.body
//  })
//
//  window.editor = view
//  window.isEditorReady = true
//
//  const editorEl = document.querySelector('.cm-editor')
//  const bg = window.getComputedStyle(editorEl).backgroundColor
//  document.body.style.backgroundColor = bg
//
//  window.insertText = function(text) {
//    view.dispatch(view.state.replaceSelection(text));
//    view.focus();
//  };
//
//  window.performBackspace = function() {
//    deleteCharBackward(view);
//    view.focus();
//  };
//
//  window.performEnter = function() {
//     insertNewlineAndIndent(view);
//     view.focus();
//  };
//
//  window.performCopy = function() {
//      const selection = view.state.selection.main;
//      const text = view.state.sliceDoc(selection.from, selection.to);
//      navigator.clipboard.writeText(text).catch(e => console.error("Copy failed:", e));
//  };
//
//  window.performPaste = async function() {
//      try {
//          const text = await navigator.clipboard.readText();
//          view.dispatch(view.state.replaceSelection(text));
//          view.focus();
//      } catch(e) {
//          console.error("Paste failed:", e);
//      }
//  };
//
//  window.performCut = function() {
//      const selection = view.state.selection.main;
//      const text = view.state.sliceDoc(selection.from, selection.to);
//      navigator.clipboard.writeText(text);
//      view.dispatch({
//          changes: { from: selection.from, to: selection.to }
//      });
//  };
//
//  window.performUndo = function() {
//      undo(view);
//  };
//
//  window.performRedo = function() {
//      redo(view);
//  };
//
//  window.performSelectAll = function() {
//      view.dispatch({
//          selection: { anchor: 0, head: view.state.doc.length }
//      });
//  };
//
//  window.performFind = function() {
//      openSearchPanel(view);
//  };
//
//  window.closeFindPanel = function() {
//      closeSearchPanel(view);
//  };
//
//  function parseJSONMessages(text) {
//    const messages = [];
//    let depth = 0;
//    let inString = false;
//    let escape = false;
//    let startIdx = -1;
//    let i = 0;
//
//    while (i < text.length) {
//      const char = text[i];
//
//      if (escape) {
//        escape = false;
//        i++;
//        continue;
//      }
//
//      if (char === '\\' && inString) {
//        escape = true;
//        i++;
//        continue;
//      }
//
//      if (char === '"' && !escape) {
//        inString = !inString;
//        i++;
//        continue;
//      }
//
//      if (inString) {
//        i++;
//        continue;
//      }
//
//      if (char === '{') {
//        if (depth === 0) startIdx = i;
//        depth++;
//      } else if (char === '}') {
//        depth--;
//        if (depth === 0 && startIdx !== -1) {
//          const jsonStr = text.substring(startIdx, i + 1);
//          try {
//            const parsed = JSON.parse(jsonStr);
//            messages.push(parsed);
//          } catch (e) {
//            console.error('Failed to parse JSON:', jsonStr.substring(0, 100));
//          }
//          startIdx = -1;
//        }
//      }
//
//      i++;
//    }
//
//    return {
//      messages,
//      remaining: startIdx !== -1 ? text.substring(startIdx) : ''
//    };
//  }
//
//  ws.onopen = () => {
//    console.log('✓ Connected to LSP server');
//    sendRequest(ws, 'initialize', {
//      processId: null,
//      rootUri: rootURI,
//      capabilities: {
//        textDocument: {
//          completion: {
//            completionItem: {
//              snippetSupport: false,
//              documentationFormat: ['plaintext', 'markdown']
//            },
//            contextSupport: true
//          },
//          publishDiagnostics: {},
//          hover: {
//            contentFormat: ['plaintext']
//          }
//        }
//      }
//    }).then((result) => {
//      console.log('✅ Server initialized:', result.serverInfo?.name);
//      sendNotification(ws, 'initialized', {});
//      sendNotification(ws, 'textDocument/didOpen', {
//        textDocument: {
//          uri: URI,
//          languageId: 'cpp',
//          version: 1,
//          text: view?.state.doc.toString() || ''
//        }
//      });
//    }).catch(err => {
//      console.error('Initialize error:', err);
//    });
//  };
//
//  ws.onerror = (error) => {
//    console.error('WebSocket error:', error)
//  }
//
//  ws.onmessage = (event) => {
//    try {
//      const data = event.data.toString();
//      messageBuffer += data;
//      messageBuffer = messageBuffer.replace(/Content-Length:\s*\d+\s*/g, '');
//
//      const { messages, remaining } = parseJSONMessages(messageBuffer);
//      messageBuffer = remaining;
//
//      for (const message of messages) {
//        if (message.id !== undefined && pendingRequests.has(message.id)) {
//          const { resolve, reject, timeoutId } = pendingRequests.get(message.id);
//          clearTimeout(timeoutId);
//          pendingRequests.delete(message.id);
//
//          if (message.error) {
//            console.error('❌ LSP Error:', message.error);
//            reject(message.error);
//          } else {
//            console.log(`✓ Response for request ${message.id}`);
//            resolve(message.result);
//          }
//        }
//
//        if (message.method === 'textDocument/publishDiagnostics') {
//          handleDiagnostics(message.params);
//        }
//      }
//    } catch (e) {
//      console.error('Error processing WebSocket message:', e);
//    }
//  };
//
//  function sendRequest(ws, method, params, timeout = 10000) {
//    return new Promise((resolve, reject) => {
//      const id = messageId++;
//      const message = {
//        jsonrpc: '2.0',
//        id,
//        method,
//        params
//      };
//
//      const timeoutId = setTimeout(() => {
//        if (pendingRequests.has(id)) {
//          pendingRequests.delete(id);
//          console.error(`⏱️ Request ${id} (${method}) timed out`);
//          reject(new Error(`Request timeout: ${method}`));
//        }
//      }, timeout);
//
//      pendingRequests.set(id, { resolve, reject, timeoutId });
//
//      if (ws.readyState === WebSocket.OPEN) {
//        console.log(`→ Request ${id}: ${method}`);
//        ws.send(JSON.stringify(message));
//      } else {
//        clearTimeout(timeoutId);
//        pendingRequests.delete(id);
//        reject(new Error('WebSocket not open'));
//      }
//    });
//  }
//
//  function sendNotification(ws, method, params) {
//    const message = {
//      jsonrpc: '2.0',
//      method,
//      params
//    };
//
//    if (ws.readyState === WebSocket.OPEN) {
//      ws.send(JSON.stringify(message));
//    }
//  }
//}
//
//setupEditor();

// Imports
import { EditorView, basicSetup } from "codemirror"
import { EditorState, StateEffect, StateField } from "@codemirror/state"
import { oneDark } from "@codemirror/theme-one-dark"
import { autocompletion, completionKeymap, closeCompletion, startCompletion, completionStatus } from "@codemirror/autocomplete"
import { keymap } from "@codemirror/view"
import { linter, lintGutter } from "@codemirror/lint"
import { cpp } from "@codemirror/lang-cpp"
import { insertNewlineAndIndent, deleteCharBackward } from "@codemirror/commands"

window.code_ = ""

function setupEditor() {
  const ws = new WebSocket("ws://localhost:8081/clsp")
  let view = null

  let lastCompletionTime = 0
  let URI = "file:///main.cpp"
  let rootURI = "file:///"
  let pendingRequests = new Map()
  let messageId = 0

  let messageBuffer = '';
  let currentDiagnostics = []; // Store current diagnostics

  const setFileUri = (uri) => {URI = uri}
  const setRootUri = (uri) => {rootURI = uri}

  function handleDiagnostics(params) {
    if(!view) return
    try {
      const diagnostics = []
      for(const d of params.diagnostics) {
        try {
          if(d.range.start.line < 0 || d.range.end.line < 0) continue
          if(d.range.start.line + 1 > view.state.doc.lines || d.range.end.line + 1 > view.state.doc.lines) continue;

          const startLine = view.state.doc.line(d.range.start.line + 1)
          const endLine = view.state.doc.line(d.range.end.line + 1)
          const startChar = Math.min(d.range.start.character, startLine.length)
          const endChar = Math.min(d.range.end.character, endLine.length)
          const from = startLine.from + startChar
          const to = endLine.from + endChar

          if(to < from) continue

          diagnostics.push({
            from: from,
            to: Math.max(from + 1, to),
            severity: d.severity === 1 ? 'error' :
                      d.severity === 2 ? 'warning' : 'info',
            message: d.message || 'Unknown error',
            source: d.source || 'clangd'
          })
        } catch(e) {
          console.error('Error processing diagnostic:', e)
        }
      }

      console.log(`📊 ${diagnostics.length} diagnostics`);

      // Store diagnostics - they will be returned by the linter function
      currentDiagnostics = diagnostics;

      // Force linter update by triggering a view update
      view.dispatch({})
    } catch(e) {
      console.error('Error in handleDiagnostics:', e)
    }
  }

  async function lspCompletionSource(context) {
    const now = Date.now()

    // Always allow explicit completions
    if(!context.explicit) {
      // For automatic completions, check timing
      if(now - lastCompletionTime < 200) {
        return null
      }
    }

    lastCompletionTime = now;

    const line = context.state.doc.lineAt(context.pos)
    const lineNumber = line.number - 1
    const character = context.pos - line.from

    try {
      const result = await sendRequest(ws, 'textDocument/completion', {
        textDocument: {uri: URI},
        position: {line: lineNumber, character},
        context: {
          triggerKind: context.explicit ? 1 : 2,
        }
      }, 5000)

      if(!result) {
        return null
      }

      const items = result.items || result
      if(!Array.isArray(items) || items.length === 0) {
        return null
      }

      console.log(`✨ Got ${items.length} completion items`)

      const options = items.slice(0, 50).map(item => {
        const cleanLabel = item.label.replace(/^[•\s]+/, '').trim()
        let textToInsert = cleanLabel

        if(item.insertText) textToInsert = item.insertText
        else if(item.textEdit && item.textEdit.newText) textToInsert = item.textEdit.newText

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
          apply: textToInsert
        }
      })

      const wordBefore = context.matchBefore(/\w*/)
      return {
        from: wordBefore ? wordBefore.from : context.pos,
        options,
        validFor: /^\w*$/
      }
    } catch(e) {
      console.error('Completion error:', e)
      return null
    }
  }

  // THIS IS THE KEY! Linter function returns the stored diagnostics
  const lspLinter = linter((view) => {
    // Return a copy of current diagnostics
    // This ensures diagnostics persist across document changes
    return currentDiagnostics.filter(d => {
      // Validate diagnostics are still within document bounds
      return d.from >= 0 &&
             d.to <= view.state.doc.length &&
             d.from < d.to
    })
  })

  const startState = EditorState.create({
    doc: "",
    extensions: [
      basicSetup,
      oneDark,
      cpp(),
      lspLinter, // Use the linter that returns stored diagnostics
      lintGutter(), // Required for squiggly lines and gutter markers
      autocompletion({
        override: [lspCompletionSource],
        activateOnTyping: true,
        defaultKeymap: true,
        maxRenderedOptions: 50,
        closeOnBlur: false
      }),
      keymap.of([
        ...completionKeymap,
        {
          key: `Escape`,
          run: closeCompletion
        }
      ]),
      EditorView.updateListener.of((update) => {
        if(update.docChanged) window.code_ = update.state.doc.toString()

        if(update.docChanged && ws.readyState === WebSocket.OPEN && view) {
          sendNotification(ws, 'textDocument/didChange', {
            textDocument: {
              uri: URI,
              version: Date.now()
            },
            contentChanges: [{text: update.state.doc.toString()}]
          })
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

  window.insertText = function(text) {
    view.dispatch(view.state.replaceSelection(text));
    view.focus();
  };

  window.performBackspace = function() {
    deleteCharBackward(view);
    view.focus();
  };

  window.performEnter = function() {
     insertNewlineAndIndent(view);
     view.focus();
  };

  window.performCopy = function() {
      const selection = view.state.selection.main;
      const text = view.state.sliceDoc(selection.from, selection.to);
      navigator.clipboard.writeText(text).catch(e => console.error("Copy failed:", e));
  };

  window.performPaste = async function() {
      try {
          const text = await navigator.clipboard.readText();
          view.dispatch(view.state.replaceSelection(text));
          view.focus();
      } catch(e) {
          console.error("Paste failed:", e);
      }
  };

  window.performCut = function() {
      const selection = view.state.selection.main;
      const text = view.state.sliceDoc(selection.from, selection.to);
      navigator.clipboard.writeText(text);
      view.dispatch({
          changes: { from: selection.from, to: selection.to }
      });
  };

  window.performUndo = function() {
      undo(view);
  };

  window.performRedo = function() {
      redo(view);
  };

  window.performSelectAll = function() {
      view.dispatch({
          selection: { anchor: 0, head: view.state.doc.length }
      });
  };

  window.performFind = function() {
      openSearchPanel(view);
  };

  window.closeFindPanel = function() {
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
    console.log('✓ Connected to LSP server');
    sendRequest(ws, 'initialize', {
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
    }).then((result) => {
      console.log('✅ Server initialized:', result.serverInfo?.name);
      sendNotification(ws, 'initialized', {});
      sendNotification(ws, 'textDocument/didOpen', {
        textDocument: {
          uri: URI,
          languageId: 'cpp',
          version: 1,
          text: view?.state.doc.toString() || ''
        }
      });
    }).catch(err => {
      console.error('Initialize error:', err);
    });
  };

  ws.onerror = (error) => {
    console.error('WebSocket error:', error)
  }

  ws.onmessage = (event) => {
    try {
      const data = event.data.toString();
      messageBuffer += data;
      messageBuffer = messageBuffer.replace(/Content-Length:\s*\d+\s*/g, '');

      const { messages, remaining } = parseJSONMessages(messageBuffer);
      messageBuffer = remaining;

      for (const message of messages) {
        if (message.id !== undefined && pendingRequests.has(message.id)) {
          const { resolve, reject, timeoutId } = pendingRequests.get(message.id);
          clearTimeout(timeoutId);
          pendingRequests.delete(message.id);

          if (message.error) {
            console.error('❌ LSP Error:', message.error);
            reject(message.error);
          } else {
            console.log(`✓ Response for request ${message.id}`);
            resolve(message.result);
          }
        }

        if (message.method === 'textDocument/publishDiagnostics') {
          handleDiagnostics(message.params);
        }
      }
    } catch (e) {
      console.error('Error processing WebSocket message:', e);
    }
  };

  function sendRequest(ws, method, params, timeout = 10000) {
    return new Promise((resolve, reject) => {
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

      if (ws.readyState === WebSocket.OPEN) {
        console.log(`→ Request ${id}: ${method}`);
        ws.send(JSON.stringify(message));
      } else {
        clearTimeout(timeoutId);
        pendingRequests.delete(id);
        reject(new Error('WebSocket not open'));
      }
    });
  }

  function sendNotification(ws, method, params) {
    const message = {
      jsonrpc: '2.0',
      method,
      params
    };

    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(message));
    }
  }
}

setupEditor();