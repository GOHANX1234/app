cmake_minimum_required(VERSION 3.22.1)
project("sarrows_native")

# ── Security / Crypto helpers ───────────────────────────────────────────────
add_library(
    sarrows_native
    SHARED
    sarrows_jni.cpp
    cookie_manager.cpp
    api_security.cpp
    crypto_utils.cpp
)

# Android log + android libraries
find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(
    sarrows_native
    ${log-lib}
    ${android-lib}
)

target_compile_options(sarrows_native PRIVATE
    -std=c++17
    -O2
    -fvisibility=hidden
    -Wall
    -Wextra
    -fstack-protector-strong
)

# Strip debug info from release builds
if(NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_options(sarrows_native PRIVATE -DNDEBUG)
endif()
