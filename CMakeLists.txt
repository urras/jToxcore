cmake_minimum_required (VERSION 2.6)
project (jToxcore)

set(CMAKE_MODULE_PATH "${CMAKE_SOURCE_DIR}/cmake/Modules" ${CMAKE_MODULE_PATH})
# Version
set(JTOXCORE_VERSION 0.5.1)

# JNI Header information
set(JNI_HEADER_LOCATION "${CMAKE_BINARY_DIR}/header")
set(JNI_HEADER_NAME "JTox.h")

# Target name for .jar
set(JAR_TARGET_NAME jToxcore)

# Target name for shared library
set(LIB_TARGET_NAME jtoxcore)

# Subdir for Java code
add_subdirectory (src)
add_subdirectory (jni)
