import { EditorView, basicSetup } from "codemirror"
import { EditorState, StateField, StateEffect, RangeSetBuilder } from "@codemirror/state"
import { oneDark } from "@codemirror/theme-one-dark"
import { Decoration, ViewPlugin, WidgetType } from "@codemirror/view"

// Global state
let editor = null;
let currentFilePath = "";

// Conflict block tracking
const conflicts = [];

/**
 * Widget for conflict resolution buttons
 */
class ConflictWidget extends WidgetType {
    constructor(conflictIndex, type) {
        super();
        this.conflictIndex = conflictIndex;
        this.type = type; // 'start', 'separator', 'end'
    }

    toDOM() {
        if (this.type === 'start') {
            const wrapper = document.createElement('div');
            wrapper.className = 'conflict-widget';

            const currentBtn = document.createElement('button');
            currentBtn.className = 'accept-current';
            currentBtn.textContent = '✓ Accept Current';
            currentBtn.onclick = () => window.resolveConflict(this.conflictIndex, 'current');

            const incomingBtn = document.createElement('button');
            incomingBtn.className = 'accept-incoming';
            incomingBtn.textContent = '✓ Accept Incoming';
            incomingBtn.onclick = () => window.resolveConflict(this.conflictIndex, 'incoming');

            const bothBtn = document.createElement('button');
            bothBtn.className = 'accept-both';
            bothBtn.textContent = '⊕ Accept Both';
            bothBtn.onclick = () => window.resolveConflict(this.conflictIndex, 'both');

            wrapper.appendChild(currentBtn);
            wrapper.appendChild(incomingBtn);
            wrapper.appendChild(bothBtn);

            return wrapper;
        }
        return document.createElement('span');
    }

    ignoreEvent() { return true; }
}

// Line decoration classes
const conflictMarkerDeco = Decoration.line({ class: "conflict-marker-line" });
const currentChangeDeco = Decoration.line({ class: "current-change-line" });
const incomingChangeDeco = Decoration.line({ class: "incoming-change-line" });

/**
 * Parse conflict markers and return conflict info
 */
function parseConflicts(content) {
    const lines = content.split('\n');
    const foundConflicts = [];
    let currentConflict = null;

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];

        if (line.startsWith('<<<<<<<')) {
            currentConflict = {
                startLine: i,
                separatorLine: -1,
                endLine: -1,
                currentStart: i + 1,
                currentEnd: -1,
                incomingStart: -1,
                incomingEnd: -1
            };
        } else if (line.startsWith('=======') && currentConflict) {
            currentConflict.separatorLine = i;
            currentConflict.currentEnd = i - 1;
            currentConflict.incomingStart = i + 1;
        } else if (line.startsWith('>>>>>>>') && currentConflict) {
            currentConflict.endLine = i;
            currentConflict.incomingEnd = i - 1;
            foundConflicts.push(currentConflict);
            currentConflict = null;
        }
    }

    return foundConflicts;
}

/**
 * Create decorations for conflict highlighting
 */
function createConflictDecorations(view) {
    const content = view.state.doc.toString();
    const foundConflicts = parseConflicts(content);

    // Store for later use
    conflicts.length = 0;
    conflicts.push(...foundConflicts);

    // Collect all decorations with their positions
    const decorations = [];

    for (let ci = 0; ci < foundConflicts.length; ci++) {
        const c = foundConflicts[ci];

        // Start marker line + widget
        if (c.startLine < view.state.doc.lines) {
            const startPos = view.state.doc.line(c.startLine + 1).from;
            decorations.push({ pos: startPos, side: 0, deco: conflictMarkerDeco });
            decorations.push({
                pos: startPos,
                side: 1,
                deco: Decoration.widget({
                    widget: new ConflictWidget(ci, 'start'),
                    side: 1
                })
            });
        }

        // Current change lines (between start and separator)
        for (let i = c.currentStart; i <= c.currentEnd && i < view.state.doc.lines; i++) {
            const pos = view.state.doc.line(i + 1).from;
            decorations.push({ pos: pos, side: 0, deco: currentChangeDeco });
        }

        // Separator marker
        if (c.separatorLine >= 0 && c.separatorLine < view.state.doc.lines) {
            const sepPos = view.state.doc.line(c.separatorLine + 1).from;
            decorations.push({ pos: sepPos, side: 0, deco: conflictMarkerDeco });
        }

        // Incoming change lines (between separator and end)
        for (let i = c.incomingStart; i <= c.incomingEnd && i < view.state.doc.lines; i++) {
            const pos = view.state.doc.line(i + 1).from;
            decorations.push({ pos: pos, side: 0, deco: incomingChangeDeco });
        }

        // End marker
        if (c.endLine >= 0 && c.endLine < view.state.doc.lines) {
            const endPos = view.state.doc.line(c.endLine + 1).from;
            decorations.push({ pos: endPos, side: 0, deco: conflictMarkerDeco });
        }
    }

    // Sort by position, then by side
    decorations.sort((a, b) => {
        if (a.pos !== b.pos) return a.pos - b.pos;
        return a.side - b.side;
    });

    // Add to builder in sorted order
    const builder = new RangeSetBuilder();
    for (const d of decorations) {
        builder.add(d.pos, d.pos, d.deco);
    }

    return builder.finish();
}

/**
 * Plugin for conflict decorations
 */
const conflictPlugin = ViewPlugin.fromClass(class {
    decorations;

    constructor(view) {
        this.decorations = createConflictDecorations(view);
    }

    update(update) {
        if (update.docChanged) {
            this.decorations = createConflictDecorations(update.view);
        }
    }
}, {
    decorations: v => v.decorations
});

/**
 * Create the CodeMirror editor
 */
function createEditor(container, content) {
    const state = EditorState.create({
        doc: content,
        extensions: [
            basicSetup,
            oneDark,
            conflictPlugin,
            EditorView.theme({
                "&": { height: "100%" },
                ".cm-scroller": { overflow: "auto" },
                ".cm-content": { padding: "8px 0" }
            })
        ]
    });

    return new EditorView({
        state,
        parent: container
    });
}

/**
 * Set merge content - called from Android
 */
window.setMergeContent = function (conflictContent, filePath) {
    console.log('[MergeEditor] setMergeContent called with path:', filePath);
    console.log('[MergeEditor] Content length:', conflictContent ? conflictContent.length : 0);
    console.log('[MergeEditor] Content preview:', conflictContent ? conflictContent.substring(0, 100) : 'null');

    try {
        currentFilePath = filePath;

        // Update filename
        const filenameEl = document.getElementById('merge-filename');
        if (!filenameEl) {
            console.error('[MergeEditor] ERROR: merge-filename element not found!');
            return;
        }
        const filename = filePath.split('/').pop();
        filenameEl.textContent = `Resolving: ${filename}`;
        console.log('[MergeEditor] Filename updated to:', filename);

        // Clear and create editor
        const container = document.getElementById('editor');
        if (!container) {
            console.error('[MergeEditor] ERROR: editor container element not found!');
            return;
        }
        console.log('[MergeEditor] Editor container found, clearing...');
        container.innerHTML = '';

        console.log('[MergeEditor] Creating CodeMirror editor...');
        editor = createEditor(container, conflictContent);
        console.log('[MergeEditor] Editor created successfully');

        console.log('[MergeEditor] Content loaded with', conflicts.length, 'conflicts');
    } catch (e) {
        console.error('[MergeEditor] ERROR in setMergeContent:', e.message);
        console.error('[MergeEditor] Stack:', e.stack);
    }
};

/**
 * Resolve a specific conflict
 */
window.resolveConflict = function (conflictIndex, resolution) {
    if (!editor || conflictIndex >= conflicts.length) return;

    const c = conflicts[conflictIndex];
    const doc = editor.state.doc;
    const lines = doc.toString().split('\n');

    // Get the content for each section
    const currentContent = lines.slice(c.currentStart, c.currentEnd + 1).join('\n');
    const incomingContent = lines.slice(c.incomingStart, c.incomingEnd + 1).join('\n');

    // Determine replacement
    let replacement = '';
    switch (resolution) {
        case 'current':
            replacement = currentContent;
            break;
        case 'incoming':
            replacement = incomingContent;
            break;
        case 'both':
            replacement = currentContent + '\n' + incomingContent;
            break;
    }

    // Calculate positions
    const startPos = doc.line(c.startLine + 1).from;
    const endPos = doc.line(c.endLine + 1).to;

    // Apply the change
    editor.dispatch({
        changes: {
            from: startPos,
            to: endPos,
            insert: replacement
        }
    });

    console.log('[MergeEditor] Resolved conflict', conflictIndex, 'with', resolution);
};

/**
 * Get the current content
 */
window.getMergedContent = function () {
    if (!editor) return '';
    return editor.state.doc.toString();
};

/**
 * Check if there are remaining conflicts
 */
window.hasConflicts = function () {
    if (!editor) return false;
    const content = editor.state.doc.toString();
    return content.includes('<<<<<<<') && content.includes('>>>>>>>');
};

/**
 * Save the merged result - notify Android
 */
window.saveMerge = function () {
    const content = window.getMergedContent();

    if (window.hasConflicts()) {
        // Warn about remaining conflicts
        console.log('[MergeEditor] Warning: File still has unresolved conflicts');
    }

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
console.log('[MergeEditor] Inline merge editor initialized');
