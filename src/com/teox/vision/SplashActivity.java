package com.teox.vision;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
/**
 * Activity for displaying SplashScreen and load necessary files
 * @author Teo Xanthopoulos 
 */
public class SplashActivity extends Activity {
	/**
	 * Debug Tag for use logging debug output to LogCat
	 */
	private static final String 		TAG = "Vision::SplashActivity";
	/**
	 * Splash Thread declaration
	 */
	private Thread 						splashThread;
	/**
	 * Time in milliseconds to wait in SplashScreen
	 */
	protected int 						splashTime = 3500;	
	/**
	 * Image folder absolute path
	 */
	private static final String 		ImgPath = "/data/data/com.teox.vision/images";
	/**
	 * Classifier files declaration
	 */
	private File 						mEyeCascadeFile, mEyeGCascadeFile, mFaceCascadeFile;
	/**
	 * Boolean variable used for checking image folder
	 */
	boolean 							isImgDirCreated = false;
	/**
	 * Boolean variable for checking eye classifier
	 */
	boolean								eyeOk = false;
	/**
	 * Boolean variable for checking eye glasses classifier
	 */
	boolean								eyeGOk = false;
	/**
	 * Boolean variable for checking face classifier
	 */
	boolean								faceOk = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		
		final SplashActivity splashScreen = this;
		// Thread for display the SplashScreen and load necessary files
		splashThread = new Thread(){
			@Override
			public void run() {
				try{
					synchronized(this){
						 // Create image folder
						 isImgDirCreated = createImageFolder(); 
						 // Check image folder
						 if(isImgDirCreated){
								Log.i(TAG, "Image Folder ok");  
						 }else{
								Log.i(TAG, "Image Folder already exists");  
						 }	
						 File eF = new File("/data/data/com.teox.vision/app_EyeCascade/haarcascade_eye.xml");
						 File eGF = new File("/data/data/com.teox.vision/app_EyeGCascade/haarcascade_eye_tree_eyeglasses.xml");
						 File fF = new File("/data/data/com.teox.vision/app_cascade/lbpcascade_frontalface.xml");
						 // If eye classifier does not exist, load it
						 if(!eF.exists()){
							 eyeOk = loadCascadeEyeFromRes();
							 Log.i(TAG, "Eye Cascade loaded: "+String.valueOf(eyeOk)); 
						 }else{
							 eyeOk = true;
						 }
						 // If eye glasses classifier does not exist, load it
						 if(!eGF.exists()){
							 eyeGOk = loadCascadeEyeGFromRes();
							 Log.i(TAG, "Eye Glasses Cascade loaded: "+String.valueOf(eyeGOk));
						 }else{
							 eyeGOk = true; 
						 }
						 // If face classifier does not exist, load it
						 if(!fF.exists()){
							 faceOk = loadCascadeFaceFromRes();
							 Log.i(TAG, "Face Cascade loaded: "+String.valueOf(faceOk));
						 }else{
							 faceOk = true;  
						 }	
						 // if all the classifiers already exists, wait splashTime
						 if(eyeOk && eyeGOk && faceOk){ 
							 wait(splashTime);
						 }					
					}					
				}catch(InterruptedException e){
					Log.i(TAG, "InterruptedException");
				}finally{
					finish();
					Intent sS = new Intent(splashScreen, StartScreenActivity.class);
					startActivity(sS);
				}
			}							
		};
		splashThread.start();		
	}	
	/**
	 * Method for creating image folder inside application folder ("/data/data/com.teox.vision/images")
	 * @author Teo Xanthopoulos
	 * @return true if folder created successfully, false if folder does not created successfully or already exists
	 */
	public boolean createImageFolder(){		
		File imageDirectory = new File(ImgPath);	
		return imageDirectory.mkdir();		 	
	}	
	/**
	 * This method copies the file haarcascade_eye.xml into application folder ("/data/data/com.teox.vision/app_EyeCascade")
	 * @author Teo Xanthopoulos
	 * @throws IOException
	 * @return true if file copied successfully, false if file did not copied successfully
	 */
	public boolean loadCascadeEyeFromRes(){		
		try{
			InputStream is = getResources().openRawResource(R.raw.haarcascade_eye);
			File cascadeDir = getDir("EyeCascade", Context.MODE_PRIVATE);
			mEyeCascadeFile = new File(cascadeDir, "haarcascade_eye.xml");
			FileOutputStream os = new FileOutputStream(mEyeCascadeFile);
			
			byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close(); 
            os.close(); 
            return true;
		}catch(IOException e){
			e.printStackTrace();
			Log.e(TAG, "Failed to load eye cascade. Exception thrown: " + e);
			return false;
		}		
	}
	/**
	 * This method copies the file haarcascade_eye_tree_eyeglasses.xml into application folder ("/data/data/com.teox.vision/app_EyeGCascade")
	 * @author Teo Xanthopoulos
	 * @throws IOException
	 * @return true if file copied successfully, false if file did not copied successfully
	 */
	public boolean loadCascadeEyeGFromRes(){
		try{
			InputStream is = getResources().openRawResource(R.raw.haarcascade_eye_tree_eyeglasses);
			File cascadeDir = getDir("EyeGCascade", Context.MODE_PRIVATE);
			mEyeGCascadeFile = new File(cascadeDir, "haarcascade_eye_tree_eyeglasses.xml");
			FileOutputStream os = new FileOutputStream(mEyeGCascadeFile);
			
			byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            return true;
		}catch(IOException e){
			e.printStackTrace();
			Log.e(TAG, "Failed to load eye glasses cascade. Exception thrown: " + e);
			return false;
		}
	}
	/**
	 * This method copies the file lbpcascade_frontalface.xml into application folder ("/data/data/com.teox.vision/app_EyeGCascade")
	 * @author Teo Xanthopoulos
	 * @throws IOException
	 * @return true if file copied successfully, false if file did not copied successfully
	 */
	public boolean loadCascadeFaceFromRes(){
		try{
			InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
			File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
			mFaceCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
			FileOutputStream os = new FileOutputStream(mFaceCascadeFile);
			
			byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            return true;
		}catch(IOException e){
			e.printStackTrace();
			Log.e(TAG, "Failed to load face cascade. Exception thrown: " + e);
			return false;
		}
	}	
}
