package com.pttiot.rtcim.core;

import com.pttiot.rtcim.eventbus.ChatEvent;
import com.pttiot.rtcim.eventbus.IoConnectedEvent;
import com.pttiot.rtcim.eventbus.IoErrorEvent;
import com.pttiot.rtcim.eventbus.IoJoinRoomEvent;
import com.pttiot.rtcim.eventbus.IoMesageEvent;
import com.pttiot.rtcim.eventbus.IoOtherJoinEvent;
import com.pttiot.rtcim.eventbus.IoOtherLeaveEvent;
import com.pttiot.rtcim.eventbus.IoRoomFullEvent;
import com.pttiot.rtcim.util.LogUtils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by gjy on 2019/5/5.
 *
 * 免费IM（含单聊，群聊，聊天室），免费一对一视频聊天（回音消除），语音聊天，直播连麦，白板，小班课，
 * 多人会议，局域网无服务器直连，兼容webRTC, 支持webRTC加速，P2P高清传输，安卓、iOS、web互通，支持门禁可视对讲，
 * 电视盒子，树莓派，海思，全志，任天堂switch，云游戏，OTT设备，C语言自研方案
 */

public class SignalClient {

    private static final String TAG = "SignalClient";
    private volatile static SignalClient mInstance;

    private Socket mSocket;
    private String mRoomId;
    private String sessionId;

    private SignalClient() {
    }

    public static SignalClient getInstance() {
        if (mInstance == null) {     //1  第一次检查
            synchronized (SignalClient.class) { //2 加锁
                if (mInstance == null) {  //3 第二次检查
                    mInstance = new SignalClient(); //4 初始化
                }
            }
        }
        return mInstance;
    }
    public void sendMessage(JSONObject message) {
        LogUtils.d (  "broadcast: " + message);
        if (mSocket == null) {
            return;
        }
        mSocket.emit("message", mRoomId, message);
    }
    public void sendRtcSdp(JSONObject message) {
        LogUtils.d (  "broadcast: " + message);
        if (mSocket == null) {
            return;
        }
        mSocket.emit("rtcsdp", mRoomId, message);
    }
    public void leaveRoom() {

        LogUtils.d (  "leaveRoom: " + mRoomId);
        if (mSocket == null) {
            return;
        }

        mSocket.emit("leave", mRoomId);
        mSocket.close();
        mSocket = null;
    }
     /*Room 逻辑***/
     public void joinRoom(String url, String roomName) {

         LogUtils.d (  "joinRoom: " + url + ", " + roomName );
         try {
             mSocket = IO.socket(url);
             mSocket.connect();
         } catch (URISyntaxException e) {
             e.printStackTrace();
             LogUtils.e("ERROR=>"+e.toString());
             return;
         }
         //mUserId = userId;
         mRoomId = roomName;
         listenSignalEvents();

         mSocket.emit("join", mRoomId);
     }

      //侦听从服务器收到的消息
      private void listenSignalEvents() {

          if (mSocket == null) {
              return;
          }
          mSocket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
              @Override
              public void call(Object... args) {

                  LogUtils.e ( "onConnectError: " + args );
                  EventBus.getDefault ().post ( new IoErrorEvent () );
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
                  EventBus.getDefault ().post ( new IoConnectedEvent (2) );
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
                  String userId = (String) args[1];
                  //Log.i(TAG, "onRemoteUserJoined: " + userId);
                  LogUtils.e ( "joined, room:" + roomName + "uid:" + userId);
                  EventBus.getDefault ().post ( new IoJoinRoomEvent (roomName,null,5,userId,userId) );

              }
          });
          mSocket.on("leaved", new Emitter.Listener() {
              @Override
              public void call(Object... args) {
                  String roomName = String.valueOf ( args[0]);
                  String userId = (String) args[1];
                  LogUtils.e (  "onUserLeaved, room:" + roomName + "uid:" + userId);
                  EventBus.getDefault ().post ( new IoJoinRoomEvent (roomName,null,5,userId,userId) );
              }
          });

          mSocket.on("otherjoin", new Emitter.Listener() {

              @Override
              public void call(Object... args) {

                  try
                  {
                      LogUtils.e ( "onRemoteUserJoined, room:" +  args[0] + "uid:" + args[1]);
                      String roomName = String.valueOf ( args[0]);
                      String userId = (String) args[1];
                      LogUtils.e ( "onRemoteUserJoined, room:" + roomName + "uid:" + userId);
                      EventBus.getDefault ().post ( new IoOtherJoinEvent (roomName,userId) );

                  }catch (ClassCastException e)
                  {
                      LogUtils.e ( "onRemoteUserJoined, room:" +  args[0] + "uid:" + args[1]);
                  }


              }
          });

          mSocket.on("bye", new Emitter.Listener() {
              @Override
              public void call(Object... args) {
                  String roomName =String.valueOf ( args[0]);
                  String userId = (String) args[1];
                  LogUtils.e ("onRemoteUserLeaved, room:" + roomName + "uid:" + userId);
                  EventBus.getDefault ().post ( new IoOtherLeaveEvent (roomName,userId) );

              }
          });
          mSocket.on("full", new Emitter.Listener() {
              @Override
              public void call(Object... args) {

                  //释放资源
                  mSocket.disconnect();
                  mSocket.close();
                  mSocket = null;

                  String roomName = String.valueOf ( args[0]);
                  String userId = (String) args[1];
                  LogUtils.e ( "onRoomFull, room:" + roomName + "uid:" + userId);
                  EventBus.getDefault ().post ( new IoRoomFullEvent (roomName,userId) );

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
                          EventBus.getDefault ().post ( new IoMesageEvent ( msg ,roomName ));
                      }else
                      {
                          LogUtils.e ("chat ->  "+msg.getString("user") +"     "+msg.getString("msg"));
                          EventBus.getDefault().post(new ChatEvent(msg.getString("user"),roomName,msg.getString("msg")));
                      }

                  } catch (JSONException e) {
                      e.printStackTrace();
                  }

              }
          });


      }

    public String getSessionId() {
        return sessionId;
    }
}
