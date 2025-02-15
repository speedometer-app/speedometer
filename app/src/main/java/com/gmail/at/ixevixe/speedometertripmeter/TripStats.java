package com.gmail.at.ixevixe.speedometertripmeter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.Service;
import android.content.Context;

public class TripStats implements Serializable 
{
	private static final long serialVersionUID = -5209140152281672745L;
	
	String filename = System.currentTimeMillis() + ".trip";
	
	String title = "";
	Date created = new Date();
	int traveltime = 0;
	float distance = 0;
	ArrayList<TripPath> paths = new ArrayList<TripPath>();
	
	TripStats(String title){
		this.title = title;
	}
	
	public void write(Activity context) {
		try {
			write((Context)context);
		} catch (Exception e) {
			 ((SpeedometerContext)context.getApplication()).errexit(context, e);
		}
	}
	
	public void write(Service context) {
		try {
			write((Context)context);
		} catch (Exception e) {
			 ((SpeedometerContext)context.getApplication()).errexitservice(context, e);
		}
	}
	
	public void write(Context context) throws Exception{
		FileOutputStream fos;
		fos = context.openFileOutput(this.filename, Context.MODE_PRIVATE);
		ObjectOutputStream os = new ObjectOutputStream(fos);
		os.writeObject(this);
		os.close();
		fos.close();
	}
	
	public void removepath(int index){
		paths.get(index - 1).traveltime += paths.get(index).traveltime;
		paths.get(index - 1).distance += paths.get(index).distance;
		paths.remove(index);
	}
	
	public static TripStats load(Context context, String filename){
		TripStats tripstats = null;
		
		try {
	        FileInputStream fis = context.openFileInput(filename);
	        ObjectInputStream is = new ObjectInputStream(fis);
	        tripstats = (TripStats) is.readObject();
	        is.close();
	        fis.close();
	    } catch (Exception e) {}
		
		return tripstats;
		
		
	}
}