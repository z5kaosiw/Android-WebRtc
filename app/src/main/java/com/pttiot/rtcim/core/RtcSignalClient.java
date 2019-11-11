package com.pttiot.rtcim.core;

import android.os.Build;

import com.pttiot.rtcim.core.parm.SignalType;
import com.pttiot.rtcim.eventbus.ChatEvent;
import com.pttiot.rtcim.eventbus.IoConnectedEvent;
import com.pttiot.rtcim.eventbus.IoErrorEvent;
import com.pttiot.rtcim.eventbus.IoJoinRoomEvent;
import com.pttiot.rtcim.eventbus.IoMesageEvent;
import com.pttiot.rtcim.eventbus.IoOtherJoinEvent;
import com.pttiot.rtcim.eventbus.IoOtherLeaveEvent;
import com.pttiot.rtcim.eventbus.IoPttEvent;
import com.pttiot.rtcim.util.LogUtils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class RtcSignalClient {

    private static final String TAG = "SignalClient";
    private volatile static RtcSignalClient mInstance;

    private Socket mSocket;
    private String mRoomId;
    private String sessionId;

    private RtcSignalClient() {
    }

    public static RtcSignalClient getInstance() {
        if (mInstance == null) {     //1  第一次检查
            synchronized (RtcSignalClient.class) { //2 加锁
                if (mInstance == null) {  //3 第二次检查
                    mInstance = new RtcSignalClient(); //4 初始化
                }
            }
        }
        return mInstance;
    }
    public void sendPttSignal(JSONObject message) {
        LogUtils.e (  "broadcast: " + message);
        if (mSocket == null) {
            return;
        }
        mSocket.emit("ptt", mRoomId, message);
    }
    public void sendMessage(JSONObject message) {
        LogUtils.e (  "broadcast: " + message);
        if (mSocket == null) {
            return;
        }
        mSocket.emit("message", mRoomId, message);
    }
    public void sendRtcSdp(JSONObject message) {

        LogUtils.e (  "sendRtcSdp: " + message);
        if (mSocket == null) {
            return;
        }
        mSocket.emit("rtcsdp", mRoomId, message);
    }
    public void leaveRoom(int rid) {

        LogUtils.e (  "leaveRoom: " + rid);
        if (mSocket == null) {
            return;
        }

        mSocket.emit("leave", rid);
//        mSocket.close();
//        mSocket = null;
    }
    public  boolean  conect(String url)
    {
        try {
            mSocket = IO.socket(url);
            mSocket.connect();
            //list signal
            listenSignalEvents();
            return true;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            LogUtils.e("ERROR=>"+e.toString());
            return false;
        }
    }
    public   void disconnect()
    {
        if (mSocket == null)
            return;
        //释放资源
        mSocket.disconnect();
        mSocket.close();
        mSocket = null;
    }
    /*Room 逻辑***/
    public void joinRoom( String roomName,int command_id) {

        //mUserId = userId;
        mRoomId = roomName;

        try {
            JSONObject message = new JSONObject();
            message.put("uid",  Build.SERIAL);
            message.put("command",command_id /*SignalType.CHAT_COMMAND*/);
            mSocket.emit("join", mRoomId,message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void listenSignalEvents() {

        if (mSocket == null) {
            return;
        }
        mSocket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                LogUtils.e ( "onConnectError: " + args );
                EventBus.getDefault ().post ( new IoErrorEvent() );
            }
        });
        mSocket.on(Socket.EVENT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                LogUtils.e ( "onError: " + args);
                EventBus.getDefault ().post ( new IoErrorEvent () );
            }
        });

        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                sessionId = mSocket.id();
                LogUtils.e ( "onConnected mSocket.id=>"+sessionId);
                EventBus.getDefault ().post ( new IoConnectedEvent(2) );
            }
        });
        mSocket.on(Socket.EVENT_CONNECTING, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LogUtils.e ( "onConnecting");
                EventBus.getDefault ().post ( new IoConnectedEvent (1) );
            }
        });
        mSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LogUtils.e (  "onDisconnected");
                EventBus.getDefault ().post ( new IoConnectedEvent (3) );
            }
        });
        mSocket.on("joined", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String roomName = String.valueOf ( args[0]);
//                String userId = (String) args[1];
                JSONObject msg = (JSONObject) args[1];
                try {
                    String sid = msg.getString("sid");
                    String uid = msg.getString("uid");
                    String count = msg.getString("count");
                    LogUtils.e ( "joined  room: uid=" +uid);
                    // List<String>  users   = (List<String>) msg.getJSONArray("users");
                    JSONArray users = msg.getJSONArray("users");
                    List<String> uses = new ArrayList<>();
                    for (int i = 0; i < users.length(); i++) {
                        String use =  (String)users.get(i);
                        if (!use.equals(uid))
                        {
                            uses.add(use);
                        }
                        LogUtils.e ( "遍历 user:" + use);
                    }
                    EventBus.getDefault ().post ( new IoJoinRoomEvent(roomName,uses,Integer.parseInt(count),uid,uid) );


                } catch (JSONException e) {
                    e.printStackTrace();
                    LogUtils.e ( "JSONException joined, room:" + e.toString());
                }



            }
        });
        mSocket.on("leaved", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String roomName = String.valueOf ( args[0]);
                String userId = (String) args[1];
                LogUtils.e (  "onUserLeaved, room:" + roomName + "uid:" + userId);
                //EventBus.getDefault ().post ( new IoJoinRoomEvent (roomName,userId) );
            }
        });

        mSocket.on("otherjoin", new Emitter.Listener() {

            @Override
            public void call(Object... args) {

                try {
                    LogUtils.e("onRemoteUserJoined, room:" + args[0] + "uid:" + args[1]);
                    String roomName = String.valueOf(args[0]);
                    JSONObject msg = (JSONObject) args[1];

                    String sid = msg.getString("sid");
                    String uid = msg.getString("uid");
                    String count = msg.getString("count");

                    EventBus.getDefault().post(new IoOtherJoinEvent(roomName, uid));

                } catch (ClassCastException e) {
                    LogUtils.e("ClassCastException onRemoteUserJoined, room:" + args[0] + "uid:" + args[1]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        });

        mSocket.on("quite", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String roomName =String.valueOf ( args[0]);
                JSONObject msg = (JSONObject) args[1];

                try {

                    String sid = msg.getString("sid");
                    String count = msg.getString("count");
                    String userId = msg.getString("uid");
                    LogUtils.e ("onRemoteUserLeaved, room:" + roomName + "uid:" + userId);
                    EventBus.getDefault ().post ( new IoOtherLeaveEvent(roomName,userId) );

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
        mSocket.on("full", new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                String roomName = String.valueOf ( args[0]);
                String userId = (String) args[1];
                LogUtils.e ( "onRoomFull, room:" + roomName + "uid:" + userId);
                //EventBus.getDefault ().post ( new IoRoomFullEvent(roomName,userId) );

            }
        });
        mSocket.on("message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String roomName = String.valueOf ( args[0]);
                JSONObject msg = (JSONObject) args[1];
                LogUtils.e ( "onMessage, room:" + roomName + "data:" + msg);

                try {
                    if (msg.has("type"))
                    {

                        EventBus.getDefault ().post ( new IoMesageEvent( msg , roomName ));

                    }else  if (msg.has("ptt")){

                        int ptt = msg.getInt("ptt");
                        String rid = msg.getString("uid");
                        EventBus.getDefault ().post ( new IoPttEvent( ptt , rid ));

                    }else
                    {
                        LogUtils.e ("chat ->  "+msg.getString("user") +"     "+msg.getString("msg"));
                        EventBus.getDefault().post(new ChatEvent(msg.getString("user"),roomName,msg.getString("msg")));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    LogUtils.e("message type ERROR = > "+e.toString());
                }

            }
        });

    }

    public String getSessionId() {
        return sessionId;
    }
}