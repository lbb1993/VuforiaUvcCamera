#===============================================================================
#Copyright (c) 2018 PTC Inc. All Rights Reserved.
#
#Vuforia is a trademark of PTC Inc., registered in the United States and other
#countries.
#==============================================================================

project(UVCDriver)
cmake_minimum_required(VERSION 3.6.0)

if (NOT EXTERNAL_UVC_DIR)
    message(FATAL_ERROR "EXTERNAL_UVC_DIR not defined. Define by -DEXTERNAL_UVC_DIR='[dir]'")
endif()

if (NOT VUFORIA_HEADER_DIR)
    message(FATAL_ERROR "VUFORIA_HEADER_DIR not defined. Define by -DVUFORIA_HEADER_DIR='[dir]'")
endif()

###############################################################################
# Set variables
###############################################################################

set(ROOT_DIR                ${CMAKE_CURRENT_LIST_DIR})
set(SRC_DIR                 ${ROOT_DIR}/src)
set(INC_DIR                 ${ROOT_DIR}/include)
set(JAVA_PACKAGE_DIR        ${SRC_DIR}/Java/com/vuforia/samples/uvcDriver)

# root directory for external binary libraries (libusb, libuvc, libjpeg)
set(EXTERNAL_UVC_LIB_DIR    ${EXTERNAL_UVC_DIR}/../libs/${TARGET_ARCH})
set(EXTERNAL_UVC_INC_DIR    ${EXTERNAL_UVC_DIR}/libuvc/include
                            ${EXTERNAL_UVC_DIR}/libusb
                            ${EXTERNAL_UVC_DIR})

set(LIBUVC_SO               ${EXTERNAL_UVC_LIB_DIR}/libuvc.so)
set(LIBUSB_SO               ${EXTERNAL_UVC_LIB_DIR}/libusb100.so)
set(LIBJPEG_TURBO_SO        ${EXTERNAL_UVC_LIB_DIR}/libjpeg-turbo1500.so)

set(TARGET_UVC_DRIVER_LIB       "UVCDriver")
set(TARGET_UVC_DRIVER_JAVA_LIB  "UVCDriver_Java")
set(UVC_DRIVER_JAR_NAME         "UVCDriver")

###############################################################################
# Target UVCDriver
###############################################################################

set(LIB_SRC
    ${SRC_DIR}/UVCDriver.cpp
    ${SRC_DIR}/UVCExternalCamera.cpp
)

set(LIB_INC
    ${INC_DIR}/UVCDriver.h
    ${INC_DIR}/UVCExternalCamera.h
)

add_library(${TARGET_UVC_DRIVER_LIB} SHARED ${LIB_SRC} ${LIB_INC})
			 
target_link_libraries(${TARGET_UVC_DRIVER_LIB}
                      ${LIBUVC_SO}
                      ${LIBUSB_SO}
                      ${LIBJPEG_TURBO_SO}
                      log
)

target_include_directories(${TARGET_UVC_DRIVER_LIB}
                           PUBLIC
                           ${INC_DIR}
                           ${VUFORIA_HEADER_DIR}
                           ${EXTERNAL_UVC_INC_DIR}
)

install(TARGETS ${TARGET_UVC_DRIVER_LIB}
       RUNTIME DESTINATION bin/${CMAKE_SYSTEM_NAME}/${TARGET_ARCH}
       LIBRARY DESTINATION bin/${CMAKE_SYSTEM_NAME}/${TARGET_ARCH}
       ARCHIVE DESTINATION bin/${CMAKE_SYSTEM_NAME}/${TARGET_ARCH})

install(FILES ${LIBUVC_SO} DESTINATION bin/${CMAKE_SYSTEM_NAME}/${TARGET_ARCH})
install(FILES ${LIBUSB_SO} DESTINATION bin/${CMAKE_SYSTEM_NAME}/${TARGET_ARCH})
install(FILES ${LIBJPEG_TURBO_SO} DESTINATION bin/${CMAKE_SYSTEM_NAME}/${TARGET_ARCH})

###############################################################################
# Target UVCDriver_Java
###############################################################################

find_package(Java REQUIRED)
include(UseJava)

set(CMAKE_JAVA_COMPILE_FLAGS "-source" "1.7" "-target" "1.7")

set(JAVA_SRC
    ${JAVA_PACKAGE_DIR}/USBController.java
    ${JAVA_PACKAGE_DIR}/CalibrationController.java
)

add_jar(${TARGET_UVC_DRIVER_JAVA_LIB} 
        SOURCES ${JAVA_SRC}
        OUTPUT_NAME ${UVC_DRIVER_JAR_NAME}
        INCLUDE_JARS $ENV{ANDROID_HOME}/platforms/android-${ANDROID_NATIVE_API_LEVEL}/android.jar
)

install_jar(${TARGET_UVC_DRIVER_JAVA_LIB}
        RUNTIME DESTINATION bin/${CMAKE_SYSTEM_NAME}/
        LIBRARY DESTINATION bin/${CMAKE_SYSTEM_NAME}/
        ARCHIVE DESTINATION bin/${CMAKE_SYSTEM_NAME}/)
