/*===============================================================================
Copyright (c) 2016-2018 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.VuforiaSamples.app.ObjectRecognition;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.vuforia.Device;
import com.vuforia.Matrix44F;
import com.vuforia.ObjectTarget;
import com.vuforia.ObjectTargetResult;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.TrackableResultList;
import com.vuforia.Vuforia;
import com.vuforia.SampleApplication.SampleAppRenderer;
import com.vuforia.SampleApplication.SampleAppRendererControl;
import com.vuforia.SampleApplication.SampleApplicationSession;
import com.vuforia.SampleApplication.utils.CubeObject;
import com.vuforia.SampleApplication.utils.CubeShaders;
import com.vuforia.SampleApplication.utils.LoadingDialogHandler;
import com.vuforia.SampleApplication.utils.SampleMath;
import com.vuforia.SampleApplication.utils.SampleUtils;
import com.vuforia.SampleApplication.utils.Texture;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * The renderer class for the ObjectTargets sample.
 *
 * In the renderFrame() function you can render augmentations to display over the Target
 */
public class ObjectTargetRenderer implements GLSurfaceView.Renderer, SampleAppRendererControl
{
    private static final String LOGTAG = "ObjectTargetRenderer";
    
    private final SampleApplicationSession vuforiaAppSession;
    private final ObjectTargets mActivity;
    private final SampleAppRenderer mSampleAppRenderer;

    private Vector<Texture> mTextures;
    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int texSampler2DHandle;
    private int mvpMatrixHandle;
    private int opacityHandle;
    private int colorHandle;
    
    private CubeObject mCubeObject;
    
    private Renderer mRenderer;

    private boolean mIsActive = false;

    ObjectTargetRenderer(ObjectTargets activity,
        SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivity, Device.MODE.MODE_AR,
                false, .01f, 5f);
    }
    

    @Override
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;

        // Call our function to render content from SampleAppRenderer class
        mSampleAppRenderer.render();
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
    }
    
    
    // Called when the surface changes size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");
        
        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mSampleAppRenderer.onConfigurationChanged(mIsActive);

        initRendering();
    }


    public void setActive(boolean active)
    {
        mIsActive = active;

        if(mIsActive)
            mSampleAppRenderer.configureVideoBackground();
    }


    private void initRendering()
    {
        mCubeObject = new CubeObject();
        
        mRenderer = Renderer.getInstance();
        
        // Now generate the OpenGL texture objects and add settings
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
        SampleUtils.checkGLError("ObjectTarget GLInitRendering");
        
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
            : 1.0f);
        
        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
            CubeShaders.CUBE_MESH_VERTEX_SHADER,
            CubeShaders.CUBE_MESH_FRAGMENT_SHADER);
        
        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexTexCoord");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "texSampler2D");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "modelViewProjectionMatrix");
        opacityHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "opacity");
        colorHandle = GLES20.glGetUniformLocation(shaderProgramID, "color");

        mActivity.loadingDialogHandler
            .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
    }


    public void updateRenderingPrimitives()
    {
        mSampleAppRenderer.updateRenderingPrimitives();
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

        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

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

        // did we find any trackables this frame?
        TrackableResultList trackableResultList = state.getTrackableResults();
        for (TrackableResult result : trackableResultList)
        {
            if (result.isOfType(ObjectTargetResult.getClassType()))
            {
                Trackable trackable = result.getTrackable();

                modelMatrix = Tool.convertPose2GLMatrix(result.getPose());
                float[] modelViewMatrix = modelMatrix.getData();

                printUserData(trackable);

                ObjectTarget objectTarget = (ObjectTarget) trackable;
                float[] objectSize = objectTarget.getSize().getData();

                // Apply local transformation to our model
                Matrix.translateM(modelViewMatrix, 0, objectSize[0] / 2, objectSize[1] / 2,
                        objectSize[2] / 2);

                Matrix.scaleM(modelViewMatrix, 0, objectSize[0] / 2,
                        objectSize[1] / 2, objectSize[2] / 2);

                // Renders the augmentation
                renderModel(projectionMatrix, devicePoseMatrix.getData(), modelViewMatrix);

                SampleUtils.checkGLError("Object Target Render Frame");
            }
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_BLEND);

        mRenderer.end();
    }


    private void renderModel(float[] projectionMatrix, float[] viewMatrix, float[] modelMatrix)
    {
        float[] modelViewProjection = new float[16];

        // Combine device pose (view matrix) with model matrix
        Matrix.multiplyMM(modelMatrix, 0, viewMatrix, 0, modelMatrix, 0);

        // Do the final combination with the projection matrix
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0);

        // Activate the shader program and bind the vertex/normal/tex coords
        GLES20.glUseProgram(shaderProgramID);

        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                false, 0, mCubeObject.getVertices());
        GLES20.glUniform1f(opacityHandle, 0.3f);
        GLES20.glUniform3f(colorHandle, 0.0f, 0.0f, 0.0f);
        GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                GLES20.GL_FLOAT, false, 0, mCubeObject.getTexCoords());

        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                mTextures.get(0).mTextureID[0]);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                modelViewProjection, 0);
        GLES20.glUniform1i(texSampler2DHandle, 0);

        // Pass the model view matrix to the shader
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                modelViewProjection, 0);

        // Finally render
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                mCubeObject.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                mCubeObject.getIndices());

        // Disable the enabled arrays
        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);
    }

    
    private void printUserData(Trackable trackable)
    {
        String userData = (String) trackable.getUserData();
        Log.d(LOGTAG, "UserData:Retreived User Data	\"" + userData + "\"");
    }
    

    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
    }
}
