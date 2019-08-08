/*===============================================================================
Copyright (c) 2018 PTC Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/

#include "UVCExternalCamera.h"

#include <android/log.h>
#include <string>

#define JAVA_USB_CONTROLLER_CLASS "com/vuforia/samples/uvcDriver/USBController"
#define JAVA_CALIBRATION_CONTROLLER_CLASS "com/vuforia/samples/uvcDriver/CalibrationController"
#define MODULE_TAG "UVCExternalCamera"

#define LOG_D(...) __android_log_print(ANDROID_LOG_DEBUG, MODULE_TAG, __VA_ARGS__)
#define LOG_I(...) __android_log_print(ANDROID_LOG_INFO, MODULE_TAG, __VA_ARGS__)
#define LOG_E(...) __android_log_print(ANDROID_LOG_ERROR, MODULE_TAG, __VA_ARGS__)

// This macro is set to 0 because the libuvc fork by saki4510t that we use here
// doesn't implement uvc_get_focus_simple_range() and uvc_set_focus_simple_range()
// even though both of them are declared in the header file.
#define SIMPLE_FOCUS_METHODS_IMPLEMENTED 0

namespace
{
enum UVCInputTerminalBitShift
{
    BIT_SHIFT_CT_SCANNING_MODE_CONTROL,
    BIT_SHIFT_CT_AE_MODE_CONTROL,
    BIT_SHIFT_CT_AE_PRIORITY_CONTROL,
    BIT_SHIFT_CT_EXPOSURE_TIME_ABSOLUTE_CONTROL,
    BIT_SHIFT_CT_EXPOSURE_TIME_RELATIVE_CONTROL,
    BIT_SHIFT_CT_FOCUS_ABSOLUTE_CONTROL,
    BIT_SHIFT_CT_FOCUS_RELATIVE_CONTROL,
    BIT_SHIFT_CT_IRIS_ABSOLUTE_CONTROL,
    BIT_SHIFT_CT_IRIS_RELATIVE_CONTROL,
    BIT_SHIFT_CT_ZOOM_ABSOLUTE_CONTROL,
    BIT_SHIFT_CT_ZOOM_RELATIVE_CONTROL,
    BIT_SHIFT_CT_PANTILT_ABSOLUTE_CONTROL,
    BIT_SHIFT_CT_PANTILT_RELATIVE_CONTROL,
    BIT_SHIFT_CT_ROLL_ABSOLUTE_CONTROL,
    BIT_SHIFT_CT_ROLL_RELATIVE_CONTROL,

    BIT_SHIFT_CT_FOCUS_AUTO_CONTROL = 17,
    BIT_SHIFT_CT_PRIVACY_CONTROL,
    BIT_SHIFT_CT_FOCUS_SIMPLE_CONTROL,
    BIT_SHIFT_CT_DIGITAL_WINDOW_CONTROL,
    BIT_SHIFT_CT_REGION_OF_INTEREST_CONTROL
};

enum UVCProcessingUnitBitShift
{
    BIT_SHIFT_PU_BRIGHTNESS_CONTROL,
    BIT_SHIFT_PU_CONTRAST_CONTROL,
    BIT_SHIFT_PU_HUE_CONTROL,
    BIT_SHIFT_PU_SATURATION_CONTROL,
    BIT_SHIFT_PU_SHARPNESS_CONTROL,
    BIT_SHIFT_PU_GAMMA_CONTROL,
    BIT_SHIFT_PU_WHITE_BALANCE_TEMPERATURE_CONTROL,
    BIT_SHIFT_PU_WHITE_BALANCE_COMPONENT_CONTROL,
    BIT_SHIFT_PU_BACKLIGHT_COMPENSATION_CONTROL,
    BIT_SHIFT_PU_GAIN_CONTROL,
    BIT_SHIFT_PU_POWER_LINE_FREQUENCY_CONTROL,
    BIT_SHIFT_PU_HUE_AUTO_CONTROL,
    BIT_SHIFT_PU_WHITE_BALANCE_TEMPERATURE_AUTO_CONTROL,
    BIT_SHIFT_PU_WHITE_BALANCE_COMPONENT_AUTO_CONTROL,
    BIT_SHIFT_PU_DIGITAL_MULTIPLIER_CONTROL,
    BIT_SHIFT_PU_DIGITAL_MULTIPLIER_LIMIT_CONTROL,
    BIT_SHIFT_PU_ANALOG_VIDEO_STANDARD_CONTROL,
    BIT_SHIFT_PU_ANALOG_LOCK_STATUS_CONTROL,
    BIT_SHIFT_PU_CONTRAST_AUTO_CONTROL
};

enum UVCExposureMode
{
    EXPOSURE_MODE_MANUAL = 1,
    EXPOSURE_MODE_AUTO = 2,
    EXPOSURE_MODE_SHUTTER_PRIORITY = 4,
    EXPOSURE_MODE_APERTURE_PRIORITY = 8
};

enum UVCAutoFocusMode
{
    AUTO_FOCUS_MODE_FIXED,
    AUTO_FOCUS_MODE_AUTO
};

enum UVCSimpleFocusMode
{
    SIMPLE_FOCUS_MODE_FULL_RANGE,
    SIMPLE_FOCUS_MODE_MACRO,
    SIMPLE_FOCUS_MODE_PEOPLE,
    SIMPLE_FOCUS_MODE_SCENE
};

void
printCameraControlSupport(const uvc_input_terminal_t* inputTerminal, const uvc_processing_unit_t* processingUnit)
{
    LOG_D("UVC_CT_SCANNING_MODE_CONTROL supported: %s",             (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_SCANNING_MODE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_AE_MODE_CONTROL supported: %s",                   (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_AE_MODE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_AE_PRIORITY_CONTROL supported: %s",               (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_AE_PRIORITY_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_EXPOSURE_TIME_ABSOLUTE_CONTROL supported: %s",    (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_EXPOSURE_TIME_ABSOLUTE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_EXPOSURE_TIME_RELATIVE_CONTROL supported: %s",    (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_EXPOSURE_TIME_RELATIVE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_FOCUS_ABSOLUTE_CONTROL supported: %s",            (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_FOCUS_ABSOLUTE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_FOCUS_RELATIVE_CONTROL supported: %s",            (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_FOCUS_RELATIVE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_IRIS_ABSOLUTE_CONTROL supported: %s",             (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_IRIS_ABSOLUTE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_IRIS_RELATIVE_CONTROL supported: %s",             (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_IRIS_RELATIVE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_ZOOM_ABSOLUTE_CONTROL supported: %s",             (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_ZOOM_ABSOLUTE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_ZOOM_RELATIVE_CONTROL supported: %s",             (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_ZOOM_RELATIVE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_PANTILT_ABSOLUTE_CONTROL supported: %s",          (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_PANTILT_ABSOLUTE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_PANTILT_RELATIVE_CONTROL supported: %s",          (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_PANTILT_RELATIVE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_ROLL_ABSOLUTE_CONTROL supported: %s",             (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_ROLL_ABSOLUTE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_ROLL_RELATIVE_CONTROL supported: %s",             (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_ROLL_RELATIVE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_FOCUS_AUTO_CONTROL supported: %s",                (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_FOCUS_AUTO_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_PRIVACY_CONTROL supported: %s",                   (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_PRIVACY_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_FOCUS_SIMPLE_CONTROL supported: %s",              (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_FOCUS_SIMPLE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_DIGITAL_WINDOW_CONTROL supported: %s",            (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_DIGITAL_WINDOW_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_CT_REGION_OF_INTEREST_CONTROL supported: %s",        (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_REGION_OF_INTEREST_CONTROL)) ? "YES" : "NO");

    LOG_D("UVC_PU_BRIGHTNESS_CONTROL supported: %s",                        (processingUnit->bmControls & (1 << BIT_SHIFT_PU_BRIGHTNESS_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_CONTRAST_CONTROL supported: %s",                          (processingUnit->bmControls & (1 << BIT_SHIFT_PU_CONTRAST_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_HUE_CONTROL supported: %s",                               (processingUnit->bmControls & (1 << BIT_SHIFT_PU_HUE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_SATURATION_CONTROL supported: %s",                        (processingUnit->bmControls & (1 << BIT_SHIFT_PU_SATURATION_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_SHARPNESS_CONTROL supported: %s",                         (processingUnit->bmControls & (1 << BIT_SHIFT_PU_SHARPNESS_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_GAMMA_CONTROL supported: %s",                             (processingUnit->bmControls & (1 << BIT_SHIFT_PU_GAMMA_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_WHITE_BALANCE_TEMPERATURE_CONTROL supported: %s",         (processingUnit->bmControls & (1 << BIT_SHIFT_PU_WHITE_BALANCE_TEMPERATURE_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_WHITE_BALANCE_COMPONENT_CONTROL supported: %s",           (processingUnit->bmControls & (1 << BIT_SHIFT_PU_WHITE_BALANCE_COMPONENT_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_BACKLIGHT_COMPENSATION_CONTROL supported: %s",            (processingUnit->bmControls & (1 << BIT_SHIFT_PU_BACKLIGHT_COMPENSATION_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_GAIN_CONTROL supported: %s",                              (processingUnit->bmControls & (1 << BIT_SHIFT_PU_GAIN_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_POWER_LINE_FREQUENCY_CONTROL supported: %s",              (processingUnit->bmControls & (1 << BIT_SHIFT_PU_POWER_LINE_FREQUENCY_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_HUE_AUTO_CONTROL supported: %s",                          (processingUnit->bmControls & (1 << BIT_SHIFT_PU_HUE_AUTO_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_WHITE_BALANCE_TEMPERATURE_AUTO_CONTROL supported: %s",    (processingUnit->bmControls & (1 << BIT_SHIFT_PU_WHITE_BALANCE_TEMPERATURE_AUTO_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_WHITE_BALANCE_COMPONENT_AUTO_CONTROL supported: %s",      (processingUnit->bmControls & (1 << BIT_SHIFT_PU_WHITE_BALANCE_COMPONENT_AUTO_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_DIGITAL_MULTIPLIER_CONTROL supported: %s",                (processingUnit->bmControls & (1 << BIT_SHIFT_PU_DIGITAL_MULTIPLIER_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_DIGITAL_MULTIPLIER_LIMIT_CONTROL supported: %s",          (processingUnit->bmControls & (1 << BIT_SHIFT_PU_DIGITAL_MULTIPLIER_LIMIT_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_ANALOG_VIDEO_STANDARD_CONTROL supported: %s",             (processingUnit->bmControls & (1 << BIT_SHIFT_PU_ANALOG_VIDEO_STANDARD_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_ANALOG_LOCK_STATUS_CONTROL supported: %s",                (processingUnit->bmControls & (1 << BIT_SHIFT_PU_ANALOG_LOCK_STATUS_CONTROL)) ? "YES" : "NO");
    LOG_D("UVC_PU_CONTRAST_AUTO_CONTROL supported: %s",                     (processingUnit->bmControls & (1 << BIT_SHIFT_PU_CONTRAST_AUTO_CONTROL)) ? "supported" : "not supported");
}

uvc_frame_format
getUVCPixelFormat(Vuforia::Driver::PixelFormat vuforiaFormat)
{
    uvc_frame_format uvcFormat = UVC_FRAME_FORMAT_UNKNOWN;

    switch (vuforiaFormat)
    {
        case Vuforia::Driver::PixelFormat::YUYV:
            uvcFormat = UVC_FRAME_FORMAT_YUYV;
            break;
        default:
            LOG_D("No suitable UVC frame format for Vuforia frame format: %d", vuforiaFormat);
    }

    return uvcFormat;
}

Vuforia::Driver::PixelFormat
getVuforiaPixelFormat(uvc_frame_format uvcFormat)
{
    Vuforia::Driver::PixelFormat vuforiaFormat = Vuforia::Driver::PixelFormat::UNKNOWN;

    switch (uvcFormat)
    {
        case UVC_FRAME_FORMAT_YUYV:
            vuforiaFormat = Vuforia::Driver::PixelFormat::YUYV;
            break;
        default:
            LOG_D("No suitable Vuforia frame format for UVC image format: %d", uvcFormat);
    }

    return vuforiaFormat;
}

uint64_t
getCurrentTimestamp()
{
    // Vuforia timestamp is in 1ns unit
    struct timespec t;
    clock_gettime(CLOCK_MONOTONIC, &t);
    uint64_t timeNanoSecMonotonic = t.tv_sec * 1000000000ULL + t.tv_nsec;
    return timeNanoSecMonotonic;
}

// Wrapper that gets the JNIEnv pointer from JVM and attaches it to the current
// thread if necessary. In case the thread was attached, it will be released
// in the destructor.
class ScopedJNIEnv
{
public:
    ScopedJNIEnv(JavaVM* javaVM, jint jniVersion)
        : mJavaVM(javaVM)
    {
        if (mJavaVM->GetEnv((void**) &mJNIEnv, jniVersion) == JNI_EDETACHED)
        {
            mJavaVM->AttachCurrentThread(&mJNIEnv, nullptr);
            mThreadAttached = true;
        }
    }

    ~ScopedJNIEnv()
    {
        if (mThreadAttached)
        {
            mJavaVM->DetachCurrentThread();
        }
    }

    JNIEnv*
    operator->() const
    {
        return mJNIEnv;
    }

private:
    bool mThreadAttached = false;
    JNIEnv* mJNIEnv = nullptr;
    JavaVM* mJavaVM = nullptr;
};

// Callback that is called from libuvc when a new frame is available.
void
uvcCallbackFunc(uvc_frame_t* inFrame, void* user_ptr)
{
    UVCExternalCamera* uvcExternalCamera = static_cast<UVCExternalCamera*>(user_ptr);

    Vuforia::Driver::CameraFrame frame;
    frame.index = inFrame->sequence;
    frame.width = inFrame->width;
    frame.height = inFrame->height;
    frame.format = getVuforiaPixelFormat(inFrame->frame_format);
    frame.stride = inFrame->step;
    frame.buffer = reinterpret_cast<uint8_t*>(inFrame->data);
    frame.bufferSize = inFrame->data_bytes;
    // libuvc doesn't have proper timestamp implemented for the incoming frame
    // so we generate it ourselves here
    frame.timestamp = getCurrentTimestamp();
    frame.exposureTime = uvcExternalCamera->getExposureValue();
    frame.intrinsics = uvcExternalCamera->getCameraIntrinsics();

    Vuforia::Driver::CameraCallback* vuforiaCallback = uvcExternalCamera->getCallback();
    if (vuforiaCallback)
    {
        vuforiaCallback->onNewCameraFrame(&frame);
    }
    else
    {
        LOG_E("Camera frame callback to Vuforia is not found");
    }
}
}

UVCExternalCamera::UVCExternalCamera(Vuforia::Driver::PlatformData* platformData)
{
    if (platformData != nullptr)
    {
        mJniVersion = platformData->jniVersion;
        mActivity = platformData->activity;
        mJavaVM = platformData->javaVM;
    }
}

UVCExternalCamera::~UVCExternalCamera()
{
    stop();
    close();

    ScopedJNIEnv jniEnv(mJavaVM, mJniVersion);

    if (mUSBControllerClass != nullptr)
    {
        jniEnv->DeleteGlobalRef(mUSBControllerClass);
        mUSBControllerClass = nullptr;
    }

    if (mUSBControllerObj != nullptr)
    {
        jniEnv->DeleteGlobalRef(mUSBControllerObj);
        mUSBControllerObj = nullptr;
    }

    if (mCalibrationControllerClass != nullptr)
    {
        jniEnv->DeleteGlobalRef(mCalibrationControllerClass);
        mCalibrationControllerClass = nullptr;
    }

    if (mCalibrationControllerObj != nullptr)
    {
        jniEnv->DeleteGlobalRef(mCalibrationControllerObj);
        mCalibrationControllerObj = nullptr;
    }
}


//=============================================================================
// PUBLIC INTERFACE IMPLEMENTATION
//=============================================================================

bool
UVCExternalCamera::open()

{
    LOG_D("open() method invoke");

    uvc_error_t result;

    ScopedJNIEnv jniEnv(mJavaVM, mJniVersion);

    if (mUSBControllerClass == nullptr)
    {
        jclass usbControllerClass = jniEnv->FindClass(JAVA_USB_CONTROLLER_CLASS);
        if (usbControllerClass == nullptr)
        {
            LOG_E("Failed to get Java USBController class");
            return false;
        }

        mUSBControllerClass = (jclass) jniEnv->NewGlobalRef(usbControllerClass);
        jniEnv->DeleteLocalRef(usbControllerClass);
    }

    if (mUSBControllerObj == nullptr)
    {
        jmethodID mid_constructor = jniEnv->GetMethodID(mUSBControllerClass, "<init>", "(Landroid/app/Activity;)V");
        if (mid_constructor == nullptr)
        {
            LOG_E("Failed to get constructor method");
            return false;
        }

        jobject usbControllerObj = jniEnv->NewObject(mUSBControllerClass, mid_constructor, mActivity);
        if (usbControllerObj == nullptr)
        {
            LOG_E("Failed to get Java USBController object");
            return false;
        }

        mUSBControllerObj = (jobject) jniEnv->NewGlobalRef(usbControllerObj);
        jniEnv->DeleteLocalRef(usbControllerObj);
    }

    if (mCalibrationControllerClass == nullptr)
    {
        jclass calibrationControllerClass = jniEnv->FindClass(JAVA_CALIBRATION_CONTROLLER_CLASS);
        if (calibrationControllerClass == nullptr)
        {
            LOG_E("Failed to get Java CalibrationController class");
            return false;
        }

        mCalibrationControllerClass = (jclass) jniEnv->NewGlobalRef(calibrationControllerClass);
        jniEnv->DeleteLocalRef(calibrationControllerClass);
    }

    if (mCalibrationControllerObj == nullptr)
    {
        jmethodID mid_constructor = jniEnv->GetMethodID(mCalibrationControllerClass, "<init>", "(Landroid/app/Activity;)V");
        if (mid_constructor == nullptr)
        {
            LOG_E("Failed to get constructor method");
            return false;
        }

        jobject calibrationControllerObj = jniEnv->NewObject(mCalibrationControllerClass, mid_constructor, mActivity);
        if (calibrationControllerObj == nullptr)
        {
            LOG_E("Failed to get Java CalibrationController object");
            return false;
        }

        mCalibrationControllerObj = (jobject) jniEnv->NewGlobalRef(calibrationControllerObj);
        jniEnv->DeleteLocalRef(calibrationControllerObj);
    }

    result = UVC_ERROR_OTHER;

    // Iterate through all the usb devices, since we do not know which one is the camera
    int numDevices = getNumDevices();
    for (int idx = 0; idx < numDevices; idx++)
    {
        LOG_D("执行useDevice: %d", idx);
        if (!useDevice(idx))
        {
           
            LOG_E("Failed open Device: %d", idx);

            // Either we can't get hold of the device at this index
            // or the permission for this device is denied
            continue;
        }

        char* usbfs = getUSBFS();
        if (!usbfs)
        {
            LOG_E("Failed to get USBFS for device: %d", idx);
            continue;
        }

        if (mContext)
        {
            LOG_I("Previous uvc-context found, deinitalizing old one");
            uvc_exit(mContext);
            mContext = nullptr;
        }

        result = uvc_init2(&mContext, nullptr, usbfs);
        if (result != UVC_SUCCESS)
        {
            free(usbfs);
            LOG_E("Failed to initialize UVC : error %d", static_cast<int>(result));
            continue;
        }
        free(usbfs);

        result = uvc_get_device_with_fd(mContext, &mDevice, getVendorId(), getProductId(), nullptr, getFileDescriptor(), getBusNumber(), getDeviceNumber());
        if (result != UVC_SUCCESS)
        {
            LOG_E("Failed to get usb device for index: %d, error %d", idx, static_cast<int>(result));
            continue;
        }

        result = uvc_open(mDevice, &mDeviceHandle);
        if (result == UVC_SUCCESS)
        {
            // We successfully opened the first found usb camera
            LOG_I("Successfully opened usb device for index: %d", idx);
            break;
        }
        else
        {
            LOG_E("Failed to open usb device for index: %d, error %d", idx, static_cast<int>(result));
        }
    }

    if (result != UVC_SUCCESS)
    {
        // If we get failure here, it means that either:
        // 1. All the usb devices are NOT camera, or
        // 2. We fail to open all the connected usb cameras
        return false;
    }

    getSupportedCameraModes();

    // Log the camera capabilities
    uvc_print_diag(mDeviceHandle, stdout);
    printCameraControlSupport(uvc_get_input_terminals(mDeviceHandle), uvc_get_processing_units(mDeviceHandle));

    return true;
}

bool
UVCExternalCamera::close()
{
    if (mDeviceHandle != nullptr)
    {
        uvc_close(mDeviceHandle);
        mDeviceHandle = nullptr;
    }

    if (mDevice != nullptr)
    {
        uvc_unref_device(mDevice);
        mDevice = nullptr;
    }

    if (mContext != nullptr)
    {
        uvc_exit(mContext);
        mContext = nullptr;
    }

    return true;
}

bool
UVCExternalCamera::start(Vuforia::Driver::CameraMode cameraMode, Vuforia::Driver::CameraCallback* cb)
{
    if (mDeviceHandle == nullptr)
    {
        LOG_E("Failed attempt to start camera. No device handle found.");
        return false;
    }
    mCallback = cb;

    // Get camera calibration for the current opened device and the specified width and height
    mCameraIntrinsics = getCalibrationValue(getVendorId(), getProductId(), cameraMode.width, cameraMode.height);

    // Get stream control for specified parameters
    uvc_error_t result = uvc_get_stream_ctrl_format_size(mDeviceHandle, &mStreamControl, getUVCPixelFormat(cameraMode.format), cameraMode.width, cameraMode.height, cameraMode.fps);
    if (result != UVC_SUCCESS)
    {
        LOG_E("Failed to get camera stream control : error %d", static_cast<int>(result));
        return false;
    }

    // Start the camera capture
    result = uvc_start_streaming(mDeviceHandle, &mStreamControl, &uvcCallbackFunc, this, 0);
    if (result != UVC_SUCCESS)
    {
        LOG_E("Failed to start camera stream : error %d", static_cast<int>(result));
        return false;
    }

    return true;
}

bool
UVCExternalCamera::stop()
{
    if (mDeviceHandle == nullptr)
    {
        LOG_E("Failed attempt to stop camera. No device handle found.");
        return false;
    }

    uvc_stop_streaming(mDeviceHandle);
    return true;
}

uint32_t
UVCExternalCamera::getNumSupportedCameraModes()
{
    return mSupportedCameraModes.size();
}

bool
UVCExternalCamera::getSupportedCameraMode(uint32_t index, Vuforia::Driver::CameraMode* out)
{
    if (index >= mSupportedCameraModes.size())
    {
        LOG_E("Invalid camera mode index");
        return false;
    }

    *out = mSupportedCameraModes[index];
    return true;
}

bool
UVCExternalCamera::supportsExposureMode(Vuforia::Driver::ExposureMode parameter)
{
    const uvc_input_terminal_t* inputTerminal = uvc_get_input_terminals(mDeviceHandle);
    if (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_AE_MODE_CONTROL))
    {
        uint8_t supportedMode = 0;
        uvc_error_t result = uvc_get_ae_mode(mDeviceHandle, &supportedMode, UVC_GET_RES);

        switch (parameter)
        {
            case Vuforia::Driver::ExposureMode::AUTO:
                // Unless otherwise stated auto exposure is always continuous, not only for one exposure
                return false;
            case Vuforia::Driver::ExposureMode::CONTINUOUS_AUTO:
                // Both usb exposure mode AUTO and APERTURE PRIORITY are essentially continuous auto exposure
                // 1. AUTO - auto exposure time, auto iris
                // 2. APERTURE_PRIORITY - auto exposure time, manual iris
                return supportedMode & EXPOSURE_MODE_AUTO || supportedMode & EXPOSURE_MODE_APERTURE_PRIORITY;
            case Vuforia::Driver::ExposureMode::MANUAL:
                return supportedMode & EXPOSURE_MODE_MANUAL;
            case Vuforia::Driver::ExposureMode::SHUTTER_PRIORITY:
                return supportedMode & EXPOSURE_MODE_SHUTTER_PRIORITY;
            default:
                LOG_E("Unknown exposure mode: %d", parameter);
        }
    }

    return false;
}

Vuforia::Driver::ExposureMode
UVCExternalCamera::getExposureMode()
{
    uint8_t mode = 0;
    uvc_error_t result = uvc_get_ae_mode(mDeviceHandle, &mode, UVC_GET_CUR);
    if (result != UVC_SUCCESS)
    {
        LOG_E("Failed to get exposure mode : error %d", static_cast<int>(result));
        return Vuforia::Driver::ExposureMode::UNKNOWN;
    }

    switch (mode)
    {
        case EXPOSURE_MODE_MANUAL:
            return Vuforia::Driver::ExposureMode::MANUAL;
        case EXPOSURE_MODE_AUTO:
        case EXPOSURE_MODE_APERTURE_PRIORITY:
            // Both usb exposure mode AUTO and APERTURE PRIORITY are essentially continuous auto exposure
            // 1. AUTO - auto exposure time, auto iris
            // 2. APERTURE_PRIORITY - auto exposure time, manual iris
            return Vuforia::Driver::ExposureMode::CONTINUOUS_AUTO;
        case EXPOSURE_MODE_SHUTTER_PRIORITY:
            return Vuforia::Driver::ExposureMode::SHUTTER_PRIORITY;
        default:
            LOG_E("Unknown exposure mode : %d", mode);
    }

    return Vuforia::Driver::ExposureMode::UNKNOWN;
}

bool
UVCExternalCamera::setExposureMode(Vuforia::Driver::ExposureMode mode)
{
    uvc_error_t result;

    switch (mode)
    {
        case Vuforia::Driver::ExposureMode::AUTO:
            // Unless otherwise stated auto exposure is always continuous, not only for one exposure
            LOG_E("Auto exposure mode for only one exposure is not supported");
            return false;
        case Vuforia::Driver::ExposureMode::CONTINUOUS_AUTO:
        {
            // Since both usb exposure mode AUTO and APERTURE PRIORITY are essentially continuous auto exposure
            // we try both of them and see which one is going through

            // Try to set to AUTO mode
            result = uvc_set_ae_mode(mDeviceHandle, EXPOSURE_MODE_AUTO);
            if (result == UVC_SUCCESS)
            {
                LOG_D("Successfully set exposure mode to AUTO");
                return true;
            }
            else
            {
                LOG_E("Failed to set exposure mode to AUTO : error %d. Trying to set exposure mode to APERTURE PRIORITY.", static_cast<int>(result));
            }

            // If AUTO is not successful, try to set to APERTURE PRIORITY mode
            result = uvc_set_ae_mode(mDeviceHandle, EXPOSURE_MODE_APERTURE_PRIORITY);
            if (result == UVC_SUCCESS)
            {
                LOG_D("Successfully set exposure mode to APERTURE PRIORITY");
                return true;
            }
            else
            {
                LOG_E("Failed to set exposure mode to APERTURE PRIORITY : error %d", static_cast<int>(result));
            }

            // If we are here, our attempt to set to either AUTO or APERTURE PRIORITY both ends in failure
            LOG_E("Failed to set exposure mode to AUTO or APERTURE PRIORITY : error %d", static_cast<int>(result));
            return false;
        }
        case Vuforia::Driver::ExposureMode::MANUAL:
        {
            result = uvc_set_ae_mode(mDeviceHandle, EXPOSURE_MODE_MANUAL);
            if (result != UVC_SUCCESS)
            {
                LOG_E("Failed to set exposure mode to MANUAL : error %d", static_cast<int>(result));
                return false;
            }

            return true;
        }
        case Vuforia::Driver::ExposureMode::SHUTTER_PRIORITY:
        {
            result = uvc_set_ae_mode(mDeviceHandle, EXPOSURE_MODE_SHUTTER_PRIORITY);
            if (result != UVC_SUCCESS)
            {
                LOG_E("Failed to set exposure mode to SHUTTER PRIORITY : error %d", static_cast<int>(result));
                return false;
            }

            return true;
        }
        default:
            LOG_E("Unknown exposure mode : %d", mode);
    }

    return false;
}

bool
UVCExternalCamera::supportsExposureValue()
{
    const uvc_input_terminal_t* inputTerminal = uvc_get_input_terminals(mDeviceHandle);
    return inputTerminal->bmControls & (1 << BIT_SHIFT_CT_EXPOSURE_TIME_ABSOLUTE_CONTROL);
}

uint64_t
UVCExternalCamera::getExposureValueMin()
{
    int minExposureTime = 0;

    uvc_error_t result = uvc_get_exposure_abs(mDeviceHandle, &minExposureTime, UVC_GET_MIN);
    if (result != UVC_SUCCESS)
    {
        LOG_E("Failed to get min exposure time : error %d", static_cast<int>(result));
    }

    // UVC exposure time unit is 100us, while Vuforia expected unit is 1ns
    return static_cast<uint64_t>(minExposureTime) * 100000;
}

uint64_t
UVCExternalCamera::getExposureValueMax()
{
    int maxExposureTime = 0;

    uvc_error_t result = uvc_get_exposure_abs(mDeviceHandle, &maxExposureTime, UVC_GET_MAX);
    if (result != UVC_SUCCESS)
    {
        LOG_E("Failed to get max exposure time : error %d", static_cast<int>(result));
    }

    // UVC exposure time unit is 100us, while Vuforia expected unit is 1ns
    return static_cast<uint64_t>(maxExposureTime) * 100000;
}

uint64_t
UVCExternalCamera::getExposureValue()
{
    int exposureTime = 0;

    uvc_error_t result = uvc_get_exposure_abs(mDeviceHandle, &exposureTime, UVC_GET_CUR);
    if (result != UVC_SUCCESS)
    {
        LOG_E("Failed to get exposure time : error %d", static_cast<int>(result));
    }

    // UVC exposure time unit is 100us, while Vuforia expected unit is 1ns
    return static_cast<uint64_t>(exposureTime) * 100000;
}

bool
UVCExternalCamera::setExposureValue(uint64_t exposureTime)
{
    uvc_error_t result;

    // UVC exposure time unit is 100us, while Vuforia expected unit is 1ns
    uint32_t value = static_cast<uint32_t>(exposureTime / 100000);
    result = uvc_set_exposure_abs(mDeviceHandle, value);
    if (result != UVC_SUCCESS)
    {
        LOG_E("Failed to set exposure time to %d : error %d", value, static_cast<int>(result));
        return false;
    }

    return true;
}

bool
UVCExternalCamera::supportsFocusMode(Vuforia::Driver::FocusMode parameter)
{
    const uvc_input_terminal_t* inputTerminal = uvc_get_input_terminals(mDeviceHandle);

    switch (parameter)
    {
        case Vuforia::Driver::FocusMode::AUTO:
            // Unless otherwise stated auto focus is always continuous, not only for one exposure
            return false;
        case Vuforia::Driver::FocusMode::CONTINUOUS_AUTO:
            return (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_FOCUS_AUTO_CONTROL));
        case Vuforia::Driver::FocusMode::MACRO:
        case Vuforia::Driver::FocusMode::INFINITY_FOCUS:
#if SIMPLE_FOCUS_METHODS_IMPLEMENTED
            return (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_FOCUS_SIMPLE_CONTROL));
#else
            return false;
#endif
        case Vuforia::Driver::FocusMode::FIXED:
            // Fixed focus mode is always supported. But whether we support
            // getting / setting the absolute focus value is another matter.
            return true;
        default:
            LOG_E("Unknown focus mode: %d", parameter);
    }

    return false;
}

Vuforia::Driver::FocusMode
UVCExternalCamera::getFocusMode()
{
    uvc_error_t result;
    uint8_t state = 0;

    // Check whether auto focus is on
    result = uvc_get_focus_auto(mDeviceHandle, &state, UVC_GET_CUR);
    if (result == UVC_SUCCESS && state == 1)
    {
        return Vuforia::Driver::FocusMode::CONTINUOUS_AUTO;
    }

#if SIMPLE_FOCUS_METHODS_IMPLEMENTED
    // Check whether simple focus mode is set to macro or scene
    result = uvc_get_focus_simple_range(mDeviceHandle, &state, UVC_GET_CUR);
    if (result == UVC_SUCCESS)
    {
        switch (state)
        {
            case SIMPLE_FOCUS_MODE_MACRO:
                return Vuforia::Driver::FocusMode::MACRO;
            case SIMPLE_FOCUS_MODE_SCENE:
                return Vuforia::Driver::FocusMode::INFINITY_FOCUS;
            default:
                break;
        }
    }
#endif

    // If all else failed, then we are essentially on fixed focus mode
    return Vuforia::Driver::FocusMode::FIXED;
}

bool
UVCExternalCamera::setFocusMode(Vuforia::Driver::FocusMode mode)
{
    uvc_error_t result;

    switch (mode)
    {
        case Vuforia::Driver::FocusMode::AUTO:
            // Unless otherwise stated auto focus is always continuous, not only for one exposure
            LOG_E("Auto focus mode for only one exposure is not supported");
            return false;
        case Vuforia::Driver::FocusMode::CONTINUOUS_AUTO:
        {
            result = uvc_set_focus_auto(mDeviceHandle, AUTO_FOCUS_MODE_AUTO);
            if (result != UVC_SUCCESS)
            {
                LOG_E("Failed to set auto focus mode to AUTO : error %d", static_cast<int>(result));
                return false;
            }

            return true;
        }
        case Vuforia::Driver::FocusMode::MACRO:
        {
#if SIMPLE_FOCUS_METHODS_IMPLEMENTED
            result = uvc_set_focus_simple_range(mDeviceHandle, SIMPLE_FOCUS_MODE_MACRO);
            if (result != UVC_SUCCESS)
            {
                LOG_E("Failed to set simple focus mode to MACRO : error %d", static_cast<int>(result));
                return false;
            }

            return true;
#else
            LOG_D("Implementation to set simple focus mode to MACRO is not found");
            return false;
#endif
        }
        case Vuforia::Driver::FocusMode::INFINITY_FOCUS:
        {
#if SIMPLE_FOCUS_METHODS_IMPLEMENTED
            result = uvc_set_focus_simple_range(mDeviceHandle, SIMPLE_FOCUS_MODE_SCENE);
            if (result != UVC_SUCCESS)
            {
                LOG_E("Failed to set simple focus mode to SCENE : error %d", static_cast<int>(result));
                return false;
            }

            return true;
#else
            LOG_D("Implementation to set simple focus mode to SCENE is not found");
            return false;
#endif
        }
        case Vuforia::Driver::FocusMode::FIXED:
        {
            // If we support auto focus, set the auto focus mode to FIXED
            const uvc_input_terminal_t* inputTerminal = uvc_get_input_terminals(mDeviceHandle);
            if (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_FOCUS_AUTO_CONTROL))
            {
                result = uvc_set_focus_auto(mDeviceHandle, AUTO_FOCUS_MODE_FIXED);
                if (result != UVC_SUCCESS)
                {
                    LOG_E("Failed to set auto focus mode to FIXED : error %d", static_cast<int>(result));
                    return false;
                }
            }

// If we support simple focus, set it to FULL_RANGE mode
#if SIMPLE_FOCUS_METHODS_IMPLEMENTED
            if (inputTerminal->bmControls & (1 << BIT_SHIFT_CT_FOCUS_SIMPLE_CONTROL))
            {
                result = uvc_set_focus_simple_range(mDeviceHandle, SIMPLE_FOCUS_MODE_FULL_RANGE);
                if (result != UVC_SUCCESS)
                {
                    LOG_E("Failed to set simple focus mode to FULL_RANGE : error %d", static_cast<int>(result));
                    return false;
                }
            }
#endif

            return true;
        }
        default:
            LOG_E("Unknown focus mode: %d", mode);
    }

    return false;
}

bool
UVCExternalCamera::supportsFocusValue()
{
    const uvc_input_terminal_t* inputTerminal = uvc_get_input_terminals(mDeviceHandle);
    return inputTerminal->bmControls & (1 << BIT_SHIFT_CT_FOCUS_ABSOLUTE_CONTROL);
}

float
UVCExternalCamera::getFocusValueMin()
{
    short minFocusVal = -1;

    uvc_error_t result = uvc_get_focus_abs(mDeviceHandle, &minFocusVal, UVC_GET_MIN);
    if (result != UVC_SUCCESS)
    {
        LOG_E("Failed to get min focus value : error %d", static_cast<int>(result));
    }

    return minFocusVal;
}

float
UVCExternalCamera::getFocusValueMax()
{
    short maxFocusVal = -1;

    uvc_error_t result = uvc_get_focus_abs(mDeviceHandle, &maxFocusVal, UVC_GET_MAX);
    if (result != UVC_SUCCESS)
    {
        LOG_E("Failed to get max focus value : error %d", static_cast<int>(result));
    }

    return maxFocusVal;
}

float
UVCExternalCamera::getFocusValue()
{
    short focusVal = -1;

    uvc_error_t result = uvc_get_focus_abs(mDeviceHandle, &focusVal, UVC_GET_CUR);
    if (result != UVC_SUCCESS)
    {
        LOG_E("Failed to get focus value : error %d", static_cast<int>(result));
    }

    return focusVal;
}

bool
UVCExternalCamera::setFocusValue(float value)
{
    short focusVal = static_cast<short>(value);

    uvc_error_t result = uvc_set_focus_abs(mDeviceHandle, focusVal);
    if (result != UVC_SUCCESS)
    {
        LOG_E("Failed to set focus value to %d : error %d", focusVal, static_cast<int>(result));
        return false;
    }

    return true;
}


//=============================================================================
// PUBLIC METHODS USED BY THE CALLBACK
//=============================================================================

Vuforia::Driver::CameraCallback*
UVCExternalCamera::getCallback()
{
    return mCallback;
}

Vuforia::Driver::CameraIntrinsics
UVCExternalCamera::getCameraIntrinsics()
{
    return mCameraIntrinsics;
}

//=============================================================================
// PRIVATE METHODS
//=============================================================================

void
UVCExternalCamera::getSupportedCameraModes()
{
    mSupportedCameraModes.clear();

    for (const uvc_format_desc_t* formatDesc = uvc_get_format_descs(mDeviceHandle); formatDesc != nullptr; formatDesc = formatDesc->next)
    {
        if (formatDesc->bDescriptorSubtype != UVC_VS_FORMAT_UNCOMPRESSED) {
            continue;
        }

        Vuforia::Driver::PixelFormat pixelFormat = Vuforia::Driver::PixelFormat::UNKNOWN;
        std::string fourccFormat(formatDesc->fourccFormat, formatDesc->fourccFormat + sizeof(formatDesc->fourccFormat));

        if (fourccFormat == "YUY2") {
            pixelFormat = Vuforia::Driver::PixelFormat::YUYV;
        }

        if (pixelFormat == Vuforia::Driver::PixelFormat::UNKNOWN) {
            LOG_D("Frame format %s is not currently supported by Vuforia. Skipping.", formatDesc->fourccFormat);
            continue;
        }

        for (uvc_frame_desc_t* frameDesc = formatDesc->frame_descs; frameDesc != nullptr; frameDesc = frameDesc->next)
        {
            if (frameDesc->bDescriptorSubtype != UVC_VS_FRAME_UNCOMPRESSED) {
                continue;
            }

            for (uint32_t* intervalPtr = frameDesc->intervals; intervalPtr != nullptr; intervalPtr++)
            {
                Vuforia::Driver::CameraMode mode;
                mode.format = pixelFormat;
                mode.fps = 10000000 / (*intervalPtr); // UVC interval unit is 100ns
                mode.width = frameDesc->wWidth;
                mode.height = frameDesc->wHeight;

                mSupportedCameraModes.push_back(mode);
                break;
            }
        }
    }
}

//=============================================================================
// PRIVATE JNI METHODS
//=============================================================================

int
UVCExternalCamera::getNumDevices()
{
    ScopedJNIEnv jniEnv(mJavaVM, mJniVersion);

    jmethodID mid_getNumDevices = jniEnv->GetMethodID(mUSBControllerClass, "getNumDevices", "()I");
    if (mid_getNumDevices == nullptr)
    {
        LOG_E("Failed to get 'getNumDevices' method");
        return -1;
    }

    jint numDevices = jniEnv->CallIntMethod(mUSBControllerObj, mid_getNumDevices);
    LOG_D("Number of USB Devices: %d", numDevices);

    return numDevices;
}

bool
UVCExternalCamera::useDevice(int index)
{
    ScopedJNIEnv jniEnv(mJavaVM, mJniVersion);

    jmethodID mid_useDevice = jniEnv->GetMethodID(mUSBControllerClass, "useDevice", "(I)Z");
    if (mid_useDevice == nullptr)
    {
        LOG_E("Failed to get 'useDevice' method");
        return false;
    }

    jboolean result = jniEnv->CallBooleanMethod(mUSBControllerObj, mid_useDevice, index);
    return result;
}

int
UVCExternalCamera::getVendorId()
{
    ScopedJNIEnv jniEnv(mJavaVM, mJniVersion);

    jmethodID mid_getVendorId = jniEnv->GetMethodID(mUSBControllerClass, "getVendorId", "()I");
    if (mid_getVendorId == nullptr)
    {
        LOG_E("Failed to get 'getVendorId' method");
        return -1;
    }

    jint vendorId = jniEnv->CallIntMethod(mUSBControllerObj, mid_getVendorId);
    LOG_D("VendorId: 0x%04x", vendorId);

    return vendorId;
}

int
UVCExternalCamera::getProductId()
{
    ScopedJNIEnv jniEnv(mJavaVM, mJniVersion);

    jmethodID mid_getProductId = jniEnv->GetMethodID(mUSBControllerClass, "getProductId", "()I");
    if (mid_getProductId == nullptr)
    {
        LOG_E("Failed to get 'getProductId' method");
        return -1;
    }

    jint productId = jniEnv->CallIntMethod(mUSBControllerObj, mid_getProductId);
    LOG_D("ProductId: 0x%04x", productId);

    return productId;
}

int
UVCExternalCamera::getFileDescriptor()
{
    ScopedJNIEnv jniEnv(mJavaVM, mJniVersion);

    jmethodID mid_getFileDescriptor = jniEnv->GetMethodID(mUSBControllerClass, "getFileDescriptor", "()I");
    if (mid_getFileDescriptor == nullptr)
    {
        LOG_E("Failed to get 'getFileDescriptor' method");
        return -1;
    }

    jint fileDescriptor = jniEnv->CallIntMethod(mUSBControllerObj, mid_getFileDescriptor);
    LOG_D("FileDescriptor: %d", fileDescriptor);

    return fileDescriptor;
}

char*
UVCExternalCamera::getUSBFS()
{
    ScopedJNIEnv jniEnv(mJavaVM, mJniVersion);

    jmethodID mid_getUSBFS = jniEnv->GetMethodID(mUSBControllerClass, "getUSBFS", "()Ljava/lang/String;");
    if (mid_getUSBFS == nullptr)
    {
        LOG_E("Failed to get 'getUSBFS' method");
        return nullptr;
    }

    jstring jstr_usbfs = (jstring) jniEnv->CallObjectMethod(mUSBControllerObj, mid_getUSBFS);
    if (jstr_usbfs == nullptr)
    {
        LOG_E("Failed to get USBFS");
        return nullptr;
    }

    jboolean isCopy;
    const char* utf_usbfs = jniEnv->GetStringUTFChars(jstr_usbfs, &isCopy);
    if (utf_usbfs == nullptr)
    {
        LOG_E("Failed to convert USBFS to UTF char");
        return nullptr;
    }

    char* usbfs = strdup(utf_usbfs);
    LOG_D("USBFS: %s", usbfs);

    jniEnv->ReleaseStringUTFChars(jstr_usbfs, utf_usbfs);
    jniEnv->DeleteLocalRef(jstr_usbfs);

    return usbfs;
}

int
UVCExternalCamera::getBusNumber()
{
    ScopedJNIEnv jniEnv(mJavaVM, mJniVersion);

    jmethodID mid_getBusNumber = jniEnv->GetMethodID(mUSBControllerClass, "getBusNumber", "()I");
    if (mid_getBusNumber == nullptr)
    {
        LOG_E("Failed to get 'getBusNumber' method");
        return -1;
    }

    jint busNumber = jniEnv->CallIntMethod(mUSBControllerObj, mid_getBusNumber);
    LOG_D("BusNumber: %d", busNumber);

    return busNumber;
}

int
UVCExternalCamera::getDeviceNumber()
{
    ScopedJNIEnv jniEnv(mJavaVM, mJniVersion);

    jmethodID mid_getDeviceNumber = jniEnv->GetMethodID(mUSBControllerClass, "getDeviceNumber", "()I");
    if (mid_getDeviceNumber == nullptr)
    {
        LOG_E("Failed to get 'getDeviceNumber' method");
        return -1;
    }

    jint deviceNumber = jniEnv->CallIntMethod(mUSBControllerObj, mid_getDeviceNumber);
    LOG_D("DeviceNumber: %d", deviceNumber);

    return deviceNumber;
}

Vuforia::Driver::CameraIntrinsics
UVCExternalCamera::getCalibrationValue(int vid, int pid, int width, int height)
{
    ScopedJNIEnv jniEnv(mJavaVM, mJniVersion);

    Vuforia::Driver::CameraIntrinsics intrinsics; // default intrinsics with all zero values

    jmethodID mid_getCalibrationValue = jniEnv->GetMethodID(mCalibrationControllerClass, "getCalibrationValue", "(IIII)[F");
    if (mid_getCalibrationValue == nullptr)
    {
        LOG_E("Failed to get 'getCalibrationValue' method");
        return intrinsics;
    }

    jfloatArray calibrationValue = (jfloatArray) jniEnv->CallObjectMethod(mCalibrationControllerObj, mid_getCalibrationValue, vid, pid, width, height);
    if (calibrationValue == nullptr)
    {
        LOG_E("No calibration value for [VID: %d, PID: %d, Width: %d, Height: %d]", vid, pid, width, height);
        return intrinsics;
    }

    if (jniEnv->GetArrayLength(calibrationValue) != 12)
    {
        LOG_E("Calibration value for [VID: %d, PID: %d, Width: %d, Height: %d] is not of a correct length", vid, pid, width, height);
        return intrinsics;
    }

    jfloat* calibrationArr = jniEnv->GetFloatArrayElements(calibrationValue, 0);

    intrinsics.principalPointX = calibrationArr[0];
    intrinsics.principalPointY = calibrationArr[1];
    intrinsics.focalLengthX = calibrationArr[2];
    intrinsics.focalLengthY = calibrationArr[3];
    memcpy(intrinsics.distortionCoefficients, &(calibrationArr[4]), 8 * sizeof(float));

    jniEnv->ReleaseFloatArrayElements(calibrationValue, calibrationArr, 0);

    return intrinsics;
}
