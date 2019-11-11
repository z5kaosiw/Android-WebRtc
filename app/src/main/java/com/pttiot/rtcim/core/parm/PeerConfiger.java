package com.pttiot.rtcim.core.parm;

public interface PeerConfiger {

 /*
  *   Video Services
  * ***/
 int VIDEO_RESOLUTION_WIDTH =240 ;
 int VIDEO_RESOLUTION_HEIGHT =320 ;
 int FPS = 15;
 /*
  *  ice sERVER
  * ***/
 String ICE_SERVICES_URI = "turn:47.99.207.177:3478";
 String ICE_SERVICES_USER = "guo";
 String ICE_SERVICES_PASSWD = "123456";
 String VIDEO_CODEC_H264 = "H264";
 String VIDEO_TRACK_ID = "ARDAMSv0";
 String AUDIO_TRACK_ID = "ARDAMSa0";
 String MEDIA_TRACK_ID = "ARDAMS";

 String ROOMID  = "rid";
 String MEDIA_TYPE  = "mtype";




}
