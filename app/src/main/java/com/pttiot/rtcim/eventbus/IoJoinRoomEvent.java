package com.pttiot.rtcim.eventbus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by gjy on 2019/5/5.
 */

public class IoJoinRoomEvent {

    public String roomid;
    public List<String> users  ;
    public int  roomUserCount ;
    public String  sid ;
    public String  uid ;

    public IoJoinRoomEvent(String roomid, List<String> users, int roomUserCount, String sid, String uid) {
        this.roomid = roomid;
        this.users = users;
        this.roomUserCount = roomUserCount;
        this.sid = sid;
        this.uid = uid;
    }
}
