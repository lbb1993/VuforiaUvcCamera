#===============================================================================
# Copyright (c) 2018 PTC Inc. All Rights Reserved.
#
# Confidential and Proprietary - Protected under copyright and other laws.
# Vuforia is a trademark of PTC Inc., registered in the United States and other
# countries.
#===============================================================================
import argparse
import os
import subprocess

VALID_ARCHS = ["armeabi-v7a", "arm64-v8a", "x86"]

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("-a", "--arch",
                        help="Architectures. Comma separated list of: armeabi-v7a, arm64-v8a, x86",
                        default="armeabi-v7a,arm64-v8a,x86")

    parser.add_argument("-i", "--install",
                        help="Install directory.",
                        default=os.path.join("build"))

    parser.add_argument("-o", "--output",
                        help="Location of generated build files.",
                        default=os.path.join("build"))

    parser.add_argument("-bt", "--build-type",
                        help="Build type. Release or Debug",
                        choices=["Release", "Debug"],
                        default="Release")

    parser.add_argument("-vh", "--vuforia-header-dir",
                        help="Directory that contains Vuforia/Driver/Driver.h.",
                        default=os.path.join("..", "..", "build", "include"))

    parser.add_argument("-ue", "--uvc-external-dir",
                        help="Directory containing libusb, libuvc and libjpeg-turbo build files. By default this expects the UVCCamera-repository layout and points to UVCCamera/libuvccamera/src/main/jni",
                        default=os.path.join("UVCCamera", "libuvccamera", "src", "main", "jni"))

    args = parser.parse_args()

    arch_string = args.arch
    if arch_string:
        archs = arch_string.split(",")
    else:
        archs = VALID_ARCHS
        print("No architectures selected. using defaults: {0}".format(str(archs)))

    for arch in archs:
        if arch not in VALID_ARCHS:
            print("error: Invalid architecture: {0}".format(arch))
            exit(1)

    cwd = os.getcwd()
    platform = "android"
    output_rootdir = os.path.abspath(args.output)
    install_rootdir = os.path.abspath(args.install)
    build_type = args.build_type
    vuforia_header_dir = os.path.abspath(args.vuforia_header_dir)
    android_toolchain_dir = os.path.join(cwd, "cmake")
    external_uvc_dir = os.path.abspath(args.uvc_external_dir)

    # If we're on windows fix up paths to remove backslashes that break cmake install
    if os.name == 'nt':
        install_rootdir = install_rootdir.replace('\\', '/')
        vuforia_header_dir = vuforia_header_dir.replace('\\', '/')
        external_uvc_dir = external_uvc_dir.replace('\\', '/')
    
    if not os.path.exists(output_rootdir):
        os.makedirs(output_rootdir)

    for arch in archs:
        print("Generating arch: {0}".format(arch))
        output_dir = os.path.join(output_rootdir, platform, arch)
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)

        toolchain_file = os.path.join(android_toolchain_dir, "android.toolchain.{0}.cmake".format(arch))
        generator = "Ninja"

        ret = subprocess.call(
            ["cmake",
              "-G", generator,
            "-DVUFORIA_HEADER_DIR='{0}'".format(vuforia_header_dir),
            "-DEXTERNAL_UVC_DIR='{0}'".format(external_uvc_dir),
            "-DCMAKE_INSTALL_PREFIX='{0}'".format(install_rootdir),
            "-DCMAKE_TOOLCHAIN_FILE='{0}'".format(toolchain_file),
            "-DCMAKE_BUILD_TYPE={0}".format(build_type),
            cwd],
            cwd=output_dir)

        if ret != 0:
            print("error: Project generation with cmake failed.")
            exit(1)

        print("Building arch: {0}".format(arch))

        ret = subprocess.call(
            ["cmake",
            "--build", ".",
            "--target", "install"],
            cwd=output_dir)

        if ret != 0:
            print("error: Project generation with cmake failed.")
            exit(1)

    print("Project generation and compilation succeeeded.")
    exit(0)
