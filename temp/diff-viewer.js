import { EditorView, basicSetup } from "codemirror"
import { EditorState, RangeSetBuilder } from "@codemirror/state"
import { oneDark } from "@codemirror/theme-one-dark"
import { Decoration, ViewPlugin } from "@codemirror/view"
import { cpp } from "@codemirror/lang-cpp"


// Global state
let currentEditor = null;
let incomingEditor = null;
let currentFilePath = "";

// Line decoration for added lines
const addedLineDecoration = Decoration.line({ class: "diff-line-added" });
const removedLineDecoration = Decoration.line({ class: "diff-line-removed" });
const unchangedLineDecoration = Decoration.line({ class: "diff-line-unchanged" });

/**
 * Create a read-only CodeMirror editor for diff viewing
 */
function createDiffEditor(container, content, lineTypes = []) {
    // Create line decorations based on diff type
    const decorateLines = ViewPlugin.fromClass(class {
        decorations;

        constructor(view) {
            this.decorations = this.buildDecorations(view);
        }

        update(update) {
            if (update.docChanged || update.viewportChanged) {
                this.decorations = this.buildDecorations(update.view);
            }
        }

        buildDecorations(view) {
            const builder = new RangeSetBuilder();
            for (let i = 1; i <= view.state.doc.lines; i++) {
                const line = view.state.doc.line(i);
                const type = lineTypes[i - 1] || 'unchanged';

                if (type === 'added') {
                    builder.add(line.from, line.from, addedLineDecoration);
                } else if (type === 'removed') {
                    builder.add(line.from, line.from, removedLineDecoration);
                }
            }
            return builder.finish();
        }
    }, {
        decorations: v => v.decorations
    });

    const state = EditorState.create({
        doc: content,
        extensions: [
            basicSetup,
            oneDark,
            EditorState.readOnly.of(true),
            EditorView.editable.of(false),
            decorateLines,
            EditorView.theme({
                "&": { height: "100%" },
                ".cm-scroller": { overflow: "auto" },
                ".cm-content": { padding: "8px 0" },
                ".cm-line": { paddingLeft: "24px" }
            })
        ]
    });

    return new EditorView({
        state,
        parent: container
    });
}

/**
 * Set diff content - called from Android
 * @param {string} currentCode - Current version (HEAD) content
 * @param {string} incomingCode - Incoming version content
 * @param {string} filePath - File path for display
 */
window.setDiffContent = function (currentCode, incomingCode, filePath) {
    currentFilePath = filePath;

    // Update filename displays
    const filename = filePath.split('/').pop();
    document.getElementById('current-filename').textContent = filename;
    document.getElementById('incoming-filename').textContent = filename;

    // Clear existing editors
    const currentContainer = document.getElementById('current-editor');
    const incomingContainer = document.getElementById('incoming-editor');
    currentContainer.innerHTML = '';
    incomingContainer.innerHTML = '';

    // Compute line-level diff
    const currentLines = currentCode.split('\n');
    const incomingLines = incomingCode.split('\n');
    const { currentTypes, incomingTypes } = computeLineDiff(currentLines, incomingLines);

    // Create editors with diff decorations
    currentEditor = createDiffEditor(currentContainer, currentCode, currentTypes);
    incomingEditor = createDiffEditor(incomingContainer, incomingCode, incomingTypes);

    // Sync scrolling
    setupScrollSync(currentEditor, incomingEditor);
};

/**
 * Simple line-level diff computation
 */
function computeLineDiff(currentLines, incomingLines) {
    const currentTypes = [];
    const incomingTypes = [];

    // Build a set of lines from each side
    const currentSet = new Set(currentLines);
    const incomingSet = new Set(incomingLines);

    // Mark lines in current that don't exist in incoming as "removed"
    for (const line of currentLines) {
        if (!incomingSet.has(line)) {
            currentTypes.push('removed');
        } else {
            currentTypes.push('unchanged');
        }
    }

    // Mark lines in incoming that don't exist in current as "added"
    for (const line of incomingLines) {
        if (!currentSet.has(line)) {
            incomingTypes.push('added');
        } else {
            incomingTypes.push('unchanged');
        }
    }

    return { currentTypes, incomingTypes };
}

/**
 * Synchronize scrolling between two editors
 */
function setupScrollSync(editor1, editor2) {
    let syncing = false;

    editor1.dom.addEventListener('scroll', () => {
        if (syncing) return;
        syncing = true;
        const scrollTop = editor1.scrollDOM.scrollTop;
        const scrollRatio = scrollTop / (editor1.scrollDOM.scrollHeight - editor1.scrollDOM.clientHeight);
        const targetScroll = scrollRatio * (editor2.scrollDOM.scrollHeight - editor2.scrollDOM.clientHeight);
        editor2.scrollDOM.scrollTop = targetScroll;
        requestAnimationFrame(() => syncing = false);
    });

    editor2.dom.addEventListener('scroll', () => {
        if (syncing) return;
        syncing = true;
        const scrollTop = editor2.scrollDOM.scrollTop;
        const scrollRatio = scrollTop / (editor2.scrollDOM.scrollHeight - editor2.scrollDOM.clientHeight);
        const targetScroll = scrollRatio * (editor1.scrollDOM.scrollHeight - editor1.scrollDOM.clientHeight);
        editor1.scrollDOM.scrollTop = targetScroll;
        requestAnimationFrame(() => syncing = false);
    });
}

/**
 * Get current file path
 */
window.getFilePath = function () {
    return currentFilePath;
};

/**
 * Close the diff viewer - notify Android
 */
window.closeDiffViewer = function () {
    if (window.AndroidDiff) {
        window.AndroidDiff.onClose();
    }
};

// Signal that the diff viewer is ready
window.isDiffViewerReady = true;
console.log('[DiffViewer] Diff viewer initialized');
