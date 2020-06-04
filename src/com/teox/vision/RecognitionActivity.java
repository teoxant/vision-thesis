package com.teox.vision;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Window;
/**
 * Activity for recognizing the person in front of the camera. Opens device's camera with a VisionView class object.
 * Also with this object calls methods from native part.
 * Initially it loads the 3 classifiers (lbpcascade_frontalface.xml, haarcascade_eye.xml, haarcascade_eye_tree_eyeglasses.xml)
 * and then loads all the necessary data (names, labels, trainedModel)
 * @author Teo Xanthopoulos
 */
public class RecognitionActivity extends Activity {
	/**
	 * Debug Tag for use logging debug output to LogCat
	 */
	private static final String		TAG = "Vision::RecognitionActivity";
	/**
	 * Object declaration of VisionView class
	 */
	private VisionView 				mView;
	/**
	 * Strings for containing classifiers paths
	 */
	private String 					faceCascadeDir, eyeCascadeDir, eyeGCascadeDir;
	/**
	 * Boolean variables for checking if trainedModel, names and labels loaded successfully
	 */
    public boolean					isModelLoaded, isNamesLoaded, isLabelsLoaded;
    /**
     * Alert Dialog for displaying error in loading necessary data
     */
    AlertDialog 					alertDialog;  
        
    public RecognitionActivity() {
    	Log.i(TAG, "Instantiated new " + this.getClass());
    }
    
    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        mView.releaseCamera();
    }
    
    @Override
	public void onBackPressed() {
		super.onBackPressed();
		mView.RedefineValues();
		mView.toggleStartMode();
	}

	@Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        if( !mView.openCamera() ) {
            AlertDialog ad = new AlertDialog.Builder(this).create();  
            ad.setCancelable(false); // This blocks the 'BACK' button  
            ad.setMessage(this.getResources().getString(R.string.errorCamera));  
            ad.setButton(this.getResources().getString(R.id.ok_button), new DialogInterface.OnClickListener() {  
                public void onClick(DialogInterface dialog, int which) {  
                    dialog.dismiss();                      
                    finish();
                }  
            });  
            ad.show();
        }
    }
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		try{
        	// load cascade file from application resources
    		InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
    		File cascadeFile = getDir("cascade", Context.MODE_PRIVATE);		
    		faceCascadeDir = cascadeFile.toString()+"/lbpcascade_frontalface.xml";
    		is.close();   		
    		
    		//load eye Cascade file from application resources
    		is = getResources().openRawResource(R.raw.haarcascade_eye);
    		File eyeCascadeFile = getDir("EyeCascade", Context.MODE_PRIVATE);
    		eyeCascadeDir = eyeCascadeFile.toString()+"/haarcascade_eye.xml";
    		is.close();
    		
    		//load eye_tree_eyeglasses Cascade file from application resources
    		is = getResources().openRawResource(R.raw.haarcascade_eye_tree_eyeglasses);
    		File eyeGCascadeFile = getDir("EyeGCascade", Context.MODE_PRIVATE);
    		eyeGCascadeDir = eyeGCascadeFile.toString()+"/haarcascade_eye_tree_eyeglasses.xml";
    		is.close();
        }catch(IOException e){
    		e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
    	} 
        
        mView = new VisionView(this); 
        // Open camera
        setContentView(mView);		        
        //Keep screen on
        mView.setKeepScreenOn(true);
        //Load the classifiers
        mView.initialize3Detectors(faceCascadeDir, eyeCascadeDir, eyeGCascadeDir); 
        // Load trainedModel
        isModelLoaded = mView.loadModel();
        // Load names
        isNamesLoaded = mView.loadNames(); 
        // Load labels
        isLabelsLoaded = mView.loadLabels();
        // Check if data loaded OK
        checkLoadData(isModelLoaded, isNamesLoaded, isLabelsLoaded); 
	}
    
    /**
     * This method checks if all the necessary data loaded successfully. It displays a dialog window if something didn't load OK
     * @author Teo Xanthopoulos
     * @param modelOk Boolean variable for checking trainedModel
     * @param namesOk Boolean variable for checking names
     * @param labelsOk Boolean variable for checking labels
     */
    public void checkLoadData(boolean modelOk, boolean namesOk, boolean labelsOk){
    	if((modelOk) && (namesOk) && (labelsOk)){
    		mView.toggleRecMode();
    	}else{
    		mView.toggleStartMode();
    		alertDialog = new AlertDialog.Builder(this).create();
    		alertDialog.setCancelable(false);
    		alertDialog.setTitle(this.getResources().getString(R.string.app_name));
    		String msg = this.getResources().getString(R.string.couldNotLoad);     		
    		if(!modelOk){
    			msg += this.getResources().getString(R.string.trainedModelFile);
    		}    		
    		if(!namesOk){
    			msg += this.getResources().getString(R.string.namesFile);
    		}    		
    		if(!labelsOk){
    			msg += this.getResources().getString(R.string.labelsFile);
    		}    		
    		msg += this.getResources().getString(R.string.trainSomeFacesFirst); 
    		alertDialog.setMessage(msg);
    		alertDialog.setButton(this.getResources().getString(R.string.return_button), new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();	
					finish();
				}
			});
        	alertDialog.show();    		
    	}    	       
    }     	
}
