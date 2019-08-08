set(ANDROID_ABI armeabi-v7a)

set(TARGET_ARCH ${ANDROID_ABI})

set(ANDROID_ARM_NEON TRUE)

include(${CMAKE_CURRENT_LIST_DIR}/android.toolchain.cmake)
