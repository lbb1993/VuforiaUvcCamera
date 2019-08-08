/*===============================================================================
Copyright (c) 2018 PTC Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/

package com.vuforia.samples.uvcDriver;

import android.app.Activity;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import java.io.InputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

public final class CalibrationController
{
    private static final String MODULE_TAG = "Vuforia-UVCDriver";
    private static final String CALIBRATION_XML_FILENAME = "ExternalCameraCalibration.xml";

    private static final String VENDOR_ID_MICROSOFT = "0x045E";
    private static final String VENDOR_ID_LOGITECH = "0x046D";

    private static final String PRODUCT_ID_MICROSOFT_LIFECAM_HD_3000_1 = "0x0779";
    private static final String PRODUCT_ID_MICROSOFT_LIFECAM_HD_3000_2 = "0x0810";
    private static final String PRODUCT_ID_LOGITECH_C310_HD = "0x081B";

    private class Calibration
    {
        public int[] frameSize;
        public float[] principalPoint;
        public float[] focalLength;
        public float[] distortionCoefficients;

        public Calibration(int[] frameSize, float[] principalPoint, float[] focalLength, float[] distortionCoefficients)
        {
            this.frameSize = frameSize;
            this.principalPoint = principalPoint;
            this.focalLength = focalLength;
            this.distortionCoefficients = distortionCoefficients;
        }
    }

    // Calibration values are mapped from a [VendorID, ProductID]-pair to a list
    // of calibration values.
    private HashMap<Pair<Integer, Integer>, ArrayList<Calibration>> mCalibrationMap = null;
    private float[] mCalibrationValue = new float[12];

    public CalibrationController(Activity activity)
    {
        if (activity == null) {
            return;
        }

        mCalibrationMap = new HashMap<Pair<Integer, Integer>, ArrayList<Calibration>>();
        addDefaultCalibrations();
//        getCalibrationsFromXML(activity);
        printCalibrationMap(mCalibrationMap);
    }

    public float[] getCalibrationValue(int vid, int pid, int width, int height)
    {
        Pair<Integer, Integer> key = new Pair<Integer, Integer>(vid, pid);
        ArrayList<Calibration> calibrationList = (ArrayList<Calibration>)mCalibrationMap.get(key);
        if (calibrationList != null)
        {
            for (int idx = 0; idx < calibrationList.size(); idx++)
            {
                Calibration calibration = calibrationList.get(idx);
                if (calibration.frameSize[0] == width && calibration.frameSize[1] == height)
                {
                    mCalibrationValue[0] = calibration.principalPoint[0];
                    mCalibrationValue[1] = calibration.principalPoint[1];
                    mCalibrationValue[2] = calibration.focalLength[0];
                    mCalibrationValue[3] = calibration.focalLength[1];

                    for (int coeffIdx = 0; coeffIdx < calibration.distortionCoefficients.length; coeffIdx++)
                    {
                        mCalibrationValue[coeffIdx+4] = calibration.distortionCoefficients[coeffIdx];
                    }

                    Log.d(MODULE_TAG, "Returning calibration:");
                    printCalibration(calibration);
                    return mCalibrationValue;
                }
            }
        }

        return null;
    }

    private void addDefaultCalibrations()
    {
        // Microsoft LifeCam HD-3000
        {
            ArrayList<Calibration> calibrationList = new ArrayList<Calibration>();
            calibrationList.add(new Calibration(new int[]{640, 480},
                new float[]{318.135f, 228.374f},
                new float[]{678.154f, 678.17f},
                new float[]{0.154576f, -1.19143f, 0f, 0f, 2.06105f, 0f, 0f, 0f}));

            // Vid+pid pair.
            Pair<Integer, Integer> key = new Pair(Integer.decode(VENDOR_ID_MICROSOFT), Integer.decode(PRODUCT_ID_MICROSOFT_LIFECAM_HD_3000_1));
            mCalibrationMap.put(key, calibrationList);

            // At least one of LifeCam HD-3000 is reporting a wrong product ID, so add this too.
            ArrayList<Calibration> calibrationList2 = new ArrayList<Calibration>();
            calibrationList2.add(new Calibration(new int[]{640, 480},
                new float[]{318.135f, 228.374f},
                new float[]{678.154f, 678.17f},
                new float[]{0.154576f, -1.19143f, 0f, 0f, 2.06105f, 0f, 0f, 0f}));
            Pair<Integer, Integer> key2 = new Pair(Integer.decode(VENDOR_ID_MICROSOFT), Integer.decode(PRODUCT_ID_MICROSOFT_LIFECAM_HD_3000_2));
            mCalibrationMap.put(key2, calibrationList2);
        }

        // Logitech C310 HD
        {
            ArrayList<Calibration> calibrationList = new ArrayList<Calibration>();
            calibrationList.add(new Calibration(new int[]{640, 480},
                new float[]{316f, 230.895f},
                new float[]{817.172f, 816.951f},
                new float[]{-0.0456154f, 0.368814f, 0f, 0f, -0.899576f, 0f, 0f, 0f}));

            // Vid+pid pair.
            Pair<Integer, Integer> key = new Pair(Integer.decode(VENDOR_ID_LOGITECH), Integer.decode(PRODUCT_ID_LOGITECH_C310_HD));
            mCalibrationMap.put(key, calibrationList);
        }
    }

    private void getCalibrationsFromXML(Activity activity)
    {
        AssetManager assetManager = activity.getAssets();
        InputStream inputStream = null;

        try
        {
            inputStream = assetManager.open(CALIBRATION_XML_FILENAME);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);

            NodeList deviceList = doc.getElementsByTagName("CameraDevice");
            for (int idx = 0; idx < deviceList.getLength(); idx++)
            {
                Integer vid = 0, pid = 0;

                Node device = deviceList.item(idx);
                if (device.getNodeType() == Node.ELEMENT_NODE)
                {
                    if (((Element)device).hasAttribute("VID"))
                    {
                        String vid_str = ((Element)device).getAttribute("VID");
                        vid = Integer.decode(vid_str);
                    }

                    if (((Element)device).hasAttribute("PID"))
                    {
                        String pid_str = ((Element)device).getAttribute("PID");
                        pid = Integer.decode(pid_str);
                    }
                }

                if (vid == 0 || pid == 0)
                {
                    // Cannot find VID or PID for this device, do not proceed
                    continue;
                }

                Pair<Integer, Integer> key = new Pair(vid, pid);

                NodeList children = device.getChildNodes();
                for (int child_idx = 0; child_idx < children.getLength(); child_idx++)
                {
                    if (children.item(child_idx).getNodeName().equals("Calibration"))
                    {
                        int[] frameSize = new int[2];
                        float[] principalPoint = new float[2];
                        float[] focalLength = new float[2];
                        float[] distortionCoefficients = new float[8];

                        Node calibration = children.item(child_idx);
                        if (calibration.getNodeType() == Node.ELEMENT_NODE)
                        {
                            if (((Element)calibration).hasAttribute("size"))
                            {
                                String frameSize_str = ((Element)calibration).getAttribute("size");
                                String[] frameSize_str_arr = frameSize_str.split(" ");

                                if (frameSize_str_arr.length == 2)
                                {
                                    frameSize[0] = Integer.decode(frameSize_str_arr[0]);
                                    frameSize[1] = Integer.decode(frameSize_str_arr[1]);
                                }
                            }

                            if (((Element)calibration).hasAttribute("principal_point"))
                            {
                                String principalPoint_str = ((Element)calibration).getAttribute("principal_point");
                                String[] principalPoint_str_arr = principalPoint_str.split(" ");

                                if (principalPoint_str_arr.length == 2)
                                {
                                    principalPoint[0] = Float.parseFloat(principalPoint_str_arr[0]);
                                    principalPoint[1] = Float.parseFloat(principalPoint_str_arr[1]);
                                }
                            }

                            if (((Element)calibration).hasAttribute("focal_length"))
                            {
                                String focalLength_str = ((Element)calibration).getAttribute("focal_length");
                                String[] focalLength_str_arr = focalLength_str.split(" ");

                                if (focalLength_str_arr.length == 2)
                                {
                                    focalLength[0] = Float.parseFloat(focalLength_str_arr[0]);
                                    focalLength[1] = Float.parseFloat(focalLength_str_arr[1]);
                                }
                            }

                            if (((Element)calibration).hasAttribute("distortion_coefficients"))
                            {
                                String distortionCoefficients_str = ((Element)calibration).getAttribute("distortion_coefficients");
                                String[] distortionCoefficients_str_arr = distortionCoefficients_str.split(" ");

                                if (distortionCoefficients_str_arr.length == 8)
                                {
                                    for (int coeffIdx = 0; coeffIdx < distortionCoefficients_str_arr.length; coeffIdx++)
                                    {
                                        distortionCoefficients[coeffIdx] = Float.parseFloat(distortionCoefficients_str_arr[coeffIdx]);
                                    }
                                }
                            }
                        }

                        Calibration calibrationVal = new Calibration(frameSize, principalPoint, focalLength, distortionCoefficients);

                        if (mCalibrationMap.containsKey(key))
                        {
                            ArrayList<Calibration> calibrationList = mCalibrationMap.get(key);

                            // Check if there already is a default entry, that the user wants to overwrite.
                            boolean overwritten = false;
                            for (int calibIdx = 0; calibIdx < calibrationList.size(); calibIdx++)
                            {
                                Calibration existing = calibrationList.get(calibIdx);
                                if (existing.frameSize[0] == frameSize[0] &&
                                    existing.frameSize[1] == frameSize[1])
                                {
                                    existing.principalPoint[0] = principalPoint[0];
                                    existing.principalPoint[1] = principalPoint[1];
                                    existing.focalLength[0] = focalLength[0];
                                    existing.focalLength[1] = focalLength[1];

                                    for (int coeffIdx = 0; coeffIdx < distortionCoefficients.length; coeffIdx++)
                                    {
                                        existing.distortionCoefficients[coeffIdx] = distortionCoefficients[coeffIdx];
                                    }
                                    
                                    overwritten = true;

                                    Log.d(MODULE_TAG, "Overwrote calibration for VID: " + vid + ", PID:" + pid +
                                                      " frameSize: [" + frameSize[0] + "x" + frameSize[1] + "]");
                                }
                            }

                            if (!overwritten)
                            {
                                calibrationList.add(calibrationVal);
                            }
                        }
                        else
                        {
                            ArrayList<Calibration> calibrationList = new ArrayList<Calibration>();
                            calibrationList.add(calibrationVal);
                            mCalibrationMap.put(key, calibrationList);
                        }
                    }
                }
            }
        }
        catch (SAXParseException e)
        {
            Log.e(MODULE_TAG, "Failed to parse " + CALIBRATION_XML_FILENAME, e);
        }
        catch (Exception e)
        {
            Log.e(MODULE_TAG, "Failed to read from " + CALIBRATION_XML_FILENAME, e);
        }
    }

    private static void printCalibrationMap(HashMap map)
    {
        Iterator it = map.keySet().iterator();

        while (it.hasNext())
        {
            Pair<Integer, Integer> key = (Pair<Integer, Integer>)it.next();
            ArrayList<Calibration> value = (ArrayList<Calibration>)map.get(key);

            // Print key
            Log.d(MODULE_TAG, "VID: " + String.format("0x%04x", key.first) + ", PID: " + String.format("0x%04x", key.second));

            // Print values
            for (int idx = 0; idx < value.size(); idx++)
            {
                Calibration calibration = value.get(idx);
                printCalibration(calibration);
            }
        }
    }

    private static void printCalibration(Calibration calibration)
    {
        ArrayList<Float> distortionCoeffList = new ArrayList<Float>(calibration.distortionCoefficients.length);
        for (float f : calibration.distortionCoefficients) {
            distortionCoeffList.add(f);
        }
        
        Log.d(MODULE_TAG, "Calibration - Frame Size: " + calibration.frameSize[0] + " x " + calibration.frameSize[1]);
        Log.d(MODULE_TAG, "Calibration - Principal Point: " + calibration.principalPoint[0] + " and " + calibration.principalPoint[1]);
        Log.d(MODULE_TAG, "Calibration - Focal Length: " + calibration.focalLength[0] + " and " + calibration.focalLength[1]);
        Log.d(MODULE_TAG, "Calibration - Distortion coefficients: " + TextUtils.join(", ", distortionCoeffList));
    }
}
