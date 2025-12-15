import { EditorView, basicSetup } from "codemirror"
import { EditorState } from "@codemirror/state"
import { oneDark } from "@codemirror/theme-one-dark"
import { cpp } from "@codemirror/lang-cpp"

// Global state
let currentEditor = null;
let resultEditor = null;
let incomingEditor = null;
let currentFilePath = "";
let originalConflictContent = "";

// Parsed conflict sections
let currentContent = "";
let incomingContent = "";

/**
 * Create a CodeMirror editor
 */
function createEditor(container, content, readOnly = false) {
    const extensions = [
        basicSetup,
        oneDark,
        EditorView.theme({
            "&": { height: "100%" },
            ".cm-scroller": { overflow: "auto" },
            ".cm-content": { padding: "8px 0" }
        })
    ];

    if (readOnly) {
        extensions.push(EditorState.readOnly.of(true));
        extensions.push(EditorView.editable.of(false));
    }

    const state = EditorState.create({
        doc: content,
        extensions
    });

    return new EditorView({
        state,
        parent: container
    });
}

/**
 * Parse conflict markers from file content
 * Returns { current, incoming, base }
 */
function parseConflictMarkers(content) {
    const lines = content.split('\n');
    let current = [];
    let incoming = [];
    let base = [];
    let merged = [];

    let inCurrent = false;
    let inIncoming = false;
    let inBase = false;
    let hasConflict = false;

    for (const line of lines) {
        if (line.startsWith('<<<<<<<')) {
            hasConflict = true;
            inCurrent = true;
            inIncoming = false;
            inBase = false;
            continue;
        }
        if (line.startsWith('|||||||')) {
            inCurrent = false;
            inBase = true;
            inIncoming = false;
            continue;
        }
        if (line.startsWith('=======')) {
            inCurrent = false;
            inBase = false;
            inIncoming = true;
            continue;
        }
        if (line.startsWith('>>>>>>>')) {
            inCurrent = false;
            inIncoming = false;
            inBase = false;
            // Add placeholder for conflict in merged
            merged.push('// TODO: Resolve conflict');
            continue;
        }

        if (inCurrent) {
            current.push(line);
        } else if (inIncoming) {
            incoming.push(line);
        } else if (inBase) {
            base.push(line);
        } else {
            // Non-conflict line - add to all
            current.push(line);
            incoming.push(line);
            merged.push(line);
        }
    }

    return {
        current: current.join('\n'),
        incoming: incoming.join('\n'),
        base: base.join('\n'),
        merged: merged.join('\n'),
        hasConflict
    };
}

/**
 * Set merge content - called from Android
 * @param {string} conflictContent - Full file content with conflict markers
 * @param {string} filePath - File path
 */
window.setMergeContent = function (conflictContent, filePath) {
    currentFilePath = filePath;
    originalConflictContent = conflictContent;

    // Update filename
    const filename = filePath.split('/').pop();
    document.getElementById('merge-filename').textContent = `Resolving: ${filename}`;

    // Parse conflict markers
    const parsed = parseConflictMarkers(conflictContent);
    currentContent = parsed.current;
    incomingContent = parsed.incoming;

    // Clear existing editors
    document.getElementById('current-editor').innerHTML = '';
    document.getElementById('result-editor').innerHTML = '';
    document.getElementById('incoming-editor').innerHTML = '';

    // Create editors
    currentEditor = createEditor(
        document.getElementById('current-editor'),
        currentContent,
        true  // read-only
    );

    resultEditor = createEditor(
        document.getElementById('result-editor'),
        parsed.merged,
        false  // editable
    );

    incomingEditor = createEditor(
        document.getElementById('incoming-editor'),
        incomingContent,
        true  // read-only
    );

    console.log('[MergeEditor] Content loaded, hasConflict:', parsed.hasConflict);
};

/**
 * Set separate content for current and incoming - called from Android
 */
window.setMergeContentSeparate = function (currentCode, incomingCode, filePath) {
    currentFilePath = filePath;
    currentContent = currentCode;
    incomingContent = incomingCode;

    // Update filename
    const filename = filePath.split('/').pop();
    document.getElementById('merge-filename').textContent = `Resolving: ${filename}`;

    // Clear existing editors
    document.getElementById('current-editor').innerHTML = '';
    document.getElementById('result-editor').innerHTML = '';
    document.getElementById('incoming-editor').innerHTML = '';

    // Create editors
    currentEditor = createEditor(
        document.getElementById('current-editor'),
        currentCode,
        true
    );

    // Start with current as the merged result
    resultEditor = createEditor(
        document.getElementById('result-editor'),
        currentCode,
        false
    );

    incomingEditor = createEditor(
        document.getElementById('incoming-editor'),
        incomingCode,
        true
    );
};

/**
 * Accept current (HEAD) version
 */
window.acceptCurrent = function () {
    if (!resultEditor) return;

    resultEditor.dispatch({
        changes: {
            from: 0,
            to: resultEditor.state.doc.length,
            insert: currentContent
        }
    });

    console.log('[MergeEditor] Accepted current');
};

/**
 * Accept incoming version
 */
window.acceptIncoming = function () {
    if (!resultEditor) return;

    resultEditor.dispatch({
        changes: {
            from: 0,
            to: resultEditor.state.doc.length,
            insert: incomingContent
        }
    });

    console.log('[MergeEditor] Accepted incoming');
};

/**
 * Accept both versions (current first, then incoming)
 */
window.acceptBoth = function () {
    if (!resultEditor) return;

    const combined = currentContent + '\n' + incomingContent;

    resultEditor.dispatch({
        changes: {
            from: 0,
            to: resultEditor.state.doc.length,
            insert: combined
        }
    });

    console.log('[MergeEditor] Accepted both');
};

/**
 * Get the merged content
 */
window.getMergedContent = function () {
    if (!resultEditor) return '';
    return resultEditor.state.doc.toString();
};

/**
 * Save the merged result - notify Android
 */
window.saveMerge = function () {
    const content = window.getMergedContent();

    if (window.AndroidMerge) {
        window.AndroidMerge.onSave(currentFilePath, content);
    }

    console.log('[MergeEditor] Save requested');
};

/**
 * Close the merge editor - notify Android
 */
window.closeMergeEditor = function () {
    if (window.AndroidMerge) {
        window.AndroidMerge.onClose();
    }
};

/**
 * Get current file path
 */
window.getFilePath = function () {
    return currentFilePath;
};

// Signal that the merge editor is ready
window.isMergeEditorReady = true;
console.log('[MergeEditor] Merge editor initialized');
