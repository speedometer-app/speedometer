package com.gmail.at.ixevixe.speedometertripmeter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import static android.text.InputType.TYPE_CLASS_NUMBER;

class Odometer {
    private SpeedometerContext g;
    private double distval;
    private boolean enabled = false;
    private RandomAccessFile raf;
    private final float maxdistperupdate;

    Odometer(SpeedometerContext g, int maxdistperupdate) {
        this.g = g;
        this.maxdistperupdate = maxdistperupdate;
    }

    void updatedist(double val){
        if( !enabled )
            return;

        if( val > maxdistperupdate )
            return;

        distval += val;

        savetofile();
    }

    void setdist(double val){
        if( !enabled )
            return;

        distval = val;

        savetofile();
    }

    double getdist(){
        if( !enabled )
            return -1;

        return distval;
    }

    private void savetofile(){
        try {
            raf.seek(0);
            raf.writeDouble(distval);
        } catch (IOException e) {
            showerror("savetofile");
        }
    }

    private void preparefile(){
        try {
            raf = new RandomAccessFile(new File(g.getFilesDir(), "odometer"), "rw");
            distval = raf.readDouble();
        } catch (FileNotFoundException e) {
            showerror("preparefile");
        } catch (IOException e) {
            distval = 0;
        }
    }

    void setenabled(boolean val){
        if( val == enabled )
            return;

        enabled = val;

        if( enabled ){
            preparefile();
        } else {
            try {
                raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    boolean isenabled(){
        return enabled;
    }

    private void showerror(String msg){
        g.showerror(g, "odometer " + msg);
        setenabled(false);
    }

    void showdistprompt(Activity context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        final EditText input = new EditText(context);
        input.setInputType(TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(getdist()));
        builder.setView(input);

        builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                setdist(Double.parseDouble(input.getText().toString()));
            }
        });

        builder.setNegativeButton(R.string.dialog_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });

        builder.show();
    }
}
