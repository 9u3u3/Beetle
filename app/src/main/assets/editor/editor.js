import { EditorView, basicSetup } from "codemirror"
import { EditorState } from "@codemirror/state"
import { cpp } from "@codemirror/lang-cpp"
import {insertNewlineAndIndent, deleteCharBackward } from "@codemirror/commands";
let editor = new EditorView({
  state: EditorState.create({
    extensions: [basicSetup, cpp()],
    doc: ` #include<iostream>
int main() {
  std::cout << "Banana Spaghetti";
  return 0;
}
`
  }),
  parent: document.body
});

//window.insertText = function(text) {
//  editor.dispatch({
//    changes: { from: editor.state.selection.main.head, insert: text }  });
//  editor.focus();
//};
window.insertText = function(text) {
  editor.dispatch(editor.state.replaceSelection(text));
  editor.focus();
};
window.performBackspace = function() {
  deleteCharBackward(editor);
  editor.focus();
};

window.performEnter = function() {
   insertNewlineAndIndent(editor);
   editor.focus();
};

