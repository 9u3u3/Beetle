//import { nodeResolve } from "@rollup/plugin-node-resolve";
//import commonjs from "@rollup/plugin-commonjs";
//import polyfillNode from "rollup-plugin-polyfill-node";
//
//export default {
//  input: "./app/src/main/assets/editor/editor.js",
//  output: {
//    file: "./app/src/main/assets/editor/editor.bundle.js",
//    format: "iife",
//  },
//  plugins: [
//    nodeResolve({
//      browser: true, // ensures it prefers browser versions of deps
//      preferBuiltins: false, // avoid using Node builtins accidentally
//    }),
//    commonjs({
//      include: /node_modules/,
//      transformMixedEsModules: true, // helps when modules mix ESM+CJS syntax
//    }),
//  ],
//};

// rollup.config.js

import { nodeResolve } from "@rollup/plugin-node-resolve";
import commonjs from "@rollup/plugin-commonjs";
import polyfillNode from "rollup-plugin-polyfill-node";
import terser from "@rollup/plugin-terser";

const commonPlugins = [
  nodeResolve({
    browser: true,        // prefer browser-compatible builds
    preferBuiltins: false // don't assume Node built-ins
  }),
  polyfillNode(),         // injects shims for Node built-ins like 'events'
  commonjs({
    include: /node_modules/,
    transformMixedEsModules: true, // supports hybrid ESM+CJS modules
  }),
  terser(), // optional: minify output
];

const commonOnWarn = (warning, warn) => {
  if (warning.code === "UNRESOLVED_IMPORT" && /events/.test(warning.source)) return;
  warn(warning);
};

export default [
  // Editor bundle
  {
    input: "./app/src/main/assets/editor/editor.js",
    output: {
      file: "./app/src/main/assets/editor/editor.bundle.js",
      format: "iife",
      name: "EditorBundle",
      sourcemap: true,
    },
    plugins: commonPlugins,
    onwarn: commonOnWarn,
  },
  // Notebook bundle (for Python REPL mode)
  {
    input: "./app/src/main/assets/editor/notebook.js",
    output: {
      file: "./app/src/main/assets/editor/notebook.bundle.js",
      format: "iife",
      name: "NotebookBundle",
      sourcemap: true,
    },
    plugins: commonPlugins,
    onwarn: commonOnWarn,
  },
];

