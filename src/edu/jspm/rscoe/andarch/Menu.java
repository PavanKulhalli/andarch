package edu.jspm.rscoe.andarch;

import java.util.Vector;

import edu.jspm.rscoe.andarch.R;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class Menu extends ListActivity {
	@Override
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Vector<Item> options = new Vector<Item>();
		Item item = new Item ();
		item.text = getResources().getString(R.string.menu);
		item.type = Item.TYPE_HEADER;
		options.add(item);
		
		item = new Item();
		item.text = getResources().getString(R.string.choose_local_file);
		item.icon = new Integer (R.drawable.icon);
		options.add(item);
		
		item = new Item();
		item.text = getResources().getString(R.string.convert_model);
		item.icon = new Integer(R.drawable.model);
		options.add(item);
		
		item = new Item();
		item.text = getResources().getString(R.string.instructions);
		item.icon = new Integer(R.drawable.help);
		options.add(item);
		
		item = new Item();
		item.text = getResources().getString(R.string.quit);
		item.icon = new Integer(R.drawable.quit);
		options.add(item);
		
		setListAdapter (new MenuChooserListAdapter (options));
	}

	@Override
	protected void onListItemClick (ListView l, View v, int position, long id) {
		Item item = (Item)getListAdapter().getItem(position);
		String str = item.text;
		if (str.equals(getResources().getString(R.string.choose_local_file))) {
			//choose from models in res folder
			startActivity(new Intent(Menu.this, ModelChooser.class));
		} else if (str.equals(getResources().getString(R.string.convert_model))) {
			//convert an external file to wavefront obj
			startActivity(new Intent(Menu.this, ConvertModel.class));
		} else if(str.equals(getResources().getString(R.string.instructions))) {
			//show the instructions activity
			startActivity(new Intent(Menu.this, InstructionsActivity.class));
		} else if(str.equals(getResources().getString(R.string.quit))) {
			//exit application
			this.finish();
		}
	}

	class MenuChooserListAdapter extends BaseAdapter {
		Vector<Item> options;
		
		MenuChooserListAdapter (Vector<Item> options){
			this.options = options;
		}
		@Override
		public int getCount() {
			return options.size();
		}
	
		@Override
		public Object getItem(int position) {
			return options.get(position);
		}
	
		@Override
		public long getItemId(int position) {
			return position;
		}
	
		@Override
		public int getViewTypeCount() {
			//normal items, and the header
			return 2;
		}
		
		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}
		
		@Override
		public boolean isEnabled(int position) {
			return !(options.get(position).type==Item.TYPE_HEADER);
		}
		
		@Override
		public int getItemViewType(int position) {
			return options.get(position).type;
		}
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			Item item = options.get(position);
	        if (v == null) {
	        	LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        	switch(item.type) {            	
	        	case Item.TYPE_HEADER:            		
	                v = vi.inflate(R.layout.list_header, null);
	        		break;
	        	case Item.TYPE_ITEM:
	        		v = vi.inflate(R.layout.menu, null);
	        		break;
	        	}                
	        }   
	        if(item != null) {
	            switch(item.type) {            	
	        	case Item.TYPE_HEADER: 
	        		TextView headerText = (TextView) v.findViewById(R.id.list_header_title);
	        		if(headerText != null) {
	        			headerText.setText(item.text);
	        		}
	        		break;
	        	case Item.TYPE_ITEM:
	        		Object iconImage = item.icon;
	            	ImageView icon = (ImageView) v.findViewById(R.id.choose_model_row_icon);
	            	if(icon!=null) {
	            		if(iconImage instanceof Integer) {
	            			icon.setImageResource(((Integer)iconImage).intValue());
	            		} else if(iconImage instanceof Bitmap) {
	            			icon.setImageBitmap((Bitmap)iconImage);
	            		}
	            	}
	            	TextView text = (TextView) v.findViewById(R.id.choose_model_row_text);
	            	if(text!=null)
	            		text.setText(item.text);   
	        		break;
	        	}      
	        }
			return v;
		}
	}
}
class Item {
	public static final int TYPE_ITEM=0;
	public static final int TYPE_HEADER=1;
	public int type = TYPE_ITEM;
	public Object icon = new Integer(R.drawable.missingimage);
	public String text;
}