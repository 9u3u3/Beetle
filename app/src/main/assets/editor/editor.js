import { EditorView, basicSetup } from "codemirror"
import { EditorState } from "@codemirror/state"
import { cpp } from "@codemirror/lang-cpp"
import { oneDark } from "@codemirror/theme-one-dark"

let editor = new EditorView({
  state: EditorState.create({
    extensions: [
      basicSetup, 
      cpp(),
      oneDark
    ],
    doc: `#include<iostream>
int main() {
  std::cout << "Banana Spaghetti";
  return 0;
}
`
  }),
  parent: document.body
})

// get the theme color and apply it to the webview bg
const editorEl = document.querySelector('.cm-editor')
// getComputedStyle gets the actual value which is being shown on the screen of the selected property, when all the css rules and things are applied
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
