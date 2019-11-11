package com.pttiot.rtcim.bean;

import java.util.List;

public class OnlineUser {

   private List<String> currentgroupOnlineUsers ;

    public OnlineUser(List<String> currentgroupOnlineUsers) {
        this.currentgroupOnlineUsers = currentgroupOnlineUsers;
    }

    public List<String> getCurrentgroupOnlineUsers() {
        return currentgroupOnlineUsers;
    }

    public void setCurrentgroupOnlineUsers(List<String> currentgroupOnlineUsers) {
        this.currentgroupOnlineUsers = currentgroupOnlineUsers;
    }

    @Override
    public String toString() {
        return "OnlineUser{" +
                "currentgroupOnlineUsers=" + currentgroupOnlineUsers +
                '}';
    }
}
