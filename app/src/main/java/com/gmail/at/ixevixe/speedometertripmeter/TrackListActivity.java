package com.gmail.at.ixevixe.speedometertripmeter;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class TrackListActivity extends ListActivity {
	private SpeedometerContext g;
	private boolean filelistmode;
	private boolean recordingactive;
	
	private String tripfilename;
	
	Handler displayupdatehandler = new Handler(Looper.getMainLooper());
   	private Runnable displayupdate = new Runnable() { @Override public void run() {
   		if( !recordingactive || g.service == null ) return; 
   		
   		if( getListView().getFirstVisiblePosition() < 2){
   			g.service.writestats();
   			((TrackViewAdapter2) getListAdapter()).settripstats(TripStats.load(TrackListActivity.this, g.service.tripstats.filename));
   			((TrackViewAdapter2) getListAdapter()).notifyDataSetChanged();
   		}
        
   		displayupdatehandler.postDelayed(this, 1000);
   	}};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		g = (SpeedometerContext)getApplication();

		Intent intent = getIntent();
		tripfilename = intent.getStringExtra("tripfilename");
		
		filelistmode = tripfilename == null;
		recordingactive = g.service != null && g.service.tripstats.filename.equals(tripfilename);
		
		if( recordingactive ){ 
			g.service.writestats();
		}
		
		registerForContextMenu(getListView());
		
		g.refreshlist = true;
		
		getActionBar().setDisplayShowHomeEnabled(false);
		
		if( !filelistmode || !g.getpref_bool("showactionbar", true) ){
			getActionBar().hide();
		}
		
		if( !recordingactive && g.defaultpreferences != null ){
//			AppRater.applaunched(this, g.defaultpreferences);
		}
	}
		
	@Override
	public void onResume() {
		super.onResume();
		
		if( !filelistmode ){
			displayupdatehandler.post(displayupdate);
		}
		
		if( !g.refreshlist ) return;
		g.refreshlist = false;
		
		if( filelistmode ){
			
			File[] files = new File(getFilesDir().toString()).listFiles();
			if( files.length > 1 && files[0].lastModified() < files[1].lastModified() ){
				List<File> list = Arrays.asList(files);
				Collections.reverse(list);
				files = (File[])list.toArray();
			}
			
			ArrayList<TripStats> objlist = new ArrayList<TripStats>();
			
		    for (File f : files) {
		    	String fn = f.getName();
		        if (!f.isDirectory() && fn.substring((fn.lastIndexOf(".") + 1)).equals("trip")) {
		        	
		        	TripStats tripstats = TripStats.load(this, fn);
		        	if( tripstats != null ){
		
		        		objlist.add(tripstats);
		        	}
		        }
		    }


			Parcelable state = getListView().onSaveInstanceState();
		    setListAdapter(new TrackViewAdapter1(this, objlist));
		    getListView().onRestoreInstanceState(state);
		    
		    getListView().setCacheColorHint(Color.rgb(255, 255, 160));
		    getListView().setBackgroundColor(Color.rgb(255, 255, 160));
		    
		} else {
		
		    TripStats tripstats = TripStats.load(this, tripfilename);
		    setListAdapter(new TrackViewAdapter2(this, tripstats, recordingactive));		    
		    
		    getListView().setCacheColorHint(Color.rgb(255, 255, 200));
		    getListView().setBackgroundColor(Color.rgb(255, 255, 200));
		}

	}
	
	@Override
    public void onPause() {
    	super.onPause();
    	
    	displayupdatehandler.removeCallbacks(displayupdate);
    }
	
	@Override
	protected void onListItemClick (ListView l, View v, int position, long id) {
	    if( filelistmode ){
	    	Intent intent = new Intent(this, TrackListActivity.class);
	    	intent.putExtra("tripfilename", ((TripStats)getListAdapter().getItem(position)).filename);
			startActivity(intent);
	    } else {
	    	v.showContextMenu();
	    } 
	    
	}
	
	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		int position = ((AdapterContextMenuInfo)menuInfo).position;
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tracklistcontext, menu);
		
		if( !filelistmode ) {
			if ( position != 0 ){
				menu.getItem(0).setTitle(R.string.menu_rename1);
				menu.getItem(1).setTitle(R.string.menu_delete1);
			}
			if( position < (recordingactive ? 2 : 1) || position == getListAdapter().getCount() - 1 ) {
				menu.getItem(1).setVisible(false);
			}

			int traveltime;
			float distance;
			float maxspeed;
			int movingtime;
			if( position == 0 ) {
				TripStats object = ((TrackViewAdapter2) getListAdapter()).tripstats;
				traveltime = object.traveltime;
				distance = object.distance;
				Object[] list = g.getallparts(object.title);
				maxspeed = (float)list[1];
				movingtime = (int)list[2];
			} else {
				TripPath object = ((TrackViewAdapter2) getListAdapter()).tripstats.paths.get(((TrackViewAdapter2) getListAdapter()).tripstats.paths.size() - position);
				traveltime = object.traveltime;
				distance = object.distance;
				Object[] list = g.getallparts(object.title);
				maxspeed = (float)list[1];
				movingtime = (int)list[2];
			}

			MenuItem[] list = new MenuItem[6];
			list[0] = menu.add(getString(R.string.listmenustats_time) + g.formattime(traveltime));
			list[1] = menu.add(getString(R.string.listmenustats_movingtime) + g.formattime(movingtime));
			list[2] = menu.add(getString(R.string.listmenustats_distance) + g.formatdistance1(distance, true, 0));
			list[3] = menu.add(getString(R.string.listmenustats_avgspeed) + g.formatspeed(distance / (traveltime / 1000f)));
			list[4] = menu.add(getString(R.string.listmenustats_avgmovingspeed) + g.formatspeed(movingtime == 0 ? 0 : distance / (movingtime / 1000f)));
			list[5] = menu.add(getString(R.string.listmenustats_maxspeed) + g.formatspeed(maxspeed));
			for(MenuItem val : list){
				val.setEnabled(false);
			}
		}
	}
	

	
	@Override
	public boolean onContextItemSelected(MenuItem menuitem) {
		final int position = ((AdapterContextMenuInfo) menuitem.getMenuInfo()).position;
		
		switch(menuitem.getItemId()) {
		case R.id.action_rename:

			final Object obj = getListAdapter().getItem(position);
			
	        AlertDialog.Builder alert = new AlertDialog.Builder(this);
	        
	        alert.setTitle(R.string.dialog_rename);
	        final EditText input = new EditText(this);
	        input.setText(g.getfirstpart(filelistmode || position == 0 ? ((TripStats)obj).title : ((TripPath)obj).title));
	        alert.setView(input);

	        alert.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
	            @Override
				public void onClick(DialogInterface dialog, int whichButton) {
	            	
	                if( filelistmode || position == 0 ){
	                	TripStats tripstats = (TripStats)obj;
	                	tripstats.title = g.setpart(tripstats.title, input.getEditableText().toString(), 0);
	                	tripstats.write(TrackListActivity.this);
	                } else {
	                	TripPath trippath = (TripPath)obj;
	                	trippath.title = g.setpart(trippath.title, input.getEditableText().toString(), 0);
	                	trippath.owner.write(TrackListActivity.this);
	                }

	                if( !filelistmode && position == 0 ){
                		g.refreshlist = true;
                	}
	                
	                if( recordingactive ){
                		g.service.reloadstats();
                	}

	                ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
	            }
	        });

	        alert.setNegativeButton(R.string.dialog_cancel,
	                new DialogInterface.OnClickListener() {
	                    @Override
						public void onClick(DialogInterface dialog, int whichButton) {
	                        dialog.cancel();
	                    }
	                });
	        AlertDialog alertDialog = alert.create();
	        alertDialog.show();
        	
	        
            return true;
		case R.id.action_delete:
			
			if( filelistmode ){
				ArrayList<TripStats> tripstatslist = ((TrackViewAdapter1)getListAdapter()).tripstatslist;
				new File(getFilesDir(), tripstatslist.get(position).filename).delete();
				tripstatslist.remove(position);
			} else {
				TripStats tripstats = recordingactive ? g.service.tripstats : ((TrackViewAdapter2)getListAdapter()).tripstats;
				tripstats.removepath((tripstats.paths.size() - 1) - (position - 1));
		    	tripstats.write(TrackListActivity.this);
		    	if( recordingactive ){
            		g.service.reloadstats();
            	}
			}
			((BaseAdapter) getListAdapter()).notifyDataSetChanged();
			
			return true;
		default:
			return super.onContextItemSelected(menuitem);
		}
	}

	@Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	        getMenuInflater().inflate(R.menu.tracklistactionbar, menu);

	        return true;
	    }
	 
	 @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	        switch (item.getItemId()) {
	            case R.id.menu_settings:
	            	startActivity(new Intent(this, SpeedometerSettings.class));
	                return true;
	            default:
	                return super.onOptionsItemSelected(item);
	        }
	    }
	
}

class TrackViewAdapter1 extends BaseAdapter {

    Context context;
    ArrayList<TripStats> tripstatslist;
    
    private static LayoutInflater inflater = null;
    private SpeedometerContext g;
    
    public TrackViewAdapter1(Context context, ArrayList<TripStats> tripstatslist) {
        this.context = context;
        this.tripstatslist = tripstatslist;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        g = (SpeedometerContext)context.getApplicationContext();
    }

    @Override
    public int getCount() {
        return tripstatslist.size();
    }

    @Override
    public Object getItem(int position) {
        return tripstatslist.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        
        if (vi == null){
        	vi = inflater.inflate(R.layout.trackviewlistitem1, null);
        }
        
        TripStats tripstats = tripstatslist.get(position);
        ((TextView) vi.findViewById(R.id.textTitle)).setText(g.getfirstpart(tripstats.title));
        ((TextView) vi.findViewById(R.id.textStats)).setText(g.formattime(g.pref_showmovementtime ? g.getpart(tripstats.title, 2) : tripstats.traveltime, false) + "   " + g.formatdistance1(tripstats.distance, tripstats.distance < 10000, 0 ));
        ((TextView) vi.findViewById(R.id.textDate)).setText((tripstats.paths.size() > 1 ? "*   " : "") + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(tripstats.created));
        
        return vi;
    }

}
	 


class TrackViewAdapter2 extends BaseAdapter {

    Context context;
    TripStats tripstats;
    ArrayList<TripPath> trippathlist;
    private static LayoutInflater inflater = null;
    private SpeedometerContext g;
    private boolean recordingactive;
    
    public TrackViewAdapter2(Context context, TripStats tripstats, boolean recordingactive) {
        this.context = context;
        this.tripstats = tripstats;
        this.recordingactive = recordingactive;
        
        trippathlist = tripstats.paths;       

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        g = (SpeedometerContext)context.getApplicationContext();
    }

    public void settripstats(TripStats tripstats){
    	this.tripstats = tripstats;
    	trippathlist = tripstats.paths;
    }
    
    @Override
    public int getCount() {
        return trippathlist.size() + (trippathlist.size() == 1 ? 0 : 1);
    }

    @Override
    public Object getItem(int position) {
    	return position == 0 ? tripstats : trippathlist.get((trippathlist.size() - 1) - (position - 1));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;

        if (vi == null){
        	vi = inflater.inflate(R.layout.trackviewlistitem2, null);
        }


        if( position == 0 ){

        	((TextView) vi.findViewById(R.id.textTitle)).setText(context.getResources().getString(R.string.tracklist_total) + (g.getfirstpart(tripstats.title).isEmpty() ? "" : " - " + g.getfirstpart(tripstats.title)));
        	((TextView) vi.findViewById(R.id.textTravelTime)).setText("=   " + g.formattime(g.pref_showmovementtime ? g.getpart(tripstats.title, 2) : tripstats.traveltime, true) + "     " + g.formatdistance1(tripstats.distance, true, 0) + g.getspeedstats(tripstats.title, tripstats.traveltime, tripstats.distance,"     "));
        	((TextView) vi.findViewById(R.id.textDate)).setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(tripstats.created) + " \u2014 " + 
        			(tripstats.created.getDay() == trippathlist.get(0).created.getDay() ? 
        					DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(tripstats.created.getTime() + tripstats.traveltime)):
        					DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(tripstats.created.getTime() + tripstats.traveltime))));
		    vi.setBackgroundColor(Color.rgb(255, 255, 160));
        } else { 
        	TripPath trippath = trippathlist.get((trippathlist.size() - 1) - (position - 1));

        	String title = (g.getfirstpart(trippath.title).isEmpty() ? "" : " - " + g.getfirstpart(trippath.title));
        	((TextView) vi.findViewById(R.id.textTitle)).setText(position < getCount() - 1 ? 
        			(recordingactive && position == 1 ? context.getResources().getString(R.string.tracklist_current) + title : g.getfirstpart(trippath.title)) :
        			context.getResources().getString(R.string.tracklist_start) + title);
            ((TextView) vi.findViewById(R.id.textTravelTime)).setText("+   " + g.formattime(g.pref_showmovementtime ? g.getpart(trippath.title, 2) : trippath.traveltime) + "     " + g.formatdistance(trippath.distance) + g.getspeedstats(trippath.title, trippath.traveltime, trippath.distance,"     "));
            ((TextView) vi.findViewById(R.id.textDate)).setText( DateFormat.getTimeInstance(DateFormat.SHORT).format(trippath.created));
            
            vi.setBackgroundColor(Color.rgb(255, 255, 200));
        }
        
        return vi;
    }

}