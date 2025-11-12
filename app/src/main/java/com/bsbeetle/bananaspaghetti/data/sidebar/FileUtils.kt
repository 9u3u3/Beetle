package com.bsbeetle.bananaspaghetti.data.sidebar

object FileIconUtils {
    fun getIconPath(fileName: String): String {
        // Handle special files first (by exact name match)
        val specialFileIcon = getSpecialFileIcon(fileName.lowercase())
        if (specialFileIcon != null) {
            return "file:///android_asset/file_icons/$specialFileIcon.svg"
        }

        // Then handle by extension
        val extension = fileName.substringAfterLast('.', "")
        val iconName = when (extension.lowercase()) {
            // JavaScript & TypeScript
            "js" -> "js"
            "mjs" -> "js"
            "cjs" -> "js"
            "jsx" -> "reactjs"
            "ts" -> "typescript"
            "tsx" -> "reactts"
            "mts" -> "typescript"
            "cts" -> "typescript"

            // Web
            "html", "htm" -> "html"
            "css" -> "css"
            "scss" -> "scss"
            "sass" -> "sass"
            "less" -> "less"
            "styl", "stylus" -> "stylus"

            // JSON & Data
            "json" -> "json"
            "json5" -> "json5"
            "jsonc" -> "json"
            "jsonld" -> "jsonld"
            "jsonnet" -> "jsonnet"
            "hjson" -> "hjson"
            "xml" -> "config"
            "yaml", "yml" -> "light_yaml"
            "toml" -> "toml"
            "ini" -> "ini"
            "env" -> "dotenv"
            "properties" -> "config"

            // Programming Languages
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "py", "pyw", "pyx" -> "python"
            "pyc" -> "python"
            "pyi" -> "pytyped"
            "c" -> "c"
            "h" -> "cheader"
            "cpp", "cc", "cxx", "c++" -> "cpp"
            "hpp", "hh", "hxx" -> "cppheader"
            "cs", "csx" -> "csharp"
            "csproj" -> "csproj"
            "go" -> "go"
            "rs" -> "rust"
            "php", "phtml" -> "php"
            "rb", "ruby" -> "ruby"
            "swift" -> "swift"
            "dart" -> "dartlang"
            "lua" -> "lua"
            "pl", "pm" -> "perl"
            "r", "rdata" -> "r"
            "jl" -> "julia"
            "scala", "sc" -> "scala"
            "clj", "cljs", "cljc" -> "clojure"
            "erl", "hrl" -> "erlang"
            "ex", "exs" -> "elixir"
            "elm" -> "elm"
            "hs", "lhs" -> "haskell"
            "ml", "mli" -> "ocaml"
            "fs", "fsi", "fsx" -> "fsharp"
            "v" -> "verilog"
            "sv", "svh" -> "systemverilog"
            "vhd", "vhdl" -> "vhdl"
            "groovy", "gvy" -> "groovy"
            "gradle" -> "gradle"
            "nim" -> "nim"
            "cr" -> "crystal"
            "d" -> "dlang"
            "ada", "adb", "ads" -> "ada"
            "pas", "pp" -> "pascal"
            "for", "f90", "f95" -> "fortran"
            "cob", "cbl" -> "cobol"
            "sql" -> "sql"
            "plsql" -> "plsql"
            "psql" -> "pgsql"
            "zig" -> "zig"
            "odin" -> "odin"
            "v" -> "vlang"

            // Shell & Scripts
            "sh", "bash", "zsh" -> "shell"
            "fish" -> "shell"
            "bat", "cmd" -> "bat"
            "ps1", "psm1", "psd1" -> "powershell"

            // Markup & Documentation
            "md", "markdown", "mdown" -> "markdown"
            "mdx" -> "mdx"
            "rst" -> "text"
            "tex", "latex" -> "tex"
            "adoc", "asciidoc" -> "asciidoc"
            "org" -> "org"
            "txt" -> "text"
            "pdf" -> "pdf"
            "doc", "docx" -> "word"
            "xls", "xlsx" -> "excel"
            "ppt", "pptx" -> "powerpoint"

            // Images
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "ico" -> "image"
            "svg" -> "svg"
            "psd" -> "photoshop"
            "ai" -> "ai"
            "sketch" -> "sketch"
            "fig", "figma" -> "figma"
            "xd" -> "adobexd"
            "afdesign" -> "affinitydesigner"
            "afphoto" -> "affinityphoto"
            "afpub" -> "affinitypublisher"
            "blend" -> "blender"
            "fbx" -> "fbx"
            "obj" -> "3dmodel"
            "stl" -> "3dmodel"
            "gltf", "glb" -> "gltf"
            "avif" -> "avif"

            // Video & Audio
            "mp4", "avi", "mov", "mkv", "webm", "flv", "wmv" -> "video"
            "mp3", "wav", "ogg", "flac", "aac", "m4a", "wma" -> "audio"

            // Archives
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> "archive"

            // Config Files
            "cfg", "conf", "config" -> "config"
            "editorconfig" -> "editorconfig"
            "gitignore" -> "git"
            "gitattributes" -> "git"
            "dockerignore" -> "docker"
            "prettierrc" -> "prettier"
            "eslintrc" -> "eslint"
            "babelrc" -> "babel"

            // Build & Package Management
            "makefile", "mk" -> "makefile"
            "cmake" -> "cmake"
            "bazel" -> "bazel"
            "bzl" -> "bazel"
            "BUILD" -> "bazel"
            "ninja" -> "ninja"

            // Package Managers
            "lock" -> when {
                fileName.contains("package-lock") -> "npm"
                fileName.contains("yarn.lock") -> "yarn"
                fileName.contains("pnpm-lock") -> "pnpm"
                fileName.contains("cargo.lock") -> "cargo"
                fileName.contains("composer.lock") -> "composer"
                fileName.contains("gemfile.lock") -> "bundler"
                else -> "lock"
            }

            // Frameworks & Libraries
            "vue" -> "vue"
            "svelte" -> "svelte"
            "astro" -> "astro"
            "prisma" -> "prisma"
            "proto" -> "protobuf"
            "graphql", "gql" -> "graphql"
            "dockerfile" -> "docker"
            "tf", "tfvars" -> "terraform"
            "hcl" -> "hashicorp"

            // Mobile
            "apk" -> "android"
            "ipa" -> "apple"
            "xcodeproj", "xcworkspace" -> "xcode"

            // Database
            "db", "sqlite", "sqlite3" -> "sqlite"
            "mdb" -> "access"

            // Certificates & Security
            "cert", "crt", "pem", "key" -> "cert"
            "gpg" -> "gpg"

            // Fonts
            "ttf", "otf", "woff", "woff2", "eot" -> "font"

            // Log files
            "log" -> "log"

            // Binary & Executables
            "exe", "dll", "so", "dylib" -> "binary"
            "jar", "war" -> "jar"
            "class" -> "class"
            "wasm" -> "wasm"

            // Game Development
            "unity" -> "unity"
            "unitypackage" -> "unity"
            "godot" -> "godot"
            "gd" -> "gdscript"
            "tscn" -> "tscn"
            "tres" -> "tres"

            // Other
            "license", "licence" -> "license"
            "changelog" -> "changelog"
            "readme" -> "readme"

            else -> "default_file"
        }

        return "file:///android_asset/file_icons/$iconName.svg"
    }

    /**
     * Handles special files that should be matched by exact filename
     */
    private fun getSpecialFileIcon(fileName: String): String? {
        return when (fileName) {
            // Package managers
            "package.json" -> "nodejs"
            "package-lock.json" -> "npm"
            "yarn.lock" -> "yarn"
            "pnpm-lock.yaml" -> "pnpm"
            "bun.lockb" -> "bun"
            "composer.json" -> "composer"
            "composer.lock" -> "composer"
            "cargo.toml" -> "cargo"
            "cargo.lock" -> "cargo"
            "gemfile" -> "bundler"
            "gemfile.lock" -> "bundler"
            "podfile" -> "cocoapods"
            "pubspec.yaml" -> "dartlang"
            "requirements.txt" -> "python"
            "pipfile" -> "python"
            "poetry.lock" -> "poetry"
            "pyproject.toml" -> "python"
            "go.mod" -> "go"
            "go.sum" -> "go"

            // Config files
            "tsconfig.json" -> "tsconfig"
            "jsconfig.json" -> "jsconfig"
            "webpack.config.js" -> "webpack"
            "rollup.config.js" -> "rollup"
            "vite.config.js", "vite.config.ts" -> "vite"
            "vitest.config.js", "vitest.config.ts" -> "vitest"
            "jest.config.js", "jest.config.ts" -> "jest"
            "karma.conf.js" -> "karma"
            "cypress.config.js", "cypress.config.ts" -> "cypress"
            "playwright.config.js", "playwright.config.ts" -> "playwright"
            ".eslintrc", ".eslintrc.js", ".eslintrc.json" -> "eslint"
            ".prettierrc", ".prettierrc.js", ".prettierrc.json" -> "prettier"
            ".babelrc", ".babelrc.js", ".babelrc.json", "babel.config.js" -> "babel"
            ".editorconfig" -> "editorconfig"
            ".gitignore" -> "git"
            ".gitattributes" -> "git"
            ".gitmodules" -> "git"
            ".dockerignore" -> "docker"
            "dockerfile" -> "docker"
            "docker-compose.yml", "docker-compose.yaml" -> "docker"
            ".npmrc" -> "npm"
            ".yarnrc" -> "yarn"
            ".nvmrc" -> "node"
            ".node-version" -> "node"
            "makefile" -> "makefile"
            "cmakelists.txt" -> "cmake"
            "rakefile" -> "rake"
            "procfile" -> "procfile"
            "vagrantfile" -> "vagrant"
            ".env" -> "dotenv"
            ".env.local", ".env.development", ".env.production" -> "dotenv"

            // Framework files
            "next.config.js", "next.config.mjs" -> "next"
            "nuxt.config.js", "nuxt.config.ts" -> "nuxt"
            "svelte.config.js" -> "svelte"
            "astro.config.mjs" -> "astro"
            "remix.config.js" -> "remix"
            "gatsby-config.js" -> "gatsby"
            "angular.json" -> "angular"
            "vue.config.js" -> "vue"
            ".vuepress" -> "vuepress"
            "nest-cli.json" -> "nestjs"

            // CI/CD
            ".travis.yml" -> "travis"
            "appveyor.yml" -> "appveyor"
            ".gitlab-ci.yml" -> "gitlab"
            "circle.yml", "config.yml" -> "circleci"
            "jenkinsfile" -> "jenkins"
            ".drone.yml" -> "drone"
            "azure-pipelines.yml" -> "azurepipelines"
            ".github" -> "github"

            // Cloud & Infrastructure
            "serverless.yml" -> "serverless"
            "netlify.toml" -> "netlify"
            "vercel.json" -> "vercel"
            "railway.json", "railway.toml" -> "railway"
            "fly.toml" -> "flyio"
            "render.yaml" -> "render"
            "cloudformation.yml", "cloudformation.yaml" -> "aws"

            // Documentation
            "readme.md", "readme" -> "readme"
            "license", "license.md", "licence" -> "license"
            "changelog.md", "changelog" -> "changelog"
            "contributing.md" -> "contributing"
            "code_of_conduct.md" -> "codeofconduct"
            ".all-contributorsrc" -> "allcontributors"

            // Linting & Formatting
            ".stylelintrc", ".stylelintrc.js", ".stylelintrc.json" -> "stylelint"
            ".commitlintrc", "commitlint.config.js" -> "commitlint"
            ".huskyrc", ".husky" -> "husky"
            ".lintstagedrc" -> "lintstaged"

            // Testing
            ".nycrc", ".nycrc.json" -> "nyc"
            "jest.config.json" -> "jest"
            ".mocharc.json" -> "mocha"

            // Build output
            ".next" -> "next"
            "dist" -> "folder"
            "build" -> "folder"
            "out" -> "folder"
            "public" -> "folder"
            "static" -> "folder"
            "assets" -> "folder"
            "node_modules" -> "nodejs"
            ".git" -> "git"
            ".vscode" -> "vscode"

            else -> null
        }
    }

    /**
     * Gets icon for directories
     */
    fun getDirectoryIconPath(dirName: String, isOpen: Boolean = false): String {
        val iconName = when (dirName.lowercase()) {
            ".git" -> "git"
            ".github" -> "github"
            ".vscode" -> "vscode"
            "node_modules" -> "nodejs"
            ".next" -> "next"
            ".nuxt" -> "nuxt"
            "dist", "build", "out" -> "folder"
            "src", "source" -> "folder"
            "public", "static", "assets" -> "folder"
            "test", "tests", "__tests__" -> "folder"
            "docs", "documentation" -> "folder"
            "scripts" -> "folder"
            "config" -> "folder"
            ".husky" -> "husky"
            ".circleci" -> "circleci"
            "__pycache__" -> "python"
            "vendor" -> "folder"
            else -> if (isOpen) "default_folder_opened" else "default_folder"
        }

        return "file:///android_asset/file_icons/$iconName.svg"
    }
}


/*Special File Matching - Recognizes important files by exact name:

Config files (package.json, tsconfig.json, webpack.config.js, etc.)
CI/CD files (.travis.yml, jenkinsfile, etc.)
Documentation (README.md, LICENSE, etc.)
Framework configs (next.config.js, nuxt.config.js, etc.)


Extension-based Mapping - Covers 200+ file extensions:

All major programming languages (JS, TS, Python, Java, Kotlin, Go, Rust, etc.)
Web technologies (HTML, CSS, SCSS, LESS, etc.)
Data formats (JSON, YAML, TOML, XML, etc.)
Images (PNG, JPG, SVG, PSD, Sketch, etc.)
Build tools (Gradle, Maven, CMake, etc.)
And many more!


Smart Lock File Detection - Identifies lock files from their names:

package-lock.json → npm icon
yarn.lock → yarn icon
cargo.lock → cargo icon


Directory Icons - Bonus method getDirectoryIconPath() for special folder icons:

.git → git icon
node_modules → nodejs icon
.vscode → vscode icon
And more!


Fallback to default_file - Any unrecognized file extension falls back to the default file icon

The utility prioritizes exact filename matches over extension matching, so package.json gets the proper Node.js icon instead of just a generic JSON icon.

 */