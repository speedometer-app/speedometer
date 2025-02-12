package com.gmail.at.ixevixe.speedometertripmeter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader.TileMode;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends Activity implements View.OnClickListener, View.OnLongClickListener, PopupMenu.OnMenuItemClickListener {

	SpeedometerContext sharedcontext;

	SpeedometerView speedometerview;
	InfoView infoview;
	LocationManager locationmanager;

	ImageView popuppos;

	GnssStatus.Callback gnssstatuscallback;

	int satcount = 0;

	SensorEventListener lightsensorlistener;
	SensorManager sensormanager;
	Sensor lightsensor;
	MenuItem lightsensorinfo;

	@SuppressLint("ApplySharedPref")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedcontext = (SpeedometerContext)getApplication();
        sharedcontext.init();
        sharedcontext.mainactivity = this;

        locationmanager = (LocationManager)getSystemService(LOCATION_SERVICE);

        speedometerview = new SpeedometerView(this);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        sharedcontext.verticalorientation = displaymetrics.heightPixels >= displaymetrics.widthPixels;
        if( sharedcontext.verticalorientation ){

			int screenheight = displaymetrics.heightPixels - getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height", "dimen", "android"));
			int speedometerheight = displaymetrics.widthPixels;
			int infoviewheight = speedometerheight / 3;


        	infoview = new InfoView(this);
        	infoview.height = speedometerheight > screenheight - infoviewheight ? screenheight - speedometerheight : infoviewheight;

        	LinearLayout.LayoutParams layoutparams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	        layoutparams.setMargins(0, (screenheight - infoview.height) / 2 - speedometerheight / 2, 0, 0);
	        speedometerview.setLayoutParams(layoutparams);
        }

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f));

        LinearLayout linearlayout = new LinearLayout(this);
        linearlayout.setGravity(Gravity.CENTER_HORIZONTAL);
        linearlayout.setOrientation(LinearLayout.VERTICAL);
		try {
			linearlayout.setBackgroundColor(PreferenceManager.getDefaultSharedPreferences(this).getInt("color_containerbg", 0xff000000));
		} catch (Exception e){}

		linearlayout.addView(speedometerview);
        if( sharedcontext.verticalorientation ){
        	linearlayout.addView(spacer);
        	linearlayout.addView(infoview);
        }

        setContentView(linearlayout);

		popuppos = new ImageView(this);
		popuppos.setLayoutParams(new ViewGroup.LayoutParams(Math.round(19 * getResources().getDisplayMetrics().density), Math.round(19 * getResources().getDisplayMetrics().density)));
		popuppos.setBackgroundColor(0xffffffff);
		((ViewGroup) this.getWindow().getDecorView().findViewById(android.R.id.content)).addView(popuppos);
		popuppos.setX(1);
		popuppos.setY(1);
		popuppos.setImageResource(R.drawable.menu);

		speedometerview.setOnLongClickListener(this);
		linearlayout.setOnLongClickListener(this);
		linearlayout.setOnClickListener(this);
		popuppos.setOnClickListener(this);

        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", 0);
        if( prefs.getBoolean("firstrun", true) ){
        	sharedcontext.showhints(sharedcontext.mainactivity);
        	prefs.edit().putBoolean("firstrun", false).commit();

			for(int i = 1; i <= 5; i++) {
				sharedcontext.writecolorpreset(String.valueOf(i));
			}

			Uri ringtonename = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			if( ringtonename != null ) {
				String ringtonename1 = ringtonename.toString();
				SharedPreferences.Editor editor = sharedcontext.defaultpreferences.edit();
				for ( String val : sharedcontext.soundprefkeys){
					if( !val.matches(".*_sound11$") )
						continue;

					editor.putString(val, ringtonename1);
				}

				editor.apply();
			}
        }

		try {

			String lang = sharedcontext.defaultpreferences.getString("lang", "-");
			if( !lang.equals("-") ){
				if( Locale.getDefault().getLanguage().toUpperCase().equals(lang) ){
					SharedPreferences.Editor editor = sharedcontext.defaultpreferences.edit();
					editor.remove("lang");
					editor.apply();
				} else if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ){
					Locale locale = new Locale(lang);
					Locale.setDefault(locale);
					Resources resources = this.getResources();
					Configuration config = resources.getConfiguration();
					config.setLocale(locale);
					resources.updateConfiguration(config, resources.getDisplayMetrics());
				}
			}

		} catch (Exception e){
			e.printStackTrace();
		}

		sharedcontext.loadspeedalerts(null);

    }

    @Override
    public void onResume() {
    	super.onResume();

		sharedcontext.checkwarngps(this);

    	gpsenable();
    	speedometerview.onresume();
    	if( sharedcontext.verticalorientation ){
    		infoview.onresume();
    	}

		try {
			boolean fullscreenlandscape = sharedcontext.getpref_bool("fullscreenlandscape");
			if( !sharedcontext.verticalorientation ){
				if( fullscreenlandscape ) {
					popuppos.setVisibility(View.INVISIBLE);
					sharedcontext.hideSystemUI(this);
				} else if( sharedcontext.fullscreenlandscape ) {
					popuppos.setVisibility(View.VISIBLE);
					sharedcontext.showSystemUI(this);
				}
			}
			sharedcontext.fullscreenlandscape = fullscreenlandscape;
		} catch (Exception e){}

		if( sharedcontext.pref_colorautotogglelux > 0 ) {
			createsensorlistener();
		}
    }

    @Override
    public void onRestart() {
    	super.onRestart();
    }

    @Override
    public void onPause() {
    	super.onPause();

    	gpsdisable();
    	speedometerview.onpause();
    	if( sharedcontext.verticalorientation ){
    		infoview.onpause();
    	}

    	removesenorlistener();
    }

    @Override
    public void onStop() {
    	super.onStop();
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

		onLongClick(popuppos);

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		return onMenuItemClick(item);
    }

    @Override
	public void onClick(View v){
		this.onLongClick(v);
	}

	@SuppressLint("MissingPermission")
	@Override
	public boolean onLongClick(View v) {
		PopupMenu popup = new PopupMenu(this, popuppos);
		popup.inflate(R.menu.main);
		popup.setOnMenuItemClickListener(this);

		popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
			@Override
			public void onDismiss(PopupMenu menu) {
				lightsensorinfo = null;
			}
		});

		Menu menu = popup.getMenu();
		if( sharedcontext.service != null ){
			menu.findItem(R.id.menu_startmeter).setEnabled(false);
		} else {
			menu.findItem(R.id.menu_stopmeter).setEnabled(false);
			menu.findItem(R.id.menu_newsubtrack).setEnabled(false);
			menu.findItem(R.id.menu_pathmode).setEnabled(false);
		}

		if( !sharedcontext.verticalorientation ){
			menu.findItem(R.id.menu_pathmode).setVisible(false);
		}

		String accuracy;
		try {
			accuracy = String.format(Locale.getDefault(), "%.1f", locationmanager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getAccuracy()) + " " + getString(R.string.m);
		} catch (Exception e){
			accuracy = getString(R.string.na);
		}

		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ) {
			menu.findItem(R.id.menu_gpsinfo).setTitle(String.format(getString(R.string.menu_speedometer_gpsinfo), satcount, accuracy));
		} else {
			menu.findItem(R.id.menu_gpsinfo).setTitle(String.format(getString(R.string.menu_speedometer_gpsinfo_ver), accuracy));
		}

		menu.findItem(R.id.menu_colorautotoggle).setChecked(sharedcontext.pref_colorautotogglelux > 0);

		MenuItem speedalertsmenu = menu.findItem(R.id.menu_speedalerts);
		if( sharedcontext.pref_speedalertsprofile > 0 ) {
			speedalertsmenu.setTitle(String.format("%s / #%d", speedalertsmenu.getTitle(), sharedcontext.pref_speedalertsprofile));
		}

		popup.show();

		return true;
	}

	public boolean onMenuItemClick(MenuItem item) {
		SharedPreferences prefs = sharedcontext.defaultpreferences;

    	if( infoview == null ) {
			infoview = new InfoView(this);
		}

		switch (item.getItemId()) {
			case R.id.menu_startmeter:
				infoview.findViewById(R.id.buttonRec).performClick();

				return true;
			case R.id.menu_stopmeter:
				infoview.findViewById(R.id.buttonRec).performClick();

				return true;
			case R.id.menu_newsubtrack:
				infoview.findViewById(R.id.buttonSet).performClick();

				return true;
			case R.id.menu_pathmode:
				infoview.setpathmode(!infoview.pathmode);

				return true;
			case R.id.menu_tripmeterlog:
				infoview.findViewById(R.id.statsview).performClick();

				return true;
			case R.id.menu_colorpresets:
				if( lightsensor != null ) {
					SubMenu menuitems = item.getSubMenu();
					lightsensorinfo = menuitems.add("");
					lightsensorinfo.setEnabled(false);
				}

				return true;
			case R.id.menu_colorpreset1:
			case R.id.menu_colorpreset2:
			case R.id.menu_colorpreset3:
			case R.id.menu_colorpreset4:
			case R.id.menu_colorpreset5:
				List<Integer> presetids = Arrays.asList(R.id.menu_colorpreset1, R.id.menu_colorpreset2, R.id.menu_colorpreset3, R.id.menu_colorpreset4, R.id.menu_colorpreset5);
				sharedcontext.loadcolorpreset(presetids.indexOf(item.getItemId()) + 1, prefs);

				return true;
			case R.id.menu_colorsettings:
				startActivity(new Intent(this, SpeedometerSettings.class)
						.putExtra("showcolors", true));

				return true;
			case R.id.menu_colorautotoggle:
				if( !item.isChecked() ){
					SharedPreferences.Editor editor = prefs.edit();
					int val = prefs.getInt("colorautotogglelux_saved", 1);
					editor.putString("colorautotogglelux", Integer.toString(val == 0 ? 1 : val));
					editor.apply();
					createsensorlistener();
				} else {
					SharedPreferences.Editor editor = prefs.edit();
					editor.putInt("colorautotogglelux_saved", sharedcontext.pref_colorautotogglelux);
					editor.putString("colorautotogglelux", "0");
					editor.apply();
				}

				return true;
			case R.id.menu_speedalerts:
				SubMenu item1 = item.getSubMenu();
				item1.getItem(sharedcontext.pref_speedalertsprofile).setChecked(true);
				item1.setHeaderTitle(R.string.menu_speedalerts);

				return true;
			case R.id.menu_speedalert0:
			case R.id.menu_speedalert1:
			case R.id.menu_speedalert2:
			case R.id.menu_speedalert3:
				if( !sharedcontext.getpref_bool("speedalertsmessageshowed", false) ){
					sharedcontext.showmessagedialog(this, getString(R.string.speedalertsmessage));
					SharedPreferences.Editor editor = sharedcontext.defaultpreferences.edit();
					editor.putBoolean("speedalertsmessageshowed", true);
					editor.apply();
				}
				List<Integer> profileids = Arrays.asList(R.id.menu_speedalert0, R.id.menu_speedalert1, R.id.menu_speedalert2, R.id.menu_speedalert3);
				sharedcontext.loadspeedalerts(profileids.indexOf(item.getItemId()));

				return true;
			case R.id.menu_speedalertssettings:
				startActivity(new Intent(this, SpeedometerSettings.class)
						.putExtra("showspeedalerts", true));

				return true;
			case R.id.menu_settings:
				startActivity(new Intent(this, SpeedometerSettings.class));

				return true;
			case R.id.menu_hint:
				sharedcontext.showhints(sharedcontext.mainactivity);

				return true;
			default:
				return false;
		}
	}

	@SuppressLint("MissingPermission")
    public void gpsenable(){

		try {
			locationmanager.requestLocationUpdates(LocationManager.GPS_PROVIDER, sharedcontext.pref_gpsinterval, 0, speedometerview);

			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ){
				gnssstatuscallback = new GnssStatus.Callback(){
					public void onSatelliteStatusChanged (GnssStatus status){
						satcount = status.getSatelliteCount();
					}
				};
				locationmanager.registerGnssStatusCallback(gnssstatuscallback);
			}
		} catch (Exception e){
			if( sharedcontext.checkspassed || sharedcontext.alertcounter > 1 ) {
				sharedcontext.showerror(getApplicationContext(), e.getMessage(), false);
			}
		}


    }

    public void gpsdisable(){
    	locationmanager.removeUpdates(speedometerview);
    	speedometerview.needleSetSpeed(0);

		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ){
			locationmanager.unregisterGnssStatusCallback(gnssstatuscallback);
			satcount = 0;
		}
    }

    @SuppressLint("MissingPermission")
	void gpssetinterval(long interval){
		try {
			locationmanager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, 0, speedometerview);
		} catch (Exception e){
			if( sharedcontext.checkspassed || sharedcontext.alertcounter > 1 ) {
				sharedcontext.showerror(getApplicationContext(), e.getMessage());
			}
		}
	}

	boolean createsensorlistener(){
		if( sensormanager == null ){
			sensormanager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		}

		if( lightsensor == null ) {
			lightsensor = sensormanager.getDefaultSensor(Sensor.TYPE_LIGHT);

			if( lightsensor == null ){
				sharedcontext.showerror(this, getString(R.string.message_lightsensorerror));

				return false;
			}

			lightsensorlistener = new SensorEventListener() {
				private boolean autotoggleflag = true;

				{
					sharedcontext.loadcolorpreset(1, sharedcontext.defaultpreferences);
				}

				@Override
				public void onSensorChanged(SensorEvent event) {
					if( lightsensorinfo != null ){
						lightsensorinfo.setTitle(String.valueOf(getString(R.string.menu_speedometer_lightsensorinfo, event.values[0])));
					}

					int thres = sharedcontext.pref_colorautotogglelux;
					if( thres <= 0 )
						return;

					if( event.values[0] > thres ){
						if( !autotoggleflag ){
							autotoggleflag = true;
							sharedcontext.loadcolorpreset(1, sharedcontext.defaultpreferences);
						}
					} else {
						if( autotoggleflag ){
							autotoggleflag = false;
							sharedcontext.loadcolorpreset(2, sharedcontext.defaultpreferences);
						}
					}
				}

				@Override
				public void onAccuracyChanged(Sensor sensor, int accuracy) {

				}
			};

			sensormanager.registerListener(lightsensorlistener, lightsensor, SensorManager.SENSOR_DELAY_NORMAL);
		}

		return true;
	}

	void removesenorlistener(){
		if( sensormanager == null )
			return;

		if( lightsensorlistener != null ) {
			sensormanager.unregisterListener(lightsensorlistener);
			lightsensor = null;
		}

	}

}

class SpeedometerView extends View implements LocationListener {
	static final int ANIM_FPS = 60;
	static final float NEEDLE_ACCEL = .1f;
	static final float NEEDLE_DAMP = .7f;
	static final int ANGLE_MIN_V = -140;
	static final int ANGLE_MAX_V = 140;
	static final int ANGLE_MIN_H = -112;
	static final int ANGLE_MAX_H = 112;
	static final int ANGLE_MAX1_V = 170;
	static final int ANGLE_MAX1_H = 114;
	static final int SMOOTH_MAXANGLE = 45; 
	static final float SMOOTH_CONST = (1f / SMOOTH_MAXANGLE) * 0.35f;
	static final int MAXNICKS = 15;
	
	static final float BRIGHTNESS_DIMSCREEN = 0.1f;
	static final float BRIGHTNESS_STANDBY = 0.1f;
	
	static final int STANDBYTIMEOUT = 60000;
	static final int STANDBYTIMEOUT12 = (int)(STANDBYTIMEOUT * 0.5f);
	static final int STANDBYTIMEOUT23 = (int)(STANDBYTIMEOUT * 0.75f);
	
	static final int STANDBYGPSINTERVAL = 30000;

	private MainActivity activity;
	private Handler _handler = new Handler();
	
	private Bitmap _face;
	
	private float _scale;
	
	private Paint handPaint;
	private Path handPath;
	
	private boolean needleAnim = false;
	private float needleVelocity = 0;
	private float needleRotation;
	private float needleTargetRotation;
	private float needleSmoothVal;
	private boolean needleConstantVelocity;
	
	private float savedbrighness;
	
	private long locationchangedtime;
	
	private Random random = new Random();
	
	private boolean dimscreenmode = false;
	private boolean standbymode = true;
	public boolean dialogopen = false;

	private Paint standbytextpaint = new Paint();
	private int standbytextxpos = 0;
	private int standbytextypos = 0;
	private String standbytextval = "";

	private int pref_needlemovement = 0;

	private Runnable runnable1 = new Runnable() {
		@Override
		public void run() {
			long diff = System.currentTimeMillis() - locationchangedtime;
			if( diff >= STANDBYTIMEOUT && !dialogopen  ){
				setstandbymode(true);
			} else {
				_handler.postDelayed(this, diff > STANDBYTIMEOUT23 ? STANDBYTIMEOUT - diff : STANDBYTIMEOUT12);
			}
		}
	};
	
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
                invalidate();
        }
    };
	

	private final OnSharedPreferenceChangeListener preferencelistener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

			pref_units = activity.sharedcontext.getpref("units");
			pref_maxspeed = activity.sharedcontext.getpref("maxdisplayspeed");
			pref_scaletextsize = activity.sharedcontext.getpref("scaletextsize") * 0.01f * (verticalorientation ? 0.085f : 0.07f);

			speed2angle = (verticalorientation ? ANGLE_MAX_V + Math.abs(ANGLE_MIN_V) : ANGLE_MAX_H + Math.abs(ANGLE_MIN_H)) / (float)pref_maxspeed;
			meters2displayunits = pref_units == 0 ? 3.6f : 3.6f * (1 / 1.609344f);

			pref_needlemovement = activity.sharedcontext.getpref("needlemovement");

			if( key != null && (key.startsWith("speedalert")  || key.equals("speedalertprofile")) ){
				activity.sharedcontext.loadspeedalerts(null);
			}

			if( _face != null ){
				_face.recycle();
				_face = createface();

				handPaint.setColor(prefs.getInt("color_hand", 0xffffff8f));
				setBackgroundColor(prefs.getInt("color_containerbg", 0xff000000));
			}
		}
		  
	};
    
	private boolean verticalorientation;
	private int anglemin;
	private int anglemax;
	private int anglemax1;
	private float speed2angle;
	public int pref_maxspeed;
	public int pref_units;
	public float pref_scaletextsize;
	private float meters2displayunits;
	private Location prevlocation;
	private SharedPreferences prefs;

	public SpeedometerView(Context context) {
		super(context);
		
		activity = ((MainActivity)this.getContext());

		Point p = new Point();
		activity.getWindowManager().getDefaultDisplay().getSize(p);
		verticalorientation = p.y >= p.x;

		prefs = PreferenceManager.getDefaultSharedPreferences(activity);

		prefs.registerOnSharedPreferenceChangeListener(preferencelistener);
		preferencelistener.onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(activity), null);

		if( Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !prefs.getBoolean("hwaccel", true) ) {
			setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}

		anglemin = verticalorientation ? ANGLE_MIN_V : ANGLE_MIN_H;  
		anglemax = verticalorientation ? ANGLE_MAX_V : ANGLE_MAX_H;
		anglemax1 = verticalorientation ? ANGLE_MAX1_V : ANGLE_MAX1_H;
		needleRotation = verticalorientation ? ANGLE_MIN_V : ANGLE_MIN_H; 
		needleTargetRotation = needleRotation;
		
		handPaint = new Paint();
		handPaint.setAntiAlias(true);
		handPaint.setColor(prefs.getInt("color_hand", 0xffffff8f));
		handPaint.setShadowLayer(0.01f, -0.01f, -0.01f, 0x7f000000);
		handPaint.setStyle(Paint.Style.FILL);

		handPath = new Path();
		handPath.moveTo(0.5f, 0.5f + 0.05f);
		handPath.lineTo(0.5f - 0.020f, 0.5f + 0.05f - 0.007f);
		handPath.lineTo(0.5f - 0.005f, 0.5f - 0.43f);
		handPath.lineTo(0.5f + 0.005f, 0.5f - 0.43f);
		handPath.lineTo(0.5f + 0.020f, 0.5f + 0.05f - 0.007f);
		handPath.lineTo(0.5f, 0.5f + 0.05f);
				
		setBackgroundColor(prefs.getInt("color_containerbg", 0xff000000));

		activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		
		savedbrighness = activity.getWindow().getAttributes().screenBrightness;

		standbytextpaint.setColor(Color.WHITE);
		standbytextpaint.setTextSize(15);
		standbytextval = activity.getString(R.string.standbytext);

		setOnClickListener(new OnClickListener() {
		    @Override
			public void onClick(View v) {
		    		if( standbymode ){
		    			setstandbymode(false);
		    		} else {
		    			locationchangedtime = System.currentTimeMillis();
		    			dimscreenmode = !dimscreenmode;
		    			setbrightness(dimscreenmode ? BRIGHTNESS_DIMSCREEN : savedbrighness);
		    		}
		    }
		});
		
		/*new android.os.Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				needleSetSpeed(random.nextInt(pref_maxspeed+20));
				new android.os.Handler().postDelayed(this, 2000);
             }
		}, 1000);*/
		
	}

	   
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w = MeasureSpec.getSize(widthMeasureSpec);
		int h = MeasureSpec.getSize(heightMeasureSpec);
		
		if( verticalorientation ){
			setMeasuredDimension(w, w);
		} else {
			setMeasuredDimension((int)(h * 1.36f), h);
		}
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		_face = createface();
		_scale = w;

		Rect rect = new Rect();
		standbytextpaint.getTextBounds(standbytextval, 0, standbytextval.length(), rect);
		standbytextxpos = this.getWidth() - rect.width() - 10;
		standbytextypos = rect.height();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {

		if( standbymode ){
			canvas.drawText(standbytextval, standbytextxpos, standbytextypos, standbytextpaint);
		}

		canvas.drawBitmap(_face, 0, 0, null);

		canvas.save();

		canvas.scale(_scale, _scale);
		canvas.rotate(needleRotation, 0.5f, 0.5f);
		canvas.drawPath(handPath, handPaint);

		canvas.restore();

		float dist = needleTargetRotation - needleRotation;

		if( !needleConstantVelocity ){
			needleVelocity = (needleVelocity + dist * NEEDLE_ACCEL * needleSmoothVal) * NEEDLE_DAMP;
		}

		if( needleTargetRotation > anglemax && needleRotation >= anglemax1 ) {
			needleRotation = random.nextInt(10) + anglemax1;
		} else {
			if( (needleConstantVelocity || Math.abs(needleVelocity) < .1) && Math.abs(dist) < .1 )
			{
				needleAnim = false;
				return;
			} else {
				needleRotation += needleVelocity;
			}
		}

		_handler.post(runnable);
	}
	
	
	@Override
	public void onLocationChanged(final Location location) {
		if( standbymode ) 
			setstandbymode(false);
		
		locationchangedtime = System.currentTimeMillis();
		
		needleSetSpeed(location.getSpeed() * meters2displayunits);

		if( activity.sharedcontext.speedalerts != null ){
			activity.sharedcontext.speedalerts.Run(location.getSpeed());
		}

		if( activity.sharedcontext.service == null && activity.sharedcontext.odometer.isenabled() ){
			if( prevlocation != null ){
				float dist = location.distanceTo(prevlocation);
				if( location.getAccuracy() > activity.sharedcontext.pref_maxaccuracy || dist < 1 ){

					return;
				}
				activity.sharedcontext.odometer.updatedist(location.distanceTo(prevlocation));
			}
			prevlocation = location;
		}
	}
	
	@Override
	public void onProviderDisabled(String provider) {
		if( provider.equals("gps") )
			needleSetSpeed(0);
	}
	
	@Override
	public void onProviderEnabled(String provider) {
		
	}
	
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		
	}
	
	public void needleSetSpeed(float speed){
		needleTargetRotation = Math.min(speed * speed2angle + anglemin, anglemax1 + 1);

		if( pref_needlemovement == 1 ){
			needleSmoothVal = 1;
			needleConstantVelocity = false;
		} else if( pref_needlemovement == 2 ){
			needleSmoothVal = 1;
			needleVelocity = (needleTargetRotation - needleRotation) / ANIM_FPS / 0.2f;
			needleConstantVelocity = true;
		} else  if( pref_needlemovement == 3 ){
			needleSmoothVal = 1;
			needleVelocity = (needleTargetRotation - needleRotation);
			needleConstantVelocity = true;
		} else {

			float dist = Math.abs(needleTargetRotation - needleRotation);
			needleSmoothVal = dist > SMOOTH_MAXANGLE ? 1 : Math.min(0.8f, dist * SMOOTH_CONST);

			if (dist < 30 && speed > 30) {
				needleVelocity = (needleTargetRotation - needleRotation) / ANIM_FPS * (1000 / 1000);
				needleConstantVelocity = true;
			} else {
				needleConstantVelocity = false;
			}

		}
		if( !needleAnim ) {
    		_handler.post(runnable);
    		needleAnim = true;
    	}
	}
	
	void setbrightness(float val){
		Window window = activity.getWindow();
		WindowManager.LayoutParams layout = window.getAttributes();
		
		layout.screenBrightness = val;
		
		window.setAttributes(layout);
		
	}
	  
	void setstandbymode(boolean enable){
		
		standbymode = enable;
		
		if( enable ){
			if( !prefs.getBoolean("standbymodeenabled", false) ) {
				standbymode = false;

				return;
			}

			this.setKeepScreenOn(false);
			setbrightness(BRIGHTNESS_STANDBY);
			activity.gpssetinterval(STANDBYGPSINTERVAL);
		} else {
			locationchangedtime = System.currentTimeMillis();
			this.setKeepScreenOn(true);
			setbrightness(dimscreenmode ? BRIGHTNESS_DIMSCREEN : savedbrighness);
			activity.gpssetinterval(activity.sharedcontext.pref_gpsinterval);
			_handler.post(runnable1);
		}
	}
	
	
	Bitmap createface(){
		Bitmap bmp;
		
		
		bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bmp);
		c.scale(getWidth(), getWidth());
		
		Paint facepaint = new Paint();
		facepaint.setStyle(Paint.Style.FILL);
		facepaint.setAntiAlias(true);
		facepaint.setColor(prefs.getInt("color_facebg", 0xff646464));
		
		drawScaledOval(c, new RectF(0f, 0f, 1f, 1f), facepaint);
		
		Paint rimpaint = new Paint();
		rimpaint.setStyle(Paint.Style.STROKE);
		rimpaint.setAntiAlias(true);
		
		rimpaint.setColor(prefs.getInt("color_rimshadow", 0xff000000));
		rimpaint.setStrokeWidth(0.030f);
		drawScaledOval(c, new RectF(0.015f, 0.015f, 1 - 0.015f, 1 - 0.015f), rimpaint);
		
		rimpaint.setColor(prefs.getInt("color_riminner", 0xffe6e6e6));
		rimpaint.setStrokeWidth(0.014f);
		drawScaledOval(c, new RectF(0.007f, 0.007f, 1 - 0.007f, 1 - 0.007f), rimpaint);
		
		rimpaint.setColor(prefs.getInt("color_rimouter", 0xff000000));
		rimpaint.setStrokeWidth(0.004f);
		drawScaledOval(c, new RectF(0.002f, 0.002f, 1 - 0.002f, 1 - 0.002f), rimpaint);
		
		if( !verticalorientation ){
			float h = getHeight() / (float)getWidth();
			
			rimpaint.setColor(prefs.getInt("color_rimshadow", 0xff000000));
			rimpaint.setStrokeWidth(0.030f);
			c.drawLine(0.08f, h - 0.015f, 0.93f, h - 0.015f, rimpaint);
			
			rimpaint.setColor(prefs.getInt("color_riminner", 0xffe6e6e6));
			rimpaint.setStrokeWidth(0.010f);
			c.drawLine(0.065f, h - 0.014f, 0.935f, h - 0.014f, rimpaint);
			
			rimpaint.setColor(prefs.getInt("color_rimouter", 0xff000000));
			rimpaint.setStrokeWidth(0.004f);
			c.drawLine(0f, h - 0.008f, 1f, h - 0.008f, rimpaint);
			
			rimpaint.setStyle(Paint.Style.FILL);
			c.drawRect(0, h - 0.008f, 1, 1, rimpaint);
		}
		
		
		Paint scalepaint = new Paint();
		scalepaint.setAntiAlias(true);
		scalepaint.setStyle(Paint.Style.STROKE);
		scalepaint.setColor(prefs.getInt("color_scale1", Color.WHITE));
		scalepaint.setStrokeWidth(0.010f);
		
		Paint scaletextpaint = new Paint();
		scaletextpaint.setAntiAlias(true);
		scaletextpaint.setColor(prefs.getInt("color_scaletext", Color.WHITE));
		scaletextpaint.setTextSize(pref_scaletextsize);
		scaletextpaint.setTypeface(Typeface.SANS_SERIF);
		scaletextpaint.setTextAlign(Paint.Align.CENTER);	
		
		Paint scalepaint1 = new Paint();
		scalepaint1.setAntiAlias(true);
		scalepaint1.setStyle(Paint.Style.STROKE);
		scalepaint1.setColor(prefs.getInt("color_scale2", Color.WHITE));
		scalepaint1.setStrokeWidth(0.0063f);
		
		
		int step = 1;
		int[] steps = {1, 2, 5, 10, 20, 50};
		int s = (int)Math.ceil((float)pref_maxspeed / MAXNICKS);
		for (int i = steps.length - 2; i >= 0;  i--) {
			if( s > steps[i] ){
				step = steps[i + 1];
				break;
			}
		}
		
		float totaldegrees = verticalorientation ? Math.abs(ANGLE_MIN_V) + ANGLE_MAX_V : Math.abs(ANGLE_MIN_H) + ANGLE_MAX_H;
		int totalnicks = pref_maxspeed / step + 1;
		float degreespernick = totaldegrees / (totalnicks - 1);
		
		
		c.save();
		c.rotate((totaldegrees / 2) * -1, 0.5f, 0.5f);
		
		Rect rect = new Rect();
		Paint paint = new Paint();
		paint.setTextSize(pref_scaletextsize * 10000);
		paint.setTypeface(Typeface.SANS_SERIF);
		paint.setTextScaleX(0.9f);
		paint.setTextAlign(Paint.Align.CENTER);
		
		int j=0;
		for (int i = 0; i < totalnicks; i++) {
			c.drawLine(0.5f, 0.116f, 0.5f, 0.035f, scalepaint);
			
			String str = Integer.toString(j);
			paint.getTextBounds(str, 0, str.length(), rect);
			
			c.save();
			
			c.rotate(totaldegrees / 2 - i * degreespernick, 0.5f, 0.12f + rect.height() / 10000f + 0.025f - (rect.height() / 10000f / 2));
			
			drawScaledText(c, str, 0.5f, 0.12f + rect.height() / 10000f + 0.025f, scaletextpaint);
			c.restore();
			j += step;
			c.rotate(degreespernick, 0.5f, 0.5f);
			
		}
		c.restore();		
		
		c.save();
		if( step == 5 ){
				c.rotate((totaldegrees / 2) * -1, 0.5f, 0.5f);
				for (int i = 0; i < pref_maxspeed; i++) {
					c.drawLine(0.5f, 0.090f, 0.5f, 0.035f, scalepaint1);
					c.rotate(totaldegrees / (pref_maxspeed), 0.5f, 0.5f);
				}
		} else if ( step > 1 ){
			c.rotate((totaldegrees / 2 - degreespernick / 2) * -1, 0.5f, 0.5f);
			for (int i = 0; i < totalnicks - 1; i++) {
				c.drawLine(0.5f, 0.102f, 0.5f, 0.035f, scalepaint1);
				c.rotate(degreespernick, 0.5f, 0.5f);
			}
			if( step == 20 ){
				c.rotate((totaldegrees + degreespernick * 0.25f) * -1, 0.5f, 0.5f);
				for (int i = 0; i < totalnicks * 2 - 2; i++) {
					c.drawLine(0.5f, 0.052f, 0.5f, 0.035f, scalepaint1);
					c.rotate(degreespernick / 2, 0.5f, 0.5f);
				}
			}
			
		}
		c.restore();
		
		Paint centerpaint = new Paint();
		centerpaint.setAntiAlias(true);
		centerpaint.setDither(true);
		centerpaint.setShader(new RadialGradient(0.5f, 0.5f, 0.053f, prefs.getInt("color_center1", Color.WHITE), prefs.getInt("color_center2", Color.BLACK), TileMode.CLAMP));
		c.drawCircle(0.5f, 0.5f, 0.053f, centerpaint);
		
		Paint unitstextpaint = new Paint();
		unitstextpaint.setTextSize(0.05f);
		unitstextpaint.setTextAlign(Paint.Align.CENTER);
		unitstextpaint.setAntiAlias(true);
		unitstextpaint.setColor(prefs.getInt("color_unitstext", Color.WHITE));
		drawScaledText(c, pref_units == 0 ? "km/h" : "MPH", 0.5f, verticalorientation ? 0.7f : 0.65f, unitstextpaint);
		
		return bmp;
	}
	
	public static void drawScaledText(Canvas canvas, String text, float x, float y, Paint paint) {
		float scale = 1;
		
	    float originalStrokeWidth = paint.getStrokeWidth();
	    float originalTextSize = paint.getTextSize();
	    float textScaling = 10f/originalTextSize;
	    paint.setStrokeWidth(originalStrokeWidth * textScaling);
	    paint.setTextSize(originalTextSize * textScaling);
	    canvas.save();
	    canvas.scale(scale/textScaling, scale/textScaling);
	    canvas.drawText(text, x * textScaling, y * textScaling, paint);
	    canvas.restore();
	    paint.setStrokeWidth(originalStrokeWidth);
	    paint.setTextSize(originalTextSize);
	}
	
	public static void drawScaledOval(Canvas canvas, RectF oval, Paint paint) {
		float scale = 100;
		
		float strokewidth = paint.getStrokeWidth();
		paint.setStrokeWidth(strokewidth * scale);
	    
		canvas.save();
	    canvas.scale(1f / scale, 1f / scale);
	    canvas.drawOval(new RectF(oval.left * scale, oval.top * scale, oval.right * scale, oval.bottom * scale), paint);
	    canvas.restore();
	    
	    paint.setStrokeWidth(strokewidth);
	}

	public void onresume(){
		if( standbymode )
			setstandbymode(false);
	}
	
	public void onpause(){
		_handler.removeCallbacks(runnable1);
	}
}





class InfoView extends FrameLayout {
	MainActivity activity;
	SpeedometerContext sharedcontext;
	
	boolean pathmode = false;
	
	TextView texttime;
	TextView textdist;
	
	public int height;
	
	int deftextcolor;

	String accuracymessage = "";

	Handler displayupdatehandler = new Handler(Looper.getMainLooper());
	private Runnable displayupdate = new Runnable() { @Override
	public void run() {
		
		if( sharedcontext.service != null ){
			String speedstats = "";
			if( sharedcontext.pref_showspeedstats ){
				if( pathmode ){
					float timeval = sharedcontext.pref_showmovementspeed ? sharedcontext.service.current_msmoving : System.currentTimeMillis() - sharedcontext.service.current_starttime;
					speedstats = String.format("  %s\u200a|\u200a%s\u200a%s", timeval == 0 ? 0 : Math.round((sharedcontext.service.current_distance / (timeval / 1000f)) * sharedcontext.convertspeedunits), Math.round(sharedcontext.service.current_maxspeed * sharedcontext.convertspeedunits), sharedcontext.speedunits);
				} else {
					float timeval = sharedcontext.pref_showmovementspeed ? sharedcontext.service.total_msmoving : System.currentTimeMillis() - sharedcontext.service.total_starttime;
					speedstats = String.format("  %s\u200a|\u200a%s\u200a%s", timeval == 0 ? 0 : Math.round((sharedcontext.service.total_distance / (timeval / 1000f)) * sharedcontext.convertspeedunits), Math.round(sharedcontext.service.total_maxspeed * sharedcontext.convertspeedunits), sharedcontext.speedunits);
				}
			}
			if( sharedcontext.service.accuracy > sharedcontext.pref_maxaccuracy ){
				speedstats += String.format(accuracymessage, Math.round(sharedcontext.service.accuracy));
			}
			texttime.setText(sharedcontext.formattime((int)(sharedcontext.pref_showmovementspeed ? (pathmode ? sharedcontext.service.current_msmoving : sharedcontext.service.total_msmoving) : System.currentTimeMillis() - (pathmode ? sharedcontext.service.current_starttime : sharedcontext.service.total_starttime))));
			textdist.setText(sharedcontext.formatdistance(pathmode ? sharedcontext.service.current_distance : sharedcontext.service.total_distance) + speedstats);
		}
		
		displayupdatehandler.postDelayed(this, 1000);
	}};
	
	public InfoView(Context context) {
		super(context);
		
		activity = (MainActivity)context;
		sharedcontext = (SpeedometerContext)activity.getApplication();
		
		View.inflate(context, R.layout.infoview, this);
		
		texttime = findViewById(R.id.textTravelTime);
		textdist = findViewById(R.id.textDist);
		
		deftextcolor = texttime.getCurrentTextColor();
		
		View infoviewlayout = findViewById(R.id.statsview);
		Button buttonrec = findViewById(R.id.buttonRec);
		Button buttonset = findViewById(R.id.buttonSet);
		
		DisplayMetrics displaymetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int buttonwidth = displaymetrics.widthPixels / 3 / 2 + 4;
		android.view.ViewGroup.LayoutParams params = buttonrec.getLayoutParams();
		params.width = buttonwidth;
		buttonrec.setLayoutParams(params);
		params = buttonset.getLayoutParams();
		params.width = buttonwidth;
		buttonset.setLayoutParams(params);

		accuracymessage = context.getString(R.string.tripmeter_accuracywarning);
		
		buttonrec.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if( sharedcontext.service == null ){
					if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ){
						activity.startForegroundService(new Intent(activity, SpeedometerService.class));
					} else {
						activity.startService(new Intent(activity, SpeedometerService.class));
					}

					displayupdatehandler.post(displayupdate);
					setpathmode(false);
					setbuttonicons(true);
				} else {
					activity.stopService(new Intent(activity, SpeedometerService.class));
					texttime.setText("");
					textdist.setText("");
					displayupdatehandler.removeCallbacks(displayupdate);
					setbuttonicons(false);
				}

			}
        });
		
		buttonset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if( sharedcontext.service != null ){ 
					setpathmode(true);
					sharedcontext.service.setpoint();
				} else {
					Toast.makeText(getContext(), R.string.message_buttontip, Toast.LENGTH_LONG).show();
				}
			}
        });
		
		
		infoviewlayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(activity, TrackListActivity.class);
				if( sharedcontext.service != null ){
					intent.putExtra("tripfilename", sharedcontext.service.tripstats.filename);
				}
				
				activity.startActivity(intent);

			}
        });


		infoviewlayout.setOnTouchListener(new OnTouchListener() {
			VelocityTracker velocitytracker = VelocityTracker.obtain();
			
			@Override
			public boolean onTouch(View v, MotionEvent motionevent) {
				switch (motionevent.getAction()) {
				case MotionEvent.ACTION_DOWN:
					velocitytracker.addMovement(motionevent);
					break;
				case MotionEvent.ACTION_MOVE:
					velocitytracker.addMovement(motionevent);
					break;
				case MotionEvent.ACTION_UP:
					velocitytracker.addMovement(motionevent);
					velocitytracker.computeCurrentVelocity(1000);
					if( Math.abs(velocitytracker.getXVelocity()) > 500 ){
						setpathmode(!pathmode);
						return true;
					}
					velocitytracker.clear();
					break;
			}
				
				
				return false;
			}
        });

		buttonset.setLongClickable(true);
		buttonset.setOnLongClickListener(new OnLongClickListener(){

			@Override
			public boolean onLongClick(View v) {
				getContext().startActivity(new Intent(getContext(), SpeedometerSettings.class)
						.putExtra("showautopathscreen", true));

				return true;
			}
		});
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.getMode(heightMeasureSpec)));
		
		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
	}
	
	public void onresume(){
		if( sharedcontext.service != null ){ 
			displayupdatehandler.post(displayupdate);
		}
		setbuttonicons();
	}
	
	public void onpause(){
		displayupdatehandler.removeCallbacks(displayupdate);
	}
	
	void setpathmode(boolean enable){
		pathmode = enable;
		
		texttime.setTextColor(pathmode ? Color.YELLOW : deftextcolor);
		textdist.setTextColor(pathmode ? Color.YELLOW : deftextcolor);
		
		if( sharedcontext.service != null ){
			displayupdatehandler.removeCallbacks(displayupdate);
			displayupdatehandler.post(displayupdate);
		}
	}

	public void setbuttonicons(){
		setbuttonicons(sharedcontext.service != null);
	}

	public void setbuttonicons(boolean started){
		if( sharedcontext.pref_showbuttonicons ){
			((Button)findViewById(R.id.buttonRec)).setText(activity.getString(started ? R.string.iconrec2 : R.string.iconrec1));
			((Button)findViewById(R.id.buttonSet)).setText(activity.getString(started ? R.string.iconset2 : R.string.iconset1));
		} else {
			((Button)findViewById(R.id.buttonRec)).setText("");
			((Button)findViewById(R.id.buttonSet)).setText("");
		}
	}

}




