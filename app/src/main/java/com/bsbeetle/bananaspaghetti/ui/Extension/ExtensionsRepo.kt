package com.bsbeetle.bananaspaghetti.ui.Extension

object ExtensionsRepo {
    val extensions = listOf(
        ExtensionModel(
            id = "java",
            name = "Language Support for Java",
            author = "RedHat",
            description = "Java Linting, IntelliSense, formatting, refactoring, Maven/Gradle support and more.",
            version = "1.50.0",
            size = "179 MB",
            rating = 4.5,
            installed = false
        ),
        ExtensionModel(
            id = "python",
            name = "Python",
            author = "Microsoft",
            description = "Full Python development with IntelliSense, Jupyter, Debugger and more.",
            version = "2025.18.1",
            size = "140 MB",
            rating = 4.7,
            installed = true
        ),
        ExtensionModel(
            id = "cpp",
            name = "C/C++",
            author = "Microsoft",
            description = "C/C++ IntelliSense, debugging, code browsing and more.",
            version = "3.4.0",
            size = "122 MB",
            rating = 4.3,
            installed = false
        ),
        ExtensionModel(
            id = "rust",
            name = "Rust Analyzer",
            author = "Rust Foundation",
            description = "Feature rich Rust language support powered by rust-analyzer.",
            version = "0.10.1",
            size = "97 MB",
            rating = 4.6,
            installed = false
        )
    )
}