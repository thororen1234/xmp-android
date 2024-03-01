cmake_minimum_required(VERSION 3.4.1)
project(xmp-jni LANGUAGES C CXX)

# Add libxmp's CMakeLists.txt
add_subdirectory(libxmp)

add_library(xmp-jni SHARED xmp-jni.cpp XmpEngine.cpp)

find_package (oboe REQUIRED CONFIG)

target_link_libraries(xmp-jni xmp_static android log oboe::oboe)

# xmp-jni.c needs use of xmp.h and common.h
target_include_directories(xmp-jni PRIVATE libxmp/include libxmp/src)
