package com.pttiot.rtcim.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.pttiot.rtcim.R;
import com.pttiot.rtcim.core.RtcSignalClient;
import com.pttiot.rtcim.core.SignalClient;
import com.pttiot.rtcim.core.parm.SignalType;
import com.pttiot.rtcim.eventbus.ChatEvent;
import com.pttiot.rtcim.eventbus.IoJoinRoomEvent;
import com.pttiot.rtcim.eventbus.IoLeaveRoomEvent;
import com.pttiot.rtcim.eventbus.IoMesageEvent;
import com.pttiot.rtcim.eventbus.IoOtherJoinEvent;
import com.pttiot.rtcim.eventbus.IoOtherLeaveEvent;
import com.pttiot.rtcim.eventbus.IoRoomFullEvent;
import com.pttiot.rtcim.util.LogUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnectionFactory;

public class ChatRoomActivity extends AppCompatActivity {

    private EditText msgSendTextEt ;
    private TextView chatMsgTv ;
    private TextView rommIdTv ;
    private RtcSignalClient signalClient;
    private String roomName;
    private String _myId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);
        EventBus.getDefault().register(this);
        signalClient = RtcSignalClient.getInstance();

        chatMsgTv   = (TextView) findViewById(R.id .msgTv);
        msgSendTextEt = (EditText) findViewById(R.id .sendMsg);
        rommIdTv = (TextView) findViewById(R.id.rommID);

        Intent intent = getIntent();
        roomName = getIntent().getStringExtra("RoomName");
        signalClient.joinRoom(roomName,SignalType.CHAT_COMMAND);
        rommIdTv.append(roomName);

    }

    public  void  sendMsg(View v)
    {
        String msg = msgSendTextEt.getText().toString().trim();
        if(TextUtils.isEmpty(msg))
        {
            return;
        }
        StringBuffer buffer =new StringBuffer();
        buffer.append(_myId)
                .append(" : ")
                .append(msg)
                .append("\r\n");

        try {
            JSONObject object =new JSONObject();
            object.put("user",_myId);
            object.put("msg",msg);
            signalClient.sendMessage(object);
            chatMsgTv.append(buffer.toString());
            msgSendTextEt.setText("");

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtils.e("离开聊天室");
        signalClient.leaveRoom(Integer.parseInt(roomName));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(ChatEvent chatEvent) {

        StringBuffer buffer =new StringBuffer();
        buffer.append(chatEvent.chatId)
                .append(" : ")
                .append(chatEvent.chatMsg)
                .append("\r\n");
        LogUtils.e("->"+buffer.toString());
        chatMsgTv.append(buffer.toString());


    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoJoinRoomEvent messageEvent) {

        _myId = messageEvent.uid;
        chatMsgTv.append("已加入聊天室\n");

    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoLeaveRoomEvent messageEvent) {

        chatMsgTv.append("已的离开聊天室\n");
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoOtherJoinEvent messageEvent) {

        chatMsgTv.append("用户:"+messageEvent.userid+" 加入房间");

    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoOtherLeaveEvent messageEvent) {

        chatMsgTv.append("用户:"+messageEvent.userid+" 离开房间");
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoRoomFullEvent messageEvent) {

        finish();
    }


}
