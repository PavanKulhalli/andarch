package edu.jspm.rscoe.andarch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Date;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.util.FloatMath;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;


import edu.dhbw.andar.ARToolkit;
import edu.dhbw.andar.AndARActivity;
import edu.dhbw.andar.exceptions.AndARException;
import edu.dhbw.andar.interfaces.OpenGLRenderer;
import edu.jspm.rscoe.andarch.R;
import edu.jspm.rscoe.andarch.graphics.LightingRenderer;
import edu.jspm.rscoe.andarch.graphics.Model3D;
import edu.jspm.rscoe.andarch.models.Model;
import edu.jspm.rscoe.andarch.parser.ObjParser;
import edu.jspm.rscoe.andarch.parser.ParseException;
import edu.jspm.rscoe.andarch.parser.Util;
import edu.jspm.rscoe.andarch.util.AssetsFileUtil;
import edu.jspm.rscoe.andarch.util.BaseFileUtil;
import edu.jspm.rscoe.andarch.util.SDCardFileUtil;

/**
 * Example of an application that makes use of the AndAR toolkit.
 * @author Tobi
 *
 */
public class AugmentedModelViewerActivity extends AndARActivity implements SurfaceHolder.Callback {
	
	/**
	 * View a file in the assets folder
	 */
	public static final int TYPE_INTERNAL = 0;
	/**
	 * View a file on the sd card.
	 */
	public static final int TYPE_EXTERNAL = 1;
	
	public static final boolean DEBUG = false;
	/* Menu Options: */
	private final int MENU_SCALE = 0;
	private final int MENU_ROTATE_X = 1;
	private final int MENU_ROTATE_Y = 2;
	private final int MENU_ROTATE_Z = 3;
	private final int MENU_TRANSLATE = 4;
	private final int MENU_SCREENSHOT = 5;
	private final int MENU_STOP_OPER = 6;
	
	// touch events
	private final int DRAG = 0;
	private final int NONE = 1;
	private final int ZOOM = 2;
	
	// rotation
	private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
	
	// pinch to zoom
	private float oldDist = 100.0f;
	private float newDist;
	
	private int menuMode = MENU_STOP_OPER;
	private int screenMode = 0;
	private Model model;
	private Model3D model3d;
	private ProgressDialog waitDialog;
	private Resources res;
	ARToolkit artoolkit;
	
	public AugmentedModelViewerActivity() {
		super(false);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.setNonARRenderer(new LightingRenderer());//or might be omited
		super.setMarkerVisibility(true);
		res=getResources();
		artoolkit = getArtoolkit();		
		getSurfaceView().setOnTouchListener(new TouchEventHandler());
		getSurfaceView().getHolder().addCallback(this);
				
	}
	
	

	/**
	 * Inform the user about exceptions that occurred in background threads.
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		System.out.println("");
	}
	

    /* create the menu
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflator = getMenuInflater();
    	inflator.inflate(R.menu.transformation_menu, menu);
             return true;
    }
    
    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
	        case R.id.scale:
	        case MENU_SCALE:
	            menuMode = MENU_SCALE;
	            return true;
	        case R.id.rotate_x:
	        case MENU_ROTATE_X:
		       	 menuMode  = MENU_ROTATE_X;
	        	 return true;
	        case R.id.rotate_y:
	        case MENU_ROTATE_Y:
	        	 menuMode  = MENU_ROTATE_Y;
	        	 return true;
	        case R.id.rotate_z:
	        case MENU_ROTATE_Z:
	        	 menuMode  = MENU_ROTATE_Z;	
	            return true;
	        case R.id.translate:
	        case MENU_TRANSLATE:
	        	 menuMode  = MENU_TRANSLATE;
	            return true;
	        case R.id.stop_operation:
	        case MENU_STOP_OPER:
	        	 menuMode = MENU_STOP_OPER;
	        	return true;
	        case R.id.screenshot:
	        case MENU_SCREENSHOT:
	        	new TakeAsyncScreenshot().execute();
	        	return true;
	        case R.id.mode:
	        	if (Config.appMode == application_mode.pure_ar) {
	        		super.setMarkerVisibility(false);
	        		Config.appMode = application_mode.hybrid;
	        		Toast.makeText(AugmentedModelViewerActivity.this, getResources().getString(R.string.hybrid_mode_toast), Toast.LENGTH_SHORT).show();
	        	} else if (Config.appMode == application_mode.hybrid) {
		        		super.setMarkerVisibility(false);
		        		Config.appMode = application_mode.pure_vr;
		        		//TODO Write code to handle virtual mode
		        		Toast.makeText(AugmentedModelViewerActivity.this, getResources().getString(R.string.virtual_mode_toast), Toast.LENGTH_SHORT).show();	        		
		        	} else if (Config.appMode == application_mode.pure_vr) {
			        		super.setMarkerVisibility(true);
			        		Config.appMode = application_mode.pure_ar;
			        		//TODO Write code to handle augmented mode
			        		Toast.makeText(AugmentedModelViewerActivity.this, getResources().getString(R.string.augmented_mode_toast), Toast.LENGTH_SHORT).show();	        		
			        	}	        	
	        	return true;
        }
        return false;
    }
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    	super.surfaceCreated(holder);
    	//load the model
    	//this is done here, to assure the surface was already created, so that the preview can be started
    	//after loading the model
    	if(model == null) {
			waitDialog = ProgressDialog.show(this, "", 
	                getResources().getText(R.string.loading), true);
			waitDialog.show();
			new ModelLoader().execute();
		}
    }
    
	
    /**
     * Handles touch events.
     * @author Tobias Domhan
     *
     */
    class TouchEventHandler implements OnTouchListener {
    	
    	private float lastX=0;
    	private float lastY=0;

		/* handles the touch events.
		 * the object will either be scaled, translated or rotated, dependen on the
		 * current user selected mode.
		 * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
		 */
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			float mAngleX=0;
			float mAngleY=0;
			float x = event.getX();
			float y = event.getY();
			if(model!=null) {
				switch(event.getAction()) {
					//Action started
					default: break;
					case MotionEvent.ACTION_DOWN:
						screenMode = DRAG;
						lastX = event.getX();
						lastY = event.getY();
						break;
					case MotionEvent.ACTION_POINTER_DOWN:	// two touches: zoom
						oldDist = spacing(event);
						lastX = event.getX();
						lastY = event.getY();

						if (oldDist > 10.0f) {
							screenMode = ZOOM; // zoom
						}
						break;	
					case MotionEvent.ACTION_UP:		// no mode
						screenMode = NONE;
						oldDist = 100.0f;
						lastX = event.getX();
						lastY = event.getY();
						break;	
					case MotionEvent.ACTION_POINTER_UP:		// no mode
						screenMode = NONE;
						oldDist = 100.0f;
						lastX = event.getX();
						lastY = event.getY();

						break;	
					//Action ongoing
					case MotionEvent.ACTION_MOVE:
						float dx = x - lastX;
						float dy = y - lastY;
						if (menuMode == MENU_STOP_OPER) {
							if (event.getPointerCount() > 1 && screenMode == ZOOM) {
								newDist = spacing(event);
								if (newDist > 1.0f) {
									float scale = oldDist/newDist; // scale
									// scale in the renderer
									model.setScale(scale);
									oldDist = newDist;
								}
							}
							else if (screenMode == DRAG){
								mAngleX -= dy * TOUCH_SCALE_FACTOR;
								model.setXrot(mAngleX);
								mAngleY -= dx * TOUCH_SCALE_FACTOR;
								model.setYrot(mAngleY);
							}
						} else {
							switch( menuMode ) {
								case MENU_SCALE:
									model.setScale(-dy/50.0f);
						            break;
						        case MENU_ROTATE_X:
						        	model.setXrot(-1*dx);//dY-> Rotation um die X-Achse
						        	break;
						        case MENU_ROTATE_Y:
						        	model.setYrot(-1*dy);//dX-> Rotation um die Y-Achse
						        	break;
						        case MENU_ROTATE_Z:  
						        	model.setZrot(-1*dx);//dY-> Rotation um die Z-Achse
									break;
						        case MENU_TRANSLATE:
						        	model.setXpos(-dx/10f);
									model.setYpos(dy/10f);
						        	break;
							}
						}
						break;
					}
					lastX = x;
					lastY = y;
			}
			return true;
		}
		// finds spacing
		private float spacing(MotionEvent event) {
			float x = event.getX(0) - event.getX(1);
			float y = event.getY(0) - event.getY(1);
			return FloatMath.sqrt(x * x + y * y);
		}
    	
    }
    
	private class ModelLoader extends AsyncTask<Void, Void, Void> {
		
		
    	@Override
    	protected Void doInBackground(Void... params) {
    		
			Intent intent = getIntent();
			Bundle data = intent.getExtras();
			int type = data.getInt("type");
			String modelFileName = data.getString("name");
			BaseFileUtil fileUtil= null;
			File modelFile=null;
			switch(type) {
			case TYPE_EXTERNAL:
				fileUtil = new SDCardFileUtil();
				modelFile =  new File(URI.create(modelFileName));
				modelFileName = modelFile.getName();
				fileUtil.setBaseFolder(modelFile.getParentFile().getAbsolutePath());
				break;
			case TYPE_INTERNAL:
				fileUtil = new AssetsFileUtil(getResources().getAssets());
				fileUtil.setBaseFolder("models/");
				break;
			}
			
			//read the model file:						
			if(modelFileName.endsWith(".obj")) {
				ObjParser parser = new ObjParser(fileUtil);
				try {
					if(Config.DEBUG)
						Debug.startMethodTracing("AndObjViewer");
					if(type == TYPE_EXTERNAL) {
						//an external file might be trimmed
						BufferedReader modelFileReader = new BufferedReader(new FileReader(modelFile));
						String shebang = modelFileReader.readLine();				
						if(!shebang.equals("#trimmed")) {
							//trim the file:			
							File trimmedFile = new File(modelFile.getAbsolutePath()+".tmp");
							BufferedWriter trimmedFileWriter = new BufferedWriter(new FileWriter(trimmedFile));
							Util.trim(modelFileReader, trimmedFileWriter);
							if(modelFile.delete()) {
								trimmedFile.renameTo(modelFile);
							}					
						}
					}
					if(fileUtil != null) {
						BufferedReader fileReader = fileUtil.getReaderFromName(modelFileName);
						if(fileReader != null) {
							model = parser.parse("Model", fileReader);
							model3d = new Model3D(model);
						}
					}
					if(Config.DEBUG)
						Debug.stopMethodTracing();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
    		return null;
    	}
    	@Override
    	protected void onPostExecute(Void result) {
    		super.onPostExecute(result);
    		waitDialog.dismiss();
    		
    		//register model
    		try {
    			if(model3d!=null){
    				artoolkit.registerARObject(model3d);
    			}	
    					
			} catch (AndARException e) {
				e.printStackTrace();
			}
			startPreview();
    	}
    }
	
	class TakeAsyncScreenshot extends AsyncTask<Void, Void, Void> {
		
		private String errorMsg = null;

		@Override
		protected Void doInBackground(Void... params) {
			Bitmap bm = takeScreenshot();
			FileOutputStream fos;
			try {
				fos = new FileOutputStream("/sdcard/AndARScreenshot"+new Date().getTime()+".png");
				bm.compress(CompressFormat.PNG, 100, fos);
				fos.flush();
				fos.close();					
			} catch (FileNotFoundException e) {
				errorMsg = e.getMessage();
				e.printStackTrace();
			} catch (IOException e) {
				errorMsg = e.getMessage();
				e.printStackTrace();
			}	
			return null;
		}
		
		
		
		protected void onPostExecute(Void result) {
			
			if(errorMsg == null) {
				Toast.makeText(AugmentedModelViewerActivity.this, getResources().getText(R.string.screenshotsaved), Toast.LENGTH_SHORT ).show();
			} else {
				Toast.makeText(AugmentedModelViewerActivity.this, getResources().getText(R.string.screenshotfailed)+errorMsg, Toast.LENGTH_SHORT ).show();
			}
		};
		
	}
	
	
}
