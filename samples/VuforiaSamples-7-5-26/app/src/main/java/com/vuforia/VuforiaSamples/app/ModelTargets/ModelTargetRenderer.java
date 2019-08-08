/*===============================================================================
Copyright (c) 2016-2018 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.VuforiaSamples.app.ModelTargets;

import android.graphics.Point;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.vuforia.CameraCalibration;
import com.vuforia.Device;
import com.vuforia.GuideView;
import com.vuforia.Image;
import com.vuforia.Matrix44F;
import com.vuforia.ModelTarget;
import com.vuforia.ModelTargetResult;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableList;
import com.vuforia.TrackableResult;
import com.vuforia.TrackableResultList;
import com.vuforia.Vec2F;
import com.vuforia.Vec4F;
import com.vuforia.Vuforia;
import com.vuforia.SampleApplication.SampleAppRenderer;
import com.vuforia.SampleApplication.SampleAppRendererControl;
import com.vuforia.SampleApplication.SampleApplicationSession;
import com.vuforia.SampleApplication.utils.LightingShaders;
import com.vuforia.SampleApplication.utils.Plane;
import com.vuforia.SampleApplication.utils.SampleApplicationV3DModel;
import com.vuforia.SampleApplication.utils.SampleMath;
import com.vuforia.SampleApplication.utils.SampleUtils;
import com.vuforia.SampleApplication.utils.Texture;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * The renderer class for the ModelTargets sample.
 *
 * In the renderFrame() function you can render augmentations to display over the Target
 */
@SuppressWarnings("SuspiciousNameCombination")
public class ModelTargetRenderer implements GLSurfaceView.Renderer, SampleAppRendererControl
{
    private static final String LOGTAG = "ModelTargetRenderer";
    
    private final SampleApplicationSession vuforiaAppSession;
    private final ModelTargets mActivity;
    private final SampleAppRenderer mSampleAppRenderer;

    private Vector<Texture> mTextures;
    
    private int planeShaderProgramID;
    private int planeVertexHandle;
    private int planeTextureCoordHandle;
    private int planeMvpMatrixHandle;
    private int planeTexSampler2DHandle;
    private int planeColorHandle;

    private int guideViewHandle;
    private Vec2F guideViewScale;

    private int shaderProgramID;
    private int vertexHandle;
    private int mvpMatrixHandle;
    private int mvMatrixHandle;
    private int normalHandle;
    private int textureCoordHandle;
    private int texSampler2DHandle;
    private int normalMatrixHandle;
    private int lightPositionHandle;
    private int lightColorHandle;

    // This plane is used to display the model target guide view
    private Plane mPlane;

    private SampleApplicationV3DModel m3DModel;

    private boolean mIsActive = false;
    private boolean mModelIsLoaded = false;

    ModelTargetRenderer(ModelTargets activity, SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivity, Device.MODE.MODE_AR, vuforiaAppSession.getVideoMode(),
                false, 0.01f , 5f);

        guideViewHandle = -1;
    }


    @Override
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive || !mModelIsLoaded)
            return;

        // Call our function to render content from SampleAppRenderer class
        mSampleAppRenderer.render();
    }
    

    public void setActive(boolean active)
    {
        mIsActive = active;

        if(mIsActive)
            mSampleAppRenderer.configureVideoBackground();
    }


    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");
        
        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();

        mSampleAppRenderer.onSurfaceCreated();

        initRendering();
    }
    
    
    // Called when the surface changes size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");
        
        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mSampleAppRenderer.onConfigurationChanged(mIsActive);
    }
    

    private void initRendering()
    {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);
        
        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, t.mData);
        }
        
        planeShaderProgramID = SampleUtils.createProgramFromShaderSrc(
            TextureColorShaders.TEXTURE_COLOR_VERTEX_SHADER,
            TextureColorShaders.TEXTURE_COLOR_FRAGMENT_SHADER);

        mPlane = new Plane();

        if (planeShaderProgramID > 0)
        {
            planeVertexHandle = GLES20.glGetAttribLocation(planeShaderProgramID,
                    "vertexPosition");
            planeTextureCoordHandle = GLES20.glGetAttribLocation(planeShaderProgramID,
                    "vertexTexCoord");
            planeMvpMatrixHandle = GLES20.glGetUniformLocation(planeShaderProgramID,
                    "modelViewProjectionMatrix");
            planeTexSampler2DHandle = GLES20.glGetUniformLocation(planeShaderProgramID,
                    "texSampler2D");
            planeColorHandle = GLES20.glGetUniformLocation(planeShaderProgramID,
                    "uniformColor");

        }
        else
        {
            Log.e(LOGTAG, "Could not init plane shader");
        }

        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
                LightingShaders.LIGHTING_VERTEX_SHADER,
                LightingShaders.LIGHTING_FRAGMENT_SHADER);

        if (shaderProgramID > 0)
        {
            vertexHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexPosition");
            normalHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexNormal");
            textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexTexCoord");
            mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID, "u_mvpMatrix");
            mvMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID, "u_mvMatrix");
            normalMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID, "u_normalMatrix");
            lightPositionHandle = GLES20.glGetUniformLocation(shaderProgramID, "u_lightPos");
            lightColorHandle = GLES20.glGetUniformLocation(shaderProgramID, "u_lightColor");
            texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID, "texSampler2D");

        }
        else
        {
            Log.e(LOGTAG, "Could not init lighting shader");
        }

        if(!mModelIsLoaded)
        {
            LoadModelTask modelTask = new LoadModelTask(this);
            modelTask.execute();
        }

        guideViewScale = new Vec2F(1.0f, 1.0f);

        // Invalidate GuideView texture handle to allow creating the texture later during the rendering workflow 
        guideViewHandle = -1;
    }


    public void updateRenderingPrimitives()
    {
        mSampleAppRenderer.updateRenderingPrimitives();
    }


    private static class LoadModelTask extends AsyncTask<Void, Integer, Boolean>
    {
        private final WeakReference<ModelTargetRenderer> rendererRef;

        LoadModelTask(ModelTargetRenderer mtRenderer)
        {
            rendererRef = new WeakReference<>(mtRenderer);
        }

        protected Boolean doInBackground(Void... params)
        {
            ModelTargetRenderer renderer = rendererRef.get();

            try
            {
                renderer.m3DModel = new SampleApplicationV3DModel(false);
                renderer.m3DModel.loadModel(renderer.mActivity.getResources().getAssets(),
                        "ModelTargets/Lander.v3d");
                renderer.mModelIsLoaded = true;
            }
            catch (IOException e)
            {
                Log.e(LOGTAG, "Unable to load model");
                renderer.mModelIsLoaded = false;
            }

            return  renderer.mModelIsLoaded;
        }

        protected void onPostExecute(Boolean result)
        {
            ModelTargets activity = rendererRef.get().mActivity;

            // Hide the Loading Dialog
            activity.showProgressIndicator(false);

            // Show button layout
            activity.setBtnLayoutVisibility(View.VISIBLE);
        }
    }


    public void unloadModel()
    {
        if (m3DModel != null)
        {
            m3DModel.unloadModel();
            m3DModel = null;
        }
    }

    // The render function.
    // This function is called from the SampleAppRenderer by using the RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling its lifecycle.
    // NOTE: State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix)
    {
        // Renders video background replacing Renderer.DrawVideoBackground()
        mSampleAppRenderer.renderVideoBackground(state);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glCullFace(GLES20.GL_BACK);

        // If the Device pose exists, we can assume we will receive two
        // Trackable Results if the ModelTargetResult is available:
        // ModelTargetResult and DeviceTrackableResult
        int numExpectedResults = state.getDeviceTrackableResult() == null ? 0 : 1;
        if (state.getTrackableResults().size() <= numExpectedResults)
        {
            if (guideViewHandle < 0)
            {
                // assuming there is only one Model Target trackable in the dataset
                ModelTarget modelTarget = null;
                TrackableList trackableList = mActivity.getDataset().getTrackables();
                for (Trackable trackable : trackableList)
                {
                    if (trackable instanceof ModelTarget)
                    {
                        modelTarget = (ModelTarget)trackable;
                        break;
                    }
                }

                if (modelTarget != null)
                {
                    GuideView guideView = modelTarget.getGuideViews().at(0);

                    if (guideView != null)
                    {
                        initGuideViewTexture(state.getCameraCalibration(), guideView.getImage());
                    }
                }
            }

            Matrix44F modelMatrix = SampleMath.Matrix44FIdentity();

            Vec4F color = new Vec4F(1.0f, 1.0f, 1.0f, 1.0f);

            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glDisable(GLES20.GL_CULL_FACE);

            float orthoProjMatrix[] = new float[16];
            Matrix.orthoM(orthoProjMatrix, 0, -0.5f, 0.5f, -0.5f, 0.5f, 0, 1);
            Matrix44F orthoProjMatrix44 = new Matrix44F();
            orthoProjMatrix44.setData(orthoProjMatrix);

            renderPlaneTextured(orthoProjMatrix44, modelMatrix, guideViewScale, color, guideViewHandle);

            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);

            SampleUtils.checkGLError("Render Frame, no trackables");

        }
        else
        {
            // Set the device pose matrix as identity
            Matrix44F devicePoseMatrix = SampleMath.Matrix44FIdentity();
            Matrix44F modelMatrix;

            // Read device pose from the state and create a corresponding view matrix (inverse of the device pose)
            if (state.getDeviceTrackableResult() != null
                    && state.getDeviceTrackableResult().getStatus() != TrackableResult.STATUS.NO_POSE)
            {
                modelMatrix = Tool.convertPose2GLMatrix(state.getDeviceTrackableResult().getPose());

                // We transpose here because Matrix44FInverse returns a transposed matrix
                devicePoseMatrix = SampleMath.Matrix44FTranspose(SampleMath.Matrix44FInverse(modelMatrix));
            }

            // Did we find any trackables this frame?
            TrackableResultList trackableResultList = state.getTrackableResults();
            for (TrackableResult result : trackableResultList)
            {
                if (!result.isOfType(ModelTargetResult.getClassType()))
                {
                    continue;
                }

                modelMatrix = Tool.convertPose2GLMatrix(result.getPose());

                renderModel(projectionMatrix, devicePoseMatrix.getData(), modelMatrix.getData());

                SampleUtils.checkGLError("Model Targets Render Frame");
            }
        }

        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
    }


    private void renderModel(float[] projectionMatrix, float[] viewMatrix, float[] modelMatrix)
    {
        float[] modelViewProjection = new float[16];

        // Combine device pose (view matrix) with model matrix
        Matrix.multiplyMM(modelMatrix, 0, viewMatrix, 0, modelMatrix, 0);

        // Do the final combination with the projection matrix
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0);

        // activate the shader program and bind the vertex/normal/tex coords
        GLES20.glUseProgram(shaderProgramID);

        GLES20.glDisable(GLES20.GL_CULL_FACE);

        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                false, 0, m3DModel.getVertices());
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, m3DModel.getNormals());
        GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                GLES20.GL_FLOAT, false, 0, m3DModel.getTexCoords());

        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(normalHandle);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                mTextures.get(0).mTextureID[0]);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                modelViewProjection, 0);
        GLES20.glUniformMatrix4fv(mvMatrixHandle, 1, false, modelMatrix, 0);

        float[] inverseMatrix = new float[16];
        Matrix.invertM(inverseMatrix, 0, modelMatrix, 0);

        float normalMatrix[] = new float[16];
        Matrix.transposeM(normalMatrix, 0, inverseMatrix, 0);

        GLES20.glUniformMatrix4fv(normalMatrixHandle, 1, false, normalMatrix, 0);

        GLES20.glUniform4f(lightPositionHandle, 0.2f, -1.0f, 0.5f, -1.0f);
        GLES20.glUniform4f(lightColorHandle, 0.5f, 0.5f, 0.5f, 1.0f);

        GLES20.glUniform1i(texSampler2DHandle, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0,
                m3DModel.getNumObjectVertex());

        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(normalHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);
    }


    private void renderPlaneTextured(Matrix44F projectionMatrix, Matrix44F modelViewMatrix, Vec2F scale, Vec4F colour, int textureHandle)
    {
        float modelViewProjectionMatrix[] = projectionMatrix.getData();
        float scaledModelMatrixArray[] = modelViewMatrix.getData();

        Matrix.scaleM(scaledModelMatrixArray, 0, scale.getData()[0], scale.getData()[1], 1.0f);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix.getData(), 0, scaledModelMatrixArray, 0);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

        GLES20.glEnableVertexAttribArray(planeVertexHandle);
        GLES20.glVertexAttribPointer(planeVertexHandle, 3, GLES20.GL_FLOAT, false, 0, mPlane.getVertices());

        GLES20.glEnableVertexAttribArray(planeTextureCoordHandle);
        GLES20.glVertexAttribPointer(planeTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mPlane.getTexCoords());

        GLES20.glUseProgram(planeShaderProgramID);
        GLES20.glUniformMatrix4fv(planeMvpMatrixHandle, 1, false, modelViewProjectionMatrix, 0);
        GLES20.glUniform4f(planeColorHandle, colour.getData()[0], colour.getData()[1], colour.getData()[2], colour.getData()[3]);
        GLES20.glUniform1i(planeTexSampler2DHandle, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, Plane.NUM_PLANE_INDEX, GLES20.GL_UNSIGNED_SHORT, mPlane.getIndices());

        // disable input data structures
        GLES20.glDisableVertexAttribArray(planeTextureCoordHandle);
        GLES20.glDisableVertexAttribArray(planeVertexHandle);
        GLES20.glUseProgram(0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glDisable(GLES20.GL_BLEND);
    }


    private void initGuideViewTexture(CameraCalibration cameraCalib, Image textureImage)
    {
        if(textureImage == null || cameraCalib == null)
        {
            return;
        }

        // Get the guide view image
        guideViewHandle = SampleUtils.createTexture(textureImage);

        float guideViewAspectRatio = (float)textureImage.getWidth() / textureImage.getHeight();
        Point size = new Point();
        mActivity.getWindowManager().getDefaultDisplay().getSize(size);

        float cameraAspectRatio = (float)size.x / size.y;

        // Doing this calculation in world space, at an assumed camera near plane distance of 0.01f;
        float planeDistance = 0.01f;
        float fieldOfView = cameraCalib.getFieldOfViewRads().getData()[1];
        float nearPlaneHeight = (float)(2.0f * planeDistance * Math.tan(fieldOfView * 0.5f));
        float nearPlaneWidth = nearPlaneHeight * cameraAspectRatio;

        float planeWidth;
        float planeHeight;

        int guideViewSign = (0.0f < (guideViewAspectRatio - 1.0f) ? 1 : 0) - ((guideViewAspectRatio - 1.0f) < 0.0f  ? 1 : 0);
        int cameraSign = (0.0f < (cameraAspectRatio - 1.0f) ? 1 : 0) - ((cameraAspectRatio - 1.0f) < 0.0f ? 1 : 0);

        if (guideViewSign == cameraSign)
        {
            // Scale the guide view to fit the horizontal field of view,
            // while preserving the aspect ratio of the image.
            planeWidth = nearPlaneWidth;
            planeHeight = planeWidth / guideViewAspectRatio;
        }
        else if (cameraAspectRatio < 1.0f)  // guide view landscape, camera portrait
        {
            // scale so that the long side of the camera (height)
            // is the same length as guide view width
            planeWidth = nearPlaneHeight;
            planeHeight = planeWidth / guideViewAspectRatio;
        }
        else  // guide view portrait, camera landscape
        {
            // scale so that the long side of the camera (width)
            // is the same length as guide view height
            planeHeight = nearPlaneWidth;
            planeWidth = planeHeight * guideViewAspectRatio;
        }

        // normalize world space plane sizes into view space again
        guideViewScale = new Vec2F(planeWidth / nearPlaneWidth, -planeHeight / nearPlaneHeight);
    }

    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
    }

    public boolean isModelLoaded()
    {
        return mModelIsLoaded;
    }
}
