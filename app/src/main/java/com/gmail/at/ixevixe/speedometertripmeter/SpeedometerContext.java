package com.gmail.at.ixevixe.speedometertripmeter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.gmail.at.ixevixe.speedometertripmeter.speedalerts.Alerts;
import com.gmail.at.ixevixe.speedometertripmeter.speedalerts.SpeedAlert;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

@SuppressLint("DefaultLocale")
public class SpeedometerContext extends Application
{
	boolean verticalorientation;
	MainActivity mainactivity; 
	SpeedometerService service;	
	int pref_units;
	boolean pref_showseconds;
	boolean pref_showmeters;
	boolean pref_showbuttonicons;

	boolean refreshlist;
	
	SharedPreferences defaultpreferences;
	
	private String unit1;
	private String unit2;

	int alertcounter = 0;
	boolean checkspassed = false;
	boolean fullscreenlandscape = false;

	int pref_colorautotogglelux = 0;
	public float convertspeedunits = 0;
	String speedunits = "";
	boolean pref_showspeedstats = false;
	boolean pref_showmovementtime = false;
	boolean pref_showmovementspeed = false;
	float pref_maxaccuracy = 100;
	float pref_minspeed = 1;
	int pref_maxtimediff = 10000;
	int pref_autopathtime = 0;
	int pref_autopathdist = 0;
	Ringtone autopathringtone;
	int pref_distdecimals = 0;

	int pref_speedalertsprofile = 0;
	Alerts speedalerts;
	String[] soundprefkeys = new String[]{
			"speedalert1_sound11", "speedalert1_sound21", "speedalert1_sound",
			"speedalert2_sound11", "speedalert2_sound21", "speedalert2_sound",
			"speedalert3_sound11", "speedalert3_sound21", "speedalert3_sound",
	};

	public Odometer odometer;

	int pref_gpsinterval = 1000;

	private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			pref_units = getpref("units");
			pref_showseconds = getpref_bool("showseconds");
			pref_showmeters = getpref_bool("showmeters");
			
			unit1 = pref_units == 0 ? getResources().getString(R.string.km) : getResources().getString(R.string.mi); 
			unit2 = pref_units == 0 ? getResources().getString(R.string.m) : getResources().getString(R.string.ft);

			pref_showbuttonicons = getpref_bool("showbuttonicons");

			pref_colorautotogglelux = getpref("colorautotogglelux");
			convertspeedunits = pref_units == 0 ? 3.6f : 3.6f * (1 / 1.609344f);
			speedunits = pref_units == 0 ? getString(R.string.kmh) : getString(R.string.mph);
			pref_showspeedstats = getpref_bool("showspeedstats");
			pref_showmovementtime = getpref_bool("showmovementtime");
			pref_showmovementspeed = getpref_bool("showmovementspeed");
			pref_maxaccuracy = Integer.parseInt(sharedPreferences.getString("maxaccuracy", "100"));
			pref_minspeed = Integer.parseInt(sharedPreferences.getString("minspeed", "1"));
			pref_maxtimediff = Integer.parseInt(sharedPreferences.getString("maxtimediff", "10000"));
			pref_autopathtime = getpref("autopathtime");
			pref_autopathdist = getpref("autopathdist");
			pref_distdecimals = getpref("distdecimals");

			autopathringtone = loadringtone(SpeedometerContext.this, sharedPreferences.getString("autopathsound", null));
		}
	};
	

	public void init() {
		loadpreferences();

		odometer = new Odometer(this, getpref("maxdistperupdate"));
		if( getpref_bool("odometerenabled") ){
			odometer.setenabled(true);
		}
	}

	private  void loadpreferences(){
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		defaultpreferences = PreferenceManager.getDefaultSharedPreferences(this);
		defaultpreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

		sharedPreferenceChangeListener.onSharedPreferenceChanged(defaultpreferences, null);
	}

	int getpref(String key){
		if( defaultpreferences == null )
			loadpreferences();

		return Integer.parseInt(defaultpreferences.getString(key, "0"));
	}
	
	boolean getpref_bool(String key){
		if( defaultpreferences == null )
			loadpreferences();

		return defaultpreferences.getBoolean(key, false);
	}
	
	boolean getpref_bool(String key, boolean defval){
		if( defaultpreferences == null )
			loadpreferences();

		return defaultpreferences.getBoolean(key, defval);
	}

	String formattime(int ms){
		return formattime(ms, pref_showseconds);
	}
	
	String formattime(int ms, boolean showseconds){
		if( showseconds ){
			return String.format("%02d:%02d:%02d", ms / (60 * 60 * 1000), (ms / (60 * 1000)) % 60, (ms / 1000) % 60);
		} else {
			return String.format("%02d:%02d", ms / (60 * 60 * 1000), ms / (60 * 1000) % 60);
		}
	}
	
	String formatdistance(float meters){
		return formatdistance(meters, pref_showmeters, pref_distdecimals);
	}
	
	String formatdistance(float meters, boolean showmeters, int decimalplaces){
		if( showmeters && (decimalplaces == 0 || meters < 1000) ){
			int val1 = pref_units == 0 ? (int)meters / 1000 : (int)(meters / 1609.344f);
			int val2 = pref_units == 0 ? (int)meters % 1000 : (int)((meters % 1609.344f) * 3.28084f);
			
			if( val1 > 0 && val2 > 0 ){
				return String.format("%d\u200a%s %d\u200a%s", val1, unit1, val2, unit2);
			} else if( val1 > 0 ){
				return String.format("%d\u200a%s", val1, unit1);
			} else {
				return String.format("%d\u200a%s", val2, unit2);
			}
		} else {
			if( decimalplaces == 0  ) {
				return String.format("%d\u200a%s", Math.round(meters / (pref_units == 0 ? 1000 : 1609.344f)), unit1);
			} else {
				return String.format("%." + decimalplaces + "f\u200a%s", meters / (pref_units == 0 ? 1000 : 1609.344f), unit1);
			}
		}
		
	}
	
	void errexit(Activity activity, Exception e){
		e.printStackTrace();
		activity.finish();
	}
	
	void errexitservice(Service service , Exception e){
		e.printStackTrace();
		service.stopSelf();
	}
	
	void showhints(Context context){
		try {
			final Dialog dialog = new Dialog(context);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.WHITE));
			dialog.setCanceledOnTouchOutside(true);

			ImageView iv = new ImageView(context);
			iv.setImageResource(R.drawable.hint);
			iv.setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});
			iv.setScaleType(ScaleType.FIT_END);
			dialog.addContentView(iv, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			dialog.show();
			dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		} catch (Exception e) {}
	}
	
	void checkwarngps(final Activity context){
		try {

			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
				//if( alertcounter++ == 0 ){
				//	context.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
				//} else {
					final AlertDialog.Builder builder = new AlertDialog.Builder(context);
					builder.setMessage(getString(R.string.speedometergpspermission1))
							.setTitle(getString(R.string.speedometergpspermission))
							.setCancelable(false)
							.setPositiveButton(R.string.speedometergpspermission_button1, new DialogInterface.OnClickListener() {
								public void onClick( final DialogInterface dialog,  final int id) {
									context.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
								}
							})
							.setNegativeButton(R.string.speedometergpspermission_button2, new DialogInterface.OnClickListener() {
								public void onClick(final DialogInterface dialog,  final int id) {
									dialog.cancel();
								}
							});
					final AlertDialog alert = builder.create();
					alert.show();
				//}

				return;
			}

			final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

		    if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
				alertcounter++;

		    	final AlertDialog.Builder builder = new AlertDialog.Builder(context);
			    builder.setMessage(getString(R.string.gpswarnmessage))
			           .setCancelable(false)
			           .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
			               public void onClick( final DialogInterface dialog,  final int id) {
			            	   context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			               }
			           })
			           .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
			               public void onClick(final DialogInterface dialog,  final int id) {
			                    dialog.cancel();
			               }
			           });
			    final AlertDialog alert = builder.create();
			    alert.show();

			    return;
		    }
		
		} catch (Exception e) {
			alertcounter++;

			showerror(getApplicationContext(), e.getMessage());
		}

		checkspassed = true;
	}
	
	void showerror(Context context, String msg, Boolean... lengthlong){
		Toast.makeText(context, "error: " + msg, lengthlong.length == 0 || lengthlong[0] ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
	}

	void showmessagedialog(Context context, String msg){
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(msg)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick( final DialogInterface dialog,  final int id) {
						dialog.dismiss();
					}
				});
		final AlertDialog alert = builder.create();
		alert.show();
	}

	void loadcolorpreset(int idx, SharedPreferences prefs){
		String jsonstring = prefs.getString("colorpreset_" + idx, null);

		if(jsonstring != null){
			try {
				JSONObject jsonobject = new JSONObject(jsonstring);
				Iterator<String> keys = jsonobject.keys();

				SharedPreferences.Editor editor = prefs.edit();
				while(keys.hasNext()) {
					String key = keys.next();
					editor.putInt(key, jsonobject.getInt(key));
				}
				editor.apply();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public void loadcolorpreset(int idx){
		loadcolorpreset(idx, defaultpreferences);
	}

	void writecolorpreset(String presetname){
		JSONObject out = new JSONObject();
		Map<String,?> keys = defaultpreferences.getAll();
		for(Map.Entry<String,?> entry : keys.entrySet()){
			if( entry.getKey() != null && entry.getKey().startsWith("color_") ){
				try {
					out.put(entry.getKey(), entry.getValue());
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}

		SharedPreferences.Editor editor = defaultpreferences.edit();
		editor.putString("colorpreset_" + presetname, out.toString());
		editor.apply();
	}

	public void loadspeedalerts(Integer profile){
		if( profile == null ){
			profile = getpref("speedalertprofile");
		}

		pref_speedalertsprofile = profile;

		if( speedalerts != null ){
			speedalerts.clear();
			speedalerts = null;
		}

		if( profile > 0 ){
			String profilestring = String.format("speedalert%d_", profile);
			speedalerts = new Alerts(this,
					new SpeedAlert(this,
							Math.round(getpref(profilestring + "speed1") / convertspeedunits),
							getpref(profilestring + "color11"),
							defaultpreferences.getString(profilestring + "sound11", null),
							getpref(profilestring + "color"),
							defaultpreferences.getString(profilestring + "sound", null),
							getpref(profilestring + "delay"),
							getpref(profilestring + "repeat"),
							true),
					new SpeedAlert(this,
							Math.round(getpref(profilestring + "speed2") / convertspeedunits),
							getpref(profilestring + "color21"),
							defaultpreferences.getString(profilestring + "sound21", null),
							getpref(profilestring + "color"),
							defaultpreferences.getString(profilestring + "sound", null),
							getpref(profilestring + "delay"),
							getpref(profilestring + "repeat"),
							false)
			);
		}

		SharedPreferences.Editor editor = defaultpreferences.edit();
		editor.putString("speedalertprofile", Integer.toString(profile));
		if( profile > 0 && pref_colorautotogglelux > 0 ){
			editor.putInt("colorautotogglelux_saved", pref_colorautotogglelux);
			editor.putString("colorautotogglelux", "0");
		}
		editor.apply();
	}

	String formatdistance1(float meters, boolean showmeters, int decimalplaces){
		if( pref_units == 0 ){
			return formatdistance(meters, showmeters, decimalplaces);
		} else {
			return formatdistance(meters);
		}
	}

	String formatspeed(float meterspersecond){
		return Math.round(meterspersecond * convertspeedunits) + "\u200a" + speedunits;
	}

	String getfirstpart(String triptitle){
		if( triptitle == null ){

			return null;
		}

		String[] list = triptitle.split("\1");
		return list[0];
	}

	int getpart(String triptitle, int idx){
		if( triptitle == null ){

			return 0;
		}

		String[] list = triptitle.split("\1");
		try {
			return list.length > idx ? Integer.parseInt(list[idx]) : 0;
		} catch (Exception e) {
			return 0;
		}
	}

	Object[] getallparts(String triptitle){
		if( triptitle == null ){

			return new Object[]{"", 0, 0};
		}

		String[] list = triptitle.split("\1");
		float maxspeed;
		try {
			maxspeed = Float.parseFloat(list[1]);
		} catch (Exception e) {
			maxspeed = 0;
		}
		int movingtime;
		try {
			movingtime = Integer.parseInt(list[2]);
		} catch (Exception e) {
			movingtime = 0;
		}

		return new Object[]{list[0], maxspeed, movingtime};
	}

	String setpart(String triptitle, String val, int idx){
		if( triptitle == null ){

			triptitle = "";
		}

		String[] list = triptitle.split("\1");
		if( list.length == 1 && idx == 0 ){

			return val;
		} else {
			if( list.length <= idx ){
				String[] list1 = new String[idx + 1];
				Arrays.fill(list1,"");
				System.arraycopy(list, 0, list1, 0, list.length);
				list = list1;
			}

			list[idx] = val;

			return TextUtils.join("\1", list);
		}
	}

	String setallparts(String triptitle, String s2, String s3){
		if( triptitle == null ){
			return "" + "\1" + s2 + "\1" + s3;
		} else {
			int pos = triptitle.indexOf("\1");
			return (pos != -1 ? triptitle.substring(0, pos) : triptitle) + "\1" + s2 + "\1" + s3;
		}
	}

	String getspeedstats(String title, int traveltime, float distance, String sep){
		if( !pref_showspeedstats )
			return "";

		Object[] list = getallparts(title);
		float maxspeed = (float)list[1];
		int movingtime = (int)list[2];

		int timeval = pref_showmovementspeed ? movingtime : traveltime;
		return String.format("%s%s\u200a|\u200a%s\u200a%s", sep, timeval == 0 ? 0 : Math.round((distance / (timeval / 1000f)) * convertspeedunits), Math.round(maxspeed * convertspeedunits), speedunits);
	}

	String getstatscvs(String title, int traveltime, float distance, String sep){
		Object[] list = getallparts(title);
		float maxspeed = (float)list[1];
		int movingtime = (int)list[2];

		return formattime(Math.round(movingtime), true) + sep + Math.round(distance / (traveltime / 1000f)) + sep + Math.round(distance / (movingtime / 1000f)) + sep + maxspeed;
	}

	public String formatringtonetitle(String path){
		if( path == null || path.isEmpty() ){
			return "-";
		}

		String val = Uri.parse(path).getQueryParameter("title");
		return val != null ? val : path;
	}

	public void preferencebuttonclick(PreferenceScreen prefscreen, String keycat, String keybutton){
		int idx = 0;
		for (int i = 0; i < prefscreen.getPreferenceCount(); i++){
			if( !(prefscreen.getPreference(i) instanceof PreferenceCategory) )
				continue;

			PreferenceCategory catpref = (PreferenceCategory)prefscreen.getPreference(i);
			if(catpref.getKey().equals(keycat)){
				for( int i1 = 0; i1 < catpref.getPreferenceCount(); i1++ ){
					if( catpref.getPreference(i1).getKey().equals(keybutton) ) {
						prefscreen.onItemClick(null, null, idx + i1 + 1, 0);
					}
				}
			}
			idx += catpref.getPreferenceCount() + 1;
		}
	}

	public Ringtone loadringtone(Context context, String ringtonename){
		if( ringtonename == null || ringtonename == "" )
			return null;

		Ringtone ringtone;

		ringtone = RingtoneManager.getRingtone(context, Uri.parse(ringtonename));

		if( ringtone == null ){
			showerror(context, getString(R.string.message_ringtonenotfound, ringtonename));
		}

		return ringtone;
	}

	void playringtone(Ringtone ringtone){
		if( ringtone == null )
			return;

		ringtone.stop();
		ringtone.play();
	}


	void hideSystemUI(Activity activity) {
		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT  )
			return;

		View decorView = activity.getWindow().getDecorView();
		decorView.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_IMMERSIVE

						| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_FULLSCREEN);
	}

	void showSystemUI(Activity activity) {
		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT  )
			return;

		View decorView = activity.getWindow().getDecorView();
		decorView.setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
	}
}