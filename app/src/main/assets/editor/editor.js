import { EditorView, basicSetup } from "codemirror"
import { EditorState } from "@codemirror/state"
import { cpp } from "@codemirror/lang-cpp"

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
})
