/*===============================================================================
Copyright (c) 2018 PTC Inc. All Rights Reserved.

Confidential and Proprietary - Protected under copyright and other laws.
Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/

#include "UVCDriver.h"
#include <string>

namespace
{
UVCDriver* g_UVCVuforiaDriverInstance = nullptr;
}


//=============================================================================
// VUFORIA EXTERNAL PROVIDER-API IMPLEMENTATION
//=============================================================================
extern "C"
{
Vuforia::Driver::VuforiaDriver*
vuforiaDriver_init(Vuforia::Driver::PlatformData* platformData, void* userdata)
{
    if (g_UVCVuforiaDriverInstance == nullptr)
    {
        g_UVCVuforiaDriverInstance = new UVCDriver(platformData);
        return g_UVCVuforiaDriverInstance;
    }

    // Attempting to init multiple instances is considered an error
    return nullptr;
}

void
vuforiaDriver_deinit(Vuforia::Driver::VuforiaDriver* instance)
{
    if (instance == g_UVCVuforiaDriverInstance)
    {
        delete static_cast<UVCDriver*>(instance);
        g_UVCVuforiaDriverInstance = nullptr;
    }
}

uint32_t
vuforiaDriver_getAPIVersion()
{
    return Vuforia::Driver::VUFORIA_DRIVER_API_VERSION;
}

uint32_t
vuforiaDriver_getLibraryVersion(char* outString, const uint32_t maxLength)
{
    std::string versionCode = "UVCExternalCamera-v2";
    uint32_t numBytes = versionCode.size() > maxLength ? maxLength : versionCode.size();
    memcpy(outString, versionCode.c_str(), numBytes);
    return numBytes;
}
}


//=============================================================================
// PUBLIC INTERFACE IMPLEMENTATION
//=============================================================================

UVCDriver::UVCDriver(Vuforia::Driver::PlatformData* platformData)
    : mPlatformData(platformData)
{
}

UVCDriver::~UVCDriver()
{
}

Vuforia::Driver::ExternalCamera*
UVCDriver::createExternalCamera()
{
    if (mExternalCamera == nullptr)
    {
        mExternalCamera = new UVCExternalCamera(mPlatformData);
        return mExternalCamera;
    }

    // Creating multiple cameras considered an error
    return nullptr;
}


void
UVCDriver::destroyExternalCamera(Vuforia::Driver::ExternalCamera* instance)
{
    if (instance == mExternalCamera)
    {
        delete static_cast<UVCExternalCamera*>(instance);
        mExternalCamera = nullptr;
    }
}
