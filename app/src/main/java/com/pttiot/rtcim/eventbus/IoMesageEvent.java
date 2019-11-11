package com.pttiot.rtcim.eventbus;

import org.json.JSONObject;

/**
 * Created by gjy on 2019/5/5.
 */

public class IoMesageEvent {

    public JSONObject msg ;
    public  String  roomId;

    public IoMesageEvent(JSONObject msg, String roomId) {
        this.msg = msg;
        this.roomId = roomId;
    }

    public void setMsg(JSONObject msg) {
        this.msg = msg;
    }
}
