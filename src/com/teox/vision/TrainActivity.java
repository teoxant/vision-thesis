package com.teox.vision;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
/**
 * Activity for training the system. It has a List View for showing info about 
 * the number of trained persons, number of new "non trained" persons and the last person added.
 * Also it has 3 options: Train the system, show saved people, delete all data.
 * It uses a VisionView object for calling methods from native part.
 * @author Teo Xanthopoulos
 */
public class TrainActivity extends Activity implements OnClickListener {
	/**
	 * Debug Tag for use logging debug output to LogCat
	 */
	private static final String 		TAG = "Vision::TrainActivity";
	/**
	 * Object declaration of VisionView class
	 */
	private VisionView					mView;
	/**
	 * Boolean variable for checking if train completed successfully
	 */
	public boolean 						isOk = false;
	/**
	 * List View for showing info about the number of trained persons, number of new "non trained" persons
	 * and the last person added
	 */
	ListView							myListV;
	/**
	 * List with data that are going to display in the ListView
	 */
	ArrayList<String>					myArrayL = new ArrayList<String>();
	/**
	 * Adapter for linking ArraList with ListView
	 */
	ArrayAdapter<String>				myAdapter;
	/**
	 * Integers for number of trained persons, number of non trained persons,
	 * number of new persons (the removal of trainedNum and NonTrainedNum)
	 */
	int 								trainedNum, NonTrainedNum, newFacesNum;
	/**
	 * 3 buttons for the 3 options: Train, Show People, Delete All
	 */
	View 								trainBtn, showSavedBtn, deleteAllBtn; 
	/**
	 * Alert Dialog for displaying the saved people
	 */
	AlertDialog							savedPplDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_train);	
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		trainBtn = findViewById(R.id.start_train_button);
		trainBtn.setOnClickListener(TrainActivity.this);
		trainBtn.setEnabled(false); 
		
		showSavedBtn = findViewById(R.id.show_ppl_button);
		showSavedBtn.setOnClickListener(TrainActivity.this);
		showSavedBtn.setEnabled(false);		
		
		deleteAllBtn = findViewById(R.id.delete_all_button);
		deleteAllBtn.setOnClickListener(this);		
	
		countFaces();	
		UnlockShowSavedPplBtn();
	
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		Intent sS = new Intent(this, StartScreenActivity.class);
		sS.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(sS);
	}
	/**
	 * Method for unlocking the Show Saved People button. Initially the button is locked. This method
	 * checks if the names file exists and either unlocks the button or locks it
	 * @author Teo Xanthopoulos
	 */
	public void UnlockShowSavedPplBtn(){
		File names = new File("/data/data/com.teox.vision/names.txt");
		if(names.exists()){
			Log.i(TAG, "names file exists");
			showSavedBtn.setEnabled(true);
		}else{
			Log.i(TAG, "names file doesnt exists");
			showSavedBtn.setEnabled(false);
		}	
	}
	/**
	 * Method for displaying the Alert Dialog with the saved persons.
	 * @author Teo Xanthopoulos
	 */
	public void ShowSavedPpl(){
		savedPplDialog = new AlertDialog.Builder(this).create();
		savedPplDialog.setTitle(this.getResources().getString(R.string.savedPeople));
		savedPplDialog.setIcon(R.drawable.display);
		LayoutInflater inflater = (LayoutInflater) TrainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.saved_people, null);		
		savedPplDialog.setView(layout);	
		TextView tv = (TextView) layout.findViewById(R.id.TxtView);
		tv.setMovementMethod(new ScrollingMovementMethod());
		tv.setText(getSavedPeople());
		savedPplDialog.show();		
	}
	/**
	 * This method gets the names of the persons from names.txt file. 
	 * @author Teo Xanthopoulos
	 * @throws IOException
	 * @return A StringBuilder with the names
	 */
	public StringBuilder getSavedPeople(){
		StringBuilder builder = new StringBuilder();
		try{
			// Open the file
			FileInputStream fstream = new FileInputStream("/data/data/com.teox.vision/names.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String strLine = "", prevLine = "";
			
			//Read File Line By Line
			while ((strLine = br.readLine()) != null){	
				if(!prevLine.equals(strLine)){
					prevLine = strLine;
					builder.append(strLine).append("\n");
				}			 
			}
			fstream.close();
			br.close();
		}catch(IOException e){
			Log.i(TAG, "names file can't opened");
			return null;
		}	
		return builder;		
	}
	/**
	 * This method gets the number of trained faces and non trained faces
	 * and calculates the result that will be displayed in the ListView.
	 * Also it locks/unlocks the train button depending on conditions.
	 * e.g. If there is no data for training, then the train button is locked. 
	 * @author Teo Xanthopoulos
	 */
	public void countFaces(){
		boolean unlockBtn = false;
		mView = new VisionView(this);
		trainedNum = mView.getNumOfTrainedFaces();
		NonTrainedNum = mView.getNumOfNonTrainedFaces();
		Log.i(TAG,"trained: "+Integer.toString(trainedNum));
		Log.i(TAG, "non trained; "+Integer.toString(NonTrainedNum));		
	
		if(trainedNum == -1 && NonTrainedNum == -1){
			Log.i(TAG, "1st case");
			trainedNum = 0;
			newFacesNum = 0;
			unlockBtn = false;
		}else if(trainedNum == -1 && NonTrainedNum > 0){
			Log.i(TAG, "2nd case");
			trainedNum = 0;
			newFacesNum = NonTrainedNum;
			unlockBtn = true;
		}else if(trainedNum < NonTrainedNum){
			Log.i(TAG, "3rd case");
			newFacesNum = NonTrainedNum - trainedNum;
			unlockBtn = true;
		}else if(trainedNum == NonTrainedNum){
			Log.i(TAG, "4th case"); 
			newFacesNum = 0;
			unlockBtn = false;
		}
		
		
		myAdapter = new ArrayAdapter<String>(this, R.layout.simple_textview, myArrayL);	
		myAdapter.clear();
		myListV = (ListView) findViewById(R.id.myListView);			
		myArrayL.add(this.getResources().getString(R.string.numOfTrained)+Integer.toString(trainedNum));
		myArrayL.add(this.getResources().getString(R.string.numOfNonTrained)+Integer.toString(newFacesNum));		
		myArrayL.add(this.getResources().getString(R.string.lastPersonAdded)+getLastSavedPerson()); 
		myListV.setAdapter(myAdapter);
		trainBtn.setEnabled(false);
		if(unlockBtn){
			trainBtn.setEnabled(true);
		}
		
	}
	/**
	 * This function gets the last name from the names.txt file
	 * @author Teo Xanthopoulos
	 * @throws IOException
	 * @return A String with the last name or "-" if the file doesn't exists/can't be opened
	 */
	public String getLastSavedPerson(){
		File names = new File("/data/data/com.teox.vision/names.txt");
		String lastName="";
		if(names.exists()){
			Log.i(TAG, "names file exists");
			try{
				InputStreamReader streamReader = new InputStreamReader(new FileInputStream(names));
				BufferedReader br = new BufferedReader(streamReader);
				while(br.ready()){
					lastName = br.readLine();
				}
				br.close();
				streamReader.close();
				return lastName;
			}catch(IOException e){
				Log.i(TAG,"cant open names file");
				return "-";
			}			
		}else{
			Log.i(TAG, "names file doesnt exists");
			return "-";
		}
	}
	/**
	 * This method deletes all the data. More specifically deletes: names.txt, labels.txt, trainedModel.xml and empties the image folder.
	 * It displays a verifying dialog for delete. Also it shows a Toast for successfully deleting or not.
	 * It notifies the ListView with the data info by calling countFaces() function.
	 */
	public void deleteAll(){		
		final AlertDialog ad = new AlertDialog.Builder(this).create();
		ad.setCancelable(false);
		ad.setTitle(this.getResources().getString(R.string.app_name));
		ad.setMessage(this.getResources().getString(R.string.sure2Delete));
		ad.setButton(DialogInterface.BUTTON_POSITIVE, this.getResources().getString(R.string.ok_button), new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int arg1) {				
				boolean delImg = false;
				boolean delLabels = true;
				boolean delNames = true;
				boolean delModel = true;
				File labels = new File("/data/data/com.teox.vision/labels.txt");
				File names = new File("/data/data/com.teox.vision/names.txt");
				File model = new File("/data/data/com.teox.vision/trainedModel.xml");
				String imgPath = "/data/data/com.teox.vision/images/";				
				
				try{
					if(labels.exists()){ delLabels = labels.delete(); }
					if(names.exists()){	delNames = names.delete(); }
					if(model.exists()){	delModel = model.delete(); }
					delImg = deleteImages(imgPath);	
					Log.i(TAG, "delete labels"+String.valueOf(delLabels));
					Log.i(TAG, "delete Names:"+String.valueOf(delNames));
					Log.i(TAG, "delete Model:"+String.valueOf(delModel));
					Log.i(TAG, "delete images:"+String.valueOf(delImg));
				}catch(Exception e){
					throw new Error("Error deleting files");			
				}				
				countFaces();	
				UnlockShowSavedPplBtn();
				ad.dismiss();	
				if(!delImg || !delLabels || !delNames || !delModel){
					Toast.makeText(getApplicationContext(), TrainActivity.this.getResources().getString(R.string.errorDeleting), Toast.LENGTH_SHORT).show();
				}else{
					Toast.makeText(getApplicationContext(), TrainActivity.this.getResources().getString(R.string.succesDeleting), Toast.LENGTH_SHORT).show();
				}
			}			
		});		
		ad.setButton(DialogInterface.BUTTON_NEGATIVE, this.getResources().getString(R.string.cancel_button), new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Log.i(TAG,"button cancel");	
				dialog.dismiss();			
			}			
		});	
		ad.show();
	}
	/**
	 * This function empties the image folder. It deletes all of the images in the image folder
	 * @author Teo Xanthopoulos
	 * @param path The path to the image folder
	 * @return true if deleted successfully, false if not
	 */
	boolean deleteImages(String path){
		boolean delOk = false;
		File fileList = new File(path);		
		if(fileList != null){
			File[] fileNames = fileList.listFiles();
			for(File tmpf : fileNames){
				delOk = tmpf.delete();
			}	
			return delOk;
		}
		return delOk;
	}
	
	public void onClick(View v) {		
		switch(v.getId())
		{	
		case R.id.start_train_button:	
			final ProgressDialog pd = ProgressDialog.show(TrainActivity.this, this.getResources().getString(R.string.app_name), this.getResources().getString(R.string.processing));
			mView = new VisionView(TrainActivity.this);
			// AsyncTask for training the system and displaying a Progress Dialog
			new AsyncTask<Void, Void, Boolean>(){				
				@Override
				protected Boolean doInBackground(Void... params) {
					isOk = mView.BeginTrainning(); 								
					return isOk;
				}				
				@Override
				protected void onPostExecute(Boolean result)
		        {					
					Log.i(TAG, String.valueOf(result));
					countFaces();
		            pd.dismiss();
		            
		            AlertDialog ad = new AlertDialog.Builder(TrainActivity.this).create();  
		            ad.setCancelable(false); // This blocks the 'BACK' button 
		            ad.setTitle(TrainActivity.this.getResources().getString(R.string.app_name));		            		
		            if(result){
		            	ad.setMessage(TrainActivity.this.getResources().getString(R.string.successTraining));
		            	ad.setIcon(R.drawable.ok);
		            }else{
		            	ad.setMessage(TrainActivity.this.getResources().getString(R.string.errorTraining));
		            	ad.setIcon(R.drawable.delete);
		            }		              
		            ad.setButton(TrainActivity.this.getResources().getString(R.string.ok_button), new DialogInterface.OnClickListener() {  
		                public void onClick(DialogInterface dialog, int which) {  
		                    dialog.dismiss();  
		                }  
		            });  
		            ad.show();
		        }
			}.execute();	    		
			break;
		case R.id.show_ppl_button:
			ShowSavedPpl();
			break;
		case R.id.delete_all_button:	
			deleteAll();	
			break;
		}		
	}		
}
