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

export default {
  input: "./app/src/main/assets/editor/editor.js",
  output: {
    file: "./app/src/main/assets/editor/editor.bundle.js",
    format: "iife", // Immediately Invoked Function Expression for browser
    name: "EditorBundle", // gives a global name to access exports if needed
    sourcemap: true,
  },
  plugins: [
    nodeResolve({
      browser: true,        // prefer browser-compatible builds
      preferBuiltins: false // don’t assume Node built-ins
    }),

    polyfillNode(),         // ✅ injects shims for Node built-ins like 'events'

    commonjs({
      include: /node_modules/,
      transformMixedEsModules: true, // supports hybrid ESM+CJS modules
    }),

    terser(), // optional: minify output
  ],

  // optional: suppress the "unresolved events" warning Rollup might throw
  onwarn(warning, warn) {
    if (warning.code === "UNRESOLVED_IMPORT" && /events/.test(warning.source)) return;
    warn(warning);
  },
};
