import { nodeResolve } from "@rollup/plugin-node-resolve";
export default {
  input: "./app/src/main/assets/editor/editor.js",
  output: {
    file: "./app/src/main/assets/editor/editor.bundle.js",
    format: "iife",
  },
  plugins: [nodeResolve()],
};
