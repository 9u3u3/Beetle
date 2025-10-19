cmake_minimum_required(VERSION 3.22.1)

project("banannaspaghetti")

find_library(
        log-lib
        log
)

add_library(
        pty-lib
        SHARED
        pty-lib.cpp
)

target_link_libraries(
        pty-lib
        ${log-lib}
)
add_library(
        lsp-lib
        SHARED
        lsp-lib.cpp
)

target_link_libraries(
        lsp-lib
        ${log-lib}
)
