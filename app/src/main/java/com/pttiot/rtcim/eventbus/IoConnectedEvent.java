package com.pttiot.rtcim.eventbus;

/**
 * Created by gjy on 2019/5/5.
 */

public class IoConnectedEvent {

    /*
    *    1:connecting
    *    2:connected
    *    3:diaconnect
    *
    * **/
    public   int   type     ;

    public IoConnectedEvent ( int type ) {
        this.type = type;
    }
}
