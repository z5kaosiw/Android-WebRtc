package com.pttiot.rtcim.eventbus;

public class ChatEvent {

    public  String  chatId;
    public  String  chatRoom;
    public  String  chatMsg;

    public ChatEvent(String chatId, String chatRoom, String chatMsg) {

        this.chatId = chatId;
        this.chatRoom = chatRoom;
        this.chatMsg = chatMsg;
    }
}
