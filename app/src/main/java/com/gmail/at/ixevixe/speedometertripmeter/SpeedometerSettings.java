package com.gmail.at.ixevixe.speedometertripmeter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.DialogPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.PopupMenu;
import android.widget.Toast;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import org.json.JSONException;
import org.json.JSONObject;


public class SpeedometerSettings extends PreferenceActivity implements OnSharedPreferenceChangeListener  {
	private SpeedometerContext g;

	private ListPreference colorpresetsdialog;
	private Uri exportpath;
	String soundpickerkey;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        g = (SpeedometerContext)getApplication();
        
        addPreferencesFromResource(R.xml.preferences);
        
        List<Integer> speed = new LinkedList<Integer>(Arrays.asList(16, 30, 70, 140, 301));
        List<Integer> steps = new LinkedList<Integer>(Arrays.asList(1, 2, 5, 10, 20));
        List<String> opts = new LinkedList<String>();
        List<String> vals = new LinkedList<String>();
        
        String units = " " + (g.pref_units == 0 ? getString(R.string.kmh) : getString(R.string.mph));
        for( int i = 10; i <= 300; ){
        	if( i >= speed.get(0) ){
        		speed.remove(0);
        		steps.remove(0);
        	}
        	opts.add(i + units);
        	vals.add(Integer.toString(i));
        	i += steps.get(0);
        }
        
        ListPreference li = (ListPreference) findPreference(getString(R.string.maxdisplayspeed_key));
        li.setEntries(opts.toArray(new CharSequence[vals.size()]));
        li.setEntryValues(vals.toArray(new CharSequence[vals.size()]));
        
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        try {
			if( !getIntent().getBooleanExtra("showcolors", false) ) {
				PreferenceCategory prefcat = (PreferenceCategory) findPreference("catcolors");
				int i = prefcat.getPreferenceCount();
				while( --i >= 0 ){
					Preference item = prefcat.getPreference(i);
					if( item.getKey().equals("showcolorsbutton") || item.getKey().equals("resetcolorsbutton") || item.getKey().equals("colorpresetsbutton") || item.getKey().equals("colorautotogglelux") )
						continue;

					prefcat.removePreference(item);
				}

			} else {

				PreferenceCategory prefcat = (PreferenceCategory) findPreference("catcolors");
				for( int i= 0; i < prefcat.getPreferenceCount(); i++){
					if( prefcat.getPreference(i).getKey().equals("showcolorsbutton") ){
						prefcat.getPreference(i).setEnabled(false);

						break;
					}
				}

				new android.os.Handler().postDelayed(
					new Runnable() {
						public void run() {
							PreferenceScreen prefscreen = getPreferenceScreen();
							int i;
							int n = 0;
							for(i = 0; i < prefscreen.getPreferenceCount(); i++) {
								String key = prefscreen.getPreference(i).getKey();

								if("catcolors".equals(key))
									break;

								n += ((PreferenceCategory)prefscreen.getPreference(i)).getPreferenceCount() + 1;
							}

							getListView().setSelection(n);
						}
					}, 100);
			}

			if( g.getpref_bool("rgbpicker", false) ) {
				PreferenceScreen prefscreen = getPreferenceScreen();
				for (int i = 0; i < prefscreen.getPreferenceCount(); i++) {
					if (prefscreen.getPreference(i) instanceof PreferenceCategory) {
						PreferenceCategory prefcat = (PreferenceCategory) prefscreen.getPreference(i);
						for (int i1 = 0; i1 < prefcat.getPreferenceCount(); i1++) {
							if (prefcat.getPreference(i1) instanceof ColorPickerPreference) {
								ColorPickerPreference pref = (ColorPickerPreference) prefcat.getPreference(i1);
								pref.setHexValueEnabled(true);
							}
						}
					}
				}
			}

			colorpresetsdialog = (ListPreference)findPreference("colorpresetsbutton");
			colorpresetsdialog.setDialogTitle(getString(R.string.colorpresetsdialogtitle));
			colorpresetsdialog.setValue("");

			findPreference("odometerdistval").setEnabled(g.getpref_bool("odometerenabled"));

			LinkedList<String> list1 = new LinkedList<>();
			LinkedList<String> list2 = new LinkedList<>();
			list1.add("-");
			list2.add("0");
			String unitsdist = g.pref_units == 0 ? getResources().getString(R.string.km) : getResources().getString(R.string.mi);
			for( Integer val : new Integer[]{100, 500, 1000, 2000, 3000, 4000, 5000, 10000, 15000, 20000, 30000, 100000, 500000, 1000000} ){
				list1.add(String.format(val >= 1000 ? "%.0f %s" : "%.1f %s", val / 1000f, unitsdist));
				list2.add(String.valueOf(g.pref_units == 0 ? val : Math.round((val / 1000f) * 1609.34f)));
			}
			((ListPreference)findPreference("autopathdist")).setEntries(list1.toArray(new CharSequence[list1.size()]));
			((ListPreference)findPreference("autopathdist")).setEntryValues(list2.toArray(new CharSequence[list2.size()]));
		} catch (Exception e){
			g.showerror(getApplicationContext(), e.getMessage());
			e.printStackTrace();
		}

        findPreference("aboutbutton").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
					Toast.makeText(getApplicationContext(), "SpeedometerTripmeter v" + BuildConfig.VERSION_NAME + "\n\nixevixe@gmail.com", Toast.LENGTH_LONG).show();
					
					return true;
			    }
		});
        
        findPreference("hintbutton").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
					g.showhints(g.mainactivity);
					finish();

					return true;
			    }
		});

        findPreference("exportbutton").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {

				boolean newversion = false;

				if( Build.VERSION.SDK_INT > Build.VERSION_CODES.M ){
					if( preference != null ){
						Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
						intent.addCategory(Intent.CATEGORY_OPENABLE);
						intent.setType("text/csv");
						intent.putExtra(Intent.EXTRA_TITLE, "speedometer-tripmeter export " + DateFormat.format("yyyyMMdd-hhmmss", new java.util.Date()) + ".csv");

						startActivityForResult(intent, 1);

						return true;

					} else {
						newversion = true;
					}
				}

				String csvseparator = ";";

				String exportfn = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "speedometer-tripmeter export " + DateFormat.format("yyyyMMdd-hhmmss", new java.util.Date()) + ".csv";

				FileOutputStream fos;

				if( !newversion && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ){
					requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);

					return true;
				}

				try {
					if( !newversion && !Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ){
						throw new Exception("error: sd card not found");
					}

					fos = !newversion ? new FileOutputStream(exportfn) : (FileOutputStream) getContentResolver().openOutputStream(exportpath);
					PrintWriter w = new PrintWriter(fos);

					w.println("total/details" + csvseparator + "trip id" + csvseparator + "subtrip name" + csvseparator + "start time" + csvseparator + "h:m:s" + csvseparator + "meters");

					File[] files = new File(getFilesDir().toString()).listFiles();
					for (File f : files) {
				    	String fn = f.getName();
				        if (!f.isDirectory() && fn.substring((fn.lastIndexOf(".") + 1)).equals("trip")) {

				        	TripStats tripstats = TripStats.load(g, fn);
				        	if( tripstats != null ){

				        		String tripname = "#" + tripstats.filename.replace(".trip", "") + ( g.getfirstpart(tripstats.title).isEmpty() ?  "" : " (" + g.getfirstpart(tripstats.title) + ")" );

				        		w.println(
			        				"=" + csvseparator +
		        					tripname + csvseparator +
		        					"" + csvseparator +
		        					tripstats.created + csvseparator +
		        					g.formattime(tripstats.traveltime, true) + csvseparator +
		        					tripstats.distance + csvseparator + g.getstatscvs(tripstats.title, tripstats.traveltime, tripstats.distance, csvseparator)
			        			);

				        		for (TripPath path : tripstats.paths ) {
				        			w.println(
				        				"+" + csvseparator +
			        					tripname + csvseparator +
			        					g.getfirstpart(path.title) + csvseparator +
			        					path.created + csvseparator +
			        					g.formattime(path.traveltime, true) + csvseparator +
			        					path.distance + csvseparator + g.getstatscvs(path.title, path.traveltime, path.distance, csvseparator)
				        			);

				        		}
				        	}
				        }
				    }

					w.close();
			        fos.close();

				} catch (Exception e) {
					g.showerror(getApplicationContext(), e.getMessage());

					return true;
				}

				Toast.makeText(getApplicationContext(), "saved to file: " + exportfn, Toast.LENGTH_LONG).show();

				return true;
		    }
		});
        
        findPreference("sharebutton").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
					try { 
					    Intent i = new Intent(Intent.ACTION_SEND);  
					    i.setType("text/plain");
					    i.putExtra(Intent.EXTRA_TEXT, "\nSpeedometer EZ - GPS Speedometer&TripMeter App\nhttps://github.com/speedometer-app/speedometer\n\n");
					    startActivity(Intent.createChooser(i, ""));
					} catch(Exception e) { 
						g.showerror(getApplicationContext(), e.getMessage());
					}   
					
					return true;
			    }
		});

        /*findPreference("ratebutton").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Context context = SpeedometerSettings.this;
				try {
            		context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.getPackageName())));
        		} catch (Exception e) { 
        			g.showerror(getApplicationContext(), e.getMessage());
        		}
					
					return true;
			    }
		});*/
        
        findPreference("feedbackbutton").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				try { 
				    Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:ixevixe@gmail.com"));  
				    startActivity(Intent.createChooser(i, ""));
				} catch(Exception e) { 
					g.showerror(getApplicationContext(), e.getMessage());
				}   
					
					return true;
			    }
		});

		findPreference("siteurl").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Toast.makeText(getApplicationContext(), "https://github.com/speedometer-app/speedometer", Toast.LENGTH_LONG).show();

				return true;
			}
		});

        findPreference("showcolorsbutton").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(SpeedometerSettings.this, SpeedometerSettings.class)
						.putExtra("showcolors", true));

				finish();

				return true;
			}
		});

        findPreference("resetcolorsbutton").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(SpeedometerSettings.this).edit();
				PreferenceCategory prefcat = (PreferenceCategory) findPreference("catcolors");

				if( !getIntent().getBooleanExtra("showcolors", false) ){
					Toast.makeText(getApplication(), R.string.message_resetcolors, Toast.LENGTH_LONG).show();

					return true;
				}

				for( int i = 0; i < prefcat.getPreferenceCount(); i++ ){
					String itemkey = prefcat.getPreference(i).getKey();

					if( itemkey.equals("showcolorsbutton") || itemkey.equals("resetcolorsbutton") || itemkey.equals("colorpresetsbutton") || itemkey.equals("colorautotogglelux") )
						continue;

					editor.remove(itemkey);
				}

				editor.apply();

				finish();

				return true;
			}
		});

		findPreference("odometerdistval").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				g.odometer.showdistprompt(SpeedometerSettings.this);

				return true;
			}
		});

		findPreference("screenspeedalerts").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {

				fillspeedalertslists();

				return true;
			}
		});
		if( getIntent().getBooleanExtra("showspeedalerts", false) ){
			g.preferencebuttonclick(getPreferenceScreen(), "catspeedalerts", "screenspeedalerts");
		}

		Preference.OnPreferenceClickListener soundpickerpref = new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {

				Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);

				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(g.defaultpreferences.getString(preference.getKey(), "")));
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);

				soundpickerkey = preference.getKey();

				startActivityForResult(intent, 2);

				return true;
			}
		};
		for ( String val : g.soundprefkeys){
			findPreference(val).setOnPreferenceClickListener(soundpickerpref);
		}
		findPreference("autopathsound").setOnPreferenceClickListener(soundpickerpref);

		findPreference("showspeedalertsmessage").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				g.showmessagedialog(SpeedometerSettings.this, getString(R.string.speedalertsmessage));

				return true;
			}
		});

		Preference preference = findPreference("showcolorpresetsmessage");
		if( preference != null ) {
			preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					g.showmessagedialog(SpeedometerSettings.this, getString(R.string.colorpresetsmessage));

					return true;
				}
			});
		}

		Preference.OnPreferenceChangeListener additionalvarschange =  new Preference.OnPreferenceChangeListener(){
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				try {
					Integer.parseInt((String) newValue);
					return true;
				} catch(NumberFormatException e){
					return false;
				}
			}
		};
		for ( String val : new String[]{"maxaccuracy", "minspeed", "maxtimediff"}){
			findPreference(val).setOnPreferenceChangeListener(additionalvarschange);
		}

		findPreference("screenautopath").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				onSharedPreferenceChanged(g.defaultpreferences, "autopathsound");

				return true;
			}
		});
		if( getIntent().getBooleanExtra("showautopathscreen", false) ){
			g.preferencebuttonclick(getPreferenceScreen(), "cattripmeter", "screenautopath");
		}

    }

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		exportpath = state.getParcelable("exportpath");
		soundpickerkey = state.getString("soundpickerkey");

		onSharedPreferenceChanged(g.defaultpreferences, "speedalert");
		onSharedPreferenceChanged(g.defaultpreferences, "autopathsound");

		fillspeedalertslists();

		super.onRestoreInstanceState(state);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable("exportpath", exportpath);
		outState.putString("soundpickerkey", soundpickerkey);

		super.onSaveInstanceState(outState);
	}

	void fillspeedalertslists(){
		ListPreference listpreference = (ListPreference) findPreference("maxdisplayspeed");

		List<CharSequence> list1 = new ArrayList<CharSequence>(Arrays.asList(listpreference.getEntries()));
		List<CharSequence> list2 = new ArrayList<CharSequence>(Arrays.asList(listpreference.getEntryValues()));

		list1.add(0, "1 " + g.speedunits);
		list2.add(0, "1");
		list1.add(0, "0 " + g.speedunits);
		list2.add(0, "0");

		String[] keys = new String[]{"speedalert1_speed1", "speedalert1_speed2", "speedalert2_speed1", "speedalert2_speed2", "speedalert3_speed1", "speedalert3_speed2"};
		for ( String val : keys){
			if( ((ListPreference) findPreference(val)).getEntries() != null )
				continue;

			((ListPreference) findPreference(val)).setEntries(list1.toArray(new CharSequence[list1.size()]));
			((ListPreference) findPreference(val)).setEntryValues(list2.toArray(new CharSequence[list2.size()]));
		}

		onSharedPreferenceChanged(g.defaultpreferences, "speedalert");
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
    	if( resultCode != Activity.RESULT_OK )
    		return;

		if (requestCode == 1 ) {

			exportpath = resultData.getData();
			if( exportpath != null ) {
				findPreference("exportbutton").getOnPreferenceClickListener().onPreferenceClick(null);
			}

		} else if ( requestCode == 2 ) {

			SharedPreferences.Editor editor = g.defaultpreferences.edit();
			Uri ringtonename = (Uri) resultData.getExtras().get(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
			editor.putString(soundpickerkey, ringtonename != null ? ringtonename.toString() : "");
			editor.apply();

		}
	}

    @Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	if( key == null )
    		return;

    	if( key.equals("lang") ) {

			Toast.makeText(this, getString(R.string.message_applylanguage), Toast.LENGTH_LONG).show();

			finish();

		} else if( key.equals("colorpresetsbutton") ){

    		String presetname = sharedPreferences.getString(key, "");
    		if( !presetname.isEmpty() ) {
				g.writecolorpreset(presetname);

				Toast.makeText(this, getString(R.string.message_presetupdated, presetname), Toast.LENGTH_LONG).show();
				if( colorpresetsdialog != null ) {
					colorpresetsdialog.setValue("");
				}
			}

		} if( key.equals("colorautotogglelux") ) {

			String val = sharedPreferences.getString(key, "0");
			ListPreference pref = (ListPreference)findPreference(key);
			if( !val.equals("0") ) {
				pref.setSummary(getString(R.string.settings_autotogglesummary, val));
			} else {
				pref.setSummary(pref.getEntry());
			}

		} else if( key.equals("rgbpicker") ) {

			finish();

		} else if( key.startsWith("speedalert") ) {

			for ( String val : g.soundprefkeys){
				findPreference(val).setSummary(g.formatringtonetitle(sharedPreferences.getString(val, "")));
			}

		} else if( key.equals("autopathsound") ) {

				findPreference(key).setSummary(g.formatringtonetitle(sharedPreferences.getString(key, "")));

		} else if( key.equals("odometerenabled") ) {

    		boolean val = g.getpref_bool(key);
			findPreference("odometerdistval").setEnabled(val);
			g.odometer.setenabled(val);

		} else if( key.equals("distdecimals") || key.equals("showmeters") ) {

			new android.os.Handler().postDelayed(new Runnable() {
				public void run() {
					Toast.makeText(SpeedometerSettings.this, String.format("%s:   \"%s\";   \"%s\";   \"%s\"", getString(R.string.message_examples), g.formatdistance(112), g.formatdistance(915), g.formatdistance(33590)), Toast.LENGTH_LONG).show();
				}
			}, 500);

		} else {

			Preference pref = findPreference(key);

			if( pref instanceof ListPreference) {
				pref.setSummary(((ListPreference)pref).getEntry());
			}

		}
    }
}
