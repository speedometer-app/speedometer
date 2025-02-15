package com.gmail.at.ixevixe.speedometertripmeter;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class AppRater
{
    private final static int DAYS = 3;
    private final static int LAUNCHES = 3;
	
    public static void applaunched(Context context, SharedPreferences prefs){
        
        if( prefs.getBoolean("apprater_dontshow", false) ){ 
			return; 
		}

        SharedPreferences.Editor e = prefs.edit();

        long launchcount = prefs.getLong("apprater_launchcount", 0) + 1;
        e.putLong("apprater_launchcount", launchcount);

        Long prompttime = prefs.getLong("apprater_prompttime", 0);
        if (prompttime == 0) {
            prompttime = System.currentTimeMillis() + DAYS * 24 * 3600 * 1000;
            e.putLong("apprater_prompttime", prompttime);
        }

        if( launchcount >= LAUNCHES && System.currentTimeMillis() >= prompttime ){
			showRateDialog(context, e);
        }

        e.commit();
    }   

    public static void showRateDialog(final Context context, final SharedPreferences.Editor editor) {
        final Dialog dialog = new Dialog(context);
        
        dialog.setOnCancelListener( new DialogInterface.OnCancelListener(){
			
			public void onCancel(DialogInterface arg0) {
				editor.putLong("apprater_prompttime", System.currentTimeMillis() + DAYS * 24 * 3600 * 1000);
            	editor.commit();
			}
        });
        
        
        dialog.setTitle(" ");

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        TextView tv = new TextView(context);
        tv.setText(R.string.ratedialog1);
        tv.setWidth(240);
        tv.setPadding(10, 10, 10, 10);
        ll.addView(tv);

        Button button = new Button(context);
        button.setBackgroundColor(0xFFFFFF00);
        button.setTextColor(0xFF000000);
        button.setText(R.string.ratedialog2);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	
            	try {
            		context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.getPackageName())));
        		} catch (Exception e) { 
        			Toast.makeText(context, "google play error", Toast.LENGTH_LONG).show();
        		}
                
            	editor.putBoolean("apprater_dontshow", true);
				editor.commit();
            	
            	dialog.dismiss();
            }
        });        
        ll.addView(button);

        button = new Button(context);
        button.setBackgroundColor(0xFFFFFFFF);
        button.setTextColor(0xFF7F7F7F);
        button.setText(R.string.ratedialog3);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
			
				editor.putBoolean("apprater_dontshow", true);
				editor.commit();
				
                dialog.dismiss();
            }
        });
        ll.addView(button);

        button = new Button(context);
        button.setBackgroundColor(0xFFFFFFFF);
        button.setTextColor(0xFF7F7F7F);
        button.setText(R.string.ratedialog4);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	
                dialog.cancel();
                
            }
        });
        ll.addView(button);
        
        dialog.setContentView(ll);        
        dialog.show();        
    }


}