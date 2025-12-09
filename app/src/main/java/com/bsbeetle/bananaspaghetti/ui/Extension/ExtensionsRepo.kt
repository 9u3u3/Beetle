package com.bsbeetle.bananaspaghetti.ui.Extension

import com.bsbeetle.bananaspaghetti.R

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
            installed = false,
            iconResId = R.drawable.extension, 
            downloads = "35M",
            stars = 591
        ),
        ExtensionModel(
            id = "python",
            name = "Python",
            author = "Microsoft",
            description = "Full Python development with IntelliSense, Jupyter, Debugger and more.",
            version = "2025.18.1",
            size = "140 MB",
            rating = 4.7,
            installed = true,
            iconResId = R.drawable.extension,
            downloads = "128M",
            stars = 842
        ),
        ExtensionModel(
            id = "cpp",
            name = "C/C++",
            author = "Microsoft",
            description = "C/C++ IntelliSense, debugging, code browsing and more.",
            version = "3.4.0",
            size = "122 MB",
            rating = 4.3,
            installed = false,
            iconResId = R.drawable.cplusplus_plain,
            downloads = "91M",
            stars = 591
        ),
        ExtensionModel(
            id = "rust",
            name = "Rust Analyzer",
            author = "Rust Foundation",
            description = "Feature rich Rust language support powered by rust-analyzer.",
            version = "0.10.1",
            size = "97 MB",
            rating = 4.6,
            installed = false,
            iconResId = R.drawable.extension,
            downloads = "12M",
            stars = 320
        )
    )
}