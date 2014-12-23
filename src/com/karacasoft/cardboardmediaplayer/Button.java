package com.karacasoft.cardboardmediaplayer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;

public class Button {

	public interface OnClickListener
	{
		public void onClick();
	}
	
	private FloatBuffer mVertices; 
	private FloatBuffer mColors;
	private FloatBuffer mHighlighColors;
	private FloatBuffer mTextureCoords;
	
	private float[] mModelMatrix = new float[16];
	public float[] mMVMatrix = new float[16];
	
	private boolean mHidden = false;
	
	private int mTextureDataHandle;
	
	private OnClickListener mOnClickListener;
	
	private int[] buffers = new int[4];
	
	public Button() {
		Matrix.setIdentityM(mModelMatrix, 0);
	}

	public FloatBuffer getVertices() {
		return mVertices;
	}

	public float[] getModelMatrix() {
		return mModelMatrix;
	}

	public void setModelMatrix(float[] modelMatrix) {
		this.mModelMatrix = modelMatrix;
	}
	
	public FloatBuffer getColors() {
		return mColors;
	}
	
	public FloatBuffer getTextureCoords()
	{
		return mTextureCoords;
	}
	
	public FloatBuffer getHighlightColors()
	{
		return mHighlighColors;
	}
	
	private boolean buffersGenerated = false;
	
	public void putVertexData(float[] vertexData)
	{
		mVertices = ByteBuffer.allocateDirect(vertexData.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mVertices.put(vertexData).position(0);
		
		if(!buffersGenerated)
		{
			GLES20.glGenBuffers(4, buffers, 0);
			buffersGenerated = !buffersGenerated;
		}
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, mVertices, GLES20.GL_STATIC_DRAW);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
	}
	
	public void putColorData(float[] colorData)
	{
		mColors = ByteBuffer.allocateDirect(colorData.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mColors.put(colorData).position(0);
		
		if(!buffersGenerated)
		{
			GLES20.glGenBuffers(4, buffers, 0);
			buffersGenerated = !buffersGenerated;
		}
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, colorData.length * 4, mColors, GLES20.GL_STATIC_DRAW);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
	}
	
	public void putHighlightColorData(float[] highlightColorData)
	{
		mHighlighColors = ByteBuffer.allocateDirect(highlightColorData.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer(); 
		mHighlighColors.put(highlightColorData).position(0);
		
		if(!buffersGenerated)
		{
			GLES20.glGenBuffers(4, buffers, 0);
			buffersGenerated = !buffersGenerated;
		}
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[3]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, highlightColorData.length * 4, mHighlighColors, GLES20.GL_STATIC_DRAW);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		
	}
	
	public void putTextureCoords(float[] textureData)
	{
		mTextureCoords = ByteBuffer.allocateDirect(textureData.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mTextureCoords.put(textureData).position(0);
		
		if(!buffersGenerated)
		{
			GLES20.glGenBuffers(4, buffers, 0);
			buffersGenerated = !buffersGenerated;
		}
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[2]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, textureData.length * 4, mTextureCoords, GLES20.GL_STATIC_DRAW);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
	}

	public int[] getBuffers()
	{
		return this.buffers;
	}
	
	public int getTextureDataHandle() {
		return mTextureDataHandle;
	}

	public void setTextureDataHandle(int textureDataHandle) {
		this.mTextureDataHandle = textureDataHandle;
	}
	
	public void setOnClickListener(OnClickListener onClickListener)
	{
		this.mOnClickListener = onClickListener;
	}
	
	public void onClick()
	{
		if(mOnClickListener != null)
		{
			mOnClickListener.onClick();
		}
	}

	public boolean isHidden() {
		return mHidden;
	}

	public void setHidden(boolean hidden) {
		mHidden = hidden;
	}
	
}
