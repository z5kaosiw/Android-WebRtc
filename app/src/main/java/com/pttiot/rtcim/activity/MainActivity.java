package com.pttiot.rtcim.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.pttiot.rtcim.R;
import com.pttiot.rtcim.core.RtcSignalClient;
import com.pttiot.rtcim.core.parm.MediaType;
import com.pttiot.rtcim.core.parm.PeerConfiger;
import com.pttiot.rtcim.eventbus.IoConnectedEvent;
import com.pttiot.rtcim.eventbus.IoErrorEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import pub.devrel.easypermissions.EasyPermissions;

/**
 *  * 免费IM（含单聊，群聊，聊天室），免费一对一视频聊天（回音消除），语音聊天，直播连麦，白板，小班课，
 *  * 多人会议，局域网无服务器直连，兼容webRTC, 支持webRTC加速，P2P高清传输，安卓、iOS、web互通，支持门禁可视对讲，
 *  * 电视盒子，树莓派，海思，全志，任天堂switch，云游戏，OTT设备，C语言自研方案
 *
 * **/
public class MainActivity extends AppCompatActivity {


    private EditText roomEditText;
    private TextView statuTv;
    private boolean conect;

    public final  static String ROOM_CHAT_ID ="10000";
    public final  static String ROOM_SIGLE_AUDIO_ID ="20000";
    public final  static String ROOM_SIGLE_VIDEO_ID ="30000";
    public final  static String ROOM_GROUP_AUDIO_ID ="40000";
    public final  static String ROOM_GROUP_VIDEO_ID ="50000";
    public final  static String ROOM_PTT_ID ="60000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        roomEditText = (EditText) findViewById(R.id.RoomEditText);
        statuTv = (TextView) findViewById(R.id.statuTv);

        String[] perms = { Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (! EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "Need permissions for camera & microphone", 0, perms);
        }

//        CrashHandler.getInstance().init(getApplicationContext());
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoConnectedEvent messageEvent) {

        switch (messageEvent.type)
        {
            case  1:
                logcatOnUI("Signal Server Connecting !");
                break;
            case  2:
                logcatOnUI("Signal Server Connected !");
                break;
            case  3:
                logcatOnUI("Signal Server Disconnected!");
                break;

        }

    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoErrorEvent messageEvent) {
        logcatOnUI("ERROR:"+messageEvent);
    }

    private void logcatOnUI(String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                statuTv.setText(s);
            }
        });

    }





    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    public void contectSignal(View v)
    {
        String ipAddre = roomEditText.getText().toString();
        if(TextUtils.isEmpty(ipAddre))
        {
            logcatOnUI("请输入服务端地址");
            return;
        }
        if(ipAddre.equals("默认"))
        {
            conect = RtcSignalClient.getInstance().conect(getString(R.string.default_server));
        }else
        {
            conect = RtcSignalClient.getInstance().conect(ipAddre);

        }

        if (conect)
        {
            logcatOnUI("连接中");
        }else
        {
            logcatOnUI("连接异常");
        }
    }
    public void JoinChatRoomClick(View v)
    {
           if (!conect){
              return;
            }
            Intent intent = new Intent(MainActivity.this, ChatRoomActivity.class);
            intent.putExtra("RoomName", ROOM_CHAT_ID);
            startActivity(intent);

    }
    public void JoinAudioRoomClick(View v)
    {
            if (!conect){
              return;
           }
            Intent intent = new Intent(MainActivity.this, AudioCallActivity.class);
            intent.putExtra("RoomName", ROOM_SIGLE_AUDIO_ID);
            startActivity(intent);
    }
    public void JoinRoomClick(View v)
    {
            if (!conect){
                return;
             }
            Intent intent = new Intent (MainActivity.this, VideoCallActivity.class);
            intent.putExtra("RoomName", ROOM_SIGLE_VIDEO_ID);
            startActivity(intent);
    }

    public void enterRoom(View view) {


    }

    public void exitRoom(View view) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        RtcSignalClient.getInstance().disconnect();
    }


    public void groupChatAudio(View view) {
        if (!conect){
            return;
        }
        Intent intent =new Intent(this,GroupChatRoomActivity.class);
        intent.putExtra(PeerConfiger.MEDIA_TYPE, MediaType.TYPE_AUDIO);
        intent.putExtra(PeerConfiger.ROOMID,ROOM_GROUP_AUDIO_ID);
        startActivity(intent);
    }

    public void groupChaVideo(View view) {
        if (!conect){
            return;
        }

        Intent intent =new Intent(this,GroupChatRoomActivity.class);
        intent.putExtra(PeerConfiger.MEDIA_TYPE, MediaType.TYPE_VIDEO);
        intent.putExtra(PeerConfiger.ROOMID, ROOM_GROUP_VIDEO_ID);
        startActivity(intent);
    }

    public void channel1IntoOnclik(View view) {
        //频道1(P2P半双工)
        if (!conect){
            return;
        }
        Intent intent =new Intent(this,PocActivity.class);
        intent.putExtra(PeerConfiger.ROOMID, ROOM_PTT_ID);
        startActivity(intent);



    }

    public void channel2IntoOnclik(View view) {
        //频道2(P2P半双工)
        if (!conect){
            return;
        }

    }
}
