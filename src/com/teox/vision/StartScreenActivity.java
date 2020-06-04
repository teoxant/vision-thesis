package com.teox.vision;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.View.OnClickListener;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.TabSpec;
/**
 * Activity for the main menu. It has 4 buttons for the basic options: collect faces, train, recognize and help. Also it has 2
 * image buttons for about and exit
 * @author Teo Xanthopoulos
 */
public class StartScreenActivity extends Activity implements OnClickListener {
	/**
	 * Debug Tag for use logging debug output to LogCat
	 */
	private static final String 		TAG = "Vision::StartScreenActivity";
	/**
	 * Alert Dialogs for about, exit, help 
	 */
	AlertDialog 						myDialog, exitDialog, helpDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start_screen);
		
		View recBtn = findViewById(R.id.recognize_button);
		recBtn.setOnClickListener(this); 
		
		View collectBtn = findViewById(R.id.collect_button);
		collectBtn.setOnClickListener(this);
		
		View trainBtn = findViewById(R.id.train_button);
		trainBtn.setOnClickListener(this);
		
		View aboutBtn = findViewById(R.id.about_button);
		aboutBtn.setOnClickListener(this);
		
		View helpBtn = findViewById(R.id.help_button);
		helpBtn.setOnClickListener(this);
		
		View exitBtn = findViewById(R.id.exit_button);
		exitBtn.setOnClickListener(this); 
	}
	
	public void onClick(View v){ 		
		switch(v.getId())
		{
		case R.id.recognize_button:
			Intent sR = new Intent(this, RecognitionActivity.class);
			startActivity(sR);
			break;
		case R.id.collect_button:
			Intent sC = new Intent(this, CollectFacesActivity.class);
			startActivity(sC);
			break;
		case R.id.train_button:
			Intent sT = new Intent(this, TrainActivity.class);
			startActivity(sT);
			break;		
		case R.id.help_button: 
			showHelpBox();
			break;
		case R.id.exit_button: 
			showExitBox();
			break;
		case R.id.about_button: 
			showAboutBox(); 
			break;		 			
		}		
	}
	/**
	 * Method for getting Application's version.
	 * @author Teo Xanthopoulos
	 * @throws NameNotFoundException
	 * @return Application's version as string, or "-" if version not found
	 */
	public String getVersion(){
		try {
			return this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return "-";
		}
	}
	/**
	 * Method for displaying an about box. Using custom layout "about.xml"
	 * @author Teo Xanthopoulos
	 */
	public void showAboutBox(){
		String version = getVersion();
		myDialog = new AlertDialog.Builder(this).create();
		myDialog.setTitle(this.getResources().getString(R.string.aboutVision));
		myDialog.setIcon(R.drawable.info);
		LayoutInflater inflater = (LayoutInflater) StartScreenActivity.this
				  .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.about, null);
		myDialog.setView(layout);
		TextView tv = (TextView) layout.findViewById(R.id.vNumberTxtView);
		tv.setText(version);
		myDialog.show();			
	}
	/**
	 * Method for displaying help dialog. Using custom layout "help.xml". Also contains a TabHost
	 * @author Teo Xanthopoulos
	 */
	public void showHelpBox(){
		helpDialog = new AlertDialog.Builder(this).create();
		helpDialog.setTitle(this.getResources().getString(R.string.help));
		helpDialog.setIcon(R.drawable.help);
		LayoutInflater inflater = (LayoutInflater) StartScreenActivity.this
				  .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.help, null);
		helpDialog.setView(layout);
		
		TabHost tabHost = (TabHost) layout.findViewById(android.R.id.tabhost);
		tabHost.setup();

        TabSpec spec = tabHost.newTabSpec("Tab1");
        spec.setIndicator(this.getResources().getString(R.string.howToUse), null);
        spec.setContent(R.id.tab1);
        tabHost.addTab(spec);
  
        spec = tabHost.newTabSpec("Tab2");
        spec.setIndicator(this.getResources().getString(R.string.helpTips), null);
        spec.setContent(R.id.tab2);
        tabHost.addTab(spec);
        
        // making tabs smaller, no icon only text
        int iCnt = tabHost.getTabWidget().getChildCount();
        for(int i=0; i<iCnt; i++){
        	tabHost.getTabWidget().getChildAt(i).getLayoutParams().height /= 2;
        }
  
		helpDialog.show();
	}
	/**
	 * Method for displaying a verifying dialog for exiting.
	 * @author Teo Xanthopoulos
	 */
	public void showExitBox(){ 
		exitDialog = new AlertDialog.Builder(this).create();
		exitDialog.setTitle(this.getResources().getString(R.string.app_name));
		exitDialog.setIcon(R.drawable.exit);
		exitDialog.setCancelable(false);
		exitDialog.setMessage(this.getResources().getString(R.string.sure2Exit));
		exitDialog.setButton(DialogInterface.BUTTON_POSITIVE, this.getResources().getString(R.string.ok_button),new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {				
				exitDialog.dismiss();                      
                finish();
			}			
		});
		exitDialog.setButton(DialogInterface.BUTTON_NEGATIVE, this.getResources().getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				exitDialog.dismiss();
			}			
		});		
		exitDialog.show(); 
	}
}
	


