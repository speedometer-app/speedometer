package com.gmail.at.ixevixe.speedometertripmeter;

import java.io.Serializable;
import java.util.Date;

public class TripPath implements Serializable
{
	private static final long serialVersionUID = 2056528243184644259L;
	
	TripStats owner;
	
	String title = "";
	Date created = new Date();
	int traveltime = 0;
	float distance = 0;
	
	TripPath(String title, TripStats owner){
		this.title = title;
		this.owner = owner;
	}
}