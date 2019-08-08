set(CMAKE_SYSTEM_NAME Android)
set(CMAKE_ANDROID_NDK_TOOLCHAIN_VERSION clang)

set(ANDROID_NATIVE_API_LEVEL 22)
set(ANDROID_STL c++_static)

find_path(ANDROID_NDK_TOOLCHAIN_PATH
            NAMES android.toolchain.cmake
            PATHS
            $ENV{ANDROID_NDK_HOME}/build/cmake)

if(NOT ANDROID_NDK_TOOLCHAIN_PATH)
    message(FATAL_ERROR "Android NDK not found. Define ANDROID_NDK_HOME env variable and point it to Android NDK root directory.")
endif()

# Checking the NDK version number
file(STRINGS $ENV{ANDROID_NDK_HOME}/source.properties SOURCE_PROPERTIES_CONTENT REGEX)
string(REGEX MATCH "[0-9]+.[0-9]+.[0-9]*" ANDROIDNDK_VERSION "${SOURCE_PROPERTIES_CONTENT}")
string(REGEX MATCH "[0-9]+.[0-9]" ANDROIDNDK_VERSION_MAJOR_MINOR "${ANDROIDNDK_VERSION}")

#if(NOT ${ANDROIDNDK_VERSION_MAJOR_MINOR} VERSION_EQUAL "13.1")
#   message(FATAL_ERROR "Android NDK version mismatch. Only 13b is supported currently.")
#endif()

include(${ANDROID_NDK_TOOLCHAIN_PATH}/android.toolchain.cmake)
