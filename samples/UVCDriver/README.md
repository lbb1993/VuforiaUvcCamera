# UVCDriver sample library
## Overview
This is a sample implementation of a library that provides camera frames from UVC-compatible webcams on Android platform.

This sample uses an open source library to interact with UVC-web cameras:
https://github.com/saki4510t/UVCCamera

## Building
### Prerequisities
- Operating system:
    - macOS
    - Windows
    - Linux not tested, but should work with minor or no changes
- CMake 3.6 or greater
- Ninja build system (https://ninja-build.org)
- Android NDK 13b
    - Other versions might work, but are not currently tested. Problems have been seen with USB-devices not initializing properly when using NDK 16. If you wish to use a newer version please see: https://github.com/saki4510t/UVCCamera/issues/181
- Android SDK with API level 22 support
- lubuvc, libusb and libjpeg-turbo built from https://github.com/saki4510t/UVCCamera (master, commit 477aee87ba763d003b039ab26406724bba721e8e)

### Build steps for building libuvc, libusb, libjpeg-turbo from source
Please refer to the Vuforia developer documentation for build steps.

### Build steps using prebuilt libuvc, libusb, libjpeg-turbo
Build all architectures using default settings. This will create a build directory to [rootdir]/build and copy the binaries to [rootdir]/build/bin
```
python build.py
```

For all build options
```
python build.py --help
```

# Usage
1. Add libUVCDriver.so, libuvc, libusb and libjpeg-turbo from build/bin into your Android-app project.

    libUVCDriver.so can be added to an app by two ways depending on your build system: Using Android.mk or using Gradle. Please note that you need to substitute '[path-in-your-filesystem]' with the correct path.

    **Using Android.mk (e.g. ImageTargetsNative sample app)**

    Add library definitions to your Android.mk:
    ```
    include $(CLEAR_VARS)
    LOCAL_MODULE := libUVCDriver-prebuilt
    LOCAL_SRC_FILES = [path-in-your-filesystem]/UVCDriver/build/bin/Android/$(TARGET_ARCH_ABI)/libUVCDriver.so
    include $(PREBUILT_SHARED_LIBRARY)

    include $(CLEAR_VARS)
    LOCAL_MODULE := libuvc-prebuilt
    LOCAL_SRC_FILES = [path-in-your-filesystem]/UVCDriver/build/bin/Android/$(TARGET_ARCH_ABI)/libuvc.so
    include $(PREBUILT_SHARED_LIBRARY)

    include $(CLEAR_VARS)
    LOCAL_MODULE := libusb-prebuilt
    LOCAL_SRC_FILES = [path-in-your-filesystem]/UVCDriver/build/bin/Android/$(TARGET_ARCH_ABI)/libusb100.so
    include $(PREBUILT_SHARED_LIBRARY)

    include $(CLEAR_VARS)
    LOCAL_MODULE := libjpeg-turbo-prebuilt
    LOCAL_SRC_FILES = [path-in-your-filesystem]/UVCDriver/build/bin/Android/$(TARGET_ARCH_ABI)/libjpeg-turbo1500.so
    include $(PREBUILT_SHARED_LIBRARY)
    ```
    When defining your local module in the same Android.mk add prebuilt libraries as dependencies to your LOCAL_SHARED_LIBRARIES:
    ```
    LOCAL_SHARED_LIBRARIES := Vuforia-prebuilt libUVCDriver-prebuilt libuvc-prebuilt libusb-prebuilt libjpeg-turbo-prebuilt
    ```

    **Using Gradle (e.g. VuforiaSamples sample app)**
    This can be done in your app/build.gradle with the following:
    ```
    android {
        sourceSets.main {
            jniLibs.srcDirs += '[path-in-your-filesystem]/UVCDriver/build/bin/Android/'
        }
    }
    ```

2. Add UVCDriver.jar from build/bin into your Android-app project.

    This can be done in your app/build.gradle with the following:
    ```
    dependencies {
        implementation files("[path-in-your-filesystem]/UVCDriver/build/bin/Android/UVCDriver.jar")
    }
    ```

3. Add following call to your source code before calling Vuforia::init();

    **Java**:
    ```
    Vuforia.setDriverLibrary("libUVCDriver.so");
    ```

    **C++**:
    ```
    Vuforia::setDriverLibrary("libUVCDriver.so", nullptr);
