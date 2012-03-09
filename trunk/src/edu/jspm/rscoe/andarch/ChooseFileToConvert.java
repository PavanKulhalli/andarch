package edu.jspm.rscoe.andarch;

import java.io.File;
import java.net.URI;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class ChooseFileToConvert extends CheckFileManagerActivity {
	private final int PICK_FILE = 1;
	private final int VIEW_MODEL = 2;
	public static final int RESULT_ERROR = 3;

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    	//super.onActivityResult(requestCode, resultCode, data);
	    	switch (requestCode) {
	    		default:
		    	case PICK_FILE:
			    	switch(resultCode) {
				    	case Activity.RESULT_OK:
				    		//does file exist??
				    		File file =  new File(URI.create(data.getDataString()));
				    		if (!file.exists()) {
				    			//notify user that this file doesn't exist
				    			Toast.makeText(this, res.getText(R.string.file_doesnt_exist), TOAST_TIMEOUT).show();
				    			selectFile();
				    		} else {
				    			String fileName = data.getDataString();
				    			if(!fileName.endsWith(".dwg")) {
				    				Toast.makeText(this, res.getText(R.string.wrong_file), TOAST_TIMEOUT).show();
				    				selectFile();
				    			} else {
				    				Toast.makeText(getApplicationContext(), getResources().getString(R.string.browse_for_a_model), 3);
				    				//TODO: Add logic to convert the DWG file
				    				Intent intent = new Intent (ChooseFileToConvert.this, Menu.class);
				    				startActivity(intent);
				    			}
				    		}
				    		break;
				    	default:
				    	case Activity.RESULT_CANCELED:
				    		//back to the main activity
				    		Intent intent = new Intent(ChooseFileToConvert.this, ModelChooser.class);
				            startActivity(intent);
				    		break;
			    	}
			    	break;
		    	case VIEW_MODEL:
		    		switch(resultCode) {
				    	case Activity.RESULT_OK:
				    		//model viewer returned...let the user view a new file
				    		selectFile();
				    		break;
				    	case Activity.RESULT_CANCELED:
				    		selectFile();
				    		break;
				    	case RESULT_ERROR:
				    		//something went wrong ... notify the user
				    		if(data != null) {
					    		Bundle extras = data.getExtras();
					    		String errorMessage = extras.getString("error_message");
					    		if(errorMessage != null)
					    			Toast.makeText(this, extras.getString("error_message"), TOAST_TIMEOUT).show();
				    		}
				    		selectFile();
				    		break;	
		    		}
	    	}
	    }
}
