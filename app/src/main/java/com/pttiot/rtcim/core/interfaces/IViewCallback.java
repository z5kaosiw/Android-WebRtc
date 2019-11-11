package com.pttiot.rtcim.core.interfaces;

import org.webrtc.MediaStream;

/***
 *   add by guojianyong on 2019-5-13
 * */
public interface IViewCallback {

    void onSetLocalStream(MediaStream stream, String socketId);

    void onAddRemoteStream(MediaStream stream, String socketId);

    void onCloseWithId(String socketId);
    void onCreatePeerConnectionError(String error);
}
