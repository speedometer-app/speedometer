package com.gmail.at.ixevixe.speedometertripmeter;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

public class SpeedometerService extends Service
{

	boolean servicestarted = false;

	TripStats tripstats;

	String tripfilename;

	long total_starttime = 0;
	float total_distance = 0;
	long current_starttime = 0;
	float current_distance = 0;

	float total_maxspeed = 0;
	float current_maxspeed = 0;
	long total_msmoving = 0;
	long current_msmoving = 0;

	float accuracy = 0;

	private SpeedometerContext sharedcontext;
	private Location prevlocation;

	private LocationListener locationlistener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			if( prevlocation != null ) {

				//if( location.getAccuracy() > 10 ) return;

				accuracy = location.getAccuracy();
				if( accuracy > sharedcontext.pref_maxaccuracy ){

					return;
				}

				float dist = location.distanceTo(prevlocation);

				if( dist < 1 ) return;

				total_distance += dist;
				current_distance += dist;

				float speed = location.getSpeed();
				if( speed > total_maxspeed ){
					total_maxspeed = speed;
					current_maxspeed = speed;
				} else if( speed > current_maxspeed ){
					current_maxspeed = speed;
				}

				if( speed > sharedcontext.pref_minspeed ) {
					long timediff = location.getTime() - prevlocation.getTime();
					if (timediff > sharedcontext.pref_maxtimediff) {
						timediff = sharedcontext.pref_maxtimediff;
					}
					total_msmoving += timediff;
					current_msmoving += timediff;
				}

				if( (sharedcontext.pref_autopathtime > 0 && ((sharedcontext.pref_showmovementtime && current_msmoving > sharedcontext.pref_autopathtime) || (!sharedcontext.pref_showmovementtime && (System.currentTimeMillis() - current_starttime) > sharedcontext.pref_autopathtime)))
						|| (sharedcontext.pref_autopathdist > 0 && current_distance > sharedcontext.pref_autopathdist) ){
					setpoint();
					sharedcontext.playringtone(sharedcontext.autopathringtone);
				}

				sharedcontext.odometer.updatedist(dist);
			}

			prevlocation = location;
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

	};


	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if( servicestarted ) return START_NOT_STICKY;
		servicestarted = true;

	    sharedcontext = (SpeedometerContext)getApplication();

	    sharedcontext.service = this;

	    total_starttime = System.currentTimeMillis();
	    current_starttime = total_starttime;

	    tripstats = new TripStats("");
	    tripstats.paths.add(new TripPath("", tripstats));


	    //Notification notification = new Notification(R.drawable.ic_launcher, getText(R.string.app_name), System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, MainActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
		Notification.Builder notificationbuilder = null;
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ){
			NotificationChannel channel = new NotificationChannel("SpeedometerService", getString(R.string.servicenotification1), NotificationManager.IMPORTANCE_DEFAULT);
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);
			notificationbuilder = new Notification.Builder(this, "SpeedometerService");
		} else {
			notificationbuilder = new Notification.Builder(this);
		}
		notificationbuilder.setOngoing(true)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(getText(R.string.servicenotification1))
				.setContentText(getText(R.string.servicenotification2))
				.setContentIntent(pendingIntent);
		//notification.setLatestEventInfo(this, getText(R.string.servicenotification1), getText(R.string.servicenotification2), pendingIntent);
		startForeground(1, notificationbuilder.getNotification());

		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ){

			Toast.makeText(getApplicationContext(), getString(R.string.servicegppermission), Toast.LENGTH_LONG).show();

			stopSelf();

			return START_NOT_STICKY;
		}

		final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
		if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ){

			Toast.makeText(getApplicationContext(), getString(R.string.servicegpsnotenabled), Toast.LENGTH_LONG).show();

			stopSelf();

			return START_NOT_STICKY;
		}

		((LocationManager)getSystemService(LOCATION_SERVICE)).requestLocationUpdates(LocationManager.GPS_PROVIDER, sharedcontext.pref_gpsinterval, 0, locationlistener);
		
	    return START_NOT_STICKY;
	}

	@Override
	public void onDestroy()
	{
		((LocationManager)getSystemService(LOCATION_SERVICE)).removeUpdates(locationlistener);
		sharedcontext.service = null;
		writestats();
	} 
	
	@Override
	public IBinder onBind(Intent arg0)
	{
	    return null;
	}
	
	
	public void setpoint(){
		writestats();
		
		current_starttime = System.currentTimeMillis();
		current_distance = 0;

		current_maxspeed = 0;

		current_msmoving = 0;

		tripstats.paths.add(new TripPath("", tripstats));
	}
	
	public void writestats(){
		tripstats.traveltime = (int)(System.currentTimeMillis() - total_starttime);
		tripstats.distance = total_distance;
		
		TripPath currentpath = tripstats.paths.get(tripstats.paths.size() - 1);
		currentpath.traveltime = (int)(System.currentTimeMillis() - current_starttime);
		currentpath.distance = current_distance;

		if( total_msmoving > tripstats.traveltime ){
			total_msmoving = tripstats.traveltime;
		}
		if( current_msmoving > currentpath.traveltime ){
			current_msmoving = currentpath.traveltime ;
		}
		tripstats.title = sharedcontext.setallparts(tripstats.title, String.format(Locale.US, "%.2f", total_maxspeed), Long.toString(total_msmoving));
		currentpath.title = sharedcontext.setallparts(currentpath.title, String.format(Locale.US, "%.2f", current_maxspeed), Long.toString(current_msmoving));

		tripstats.write(this);
	    
	}
	
	public void reloadstats(){
		tripstats = TripStats.load(this, tripstats.filename);
	}
	

}