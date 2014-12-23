package com.karacasoft.cardboardmediaplayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.CardboardView.StereoRenderer;
import com.google.vrtoolkit.cardboard.EyeParams;
import com.google.vrtoolkit.cardboard.EyeTransform;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

public class MainActivity extends CardboardActivity implements StereoRenderer{

	private long mLastTimeClicked;
	private boolean mHideButtonsThreadRunning = true;
	
	private class HideButtonsThread extends Thread{
		
		@Override
		public void run() {
			while(mHideButtonsThreadRunning)
			{
				while(mButtonsHidden)
				{
					try {
						synchronized (mHideButtonsThread) {
							this.wait();
						}
					} catch (InterruptedException e) {}
				}
				long time = System.currentTimeMillis();
				if(time - mLastTimeClicked > 10000)
				{
					mButtonsHidden = true;
				}
				
			}
		}
		
	}
	
	private static final float YAW_LIMIT = 0.12f;
    private static final float PITCH_LIMIT = 0.12f;
	
	private final String vertexShaderCode = ""
			+ "uniform mat4 uMVPMatrix;"
			+ ""
			+ "attribute vec4 a_Position;"
			+ "attribute vec4 a_Color;"
			+ "attribute vec2 a_TexCoordinate;"
			+ ""
			+ "varying vec4 v_Color;"
			+ "varying vec2 v_TexCoordinate;"
			+ ""
			+ "void main(){"
			+ "    v_TexCoordinate = a_TexCoordinate;"
			+ "    v_Color = a_Color;"
			+ "    gl_Position = uMVPMatrix * a_Position;"
			+ "}"
			+ "";
	private final String fragmentShaderCode = ""
			+ "#extension GL_OES_EGL_image_external : require\n"
			+ ""
			+ "precision mediump float;"
			+ ""
			+ "varying vec4 v_Color;"
			+ "varying vec2 v_TexCoordinate;"
			+ ""
			+ "uniform samplerExternalOES u_Texture;"
			+ "uniform float f_extOESTexColorMult;"
			+ ""
			+ "uniform sampler2D u_2DTexture;"
			+ "uniform float f_2DTexColorMult;"
			+ ""
			+ "void main(){"
			+ "    gl_FragColor = v_Color + (texture2D(u_Texture, v_TexCoordinate) * f_extOESTexColorMult)"
			+ "            + (texture2D(u_2DTexture, v_TexCoordinate) * f_2DTexColorMult);"
			+ "}";
	
	
	private CardboardView cbView;
	
	//Matrices
	private float[] mCamera = new float[16];
	private float[] mViewMatrix = new float[16];
	private float[] mMVMatrix = new float[16];
	private float[] mMVPMatrix = new float[16];
	private float[] mHeadView = new float[16];
	
	
	//Handlers
	private int mProgramHandle;
	
	private int mPositionHandle;
	private int mColorHandle;
	private int mMVPMatrixHandle;
	private int mTexCoordHandle;
	private int mTexUniformHandle;
	private int mTexColorMultHandle;
	private int m2DTexUniformHandle;
	private int m2DTexColorMultHandle;
	
	private int mTexturePlayDataHandle;
	private int mTexturePauseDataHandle;
	
	private int m3DProgressDialogTextureDataHandle;
	
	private ArrayList<Button> mButtons = new ArrayList<Button>();
	private Screen mVideoScreen = new Screen();
	private Surface m3DProgressDialogSurface;
	private HideButtonsThread mHideButtonsThread = new HideButtonsThread();
	
	private boolean mCameraLocked = false;
	private boolean mHSBSActive = false;
	
	private boolean mButtonsHidden = false;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(
			    WindowManager.LayoutParams.FLAG_FULLSCREEN,
			    WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.onCreate(savedInstanceState);
		
		LinearLayout ln = new LinearLayout(this);
		
		cbView = new CardboardView(this);
		
		cbView.setRenderer(this);
		cbView.setPreserveEGLContextOnPause(true);
		setContentView(cbView);
		
		cbView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onCardboardTrigger();
			}
		});
		mHideButtonsThread.start();
	}
	
	@Override
	public void onDrawEye(EyeTransform arg0) {
		
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		
		
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		
		GLES20.glEnableVertexAttribArray(mPositionHandle);
		GLES20.glEnableVertexAttribArray(mColorHandle);
		GLES20.glEnableVertexAttribArray(mTexCoordHandle);
		
		synchronized (this) {
			if(resetCam)
			{
				float[] invertedEye = new float[16];
				Matrix.invertM(invertedEye, 0, arg0.getEyeView(), 0);
				mCamera = Utils.getCameraMatrix();
				Matrix.multiplyMM(mCamera, 0, invertedEye, 0, mCamera, 0);
				resetCam = false;
			}
		}
		
		if(!mCameraLocked)
		{
			Matrix.multiplyMM(mViewMatrix, 0, arg0.getEyeView(), 0, mCamera, 0);
		}else{
			mViewMatrix = mCamera;
		}
		
		drawScreen(arg0);
		
		if(!mVideoScreen.isFilesRead())
		{
			draw3DProgressDialog((String) getText(R.string.scanning_files), arg0);
		}else{
			if(mVideoScreen.isPlaying())
			{
			}else{
				draw3DProgressDialogWithImage("", arg0);
			}
		}
		if(!mButtonsHidden)
		{
			if(!mCameraLocked)
			{
				synchronized (this) {
					
					for (Button b : mButtons) {
						drawButton(b, arg0);
					}
					GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
					Utils.checkGlError("Bind Buffer 0");
					
					
				}
			}
		}
		
		Utils.checkGlError("OnDrawEye");
	}

	
	
	@Override
	public void onFinishFrame(Viewport arg0) {
		GLES20.glDisableVertexAttribArray(mPositionHandle);
		GLES20.glDisableVertexAttribArray(mColorHandle);
		GLES20.glDisableVertexAttribArray(mTexCoordHandle);
		
	}

	public void drawButton(Button button, EyeTransform eye)
	{
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, button.getBuffers()[0]);
		Utils.checkGlError("Bind Vertex Buffer");
		GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, 0);
		Utils.checkGlError("Vertex Buffer Pointer");
//		GLES20.glEnableVertexAttribArray(mPositionHandle);
		
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, button.getBuffers()[2]);
		Utils.checkGlError("Bind Texture Buffer");
		GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, 0);
		Utils.checkGlError("Texture Buffer Pointer");
//		GLES20.glEnableVertexAttribArray(mTexCoordHandle);
//		
//		GLES20.glEnableVertexAttribArray(mColorHandle);
		
		
		
		GLES20.glUniform1f(m2DTexColorMultHandle, 1.0f);
		Utils.checkGlError("2D Tex Color Multiplier");
		GLES20.glUniform1f(mTexColorMultHandle, 0.0f);
		Utils.checkGlError("Normal Color Multiplier");
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, button.getTextureDataHandle());
		Utils.checkGlError("Bind Texture 2D");
		GLES20.glUniform1i(m2DTexUniformHandle, 0);
		Utils.checkGlError("2D Texture Uniform Handle");
		
		
		if(isLookingAtButton(button))
		{
			Matrix.translateM(button.getModelMatrix(), 0, 0f, 0f, 0.05f);
			Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, button.getModelMatrix(), 0);
			button.mMVMatrix = mMVMatrix.clone();
			Matrix.translateM(button.getModelMatrix(), 0, 0f, 0f, -0.05f);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, button.getBuffers()[3]);
			Utils.checkGlError("Bind Color Buffer Highlight");
			GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, 0);
			Utils.checkGlError("Color Buffer Highlight");
		}else{
			Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, button.getModelMatrix(), 0);
			button.mMVMatrix = mMVMatrix.clone();
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, button.getBuffers()[1]);
			Utils.checkGlError("Bind Color Buffer Normal");
			GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, 0);
			Utils.checkGlError("Color Buffer Normal");
		}
		
		
		
		Matrix.multiplyMM(mMVPMatrix, 0, eye.getPerspective(), 0, mMVMatrix, 0);
		
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
		Utils.checkGlError("ModelVPMatrix Uniform");
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
		Utils.checkGlError("drawButton: " + mButtons.indexOf(button));
	}
	
	public void drawScreen(EyeTransform eye)
	{
		while(frameAvailable != frameAvailableCompare)
		{
			frameAvailableCompare++;
			Utils.surfaceTextureMap.get(mVideoScreen.getTextureDataHandle()).updateTexImage();
		}
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVideoScreen.getBuffers()[0]);
		Utils.checkGlError("Bind Vertex Buffer");
		GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, 0);
		Utils.checkGlError("Vertex buffer pointer");
//		GLES20.glEnableVertexAttribArray(mPositionHandle);
		if(mVideoScreen.isFilesRead())
		{
			if(mVideoScreen.isPrepared())
			{
//				if(mVideoScreen.isPlaying())
//				{
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVideoScreen.getBuffers()[1]);
					GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, 0);
//					GLES20.glEnableVertexAttribArray(mColorHandle);
					
//				}else{
//					mVideoScreen.getHighlightColors().position(0);
//					GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false,
//							4 * 4, mVideoScreen.getHighlightColors());
////					GLES20.glEnableVertexAttribArray(mColorHandle);
//				}
			}else{
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVideoScreen.getBuffers()[3]);
				GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, 0);
//				GLES20.glEnableVertexAttribArray(mColorHandle);
			}
		}else{
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVideoScreen.getBuffers()[3]);
			GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, 0);
//			GLES20.glEnableVertexAttribArray(mColorHandle);
		}
		if(mHSBSActive)
		{
			if(eye.getParams().getEye() == EyeParams.Eye.LEFT)
			{
				
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVideoScreen.getBuffers()[5]);
				GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, 0);
//				GLES20.glEnableVertexAttribArray(mTexCoordHandle);
			}else{
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVideoScreen.getBuffers()[6]);
				GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, 0);
//				GLES20.glEnableVertexAttribArray(mTexCoordHandle);
			}
		}else{
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVideoScreen.getBuffers()[4]);
			GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, 0);
//			GLES20.glEnableVertexAttribArray(mTexCoordHandle);
		}
		Utils.checkGlError("Color and Texture Handle");
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mVideoScreen.getTextureDataHandle());
		GLES20.glUniform1i(mTexUniformHandle, 1);
		Utils.checkGlError("Texture set");

		GLES20.glUniform1f(m2DTexColorMultHandle, 0.0f);
		GLES20.glUniform1f(mTexColorMultHandle, 1.0f);
		Utils.checkGlError("Color Multipliers");
		Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mVideoScreen.getModelMatrix(), 0);
		Matrix.multiplyMM(mMVPMatrix, 0, eye.getPerspective(), 0, mMVMatrix, 0);
		
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
		Utils.checkGlError("MVPMatrix set");
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
		Utils.checkGlError("drawScreen");
		
	}
	
	public void draw3DProgressDialog(String text, EyeTransform eye)
	{
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVideoScreen.getBuffers()[0]);
		GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, 0);
//		GLES20.glEnableVertexAttribArray(mPositionHandle);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVideoScreen.getBuffers()[1]);
		GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, 0);
//		GLES20.glEnableVertexAttribArray(mColorHandle);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVideoScreen.getBuffers()[4]);
		GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, 0);
//		GLES20.glEnableVertexAttribArray(mTexCoordHandle);
		
		//nullcheck
		if(m3DProgressDialogSurface == null)
		{
			Utils.surfaceTextureMap.get(m3DProgressDialogTextureDataHandle).setDefaultBufferSize(500, 250);
			m3DProgressDialogSurface = new Surface(
					Utils.surfaceTextureMap.get(m3DProgressDialogTextureDataHandle));
		}
		
		
		Canvas c = m3DProgressDialogSurface.lockCanvas(null);
		Paint p = new Paint();
		p.setColor(Color.WHITE);
		p.setTextSize(25);
		p.setTextAlign(Align.CENTER);
		c.drawARGB(255, 6, 104, 173);
		c.drawText(text, 250, 145, p);
		m3DProgressDialogSurface.unlockCanvasAndPost(c);
		Utils.surfaceTextureMap.get(m3DProgressDialogTextureDataHandle).updateTexImage();
		
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, m3DProgressDialogTextureDataHandle);
		GLES20.glUniform1i(mTexUniformHandle, 1);
		
		GLES20.glUniform1f(m2DTexColorMultHandle, 0.0f);
		GLES20.glUniform1f(mTexColorMultHandle, 1.0f);
		
		float[] modelMatrix = new float[16];
		Matrix.translateM(modelMatrix, 0, mVideoScreen.getModelMatrix(), 0, 0f, 0f, -0.5f);
		
		Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, modelMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, eye.getPerspective(), 0, mMVMatrix, 0);
		
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
		
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
		Utils.checkGlError("drawScreen");
	}
	
	Bitmap bmp;
	
	public void draw3DProgressDialogWithImage(String text, EyeTransform eye)
	{
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVideoScreen.getBuffers()[0]);
		GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, 0);
//		GLES20.glEnableVertexAttribArray(mPositionHandle);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVideoScreen.getBuffers()[1]);
		GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, 0);
//		GLES20.glEnableVertexAttribArray(mColorHandle);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVideoScreen.getBuffers()[4]);
		GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, 0);
//		GLES20.glEnableVertexAttribArray(mTexCoordHandle);
		
		
		
		//nullcheck
		if(m3DProgressDialogSurface == null)
		{
			Utils.surfaceTextureMap.get(m3DProgressDialogTextureDataHandle).setDefaultBufferSize(500, 250);
			m3DProgressDialogSurface = new Surface(
					Utils.surfaceTextureMap.get(m3DProgressDialogTextureDataHandle));
		}
		
		
		Canvas c = m3DProgressDialogSurface.lockCanvas(null);
		Paint p = new Paint();
		p.setColor(Color.WHITE);
		p.setTextSize(25);
		p.setTextAlign(Align.CENTER);
		
		Paint rectPaint = new Paint();
		rectPaint.setColor(Color.parseColor("#90000000"));
		
		c.drawARGB(255, 6, 104, 173);
		c.drawText(text, 250, 145, p);
		MediaMetadataRetriever mediaRetriever = new MediaMetadataRetriever();
		
		
		try
		{
			File currentFile = mVideoScreen.getCurrentFile();
			mediaRetriever.setDataSource(currentFile.toString());
			if(!mVideoScreen.gotThumbnail)
			{
				bmp = mediaRetriever.getFrameAtTime();
				mVideoScreen.gotThumbnail = true;
			}
			if(bmp != null)
			{
				c.drawBitmap(bmp, 0, 0, null);
				
			}
			float width = p.measureText(currentFile.getName());
			c.drawRect(250 - width / 2, 145, 250 + width / 2, 170, rectPaint);
			c.drawText(currentFile.getName(), 250, 170, p);
		}catch(Exception ex)
		{
			ex.printStackTrace();
			c.drawText("Error retrieving video thumbnail", 250, 145, p);
		}
		m3DProgressDialogSurface.unlockCanvasAndPost(c);
		Utils.surfaceTextureMap.get(m3DProgressDialogTextureDataHandle).updateTexImage();
		
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, m3DProgressDialogTextureDataHandle);
		GLES20.glUniform1i(mTexUniformHandle, 1);
		
		GLES20.glUniform1f(m2DTexColorMultHandle, 0.0f);
		GLES20.glUniform1f(mTexColorMultHandle, 1.0f);
		
		float[] modelMatrix = new float[16];
		Matrix.translateM(modelMatrix, 0, mVideoScreen.getModelMatrix(), 0, 0f, 0f, -0.5f);
		
		Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, modelMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, eye.getPerspective(), 0, mMVMatrix, 0);
		
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
		
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
		Utils.checkGlError("drawScreen");
	}
	
	@Override
	public void onNewFrame(HeadTransform arg0) {
		GLES20.glUseProgram(mProgramHandle);
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
		mTexUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
		mTexColorMultHandle = GLES20.glGetUniformLocation(mProgramHandle, "f_extOESTexColorMult");
		m2DTexUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_2DTexture");
		m2DTexColorMultHandle = GLES20.glGetUniformLocation(mProgramHandle, "f_2DTexColorMult");
		mColorHandle = GLES20.glGetUniformLocation(mProgramHandle, "f_colorMult");
		mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
		mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
		mTexCoordHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
		
		arg0.getHeadView(mHeadView, 0);
		
		if(mVideoScreen.isPlaying())
		{
			mButtons.get(0).setTextureDataHandle(mTexturePauseDataHandle);
		}else{
			mButtons.get(0).setTextureDataHandle(mTexturePlayDataHandle);
		}
		
		Utils.checkGlError("OnNewFrame");
	}

	@Override
	public void onRendererShutdown() {
		mVideoScreen.destroy();
		mHideButtonsThreadRunning = false;
		mHideButtonsThread.interrupt();
	}

	@Override
	public void onSurfaceChanged(int arg0, int arg1) {
		GLES20.glViewport(0, 0, arg0, arg1);
		Utils.checkGlError("OnSurfaceChanged");
		
	}
	private int frameAvailable = 0;
	private int frameAvailableCompare = 0;
	@Override
	public void onSurfaceCreated(EGLConfig arg0) {
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		mCamera = Utils.getCameraMatrix();
		mProgramHandle = Utils.initProgram(vertexShaderCode, fragmentShaderCode);
		initButtons();
		
		GLES20.glUseProgram(mProgramHandle);
		
		Utils.surfaceTextureMap.get(mVideoScreen.getTextureDataHandle())
			.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
			
				@Override
				public void onFrameAvailable(SurfaceTexture surfaceTexture) {
					frameAvailable++;
				}
			});
		
		
		
		Utils.checkGlError("initProgram");
		
	}
	
	private void initButtons()
	{
		Button bPlay = new Button();
		bPlay.setTextureDataHandle(Utils.loadTexture(this, R.drawable.ic_action_pause));
		
		mTexturePlayDataHandle = Utils.loadTexture(this, R.drawable.ic_action_play);
		mTexturePauseDataHandle = Utils.loadTexture(this, R.drawable.ic_action_pause);
//		bPlay.setTextureDataHandle(Utils.loadBitmap(this, R.raw.ic_action_pause));
//		
//		mTexturePlayDataHandle = Utils.loadBitmap(this, R.raw.ic_action_play);
//		mTexturePauseDataHandle = Utils.loadBitmap(this, R.raw.ic_action_pause);
		//modelMatrix settings
		float[] modelMatrix = new float[16];
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.scaleM(modelMatrix, 0, 0.25f, 0.25f, 1f);
		Matrix.translateM(modelMatrix, 0, 0.0f, -2.5f, 0.0f);
		bPlay.setModelMatrix(modelMatrix.clone());
		bPlay.putVertexData(WorldData.stdButtonVertexData);
		bPlay.putColorData(WorldData.stdButtonColorData);
		bPlay.putTextureCoords(WorldData.stdTextureCoordData);
		bPlay.putHighlightColorData(WorldData.stdButtonHighlightColorData);
		bPlay.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick() {
				if(mVideoScreen.isPrepared())
				{
					if(mVideoScreen.isSourceAdded() && !mVideoScreen.isPrepared())
					{
						try {
							mVideoScreen.prepare();
							mVideoScreen.playPause();
						} catch (IllegalStateException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}else if(mVideoScreen.isSourceAdded() && mVideoScreen.isPrepared())
					{
						mVideoScreen.playPause();
					}else{}
				}
			}
		});
		mButtons.add(bPlay);
		
		
		Button bNext = new Button();
		bNext.setTextureDataHandle(Utils.loadTexture(this, R.drawable.ic_action_next));
//		bNext.setTextureDataHandle(Utils.loadBitmap(this, R.raw.ic_action_next));
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.scaleM(modelMatrix, 0, 0.25f, 0.25f, 1f);
		Matrix.translateM(modelMatrix, 0, -2.0f, -2.5f, 0.0f);
		bNext.setModelMatrix(modelMatrix.clone());
		bNext.putVertexData(WorldData.stdButtonVertexData);
		bNext.putColorData(WorldData.stdButtonColorData);
		bNext.putTextureCoords(WorldData.stdTextureCoordData);
		bNext.putHighlightColorData(WorldData.stdButtonHighlightColorData);
		bNext.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick() {
				try {
					mVideoScreen.next();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		mButtons.add(bNext);
		
		
		Button bPrev = new Button();
		bPrev.setTextureDataHandle(Utils.loadTexture(this, R.drawable.ic_action_previous));
//		bPrev.setTextureDataHandle(Utils.loadBitmap(this, R.raw.ic_action_previous));
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.scaleM(modelMatrix, 0, 0.25f, 0.25f, 1f);
		Matrix.translateM(modelMatrix, 0, 2.0f, -2.5f, 0.0f);
		bPrev.setModelMatrix(modelMatrix.clone());
		bPrev.putVertexData(WorldData.stdButtonVertexData);
		bPrev.putColorData(WorldData.stdButtonColorData);
		bPrev.putTextureCoords(WorldData.stdTextureCoordData);
		bPrev.putHighlightColorData(WorldData.stdButtonHighlightColorData);
		bPrev.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick() {
				try {
					mVideoScreen.previous();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		mButtons.add(bPrev);
		
		
		Button bLock = new Button();
		bLock.setTextureDataHandle(Utils.loadTexture(this, R.drawable.ic_action_screen_locked_to_landscape));
//		bLock.setTextureDataHandle(Utils.loadBitmap(this, R.raw.ic_action_screen_locked_to_landscape));
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.scaleM(modelMatrix, 0, 0.25f, 0.25f, 1f);
		Matrix.translateM(modelMatrix, 0, 4.0f, -2.5f, 0.0f);
		bLock.setModelMatrix(modelMatrix.clone());
		bLock.putVertexData(WorldData.stdButtonVertexData);
		bLock.putColorData(WorldData.stdButtonColorData);
		bLock.putTextureCoords(WorldData.stdTextureCoordData);
		bLock.putHighlightColorData(WorldData.stdButtonHighlightColorData);
		bLock.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick() {
				mCamera = Utils.getCameraMatrix();
				mCameraLocked = true;
			}
		});
		mButtons.add(bLock);
		
		
		Button bHSBS = new Button();
		bHSBS.setTextureDataHandle(Utils.loadTexture(this, R.drawable.ic_action_hsbs));
//		bHSBS.setTextureDataHandle(Utils.loadBitmap(this, R.raw.ic_action_hsbs));
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.scaleM(modelMatrix, 0, 0.25f, 0.25f, 1f);
		Matrix.translateM(modelMatrix, 0, -4.0f, -2.5f, 0.0f);
		bHSBS.setModelMatrix(modelMatrix.clone());
		bHSBS.putVertexData(WorldData.stdButtonVertexData);
		bHSBS.putColorData(WorldData.stdButtonColorData);
		bHSBS.putTextureCoords(WorldData.stdTextureCoordData);
		bHSBS.putHighlightColorData(WorldData.stdButtonHighlightColorData);
		bHSBS.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick() {
				mHSBSActive = !mHSBSActive;
			}
		});
		mButtons.add(bHSBS);
		
		
		
		mVideoScreen.putVertexData(WorldData.screenVertexData);
		mVideoScreen.putColorData(WorldData.screenColorData);
		mVideoScreen.putNotReadyColorData(WorldData.screenNotReadyColorData);
		mVideoScreen.putHighlightColorData(WorldData.screenReadyColorData);
		mVideoScreen.putTextureCoords(WorldData.screenTextureCoordsData);
		mVideoScreen.putTextureCoordsLeftEye(WorldData.screenTextureCoordsLeftEyeData);
		mVideoScreen.putTextureCoordsRightEye(WorldData.screenTextureCoordsRightEyeData);
		mVideoScreen.setTextureDataHandle(Utils.loadTexture());
		mVideoScreen.setSurface(Utils.surfaceTextureMap.get(mVideoScreen.getTextureDataHandle()));
		
		m3DProgressDialogTextureDataHandle = Utils.loadTexture();
		
//		Utils.surfaceTextureMap.get(mVideoScreen.getTextureDataHandle()).setOnFrameAvailableListener(this);
	}
	
	private boolean isLookingAtButton(Button b)
	{
		float[] initVec = {0.0f, 3.0f, 1.0f, 1.0f};
        float[] objPositionVec = new float[4];

//        float[] MVMatrix = new float[16];
        
//        Matrix.multiplyMM(MVMatrix, 0, mHeadView, 0, b.mMVMatrix, 0);
        Matrix.multiplyMV(objPositionVec, 0, b.mMVMatrix, 0, initVec, 0);

        float pitch = (float)Math.atan2(objPositionVec[1], -objPositionVec[2]);
        float yaw = (float)Math.atan2(objPositionVec[0], -objPositionVec[2]);
        
        return (Math.abs(pitch) < PITCH_LIMIT) && (Math.abs(yaw) < YAW_LIMIT);
	}
	
	private boolean resetCam = false;
	
	@Override
	public void onCardboardTrigger() {
		mLastTimeClicked = System.currentTimeMillis();
		boolean noButton = true;
		if(mCameraLocked)
		{
			mCameraLocked = false;
			mCamera = Utils.getCameraMatrix();
		}else{
			if(!mButtonsHidden)
			{
				synchronized (this) {
					for (Button b : mButtons) {
						if(isLookingAtButton(b))
						{
							b.onClick();
							noButton = false;
						}
					}
					
				}
			}else{
				mButtonsHidden = false;
				synchronized (mHideButtonsThread) {
					mHideButtonsThread.notify();
				}
			}
		}
		if(noButton)
		{
			resetCam = true;
		}
		
	}
	
}
