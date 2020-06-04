package com.teox.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
/**
 * An extension of VisionViewBase class. It has all declarations of all native methods (NDK - JNI).
 * Every Activity that needs to call native methods, uses this class. 
 * @author Teo Xanthopoulos
 */
public class VisionView extends VisionViewBase{
	/**
	 * Debug Tag for use logging debug output to LogCat
	 */
	private static final String 		TAG = "VisionView";
	/**
	 * The result of multiplying the frame height to the frame width
	 */
	private int 						mFrameSize;
	/**
	 * Bitmap object that will get the processed image 
	 */
    private Bitmap 						mBitmap;
    /**
     * Array of colors to write to the Bitmap
     */
    private int[] 						mRGBA;

	public VisionView(Context context) {
		super(context);
	}

	@Override
	protected Bitmap processFrame(byte[] data) {
		int[] rgba = mRGBA;
		
		// Process the camera image in native part. Converts from YUV 420 planar to BGRA packed format
		ShowPreview(getFrameWidth(), getFrameHeight(), data, rgba);	
		
		// Put the processed image into the Bitmap object that will be returned for display on the screen.
        Bitmap bmp = mBitmap;
        bmp.setPixels(rgba, 0/* offset */, getFrameWidth() /* stride */, 0, 0, getFrameWidth(), getFrameHeight());
		
		return bmp;
	}

	@Override
	protected void onPreviewStarted(int previewWidth, int previewHeight) {
		mFrameSize = previewWidth * previewHeight;
        mRGBA = new int[mFrameSize];
        mBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);		
	}

	@Override
	protected void onPreviewStopped() { 
		if(mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        mRGBA = null;		
	}	
	/**
	 * Method for initializing the 3 detectors (1 LBP, 2 HAAR detectors). Sends
	 * paths for the 3 detectors to native part.
	 * Calls a native (JNI) function
	 * @author Teo Xanthopoulos
	 * @param c1 String containing the path for 1st detector
	 * @param c2 String containing the path for 2nd detector
	 * @param c3 String containing the path for 2nd detector 
	 */
	public void initialize3Detectors(String c1, String c2, String c3) {
		initDetectors(c1, c2, c3);
	}
	/**
	 * Method for toggling to MODE_COLLECT_FACES
	 * Calls a native (JNI) function
	 * @author Teo Xanthopoulos
	 */
	protected void toggleAddPersonMode(){
		nativeToggleAdd();		
	}	
	/**
	 * Method for toggling to MODE_RECOGNITION
	 * Calls a native (JNI) function
	 * @author Teo Xanthopoulos
	 */
	protected void toggleRecMode(){
		nativeToggleRecognize();
	}
	/**
	 * Method for toggling to MODE_STARTUP
	 * Calls a native (JNI) function
	 * @author Teo Xanthopoulos
	 */
	protected void toggleStartMode(){
		nativeToggleStartUp();
	}
	/**
	 * Method for loading the trainedModel to native part.
	 * Calls a native (JNI) function
	 * @author Teo Xanthopoulos
	 * @return true if trainedModel loaded successfully, false in not 
	 */
	protected boolean loadModel(){
		boolean isLoaded = false;		
		isLoaded = nativeLoadModel();
		return isLoaded;	
	}	
	/**
	 * Method for loading the names.txt file to native part. 
	 * Calls a native (JNI) function
	 * @author Teo Xanthopoulos
	 * @return true if names.txt file loaded successfully, false in not 
	 */
	protected boolean loadNames(){
		boolean isLoaded = false;
		isLoaded = nativeLoadNames();
		return isLoaded;
	}
	/**
	 * Method for loading the labels.txt file to native part. 
	 * Calls a native (JNI) function
	 * @author Teo Xanthopoulos
	 * @return true if labels.txt file loaded successfully, false in not 
	 */
	protected boolean loadLabels(){
		boolean isLoaded = false;
		isLoaded = nativeLoadLabels();
		return isLoaded;
	}
	/**
	 * Method for sending person's name to native part.
	 * Calls a native (JNI) function
	 * @author Teo Xanthopoulos
	 * @param pName String with the person's name 
	 */
	protected void SendpName(String pName){
		nativeSendpName(pName);
	}	
	/**
	 * Method for checking the state of image collection and then do
	 * the proper initializations etc into the native part.
	 * Calls a native (JNI) function
	 * @author Teo Xanthopoulos
	 */
	protected void checkCollection(){
		nativeCheckCollection();
	}
	/**
	 * Method for starting the train process to native part.
	 * Calls a native (JNI) function
	 * @author Teo Xanthopoulos		
	 * @return true if training completed successfully, false if not 
	 */
	protected boolean BeginTrainning(){
		Log.i(TAG, "begin trainning ");
		return nativeBeginTrainning();
	}
	/**
	 * Method for redefine some values for proper functioning of the program to native part.
	 * Calls a native (JNI) function
	 * @author Teo Xanthopoulos
	 */
	protected void RedefineValues(){
		nativeRedefineValues();
	}
	/**
	 * Debugging method for checking values of some variables by displaying them to LogCat.
	 * Calls a native (JNI) function
	 * @author Teo Xanthopoulos
	 */
	protected void ShowValues(){
		nativeShowValues();
	}	
	/**
	 * Method for getting the number of already trained persons.
	 * Calls a native (JNI) function
	 * @author Teo Xanthopoulos
	 * @return Number of trained persons, or -1 if there aren't any trained persons into the system 
	 */
	protected int getNumOfTrainedFaces(){		
		int trained = nativeGetNumOfTrainedFaces();		
		if(trained != -1){
			return trained;
		}else{
			return -1;
		}			
	}
	/**
	 * Method for getting the number of new non-trained persons.
	 * Calls a native (JNI) function
	 * @author Teo Xanthopoulos
	 * @return Number of trained persons, or -1 if there aren't any persons into the system 
	 */
	protected int getNumOfNonTrainedFaces(){
		int nonTrained = nativeGetNumOfNonTrainedFaces();
		if(nonTrained != -1){
			return nonTrained;
		}else{
			return -1;
		}
	}
	//-------------------------------------------------------------------
	// Declare the function prototypes of the C/C++ code using NDK (JNI):
	//-------------------------------------------------------------------
    public native void ShowPreview(int width, int height, byte[] yuv, int[] rgba);   
    public native void initDetectors(String cas1, String cas2, String cas3);  
    public native boolean nativeLoadModel();
    public native boolean nativeLoadNames();
    public native boolean nativeLoadLabels();
    public native void nativeToggleAdd();
    public native void nativeToggleRecognize();
    public native void nativeToggleStartUp();
    public native void nativeSendpName(String s);
    public native void nativeCheckCollection();
    public native boolean nativeBeginTrainning(); 
    public native void nativeRedefineValues();
    public native int nativeGetNumOfTrainedFaces();
    public native int nativeGetNumOfNonTrainedFaces();    
    public native void nativeShowValues();  
    
    // Load (dynamically at runtime) the C/C++ code in "libdetection_based_tracker.so" using NDK (JNI).
    static {
    	Log.i(TAG, "Call: System.loadLibrary(detection_based_tracker)");
        System.loadLibrary("detection_based_tracker");
    }
}
