// rollup.config.merge.js - Builds the merge editor bundle

import { nodeResolve } from "@rollup/plugin-node-resolve";
import commonjs from "@rollup/plugin-commonjs";
import polyfillNode from "rollup-plugin-polyfill-node";
import terser from "@rollup/plugin-terser";

export default {
    input: "./app/src/main/assets/git/merge-editor.js",
    output: {
        file: "./app/src/main/assets/git/merge-editor.bundle.js",
        format: "iife",
        name: "MergeEditorBundle",
        sourcemap: true,
    },
    plugins: [
        nodeResolve({
            browser: true,
            preferBuiltins: false
        }),
        polyfillNode(),
        commonjs({
            include: /node_modules/,
            transformMixedEsModules: true,
        }),
        terser(),
    ],
    onwarn(warning, warn) {
        if (warning.code === "UNRESOLVED_IMPORT" && /events/.test(warning.source)) return;
        warn(warning);
    },
};
