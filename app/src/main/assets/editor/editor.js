import { EditorView, basicSetup } from "codemirror"
import { EditorState } from "@codemirror/state"
import { cpp } from "@codemirror/lang-cpp"
import {insertNewlineAndIndent, deleteCharBackward } from "@codemirror/commands";
let editor = new EditorView({
  state: EditorState.create({
    extensions: [
      basicSetup, 
      cpp(),
      oneDark,
      EditorView.contentAttributes.of({
        'inputmode': 'none' // disable the soft keyboard
      })
    ],
    doc: `#include<iostream>
int main() {
  std::cout << "Banana Spaghetti";
  return 0;
}`
  }),
  parent: document.body
});

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

