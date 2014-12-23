package com.karacasoft.cardboardmediaplayer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.util.SparseArray;

public class Utils {
	
	public static float[] getCameraMatrix()
	{
		float[] camera = new float[16];
		
		float eyeX = 0.0f;
		float eyeY = 0.5f;
		float eyeZ = 0.0f;
		
		float lookX = 0.0f;
		float lookY = 0.5f;
		float lookZ = 2.0f;
		
		float upX = 0.0f;
		float upY = 1.0f;
		float upZ = 0.0f;
		
		Matrix.setLookAtM(camera, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
		return camera;
	}
	
	public static int initProgram(String vertexShaderCode, String fragmentShaderCode)
	{
		int program = GLES20.glCreateProgram();
		
		int vShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
		int fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
		
		GLES20.glAttachShader(program, vShader);
		GLES20.glAttachShader(program, fShader);
		
		GLES20.glBindAttribLocation(program, 0, "a_Position");
		GLES20.glBindAttribLocation(program, 1, "a_Color");
		
		GLES20.glLinkProgram(program);

		final int[] linkStatus = new int[1];
	    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);

	    if (linkStatus[0] == 0)
	    {
	        GLES20.glDeleteProgram(program);
	        program = 0;
	    }

		if (program == 0)
		{
		    throw new RuntimeException("Error creating program.");
		}
		return program;
	}
	
	public static int loadShader(int type, String code)
	{
		int shader = GLES20.glCreateShader(type);
		
		GLES20.glShaderSource(shader, code);
		
		GLES20.glCompileShader(shader);
		final int[] compileStatus = new int[1];
	    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
	 
	    // If the compilation failed, delete the shader.
	    if (compileStatus[0] == 0)
	    {
	        GLES20.glDeleteShader(shader);
	        shader = 0;
	    }
	    if(shader == 0)
	    {
	    	throw new RuntimeException("Error creating vertex shader.");
	    }
		
		return shader;
	}
	
	public static SparseArray<SurfaceTexture> surfaceTextureMap = new SparseArray<SurfaceTexture>();
	public static int loadTexture()
	{
		int[] textures = new int[1];
		
		GLES20.glGenTextures(1, textures, 0);
		
		SurfaceTexture st = new SurfaceTexture(textures[0]);
		
		surfaceTextureMap.put(textures[0], st);
		
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
		
		if (textures[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }
		
		return textures[0];
	}
	
	public static int loadTexture(Bitmap bmp)
	{
		int[] textures = new int[1];
		
		GLES20.glGenTextures(1, textures, 0);
		
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
		
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        
        bmp.recycle();
        
        if (textures[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }
        
        return textures[0];
		
	}
	
	public static int loadTexture(Context context, int resId)
	{
		int[] textures = new int[1];
		
		GLES20.glGenTextures(1, textures, 0);
		
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;
		 
		Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), resId, options);
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
		
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
 
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        
        bmp.recycle();
        
        if (textures[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }
        
        return textures[0];
		
	}
	
	public static int loadBitmap(Context context, int resourceId) {
		
		int[] textures = new int[1];
		
		GLES20.glGenTextures(1, textures, 0);
		
        /* Get the texture */
        InputStream is = context.getResources().openRawResource(resourceId);
        Bitmap bitmap;

        try {
            bitmap = BitmapFactory.decodeStream(is);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // Ignore.
            }
        }

        // Bind the texture object
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
 
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        
        return textures[0];
        
    }
	
	public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("Renderer", glOperation + ": glError " + error + ":" + GLUtils.getEGLErrorString(error));
            	throw new RuntimeException(glOperation + ": glError " + error + ":" + GLUtils.getEGLErrorString(error));
        }
    }
	
}
