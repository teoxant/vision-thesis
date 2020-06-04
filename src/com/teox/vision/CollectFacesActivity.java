package com.teox.vision;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
/**
 * Activity for collecting faces. Opens device's camera with a VisionView class object.
 * Also with this object calls methods from native part.
 * It loads the 3 classifiers (lbpcascade_frontalface.xml, haarcascade_eye.xml, haarcascade_eye_tree_eyeglasses.xml).
 * It has an option menu with 2 menu items: Add person, Begin Train.
 * @author Teo Xanthopoulos  
 */
public class CollectFacesActivity extends Activity implements android.view.View.OnClickListener {
	/**
	 * Debug Tag for use logging debug output to LogCat
	 */
	private static final String 		TAG = "Vision::CollectFacesActivity";
	/**
	 * Object declaration of VisionView class
	 */
	private VisionView 					mView;
	/**
	 * Strings for containing classifiers paths
	 */
	private String 						faceCascadeDir, eyeCascadeDir, eyeGCascadeDir; 
	/**
	 * Alert Dialog for adding person's name
	 */
	AlertDialog 						myDialog;
	/**
	 * Edit Text for person's name. It will be inside myDialog
	 */
    EditText 							pNameEText;
    /**
     * Declaration for menu items
     */
    private MenuItem 					mAddPerson, mFinishCollect;   
    /**
     * String for person's name. It will be sent to native part 
     */
    String 								pName = "";  
	    
	public CollectFacesActivity(){
		Log.i(TAG, "Instantiated new " + this.getClass());
	}
	
	@Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        mView.releaseCamera();
    }
	
	@Override
    protected void onResume() {
        Log.i(TAG, "onResume"); 
        super.onResume();
        if( !mView.openCamera() ) {
            AlertDialog ad = new AlertDialog.Builder(this).create();  
            ad.setCancelable(false); // This blocks the 'BACK' button  
            ad.setMessage(this.getResources().getString(R.string.errorCamera));  
            ad.setButton(this.getResources().getString(R.string.ok_button), new DialogInterface.OnClickListener() {  
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
        mView.RedefineValues();
        mView.checkCollection();    
    }
	
	/** Called when the menubar is being created by Android. */
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        mAddPerson = menu.add(this.getResources().getString(R.string.addPerson));
        mFinishCollect = menu.add(this.getResources().getString(R.string.beginTrain));        
        return true;
    }
    
    /** Called whenever the user pressed a menu item in the menubar. */
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "Menu Item selected: " + item);
		if (item == mAddPerson){
			popDialog();		      	
		}else if (item == mFinishCollect){
			mView = new VisionView(this);
			mView.RedefineValues();
			mView.toggleStartMode();
            Intent sT = new Intent(this, TrainActivity.class);
            startActivity(sT);        	
		}
		return true;
    }	
    
    @Override
	public void onBackPressed() {
		super.onBackPressed();
		mView.RedefineValues();
		mView.toggleStartMode();
	}
	
	/**
	 * Method for showing a Dialog Window for input person name
	 * @author Teo Xanthopoulos
	 */
	public void popDialog(){
		pName = "";
		mView.toggleStartMode();
		myDialog = new AlertDialog.Builder(this).create();
		myDialog.setTitle(this.getResources().getString(R.string.addPersonName));
		myDialog.setIcon(R.drawable.collect);
		LayoutInflater inflater = (LayoutInflater) CollectFacesActivity.this
			  .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			  View layout = inflater.inflate(R.layout.edt_text, null);
			  myDialog.setView(layout);
			  myDialog.show();
			  
		final Button okBtn = (Button) myDialog.findViewById(R.id.ok_button);
		Button cancelBtn = (Button) myDialog.findViewById(R.id.cancel_button);
		pNameEText = (EditText) myDialog.findViewById(R.id.editTextPersonName);
		
		okBtn.setOnClickListener(CollectFacesActivity.this);
		cancelBtn.setOnClickListener(CollectFacesActivity.this);
		okBtn.setEnabled(false);
		
		pNameEText.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			// Dynamically check the name typed from user
			public void onTextChanged(CharSequence s, int start, int before, int count) {
                s = pNameEText.getText();                
                if(TextUtils.isEmpty(s)){
    				okBtn.setEnabled(false);
    			}else if(hasOnlyLetters(s.toString()) && s.toString().length() < 20){
    				okBtn.setEnabled(true);
    			}else if(s.toString().length() > 20){
    				okBtn.setEnabled(false);
    			}else{
    				okBtn.setEnabled(false);
    			}                 
			}			
		});		
	}	
	
	/**
	 * Method for checking if string has only letters and spaces 
	 * @param s	String to be checked
	 * @return true if string is OK, false if it is not
	 * @author Teo Xanthopoulos
	 */
	public static boolean hasOnlyLetters(String s){
		if(TextUtils.isEmpty(s)){
			return false;
		}
		Pattern p = Pattern.compile("^[a-zA-Z ]*$");
		Matcher m = p.matcher(s);
		return m.matches();				
	}

	@Override
	public void onClick(View v) {		
		switch(v.getId()) {
		case R.id.ok_button:			
			pName = pNameEText.getText().toString(); 			
			mView.SendpName(pName);
			mView.checkCollection();
			mView.toggleAddPersonMode();
			myDialog.dismiss();
			break;
		case R.id.cancel_button:	
			pName = "";			
			mView.toggleStartMode(); 
			myDialog.dismiss();
			break;		
		}		
	}
}
