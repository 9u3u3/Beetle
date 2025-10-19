import { EditorView, basicSetup } from "codemirror"
import { EditorState } from "@codemirror/state"
import { cpp } from "@codemirror/lang-cpp"
import { oneDark } from "@codemirror/theme-one-dark"
import {insertNewlineAndIndent, deleteCharBackward } from "@codemirror/commands";
import { undo, redo, history } from "@codemirror/commands"
import { search, openSearchPanel } from "@codemirror/search"

window.code_ = ""

// Currently the editor is hardcoded for cpp
// It will be made dynamic later
window.editor = new EditorView({ // add the editor to window
  state: EditorState.create({
    extensions: [
      basicSetup,
      cpp(),
      oneDark,
      history(),
      search(),
      EditorView.contentAttributes.of({ // disables the virtual keyboard
        inputmode: "none",
        readonly: "true"
      }),
      EditorView.updateListener.of(update => {
        if(update.docChanged) {
            window.code_ = update.state.doc.toString()
        }
      })
    ],
    doc: "",
  }),
  parent: document.body
})

window.isEditorReady = true; // Declare that editor is loaded successfully

// get the theme color and apply it to the webview bg
// getComputedStyle gets the actual value which is being shown on the screen of the selected property,
//...when all the css rules and things are applied
const editorEl = document.querySelector('.cm-editor')
const bg = window.getComputedStyle(editorEl).backgroundColor
document.body.style.backgroundColor = bg

// Added to insert text
window.insertText = function(text) {
  editor.dispatch(editor.state.replaceSelection(text));
  editor.focus();
};

// Added for backspace
window.performBackspace = function() {
  deleteCharBackward(editor);
  editor.focus();
};

// Added for enter
window.performEnter = function() {
   insertNewlineAndIndent(editor);
   editor.focus();
};

// For copy
window.performCopy = function() {
    const selection = editor.state.selection.main;
    const text = editor.state.sliceDoc(selection.from, selection.to);
    navigator.clipboard.writeText(text).catch(e => console.error("Copy failed:", e));
};

window.performPaste = async function() {
    try {
        const text = await navigator.clipboard.readText();
        editor.dispatch(editor.state.replaceSelection(text));
        editor.focus();
    } catch(e) {
        console.error("Paste failed:", e);
    }
};

window.performCut = function() {
    const selection = editor.state.selection.main;
    const text = editor.state.sliceDoc(selection.from, selection.to);
    navigator.clipboard.writeText(text);
    editor.dispatch({
        changes: { from: selection.from, to: selection.to }
    });
};

window.performUndo = function() {
    undo(editor);
};

window.performRedo = function() {
    redo(editor);
};

window.performSelectAll = function() {
    editor.dispatch({
        selection: { anchor: 0, head: editor.state.doc.length }
    });
};

window.performFind = function() {
    openSearchPanel(editor);
};
window.closeFindPanel = function() {
    closeSearchPanel(editorView);
};

window.performSave = function() {
    // Implement save logic here
    console.log("Save triggered");
};