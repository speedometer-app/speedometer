package com.gmail.at.ixevixe.speedometertripmeter.speedalerts;

import android.media.Ringtone;
import com.gmail.at.ixevixe.speedometertripmeter.SpeedometerContext;

public class SpeedAlert {
    public static int STATE_NORMAL = 0;
    public static int STATE_PENDING = 1;
    public static int STATE_ALERTING = 2;

    private final SpeedometerContext g;
    final int speed1;
    private final int colorpreset1;
    private final String sound1;
    private final int colorpreset2;
    private final String sound2;
    private final int triggerdelay;
    private final int repeatinterval;
    private final boolean above;

    int state = 0;
    long triggertime = 0;
    long repeattime = 0;

    Ringtone ringtone1;
    Ringtone ringtone2;

    public SpeedAlert(SpeedometerContext g, int speed1, int colorpreset1, String sound1, int colorpreset2, String sound2, int triggerdelay, int repeatinterval, boolean above) {
        this.g = g;
        this.speed1 = speed1;
        this.colorpreset1 = colorpreset1;
        this.sound1 = sound1;
        this.colorpreset2 = colorpreset2;
        this.sound2 = sound2;
        this.triggerdelay = triggerdelay;
        this.repeatinterval = repeatinterval;
        this.above = above;

        if( sound1 != null ) {
            ringtone1 = g.loadringtone(g, sound1);
        }
        if( sound2 != null ) {
            ringtone2 = g.loadringtone(g, sound2);
        }
    }

    public boolean Run(float speed, boolean applyactions){
        if( (above && speed > this.speed1) || (!above && speed > 0 && speed < this.speed1) ){

            if( state != STATE_ALERTING ){
                if( triggerdelay > 0 && state != STATE_PENDING ){
                    state = STATE_PENDING;
                    triggertime = System.currentTimeMillis() + triggerdelay;
                } else if( triggerdelay == 0 || System.currentTimeMillis() > triggertime ){
                    state = STATE_ALERTING;
                    if( applyactions ) {
                        if (colorpreset1 > 0) {
                            g.loadcolorpreset(colorpreset1);
                        }
                        if (ringtone1 != null) {
                            playnotification(ringtone1);
                            if (repeatinterval > 0) {
                                repeattime = System.currentTimeMillis() + repeatinterval;
                            }
                        }
                    }

                    return true;
                }
            } else {
                if( applyactions ) {
                    if (repeatinterval > 0 && ringtone1 != null && System.currentTimeMillis() > repeattime) {
                        playnotification(ringtone1);
                        repeattime = System.currentTimeMillis() + repeatinterval;
                    }
                }
            }

        } else {

            if( state != STATE_NORMAL ){
                if( state == STATE_ALERTING && applyactions ) {
                    if (colorpreset2 > 0) {
                        g.loadcolorpreset(colorpreset2);
                    }
                    playnotification(ringtone2);
                }
                state = STATE_NORMAL;
            }

        }

        return false;
    }

    void playnotification(Ringtone ringtone){
        if( ringtone1 != null ){
            ringtone1.stop();
        }
        if( ringtone2 != null ){
            ringtone2.stop();
        }

        if( ringtone != null ){
            ringtone.play();
        }
    }

    public void clear(){
        if( ringtone1 != null ){
            ringtone1.stop();
        }
        if( ringtone2 != null ){
            ringtone2.stop();
        }
        ringtone1 = null;
        ringtone2 = null;
    }

}