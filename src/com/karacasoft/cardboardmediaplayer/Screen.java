package com.karacasoft.cardboardmediaplayer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

public class Screen {

	private FloatBuffer mVertices; 
	private FloatBuffer mColors;
	private FloatBuffer mHighlighColors;
	private FloatBuffer mNotReadyColors;
	private FloatBuffer mTextureCoords;
	
	private FloatBuffer mTextureCoordsLeftEye;
	private FloatBuffer mTextureCoordsRightEye;
	
	private float[] mModelMatrix = new float[16];
	
	private int mTextureDataHandle;
	
	private Surface surface;
	
	private MediaPlayer mMediaPlayer = new MediaPlayer();
	private ArrayList<File> fileList = new ArrayList<File>();
	private int currentVid = 0;
	private boolean sourceReady = false;
	private boolean isReady = false;
	private boolean isPlaying = false;
	private boolean readFilesFinished = false;
	
	public float mVideoWidth;
	public float mVideoHeight;
	
	public Screen() {
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, -0.5f, 0f, 0f);
		searchForVideoFiles();
		
		mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				isPlaying = false;
			}
		});
	}
	
	public boolean gotThumbnail = false;
	
	public File getCurrentFile()
	{
		return fileList.get(currentVid);
	}
	
	public void next() throws IllegalArgumentException, SecurityException, IllegalStateException, IOException
	{
		if(currentVid < fileList.size() - 1)
		{
			currentVid++;
			gotThumbnail = false;
			setSource(fileList.get(currentVid).getPath());
		}
	}
	
	public void previous() throws IllegalArgumentException, SecurityException, IllegalStateException, IOException
	{
		if(currentVid > 0)
		{
			currentVid--;
			gotThumbnail = false;
			setSource(fileList.get(currentVid).getPath());
		}
	}
	
	public void setSource(String path) throws IllegalArgumentException,
			SecurityException, IllegalStateException, IOException
	{
		if(!firstFile)
		{
			reset();
		}
		mMediaPlayer.setDataSource(path);
		
		sourceReady = true;
		isReady = false;
		prepare();
	}
	
	public boolean isSourceAdded()
	{
		return sourceReady;
	}
	
	public boolean isPrepared()
	{
		return isReady;
	}
	
	public boolean isFilesRead()
	{
		return readFilesFinished;
	}
	
	public void reset()
	{
		mMediaPlayer.reset();
		setSurface(Utils.surfaceTextureMap.get(mTextureDataHandle));
	}
	
	public void prepare() throws IllegalStateException, IOException
	{
		mMediaPlayer.prepare();
		Matrix.setIdentityM(mModelMatrix, 0);
		mVideoWidth = mMediaPlayer.getVideoWidth();
		mVideoHeight = mMediaPlayer.getVideoHeight();
		Matrix.translateM(mModelMatrix, 0, -(mVideoWidth / mVideoHeight) / 2, 0f, 0f);
		Matrix.scaleM(mModelMatrix, 0, mVideoWidth / mVideoHeight, 1f, 1f);
		isReady = true;
	}
	
	public void playPause()
	{
		if(mMediaPlayer.isPlaying())
		{
			mMediaPlayer.pause();
			isPlaying = false;
		}else{
			mMediaPlayer.start();
			isPlaying = true;
		}
	}
	
	public boolean isPlaying()
	{
		return isPlaying;
	}
	
	public void stop()
	{
		mMediaPlayer.stop();
		isPlaying = false;
	}
	
	public void setVolume(float left, float right)
	{
		mMediaPlayer.setVolume(left, right);
	}
	
	public void destroy()
	{
		mMediaPlayer.release();
		readFilesThread.interrupt();
	}
	private Thread readFilesThread;
	public void searchForVideoFiles()
	{
		
		readFilesThread = new Thread(new Runnable() {
			@Override
			public void run() {
				searchDir(Environment.getExternalStorageDirectory());
				readFilesFinished = true;
			}
		});
		readFilesThread.start();
	}
	private boolean firstFile = true;
	private void searchDir(File dir)
	{
		File[] files = dir.listFiles();
		if(files != null)
		{
			for (File file : files) {
				if(file.isDirectory())
				{
					try
					{
						searchDir(file);
					}catch(NullPointerException e)
					{
						Log.e("Screen", "FileNotFound");
					}
				}else{
					if(file.getName().endsWith(".mp4"))
					{
						fileList.add(file);
						if(firstFile)
						{
	
							try {
								setSource(file.getPath());
								firstFile = false;
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}else{
			Log.e("Screen", "Error while retrieving files.");
		}
	}
	
	public void setSurface(SurfaceTexture st)
	{
		surface = new Surface(st);
		mMediaPlayer.setSurface(surface);
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
	
	public FloatBuffer getTextureCoordsLeftEye()
	{
		return mTextureCoordsLeftEye;
	}
	
	public FloatBuffer getTextureCoordsRightEye()
	{
		return mTextureCoordsRightEye;
	}
	
	public FloatBuffer getHighlightColors()
	{
		return mHighlighColors;
	}
	
	public FloatBuffer getNotReadyColors()
	{
		return mNotReadyColors;
	}
	
	private int[] buffers = new int[7];
	private boolean buffersGenerated = false;
	
	
	public void putVertexData(float[] vertexData)
	{
		mVertices = ByteBuffer.allocateDirect(vertexData.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mVertices.put(vertexData).position(0);
		
		if(!buffersGenerated)
		{
			GLES20.glGenBuffers(7, buffers, 0);
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
			GLES20.glGenBuffers(7, buffers, 0);
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
			GLES20.glGenBuffers(7, buffers, 0);
			buffersGenerated = !buffersGenerated;
		}
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[2]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, highlightColorData.length * 4, mHighlighColors, GLES20.GL_STATIC_DRAW);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		
	}
	
	public void putNotReadyColorData(float[] notReadyColorData)
	{
		mNotReadyColors = ByteBuffer.allocateDirect(notReadyColorData.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer(); 
		mNotReadyColors.put(notReadyColorData).position(0);
		
		if(!buffersGenerated)
		{
			GLES20.glGenBuffers(7, buffers, 0);
			buffersGenerated = !buffersGenerated;
		}
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[3]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, notReadyColorData.length * 4, mNotReadyColors, GLES20.GL_STATIC_DRAW);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
	}
	
	public void putTextureCoords(float[] textureData)
	{
		mTextureCoords = ByteBuffer.allocateDirect(textureData.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mTextureCoords.put(textureData).position(0);
		
		if(!buffersGenerated)
		{
			GLES20.glGenBuffers(7, buffers, 0);
			buffersGenerated = !buffersGenerated;
		}
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[4]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, textureData.length * 4, mTextureCoords, GLES20.GL_STATIC_DRAW);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
	}

	public void putTextureCoordsLeftEye(float[] textureData)
	{
		mTextureCoordsLeftEye = ByteBuffer.allocateDirect(textureData.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mTextureCoordsLeftEye.put(textureData).position(0);
		
		if(!buffersGenerated)
		{
			GLES20.glGenBuffers(7, buffers, 0);
			buffersGenerated = !buffersGenerated;
		}
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[5]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, textureData.length * 4, mTextureCoordsLeftEye, GLES20.GL_STATIC_DRAW);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
	}
	
	public void putTextureCoordsRightEye(float[] textureData)
	{
		mTextureCoordsRightEye = ByteBuffer.allocateDirect(textureData.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mTextureCoordsRightEye.put(textureData).position(0);
		
		if(!buffersGenerated)
		{
			GLES20.glGenBuffers(7, buffers, 0);
			buffersGenerated = !buffersGenerated;
		}
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[6]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, textureData.length * 4, mTextureCoordsRightEye, GLES20.GL_STATIC_DRAW);
		
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
	
}
