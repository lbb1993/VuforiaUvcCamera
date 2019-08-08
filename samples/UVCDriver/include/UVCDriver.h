/*===============================================================================
Copyright (c) 2018 PTC Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/

#ifndef _UVC_VUFORIA_DRIVER_H_
#define _UVC_VUFORIA_DRIVER_H_

#include "UVCExternalCamera.h"
#include <Vuforia/Driver/Driver.h>

/// UVCDriver that implements the VuforiaDriver base class.
/**
 * This class is used for constructing and destroying the UVCDriver specific data source objects.
 *
 * The documentation of the public methods can be found in Vuforia/Driver/Driver.h header.
 */
class UVCDriver : public Vuforia::Driver::VuforiaDriver
{
public:
    UVCDriver(Vuforia::Driver::PlatformData* platformData);
    ~UVCDriver();

    Vuforia::Driver::ExternalCamera* VUFORIA_DRIVER_CALLING_CONVENTION createExternalCamera() override;

    void VUFORIA_DRIVER_CALLING_CONVENTION destroyExternalCamera(Vuforia::Driver::ExternalCamera* instance) override;

private:
    Vuforia::Driver::PlatformData*  mPlatformData{ nullptr };
    UVCExternalCamera*              mExternalCamera{ nullptr };
};

#endif // _UVC_VUFORIA_DRIVER_H_
