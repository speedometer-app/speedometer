package com.gmail.at.ixevixe.speedometertripmeter.speedalerts;

import com.gmail.at.ixevixe.speedometertripmeter.SpeedometerContext;

public class Alerts {
    private final SpeedometerContext g;
    private final SpeedAlert alert1;
    private final SpeedAlert alert2;
    private SpeedAlert activealert;

    public Alerts(SpeedometerContext g, SpeedAlert alert1, SpeedAlert alert2) {
        this.g = g;
        this.alert1 = alert1;
        this.alert2 = alert2;
    }

    public void Run(float speed){
        if( alert1.Run(speed, true) ){

            if( activealert != null ){
                activealert.Run(speed, false);
            }
            activealert = alert1;

            return;
        }

        if( alert2.Run(speed, true) ){

            if( activealert != null ){
                activealert.Run(speed, false);
            }
            activealert = alert2;

            return;
        }
    }

    public void clear(){
        alert1.clear();
        alert2.clear();
    }

}

