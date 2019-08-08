/*===============================================================================
Copyright (c) 2016-2018 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.VuforiaSamples.app.CloudRecognition;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.vuforia.Device;
import com.vuforia.ImageTargetResult;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.TrackableResult;
import com.vuforia.TrackableResultList;
import com.vuforia.Vuforia;
import com.vuforia.SampleApplication.SampleAppRenderer;
import com.vuforia.SampleApplication.SampleAppRendererControl;
import com.vuforia.SampleApplication.SampleApplicationSession;
import com.vuforia.SampleApplication.utils.CubeShaders;
import com.vuforia.SampleApplication.utils.SampleMath;
import com.vuforia.SampleApplication.utils.SampleUtils;
import com.vuforia.SampleApplication.utils.Teapot;
import com.vuforia.SampleApplication.utils.Texture;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * The renderer class for the CloudReco sample.
 *
 * In the renderFrame() function you can render augmentations to display over the Target
 */
public class CloudRecoRenderer implements GLSurfaceView.Renderer, SampleAppRendererControl
{
    private final SampleApplicationSession vuforiaAppSession;
    private final SampleAppRenderer mSampleAppRenderer;
    
    private static final float OBJECT_SCALE_FLOAT = 0.003f;
    
    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;
    
    private Vector<Texture> mTextures;

    // Object to be rendered
    private Teapot mTeapot;
    
    private final CloudReco mActivity;

    private boolean mIsActive = false;
    
    CloudRecoRenderer(SampleApplicationSession session, CloudReco activity)
    {
        vuforiaAppSession = session;
        mActivity = activity;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivity, Device.MODE.MODE_AR,
                false, 0.010f, 5f);
    }
    
    
    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();

        mSampleAppRenderer.onSurfaceCreated();
    }
    
    
    // Called when the surface changes size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mSampleAppRenderer.onConfigurationChanged(mIsActive);

        // Call function to initialize rendering:
        initRendering();
    }
    

    @Override
    public void onDrawFrame(GL10 gl)
    {
        // Call our function to render content from SampleAppRenderer class
        mSampleAppRenderer.render();
    }


    public void setActive(boolean active)
    {
        mIsActive = active;

        if(mIsActive)
            mSampleAppRenderer.configureVideoBackground();
    }


    private void initRendering()
    {
        // Define clear color
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
        
        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
            CubeShaders.CUBE_MESH_VERTEX_SHADER,
            CubeShaders.CUBE_MESH_FRAGMENT_SHADER);
        
        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "texSampler2D");
        mTeapot = new Teapot();
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

        // Set the device pose matrix as identity
        Matrix44F devicePoseMatrix = SampleMath.Matrix44FIdentity();
        Matrix44F modelMatrix;

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Start the target finder if we can't find an Image Target result.
        // If the Device pose exists, we can assume we will receive two
        // Trackable Results if the ImageTargetResult is available:
        // ImageTargetResult and DeviceTrackableResult
        int numExpectedResults = state.getDeviceTrackableResult() == null ? 0 : 1;
        if (state.getTrackableResults().size() <= numExpectedResults)
        {
            mActivity.startFinderIfStopped();
        }

        // Read device pose from the state and create a corresponding view matrix (inverse of the device pose)
        if(state.getDeviceTrackableResult() != null
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
            modelMatrix = Tool.convertPose2GLMatrix(result.getPose());

            if (result.isOfType(ImageTargetResult.getClassType()))
            {
                mActivity.stopFinderIfStarted();

                // Renders the augmentation
                renderModel(projectionMatrix, devicePoseMatrix.getData(), modelMatrix.getData());

                SampleUtils.checkGLError("CloudReco renderFrame");
            }
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        
        Renderer.getInstance().end();
    }


    private void renderModel(float[] projectionMatrix, float[] viewMatrix, float[] modelMatrix)
    {
        int textureIndex = 0;
        float[] modelViewProjection = new float[16];

        // Apply local transformation to our model
        Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, OBJECT_SCALE_FLOAT);
        Matrix.scaleM(modelMatrix, 0, OBJECT_SCALE_FLOAT,
            OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);

        // Combine device pose (view matrix) with model matrix
        Matrix.multiplyMM(modelMatrix, 0, viewMatrix, 0, modelMatrix, 0);

        // Do the final combination with the projection matrix
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0);
        
        // activate the shader program and bind the vertex/normal/tex coords
        GLES20.glUseProgram(shaderProgramID);
        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false,
            0, mTeapot.getVertices());
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT,
            false, 0, mTeapot.getTexCoords());
        
        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);
        
        // activate texture 0, bind it, and pass to shader
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
            mTextures.get(textureIndex).mTextureID[0]);
        GLES20.glUniform1i(texSampler2DHandle, 0);
        
        // pass the model view matrix to the shader
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
            modelViewProjection, 0);
        
        // finally draw the teapot
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mTeapot.getNumObjectIndex(),
            GLES20.GL_UNSIGNED_SHORT, mTeapot.getIndices());
        
        // disable the enabled arrays
        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);
    }
    
    
    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
    }
}
