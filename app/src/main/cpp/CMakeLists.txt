cmake_minimum_required(VERSION 3.22.1)

project("banannaspaghetti")

add_library(
        pty-lib
        SHARED
        pty-lib.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        pty-lib
        ${log-lib}
)