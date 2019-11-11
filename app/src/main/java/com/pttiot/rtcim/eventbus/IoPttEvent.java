package com.pttiot.rtcim.eventbus;

public class IoPttEvent {

     public  int     status;
     public  String  rid;

    public IoPttEvent(int status, String rid) {
        this.status = status;
        this.rid = rid;
    }
}
