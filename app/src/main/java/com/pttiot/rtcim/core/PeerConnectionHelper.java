package com.pttiot.rtcim.core;

import android.content.Context;
import android.util.Log;

import com.pttiot.rtcim.core.interfaces.IViewCallback;
import com.pttiot.rtcim.core.parm.BasePeerConnectionHelper;
import com.pttiot.rtcim.core.parm.SignalType;
import com.pttiot.rtcim.eventbus.IoConnectedEvent;
import com.pttiot.rtcim.eventbus.IoErrorEvent;
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
import org.webrtc.CameraVideoCapturer;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 *   PeerConnection Manager
 *
 *     view call back -> add  by guojianyong
 *
 * **/
public class PeerConnectionHelper extends BasePeerConnectionHelper  {

    private boolean isVideoEnable = false;
    private IViewCallback viewCallback = null;
    private final RtcSignalClient rtcSignalClient;

    /**
     * @param  _context
     * @param  viewCallback    on  view activity show
     * **/
    public PeerConnectionHelper(Context _context,IViewCallback viewCallback,boolean isVideoEnable)
    {

        super(_context);
        EventBus.getDefault().register(this);
        if ( viewCallback != null)
        {
            this.viewCallback = viewCallback;
        }
        this.isVideoEnable = isVideoEnable;
        rtcSignalClient = RtcSignalClient.getInstance();
    }
    public void  sendPttSignal(int ptt )
    {
        JSONObject message = new JSONObject();
        try {
            message.put("ptt", ptt);
            message.put("uid", _myId);
            rtcSignalClient.sendPttSignal(message);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    public void exit(String rid)
    {
        rtcSignalClient.leaveRoom(Integer.parseInt(rid));
    }
    public void joinRoom(String rid,int command_id)
    {
        rtcSignalClient.joinRoom(rid,command_id);
    }

    @Override
    protected void onAddStream(MediaStream mediaStream, String socketId) {
        LogUtils.e("onAddStream  socketid -> "+socketId);
        if (viewCallback != null) {
            viewCallback.onAddRemoteStream(mediaStream, socketId);
        }
    }

    @Override
    protected void onRemoveStream(MediaStream mediaStream, String socketId) {
        LogUtils.e("onRemoveStream  socketid -> "+socketId);
        if (viewCallback != null) {
            viewCallback.onCloseWithId( socketId);
        }
    }

    @Override
    protected void onCreatePeerConnectionError(String error) {
        if (viewCallback != null) {

            viewCallback.onCreatePeerConnectionError(error);
        }

    }
    // 调整摄像头前置后置
    public void switchCamera() {
        if (captureAndroid == null) return;
        if (captureAndroid instanceof CameraVideoCapturer) {
            CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) captureAndroid;
            cameraVideoCapturer.switchCamera(null);
        } else {
            Log.d(TAG, "Will not switch camera, video caputurer is not a camera");
        }

    }
    public void toggleSpeaker(boolean enable) {
        if (mAudioManager != null) {
            mAudioManager.setSpeakerphoneOn(enable);
        }

    }
    // 设置自己静音
    public void toggleMute(boolean enable) {
        if (_localAudioTrack != null) {
            _localAudioTrack.setEnabled(enable);
        }
    }
    //create  local media stream
    private void createLocalStream() {

        _localStream = _factory.createLocalMediaStream(MEDIA_TRACK_ID);
        audioSource  =  _factory.createAudioSource(/*CreateOptionFactory.createAudioConstraints()*/new MediaConstraints());
        _localAudioTrack = _factory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        _localAudioTrack.setEnabled(true);
        _localStream.addTrack(_localAudioTrack);
        LogUtils.e("isVideoEnable=>"+isVideoEnable);
        if (isVideoEnable)
        {
            captureAndroid = CreateOptionFactory.createVideoCapturer(_context);
            // 视频
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", _rootEglBase.getEglBaseContext());
            videoSource = _factory.createVideoSource(false/*captureAndroid.isScreencast()*/);
            captureAndroid.initialize(surfaceTextureHelper, _context.getApplicationContext(), videoSource.getCapturerObserver());
            _localVideoTrack = _factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
            _localVideoTrack.setEnabled(true);
            _localStream.addTrack(_localVideoTrack);

            captureAndroid.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);
        }
        if (viewCallback != null) {
            viewCallback.onSetLocalStream(_localStream, _myId);
        }
    }
    // create all Peer connection
    private void createPeerConnections() {
        Log.v(TAG, "createPeerConnections");
        for (String str : _connectionIdArray) {
            Peer peer = new Peer( str);
            _connectionPeerDic.put( str, peer);
        }
        LogUtils.e("_connectionPeerDic有:"+_connectionPeerDic.size());
    }
    // to all  PeerConnection  add stream
    private void addStreams() {
        Log.v(TAG, "addStreams");
        if (_localStream == null) {
            createLocalStream();
        }

        for (Map.Entry<String, Peer> entry : _connectionPeerDic.entrySet()) {

             entry.getValue().getPc().addStream(_localStream);
//            entry.getValue().getPc().addTrack(_localAudioTrack);
//            entry.getValue().getPc().addTrack(_localVideoTrack);
        }

    }
    // all peer connection  create offer sdp
    private void createOffers() {
        for (Map.Entry<String, Peer> entry : _connectionPeerDic.entrySet()) {
            
            Peer mPeer = entry.getValue();
            String pid = entry.getKey();
            LogUtils.e("pid=>"+pid+"   mPeer.id=>"+mPeer.getSocketId());
            mPeer.getPc().createOffer(new SimpleSdpObserver(){
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                   LogUtils.e(  "Create local offer success: \n" );
                    mPeer.getPc().setLocalDescription(new SimpleSdpObserver(),sessionDescription);
                    JSONObject message = new JSONObject();
                    try {
                        message.put("type", "offer");
                        message.put("sdp",  sessionDescription.description);
                        message.put("uid",  mPeer.getSocketId());
                        message.put("suid", _myId);
                        rtcSignalClient.sendRtcSdp(message);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            }, CreateOptionFactory.offerOrAnswerConstraint(isVideoEnable));
        }
    }
    // 关闭通道流
    private void closePeerConnection(String connectionId) {
        Peer mPeer = _connectionPeerDic.get(connectionId);
        if (mPeer != null) {
            mPeer.getPc().close();
        }
        _connectionPeerDic.remove(connectionId);
        _connectionIdArray.remove(connectionId);
        //callback  video view
        if (viewCallback != null) {
            viewCallback.onCloseWithId(connectionId);
        }

    }
    /* sdp  Signal */

    private void onRemoteAnswerReceived(String sdp, String uid,String suid) {
        LogUtils.e("onRemoteAnswerReceived  myId=>"+_myId+"   parm.uid=>"+uid +"  suid=>"+suid);
        //Receive Remote Answer ..
       LogUtils.e("Receive Remote Answer ..");
        Peer mPeer = _connectionPeerDic.get(suid);
        SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
        if (mPeer != null) {
            mPeer.getPc().setRemoteDescription(new SimpleSdpObserver(), sessionDescription);
            LogUtils.e("onRemoteAnswerReceived  Answer 设置成功");
        }

        //only do  some updateCallState
    }

    private void onRemoteOfferReceived(String sdp, String uid,String suid) {

        //Receive Remote Offer ..
       LogUtils.e("Receive Remote Offer ..");
        LogUtils.e("onRemoteOfferReceived  myId=>"+_myId+"   parm.uid=>"+uid +"  suid=>"+suid );
        Peer mPeer = _connectionPeerDic.get(suid);
        if (mPeer != null) {
            if (isVideoEnable) {

                //sdp = CodeUtil.preferCodec(sdp, VIDEO_CODEC_H264, false);

            }
            SessionDescription  sessionDescription = new SessionDescription(SessionDescription.Type.OFFER, sdp);
            mPeer.getPc().setRemoteDescription(new SimpleSdpObserver(), sessionDescription);


            //doAnswerCall
            mPeer.getPc().createAnswer(new SimpleSdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sdp) {
                   LogUtils.e(  "Create local Answer success: \n" );
                    LogUtils.e("onRemoteOfferReceived offer设置成功");
                       //设置本地Description
                       mPeer.getPc().setLocalDescription(new SimpleSdpObserver() ,sdp);
                    try {
                        JSONObject message = new JSONObject();
                        message.put("type", "answer");
                        message.put("sdp", sdp.description);
                        message.put("uid",  mPeer.getSocketId());
                        message.put("suid", _myId);
                        rtcSignalClient.sendRtcSdp(message);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            },new MediaConstraints());
        }

    }
    private void onRemoteCandidateReceived(String uid,String suid,String id, int label, String candidate) {
        LogUtils.e("onRemoteCandidateReceived  myId=>"+_myId+"   parm.uid=>"+uid +"  suid=>"+suid);
        Peer peer = _connectionPeerDic.get(suid);
        if (peer != null) {
            LogUtils.e("onRemoteCandidateReceived");
            IceCandidate remoteIceCandidate =  new IceCandidate(id,  label,  candidate);
            peer.getPc().addIceCandidate( remoteIceCandidate );
        }

    }
    /********************************************************************************************/
    /***************************************信令回调控制*****************************************/
    /********************************************************************************************/

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoMesageEvent messageEvent) {

        JSONObject msg =  messageEvent.msg;


        try {
            String type = msg.getString("type");
            String uid = msg.getString("uid");
            String suid = msg.getString("suid");

            if (type.equals(SignalType.OFFER_SIGNAL)) {
                String sdp = msg.getString("sdp");
                onRemoteOfferReceived(sdp,uid,suid);
            }else if(type.equals(SignalType.ANSWER_SIGNAL)) {
                String sdp = msg.getString("sdp");
                onRemoteAnswerReceived(sdp,uid,suid);
            }else if(type.equals(SignalType.CANDIDATE_SIGNAL)) {

                String id = msg.getString("id");
                int label = msg.getInt("label");
                String candidate = msg.getString("candidate");
                LogUtils.d("candidate的信息=>"+msg);
                onRemoteCandidateReceived(uid,suid,id,label,candidate);

            }else{
               LogUtils.e( "EXPLAND OTHER  SIGNAL " + type);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }



    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoJoinRoomEvent messageEvent) {
        _connectionIdArray.clear();
        _connectionIdArray = (ArrayList<String>) messageEvent.users;
        _myId =  messageEvent.uid;
        LogUtils.e("_connectionIdArray.size = > "+_connectionIdArray.size()+"  _myId=>"+_myId);

        //init peer connection
        initPeerConnection();

        if (_localStream == null)
        {
            createLocalStream();
        }
        if (_connectionIdArray.size()>0)
        {
            createPeerConnections();
            addStreams();
        }
        if (messageEvent.roomUserCount  > 1)
        {
            createOffers();
        }

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoLeaveRoomEvent messageEvent) {


    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoOtherJoinEvent messageEvent) {
        LogUtils.e("IoOtherJoinEvent _connectionIdArray.size -> "+ _connectionIdArray+"  user_id->"+messageEvent.userid);
        String uid =  messageEvent.userid;
        if (_localStream == null) {
            createLocalStream();
        }
        Peer mPeer = new Peer(uid);
        mPeer.getPc().addStream(_localStream);
        _connectionIdArray.add(uid);
        _connectionPeerDic.put(uid, mPeer);
        LogUtils.e("_connectionPeerDic有:"+_connectionPeerDic.size());
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoOtherLeaveEvent messageEvent) {

        String uid =  messageEvent.userid;
        LogUtils.e("IoOtherLeaveEvent user id =>"+uid);
        closePeerConnection(uid);

    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoRoomFullEvent messageEvent) {
        //do sometings
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoErrorEvent messageEvent) {

       LogUtils.e("ERROR=" );
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoConnectedEvent messageEvent) {

        switch (messageEvent.type)
        {
            case  1:
//                logcatOnUI("Signal Server Connecting !");
                break;
            case  2:
//                logcatOnUI("Signal Server Connected !");
                break;
            case  3:
//                logcatOnUI("Signal Server Disconnected!");
                break;

        }

    }
    public void  peerRelese()
    {
        EventBus.getDefault().unregister(this);
        //释放资源
        ArrayList myCopy;
        myCopy = (ArrayList) _connectionIdArray.clone();
        for (Object Id : myCopy) {
            closePeerConnection((String) Id);
        }
        if (_connectionIdArray != null) {
            _connectionIdArray.clear();
        }
        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }
        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }
        if (_localAudioTrack!=null)
        {
            _localAudioTrack.dispose();
        }
        if (_localVideoTrack!=null)
        {
            _localVideoTrack.dispose();
        }
        if (captureAndroid != null) {
            try {
                captureAndroid.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            captureAndroid.dispose();
            captureAndroid = null;
        }
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }
        if (_factory != null) {
            _factory.dispose();
            _factory = null;
        }
        if (viewCallback!=null)
        {
            viewCallback = null;
        }

    }
    public EglBase getEglBase(){

        return _rootEglBase;
    }
    public List<String> getConnectionIdArray(){

        return _connectionIdArray;
    }
    public  String  getMyid(){

        return _myId;
    }
    public   Map<String, Peer>  getMapConnection(){

        return _connectionPeerDic;
    }


}
