package com.pttiot.rtcim.core.parm;

/*
*  signal
*
*     expand other signale
*
*     add by guojianyong  on 2018-5-11
*
* * **/
public class SignalType {

        public  final  static  String OFFER_SIGNAL = "offer";
        public  final  static  String ANSWER_SIGNAL = "answer";
        public  final  static  String CANDIDATE_SIGNAL = "candidate";
        public  final  static  String CHAT_SIGNAL = "chat";


        /*房间类型**/
        public  final  static  String   PTT_IM= "0";
        public  final  static  String   TEMP_IM= "10";
        public  final  static  String   CHAT_IM = "1";
        public  final  static  String   ONEBYONE_AUDIO = "2";
        public  final  static  String   ONEBYONE_VIDEO = "3";
        public  final  static  String   MORE_AUDIO = "4";
        public  final  static  String   MORE_VIDEO = "5";

        /*消息会话命令类型*/
        public static final  int CHAT_COMMAND =  0xD5 ;
        public static final  int METTING_COMMAND  = 0xC5;
        public static final  int PTT_COMMAND  = 0xA5;
        public static final  int SINGLE_COMMAND  = 0xB5;
}
