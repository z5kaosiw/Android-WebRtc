package com.pttiot.rtcim.eventbus;

/**
 * Created by gjy on 2019/5/5.
 */

public class IoLeaveRoomEvent {


    public String roomid;
    public String userid;

    public IoLeaveRoomEvent ( String roomid, String userid ) {
        this.roomid = roomid;
        this.userid = userid;
    }
}
