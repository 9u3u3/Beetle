/**
 * Python Notebook - Google Colab-like interface for .py files
 * Uses # %% as cell delimiters
 */

import { EditorView, basicSetup } from "codemirror"
import { EditorState, Compartment } from "@codemirror/state"
import { python } from "@codemirror/lang-python"
import { oneDark } from "@codemirror/theme-one-dark"
import { keymap } from "@codemirror/view"
import {
    deleteCharBackward,
    deleteCharForward,
    cursorCharLeft,
    cursorCharRight,
    cursorLineUp,
    cursorLineDown,
    cursorLineStart,
    cursorLineEnd,
    insertNewlineAndIndent,
    selectAll,
    undo,
    redo
} from "@codemirror/commands"

// =========================================
// CELL DATA MODEL
// =========================================

let cells = [];           // Array of cell objects
let selectedCellId = null;
let cellIdCounter = 0;
let currentFilePath = null;
let container = null;     // Will be set after DOM loads

// Generate unique cell ID
function generateCellId() {
    return `cell-${++cellIdCounter}`;
}

// Create a new cell object
function createCell(code = '', output = '', outputType = 'stdout') {
    const id = generateCellId();
    return {
        id: id,
        code: code,
        output: output,
        outputType: outputType,  // 'stdout', 'stderr', 'image'
        isRunning: false,
        executionTime: null,
        element: null,
        editor: null  // Will hold CM6 EditorView instance
    };
}

// =========================================
// FILE PARSING
// =========================================

// Cell delimiter patterns - support both "# %%" and "#%%" formats
const CELL_DELIMITER = '# %%';
const CELL_DELIMITER_REGEX = /^#\s*%%[^\n]*/gm;  // Matches # %%, #%%, # %% [markdown] etc.

// Parse .py file content into cells
function parseFileContent(content) {
    console.log('[Notebook] parseFileContent called with:', content ? content.substring(0, 100) : 'empty');

    if (!content || content.trim() === '') {
        console.log('[Notebook] Content empty, creating default cell');
        return [createCell()];
    }

    // Split by cell delimiter (# %% or #%%)
    const parts = content.split(CELL_DELIMITER_REGEX);
    const cellsData = [];

    console.log('[Notebook] Split into', parts.length, 'parts');

    parts.forEach((part, index) => {
        // Skip empty first part before first delimiter
        if (index === 0 && part.trim() === '') {
            return;
        }

        const code = part.trim();
        if (code || index > 0) {
            cellsData.push(createCell(code));
        }
    });

    // If no cells found, create one with the content
    if (cellsData.length === 0) {
        console.log('[Notebook] No cells found, using whole content');
        cellsData.push(createCell(content.trim()));
    }

    console.log('[Notebook] Parsed', cellsData.length, 'cells');
    return cellsData;
}

// Serialize cells back to file content
function serializeCells() {
    return cells.map((cell, index) => {
        const code = cell.editor ? cell.editor.state.doc.toString() : cell.code;
        if (index === 0) {
            return `${CELL_DELIMITER}\n${code}`;
        }
        return `\n${CELL_DELIMITER}\n${code}`;
    }).join('');
}

// =========================================
// CELL UI RENDERING
// =========================================

// Render all cells
function renderCells() {
    console.log('[Notebook] renderCells called, container:', container ? 'exists' : 'null');

    if (!container) {
        console.error('[Notebook] Container is null! Cannot render cells.');
        return;
    }

    // First, save current editor content and then destroy editors
    cells.forEach(cell => {
        if (cell.editor) {
            try {
                // Save current content before destroying
                cell.code = cell.editor.state.doc.toString();
                cell.editor.destroy();
            } catch (e) {
                console.warn('[Notebook] Error destroying editor:', e);
            }
            cell.editor = null;
        }
        cell.element = null;
    });

    // Clear container completely
    container.innerHTML = '';

    if (cells.length === 0) {
        console.log('[Notebook] No cells, showing empty state');
        renderEmptyState();
        return;
    }

    console.log('[Notebook] Rendering', cells.length, 'cells');

    // Create fresh elements for all cells
    cells.forEach((cell, index) => {
        cell.element = createCellElement(cell);

        // Update cell number
        const numSpan = cell.element.querySelector('.cell-number');
        if (numSpan) {
            numSpan.textContent = `[${index + 1}]`;
        }

        container.appendChild(cell.element);
        console.log('[Notebook] Rendered cell', index + 1, ':', cell.id);
    });
}

// Create empty state UI
function renderEmptyState() {
    const empty = document.createElement('div');
    empty.className = 'empty-notebook';
    empty.innerHTML = `
    <div class="empty-notebook-icon">📝</div>
    <div>No cells yet</div>
    <button id="empty-add-btn">Add First Cell</button>
  `;
    container.appendChild(empty);

    document.getElementById('empty-add-btn').addEventListener('click', () => {
        addCellAtEnd();
    });
}

// Create DOM element for a single cell
function createCellElement(cell) {
    const div = document.createElement('div');
    div.className = 'notebook-cell';
    div.id = cell.id;
    div.dataset.cellId = cell.id;

    if (cell.isRunning) {
        div.classList.add('running');
    }
    if (cell.id === selectedCellId) {
        div.classList.add('selected');
    }

    // Cell Header
    const header = document.createElement('div');
    header.className = 'cell-header';
    header.innerHTML = `
    <span class="cell-number">[-]</span>
    <div class="cell-header-spacer"></div>
    <button class="cell-btn run-btn" title="Run Cell (Shift+Enter)">▶</button>
    <button class="cell-btn clear-btn" title="Clear Output">⌫</button>
    <button class="cell-btn delete-btn" title="Delete Cell">✕</button>
  `;

    // Capture cell.id in a const to ensure correct closure
    const cellId = cell.id;

    // Run button handler
    header.querySelector('.run-btn').addEventListener('click', (e) => {
        e.stopPropagation();
        runCell(cellId);
    });

    // Clear output button handler
    header.querySelector('.clear-btn').addEventListener('click', (e) => {
        e.stopPropagation();
        clearCellOutput(cellId);
    });

    // Delete button handler
    header.querySelector('.delete-btn').addEventListener('click', (e) => {
        e.stopPropagation();
        deleteCell(cellId);
    });

    // Select cell on click
    div.addEventListener('click', () => {
        selectCell(cellId);
    });

    div.appendChild(header);

    // Code Editor Area
    const codeDiv = document.createElement('div');
    codeDiv.className = 'cell-code';
    div.appendChild(codeDiv);

    // Initialize CodeMirror editor for this cell
    try {
        // Create compartment for inputmode so we can reconfigure it dynamically
        const inputModeCompartment = new Compartment();
        cell.inputModeCompartment = inputModeCompartment; // Store for later reconfiguration

        // Check orientation - block IME in landscape, allow in portrait
        const isLandscape = window.innerWidth > window.innerHeight;
        const inputModeExtension = isLandscape
            ? EditorView.contentAttributes.of({ inputmode: "none" })
            : [];

        const startState = EditorState.create({
            doc: cell.code,
            extensions: [
                basicSetup,
                python(),
                oneDark,
                // Dynamic inputmode compartment for landscape/portrait switching
                inputModeCompartment.of(inputModeExtension),
                keymap.of([
                    {
                        key: "Shift-Enter",
                        run: () => {
                            runCell(cell.id);
                            return true;
                        }
                    }
                ]),
                EditorView.updateListener.of((update) => {
                    if (update.docChanged) {
                        cell.code = update.state.doc.toString();
                        // Signal content change to Android for BufferCard save indicator
                        if (window.AndroidNotebook && window.AndroidNotebook.onContentChanged) {
                            window.AndroidNotebook.onContentChanged();
                        }
                        // Trigger auto-save timer
                        triggerAutoSave();
                    }
                    if (update.focusChanged && update.view.hasFocus) {
                        selectCell(cell.id);
                    }
                })
            ]
        });

        cell.editor = new EditorView({
            state: startState,
            parent: codeDiv
        });

        console.log('[Notebook] Editor created for cell', cell.id);
    } catch (err) {
        console.error('[Notebook] Error creating editor:', err);
        codeDiv.textContent = cell.code || '# Error creating editor';
    }

    // Output Area
    const outputDiv = document.createElement('div');
    outputDiv.className = `cell-output ${cell.outputType}`;
    outputDiv.id = `output-${cell.id}`;

    if (cell.output) {
        outputDiv.classList.add('visible');

        if (cell.outputType === 'image') {
            const header = document.createElement('div');
            header.className = 'cell-output-header';
            if (cell.executionTime) {
                header.innerHTML = `<span>Output</span><span class="execution-time">${cell.executionTime}s</span>`;
            }
            outputDiv.appendChild(header);

            const img = document.createElement('img');
            img.src = cell.output;
            img.alt = 'Plot output';
            outputDiv.appendChild(img);
        } else {
            if (cell.executionTime) {
                const header = document.createElement('div');
                header.className = 'cell-output-header';
                header.innerHTML = `<span>Output</span><span class="execution-time">${cell.executionTime}s</span>`;
                outputDiv.appendChild(header);
            }
            const text = document.createElement('div');
            text.textContent = cell.output;
            outputDiv.appendChild(text);
        }
    }

    div.appendChild(outputDiv);

    // Click to select
    div.addEventListener('click', () => {
        selectCell(cell.id);
    });

    return div;
}

// =========================================
// CELL OPERATIONS
// =========================================

function selectCell(cellId) {
    document.querySelectorAll('.notebook-cell.selected').forEach(el => {
        el.classList.remove('selected');
    });

    selectedCellId = cellId;

    const cellEl = document.getElementById(cellId);
    if (cellEl) {
        cellEl.classList.add('selected');
        const cell = cells.find(c => c.id === cellId);
        if (cell && cell.editor) {
            // Only focus if not already focused to avoid infinite loop
            if (!cell.editor.hasFocus) {
                cell.editor.focus();
            }
        }
    }
}

// Focus the currently selected cell's editor (called from Android on tap)
window.focusSelectedCell = function () {
    if (selectedCellId) {
        const cell = cells.find(c => c.id === selectedCellId);
        if (cell && cell.editor) {
            cell.editor.focus();
        }
    } else if (cells.length > 0 && cells[0].editor) {
        // Focus first cell if none selected
        cells[0].editor.focus();
        selectedCellId = cells[0].id;
    }
};

function addCellAtEnd() {
    const newCell = createCell();
    cells.push(newCell);
    renderCells();
    selectCell(newCell.id);
    updateStatus(`Added cell ${cells.length}`);
}

function addCellAfter(cellId) {
    const index = cells.findIndex(c => c.id === cellId);
    if (index === -1) return;

    const newCell = createCell();
    cells.splice(index + 1, 0, newCell);
    renderCells();
    selectCell(newCell.id);
}

function clearCellOutput(cellId) {
    const cell = cells.find(c => c.id === cellId);
    if (!cell) return;

    cell.output = null;
    cell.outputType = 'stdout';
    cell.executionTime = null;
    cell.textOutput = null;
    updateCellUI(cell);
    updateStatus('Output cleared');
}

function deleteCell(cellId) {
    if (cells.length <= 1) {
        updateStatus('Cannot delete last cell');
        return;
    }

    const index = cells.findIndex(c => c.id === cellId);
    if (index === -1) return;

    // Remove from array - renderCells will handle cleanup
    cells.splice(index, 1);
    renderCells();

    const newIndex = Math.min(index, cells.length - 1);
    if (cells[newIndex]) {
        selectCell(cells[newIndex].id);
    }

    updateStatus(`Deleted cell, ${cells.length} remaining`);
}

function runCell(cellId) {
    const cell = cells.find(c => c.id === cellId);
    if (!cell) return;

    const code = cell.editor ? cell.editor.state.doc.toString() : cell.code;
    if (!code.trim()) {
        updateStatus('Cell is empty');
        return;
    }

    cell.isRunning = true;
    cell.output = '';
    cell.outputType = 'stdout';
    updateCellUI(cell);
    updateStatus(`Running cell...`);

    if (window.AndroidPython && window.AndroidPython.runCode) {
        window.AndroidPython.runCode(code, cellId);
    } else {
        console.log('[Notebook] AndroidPython not available, simulating...');
        setTimeout(() => {
            onPythonResult(cellId, `Simulated output for:\n${code}`, 'stdout', 0.1);
        }, 500);
    }
}

function runAllCells() {
    updateStatus('Running all cells...');
    runCellsSequentially(0);
}

function runCellsSequentially(index) {
    if (index >= cells.length) {
        updateStatus('All cells executed');
        return;
    }

    const cell = cells[index];
    const code = cell.editor ? cell.editor.state.doc.toString() : cell.code;

    if (!code.trim()) {
        runCellsSequentially(index + 1);
        return;
    }

    cell.isRunning = true;
    updateCellUI(cell);

    window._pendingRunAllIndex = index + 1;

    if (window.AndroidPython && window.AndroidPython.runCode) {
        window.AndroidPython.runCode(code, cell.id);
    } else {
        setTimeout(() => {
            onPythonResult(cell.id, `Output ${index + 1}`, 'stdout', 0.1);
        }, 300);
    }
}

function clearAllOutputs() {
    cells.forEach(cell => {
        cell.output = null;
        cell.outputType = 'stdout';
        cell.executionTime = null;
        cell.textOutput = null;
        updateCellUI(cell);
    });
    updateStatus('All outputs cleared');
}

// =========================================
// PYTHON RESULT CALLBACK
// =========================================

window.onPythonResult = function (cellId, output, outputType, executionTime) {
    console.log(`[Notebook] Result for ${cellId}: ${outputType}`);

    const cell = cells.find(c => c.id === cellId);
    if (!cell) return;

    cell.isRunning = false;

    // If outputType is 'image', check if we already have image data from onPlotImage
    // Don't overwrite image output with empty/text output
    if (outputType === 'image' && cell.outputType === 'image' && cell.output && cell.output.startsWith('data:image')) {
        // Keep the existing image, just add text output if any
        if (output && output.trim()) {
            // Append text output (e.g., print statements before plot)
            cell.textOutput = output;
        }
    } else {
        cell.output = output;
        cell.outputType = outputType;
    }

    cell.executionTime = executionTime ? executionTime.toFixed(2) : null;

    updateCellUI(cell);
    updateStatus(`Cell executed in ${cell.executionTime || '?'}s`);

    if (window._pendingRunAllIndex !== undefined) {
        const nextIndex = window._pendingRunAllIndex;
        window._pendingRunAllIndex = undefined;
        runCellsSequentially(nextIndex);
    }
};

window.onPlotImage = function (cellId, base64Data) {
    const cell = cells.find(c => c.id === cellId);
    if (!cell) return;

    cell.output = `data:image/png;base64,${base64Data}`;
    cell.outputType = 'image';
    updateCellUI(cell);
};

// =========================================
// UI UPDATE HELPERS
// =========================================

function updateCellUI(cell) {
    const cellEl = document.getElementById(cell.id);
    if (!cellEl) return;

    if (cell.isRunning) {
        cellEl.classList.add('running');
    } else {
        cellEl.classList.remove('running');
    }

    const outputEl = document.getElementById(`output-${cell.id}`);
    if (outputEl) {
        outputEl.innerHTML = '';
        outputEl.className = `cell-output ${cell.outputType}`;

        if (cell.output) {
            outputEl.classList.add('visible');

            if (cell.executionTime) {
                const header = document.createElement('div');
                header.className = 'cell-output-header';
                header.innerHTML = `<span>Output</span><span class="execution-time">${cell.executionTime}s</span>`;
                outputEl.appendChild(header);
            }

            if (cell.outputType === 'image') {
                const img = document.createElement('img');
                img.src = cell.output;
                img.alt = 'Plot output';
                outputEl.appendChild(img);
            } else {
                const text = document.createElement('div');
                text.textContent = cell.output;
                outputEl.appendChild(text);
            }
        } else {
            outputEl.classList.remove('visible');
        }
    }
}

function updateStatus(message) {
    const statusEl = document.getElementById('notebook-status');
    if (statusEl) {
        statusEl.textContent = message;
    }
    console.log(`[Notebook] Status: ${message}`);
}

// =========================================
// ANDROID INTERFACE FUNCTIONS
// =========================================

window.setFilePath = function (path) {
    currentFilePath = path;
    console.log(`[Notebook] File path set: ${path}`);
};

window.loadContent = function (content) {
    console.log(`[Notebook] loadContent called with ${content ? content.length : 0} chars`);
    console.log(`[Notebook] Container available: ${container ? 'yes' : 'no'}`);

    // Destroy existing editors and elements completely
    cells.forEach(cell => {
        if (cell.editor) {
            try {
                cell.editor.destroy();
            } catch (e) {
                console.warn('[Notebook] Error destroying editor:', e);
            }
        }
        if (cell.element) {
            cell.element.remove();
            cell.element = null;
        }
    });

    // Clear container completely for fresh start
    if (container) {
        container.innerHTML = '';
    }

    // Parse and render
    cells = parseFileContent(content);

    // Find the max cell ID number to prevent duplicates
    let maxId = 0;
    cells.forEach(cell => {
        const match = cell.id.match(/cell-(\d+)/);
        if (match) {
            const idNum = parseInt(match[1], 10);
            if (idNum > maxId) maxId = idNum;
        }
    });
    cellIdCounter = maxId;

    renderCells();

    // Select first cell
    if (cells.length > 0) {
        selectCell(cells[0].id);
    }

    updateStatus('Notebook loaded');
};

window.getContent = function () {
    return serializeCells();
};

// =========================================
// INITIALIZATION
// =========================================

function initNotebook() {
    console.log('[Notebook] initNotebook called');

    // Get container
    container = document.getElementById('notebook-container');
    console.log('[Notebook] Container element:', container ? 'found' : 'NOT FOUND');

    if (!container) {
        console.error('[Notebook] CRITICAL: notebook-container not found in DOM!');
        return;
    }

    // Attach toolbar event listeners
    const btnAddCell = document.getElementById('btn-add-cell');
    const btnRunAll = document.getElementById('btn-run-all');
    const btnClearAll = document.getElementById('btn-clear-all');
    const btnSave = document.getElementById('btn-save');

    console.log('[Notebook] Toolbar buttons:',
        btnAddCell ? 'add✓' : 'add✗',
        btnRunAll ? 'run✓' : 'run✗',
        btnClearAll ? 'clear✓' : 'clear✗',
        btnSave ? 'save✓' : 'save✗'
    );

    if (btnAddCell) {
        btnAddCell.addEventListener('click', () => {
            addCellAtEnd();
        });
    }

    if (btnRunAll) {
        btnRunAll.addEventListener('click', () => {
            runAllCells();
        });
    }

    if (btnClearAll) {
        btnClearAll.addEventListener('click', () => {
            clearAllOutputs();
        });
    }

    if (btnSave) {
        btnSave.addEventListener('click', () => {
            window.saveContent();
        });
    }

    // Mark as ready
    window.isNotebookReady = true;

    // Notify Android that notebook is ready
    if (window.AndroidNotebookReady && window.AndroidNotebookReady.onReady) {
        console.log('[Notebook] Notifying Android that notebook is ready');
        window.AndroidNotebookReady.onReady();
    }

    updateStatus('Ready');
    console.log('[Notebook] Initialization complete');
}

// =========================================
// CONTENT SERIALIZATION & SAVE
// =========================================

// Get serialized content for saving
window.getContent = function () {
    const content = serializeCells();
    console.log('[Notebook] getContent called, returning', content.length, 'chars');
    return content;
};

// Save the notebook content
window.saveContent = function () {
    const content = serializeCells();
    console.log('[Notebook] Saving content with', cells.length, 'cells');

    if (window.AndroidNotebook && window.AndroidNotebook.saveContent) {
        window.AndroidNotebook.saveContent(content);
        updateStatus('Saved');
    } else {
        console.warn('[Notebook] AndroidNotebook.saveContent not available');
    }
};

// Trigger save from keyboard shortcut (Ctrl+S)
document.addEventListener('keydown', function (e) {
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault();
        window.saveContent();
    }
});

// =========================================
// CUSTOM KEYBOARD INPUT FUNCTIONS
// =========================================

// Get the currently focused cell's editor
function getFocusedEditor() {
    // Check if any cell editor is focused
    for (const cell of cells) {
        if (cell.editor && cell.editor.hasFocus) {
            return cell.editor;
        }
    }
    // Fallback to selected cell's editor
    if (selectedCellId) {
        const cell = cells.find(c => c.id === selectedCellId);
        if (cell && cell.editor) {
            return cell.editor;
        }
    }
    // Fallback to first cell
    if (cells.length > 0 && cells[0].editor) {
        return cells[0].editor;
    }
    return null;
}

// Insert text at cursor
window.insertText = function (text) {
    const editor = getFocusedEditor();
    if (editor) {
        editor.dispatch(editor.state.replaceSelection(text));
        editor.focus();
    }
};

// Backspace - use CodeMirror's built-in command with safety guards
window.performBackspace = function () {
    const editor = getFocusedEditor();
    if (!editor) return;

    try {
        // Guard: don't try to delete at position 0 when nothing is selected
        const sel = editor.state.selection.main;
        if (sel.from === 0 && sel.to === 0) return;

        deleteCharBackward(editor);
    } catch (e) {
        console.error('[Notebook] Backspace error:', e);
    }
};

// Delete forward
window.performDelete = function () {
    const editor = getFocusedEditor();
    if (!editor) return;

    try {
        deleteCharForward(editor);
    } catch (e) {
        console.error('[Notebook] Delete error:', e);
    }
};

// Enter/newline - use CodeMirror's auto-indent
window.performEnter = function () {
    const editor = getFocusedEditor();
    if (editor) {
        insertNewlineAndIndent(editor);
    }
};

// Tab
window.performTab = function () {
    const editor = getFocusedEditor();
    if (editor) {
        editor.dispatch(editor.state.replaceSelection('    '));
        editor.focus();
    }
};

// Cursor movement - use CodeMirror commands
window.moveUp = function () {
    const editor = getFocusedEditor();
    if (editor) {
        cursorLineUp(editor);
    }
};

window.moveDown = function () {
    const editor = getFocusedEditor();
    if (editor) {
        cursorLineDown(editor);
    }
};

window.moveLeft = function () {
    const editor = getFocusedEditor();
    if (editor) {
        cursorCharLeft(editor);
    }
};

window.moveRight = function () {
    const editor = getFocusedEditor();
    if (editor) {
        cursorCharRight(editor);
    }
};

window.moveHome = function () {
    const editor = getFocusedEditor();
    if (editor) {
        cursorLineStart(editor);
    }
};

window.moveEnd = function () {
    const editor = getFocusedEditor();
    if (editor) {
        cursorLineEnd(editor);
    }
};

// Copy/Paste/Cut
window.performCopy = function () {
    const editor = getFocusedEditor();
    if (editor) {
        const selection = editor.state.selection.main;
        const text = editor.state.sliceDoc(selection.from, selection.to);
        navigator.clipboard.writeText(text).catch(e => console.error("Copy failed:", e));
    }
};

window.performPaste = async function () {
    const editor = getFocusedEditor();
    if (editor) {
        try {
            const text = await navigator.clipboard.readText();
            editor.dispatch(editor.state.replaceSelection(text));
        } catch (e) {
            console.error("Paste failed:", e);
        }
    }
};

window.performCut = function () {
    const editor = getFocusedEditor();
    if (editor) {
        const selection = editor.state.selection.main;
        const text = editor.state.sliceDoc(selection.from, selection.to);
        navigator.clipboard.writeText(text);
        editor.dispatch(editor.state.replaceSelection(''));
    }
};

// Undo/Redo - use CodeMirror commands
window.performUndo = function () {
    const editor = getFocusedEditor();
    if (editor) {
        undo(editor);
    }
};

window.performRedo = function () {
    const editor = getFocusedEditor();
    if (editor) {
        redo(editor);
    }
};

// Select all - use CodeMirror command
window.selectAll = function () {
    const editor = getFocusedEditor();
    if (editor) {
        selectAll(editor);
    }
};

// =========================================
// ORIENTATION CHANGE HANDLING
// =========================================

// Update all cells' inputmode based on current orientation
function updateInputModeForOrientation() {
    const isLandscape = window.innerWidth > window.innerHeight;
    const inputModeExtension = isLandscape
        ? EditorView.contentAttributes.of({ inputmode: "none" })
        : [];

    cells.forEach(cell => {
        if (cell.editor && cell.inputModeCompartment) {
            cell.editor.dispatch({
                effects: cell.inputModeCompartment.reconfigure(inputModeExtension)
            });
        }
    });
}

// Listen for orientation/resize changes
window.addEventListener('resize', updateInputModeForOrientation);

// =========================================
// AUTO-SAVE FUNCTIONALITY
// =========================================

let autoSaveTimer = null;
let autoSaveEnabled = false;
let autoSaveDelay = 3000; // Default 3 seconds

window.setAutoSave = function (enabled, delaySeconds) {
    console.log('[Notebook] setAutoSave:', enabled, delaySeconds);
    autoSaveEnabled = enabled;
    if (delaySeconds) autoSaveDelay = delaySeconds * 1000;

    if (!enabled && autoSaveTimer) {
        clearTimeout(autoSaveTimer);
        autoSaveTimer = null;
    }
    return true;
};

function triggerAutoSave() {
    if (!autoSaveEnabled) return;

    if (autoSaveTimer) clearTimeout(autoSaveTimer);

    autoSaveTimer = setTimeout(() => {
        console.log('[Notebook] Auto-save triggered');
        if (window.AndroidInterface && window.AndroidInterface.saveFile) {
            window.AndroidInterface.saveFile();
        }
    }, autoSaveDelay);
}

// Start initialization
console.log('[Notebook] Script loaded, waiting for DOM...');

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initNotebook);
} else {
    // DOM already loaded
    initNotebook();
}

console.log('[Notebook] Ready');
