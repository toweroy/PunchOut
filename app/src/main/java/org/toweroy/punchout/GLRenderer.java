package org.toweroy.punchout;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static javax.microedition.khronos.opengles.GL10.GL_CLAMP_TO_EDGE;

public class GLRenderer implements Renderer {

    private final float[] mtrxProjection = new float[16];
    private final float[] mtrxView = new float[16];
    private final float[] mtrxProjectionAndView = new float[16];

    public static float vertices[];
    public static short indices[];
    public static float uvs[];
    public FloatBuffer vertexBuffer;
    public ShortBuffer drawListBuffer;
    public FloatBuffer uvBuffer;

    float mScreenWidth = 1080;
    float mScreenHeight = 1920;
    float ssu = 1.0f;
    float ssx = 1.0f;
    float ssy = 1.0f;
    float swp = 320.0f;
    float shp = 480.0f;

    // Misc
    Context mContext;
    long mLastTime;
    int mProgram;

    public GLRenderer(Context c) {
        mContext = c;
        mLastTime = System.currentTimeMillis() + 100;
    }

    public void onPause() {
        /* Do stuff to pause the renderer */
    }

    public void onResume() {
        /* Do stuff to resume the renderer */
        mLastTime = System.currentTimeMillis();
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        // Get the current time
        long now = System.currentTimeMillis();

        // We should make sure we are valid and sane
        if (mLastTime > now) return;

        // Get the amount of time the last frame took.
        long elapsed = now - mLastTime;

        // Update our example

        // Render our example
        Render(mtrxProjectionAndView);

        mLastTime = now;
    }

    private void Render(float[] m) {
        // clear Screen and Depth Buffer, we have set the clear color as black.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // get handle to vertex shader's vPosition member
        int mPositionHandle = GLES20.glGetAttribLocation(Shaders.shaderProgramImage, "vPosition");

        // Enable generic vertex attribute array
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, 3,
                GLES20.GL_FLOAT, false,
                0, vertexBuffer);

        int mTextureCoordLocation = GLES20.glGetAttribLocation(Shaders.shaderProgramImage, "a_texCoord");

        GLES20.glEnableVertexAttribArray(mTextureCoordLocation);

        // Prepare the texturecoordinates
        GLES20.glVertexAttribPointer(mTextureCoordLocation, 2, GLES20.GL_FLOAT,
                false,
                0, uvBuffer);

        // Get handle to shape's transformation matrix
        int mtrxhandle = GLES20.glGetUniformLocation(Shaders.shaderProgramImage, "uMVPMatrix");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, m, 0);

        // Get handle to textures locations
        int mSamplerLoc = GLES20.glGetUniformLocation(Shaders.shaderProgramImage,
                "s_texture");

        // Set the sampler texture unit to 0, where we have saved the texture.
        GLES20.glUniform1i(mSamplerLoc, 0);

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordLocation);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        // We need to know the current width and height.
        mScreenWidth = width;
        mScreenHeight = height;

        // Redo the Viewport, making it fullscreen.
        GLES20.glViewport(0, 0, (int) mScreenWidth, (int) mScreenHeight);

        // Clear our matrices
        for (int i = 0; i < 16; i++) {
            mtrxProjection[i] = 0.0f;
            mtrxView[i] = 0.0f;
            mtrxProjectionAndView[i] = 0.0f;
        }

        // Setup our screen width and height for normal sprite translation.
        Matrix.orthoM(mtrxProjection, 0, 0f, mScreenWidth, 0.0f, mScreenHeight, 0, 50);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Create the triangle
        SetupTriangle();

        SetupImage();

        // Set the clear color to black
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1);

        // Create the shaders
        int vertexShader = Shaders.loadShader(GLES20.GL_VERTEX_SHADER, Shaders.vertexShaderSolidColor);
        int fragmentShader = Shaders.loadShader(GLES20.GL_FRAGMENT_SHADER, Shaders.fragmentShaderSolidColor);

        Shaders.shaderProgramSolidColor = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(Shaders.shaderProgramSolidColor, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(Shaders.shaderProgramSolidColor, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(Shaders.shaderProgramSolidColor);                  // creates OpenGL ES program executables

        // Create the shaders, images
        vertexShader = Shaders.loadShader(GLES20.GL_VERTEX_SHADER,
                Shaders.vertexShaderImage);
        fragmentShader = Shaders.loadShader(GLES20.GL_FRAGMENT_SHADER,
                Shaders.fragmentShaderImage);

        Shaders.shaderProgramImage = GLES20.glCreateProgram();
        GLES20.glAttachShader(Shaders.shaderProgramImage, vertexShader);
        GLES20.glAttachShader(Shaders.shaderProgramImage, fragmentShader);
        GLES20.glLinkProgram(Shaders.shaderProgramImage);

        // Set our shader programm
        GLES20.glUseProgram(Shaders.shaderProgramImage);
    }

    private void SetupImage() {

//        // Create our UV coordinates.
//        uvs = new float[]{
//                0.0f, 0.0f,
//                0.0f, 1.0f,
//                1.0f, 1.0f,
//                1.0f, 0.0f
//        };
//
//        // The texture buffer
//        ByteBuffer bb = ByteBuffer.allocateDirect(uvs.length * 4);
//        bb.order(ByteOrder.nativeOrder());
//        uvBuffer = bb.asFloatBuffer();
//        uvBuffer.put(uvs);
//        uvBuffer.position(0);
//
//        // Generate Textures, if more needed, alter these numbers.
//        int[] texturenames = new int[1];
//        GLES20.glGenTextures(1, texturenames, 0);
//
//        // Retrieve our image from resources.
//
//        int id = mContext.getResources().getIdentifier("ic_launcher", "mipmap",
//                mContext.getPackageName());
//
//        // Temporary create a bitmap
//        Bitmap bmp = BitmapFactory.decodeResource(mContext.getResources(), id);
//
//        // Bind texture to texturename
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[0]);
//
//        // Set filtering
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
//                GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
//                GLES20.GL_LINEAR);
//
//        // Set wrapping mode
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
//                GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
//                GL_CLAMP_TO_EDGE);
//
//        // Load the bitmap into the bound texture.
//        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
//
//        // We are done using the bitmap so we should recycle it.
//        bmp.recycle();

        // We will use a randomizer for randomizing the textures from texture atlas.
        // This is strictly optional as it only effects the output of our app,
        // Not the actual knowledge.
        Random rnd = new Random();

        // 30 imageobjects times 4 vertices times (u and v)
        uvs = new float[30 * 4 * 2];

        // We will make 30 randomly textures objects
        for (int i = 0; i < 30; i++) {
            int random_u_offset = rnd.nextInt(2);
            int random_v_offset = rnd.nextInt(2);

            // Adding the UV's using the offsets
            uvs[(i * 8) + 0] = random_u_offset * 0.5f;
            uvs[(i * 8) + 1] = random_v_offset * 0.5f;
            uvs[(i * 8) + 2] = random_u_offset * 0.5f;
            uvs[(i * 8) + 3] = (random_v_offset + 1) * 0.5f;
            uvs[(i * 8) + 4] = (random_u_offset + 1) * 0.5f;
            uvs[(i * 8) + 5] = (random_v_offset + 1) * 0.5f;
            uvs[(i * 8) + 6] = (random_u_offset + 1) * 0.5f;
            uvs[(i * 8) + 7] = random_v_offset * 0.5f;
        }

        // The texture buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(uvs.length * 4);
        bb.order(ByteOrder.nativeOrder());
        uvBuffer = bb.asFloatBuffer();
        uvBuffer.put(uvs);
        uvBuffer.position(0);

        // Generate Textures, if more needed, alter these numbers.
        int[] texturenames = new int[1];
        GLES20.glGenTextures(1, texturenames, 0);

        // Retrieve our image from resources.
//        int id = mContext.getResources().getIdentifier("drawable/little-mac", null, mContext.getPackageName());
        Bitmap bmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.little_mac);
        // Temporary create a bitmap
//        Bitmap bmp = BitmapFactory.decodeResource(mContext.getResources(), id);

        // Bind texture to texturename
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[0]);

        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);

        // We are done using the bitmap so we should recycle it.
        bmp.recycle();
    }

    public void SetupTriangle() {
        // We have create the vertices of our view.
//        vertices = new float[]
//                {10.0f, 200f, 0.0f,
//                        10.0f, 100f, 0.0f,
//                        100f, 100f, 0.0f,
//                        100f, 200f, 0.0f,
//                };
//
//        indices = new short[]{0, 1, 2, 0, 2, 3}; // loop in the android official tutorial opengles why different order.
//
//        // The vertex buffer.
//        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
//        bb.order(ByteOrder.nativeOrder());
//        vertexBuffer = bb.asFloatBuffer();
//        vertexBuffer.put(vertices);
//        vertexBuffer.position(0);
//
//        // initialize byte buffer for the draw list
//        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
//        dlb.order(ByteOrder.nativeOrder());
//        drawListBuffer = dlb.asShortBuffer();
//        drawListBuffer.put(indices);
//        drawListBuffer.position(0);

        // We will need a randomizer
        Random rnd = new Random();

        // Our collection of vertices
        vertices = new float[30 * 4 * 3];

        // Create the vertex data
        for (int i = 0; i < 30; i++) {
            int offset_x = rnd.nextInt((int) swp);
            int offset_y = rnd.nextInt((int) shp);

            // Create the 2D parts of our 3D vertices, others are default 0.0f
            vertices[(i * 12) + 0] = offset_x;
            vertices[(i * 12) + 1] = offset_y + (30.0f * ssu);
            vertices[(i * 12) + 2] = 0f;
            vertices[(i * 12) + 3] = offset_x;
            vertices[(i * 12) + 4] = offset_y;
            vertices[(i * 12) + 5] = 0f;
            vertices[(i * 12) + 6] = offset_x + (30.0f * ssu);
            vertices[(i * 12) + 7] = offset_y;
            vertices[(i * 12) + 8] = 0f;
            vertices[(i * 12) + 9] = offset_x + (30.0f * ssu);
            vertices[(i * 12) + 10] = offset_y + (30.0f * ssu);
            vertices[(i * 12) + 11] = 0f;
        }

        // The indices for all textured quads
        indices = new short[30 * 6];
        int last = 0;
        for (int i = 0; i < 30; i++) {
            // We need to set the new indices for the new quad
            indices[(i * 6) + 0] = (short) (last + 0);
            indices[(i * 6) + 1] = (short) (last + 1);
            indices[(i * 6) + 2] = (short) (last + 2);
            indices[(i * 6) + 3] = (short) (last + 0);
            indices[(i * 6) + 4] = (short) (last + 2);
            indices[(i * 6) + 5] = (short) (last + 3);

            // Our indices are connected to the vertices so we need to keep them
            // in the correct order.
            // normal quad = 0,1,2,0,2,3 so the next one will be 4,5,6,4,6,7
            last = last + 4;
        }

        // The vertex buffer.
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);
    }
}
