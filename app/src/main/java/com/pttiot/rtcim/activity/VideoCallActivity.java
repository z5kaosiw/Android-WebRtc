package com.pttiot.rtcim.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.pttiot.rtcim.R;
import com.pttiot.rtcim.core.CreateOptionFactory;
import com.pttiot.rtcim.core.RtcSignalClient;
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
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
/**
 * Created by gjy on 2019/5/5.
 */

public class VideoCallActivity extends AppCompatActivity {

    private static final String TAG = "VideoCallActivity";

    private TextView mLogcatView;

    private static final int VIDEO_RESOLUTION_WIDTH = 320;
    private static final int VIDEO_RESOLUTION_HEIGHT = 640;
    private static final int VIDEO_FPS = 15;
    public static final String VIDEO_TRACK_ID = "1";//"ARDAMSv0";
    public static final String AUDIO_TRACK_ID = "2";//"ARDAMSa0";

    private String mState = "init";
    //用于数据传输
    private PeerConnection mPeerConnection;
    private PeerConnectionFactory mPeerConnectionFactory;

    //OpenGL ES
    private EglBase mRootEglBase;
    //纹理渲染
    private SurfaceTextureHelper mSurfaceTextureHelper;

    //继承自 surface view
    private SurfaceViewRenderer mLocalSurfaceView;
    private SurfaceViewRenderer mRemoteSurfaceView;

    private VideoTrack mVideoTrack;
    private AudioTrack mAudioTrack;

    private VideoCapturer mVideoCapturer;
    private RtcSignalClient signalClient;
    private String roomName;

    @Override
    protected void onCreate ( @Nullable Bundle savedInstanceState ) {
        super.onCreate ( savedInstanceState );
        setContentView(R.layout.activity_call);
        EventBus.getDefault ().register ( this );
        signalClient = RtcSignalClient.getInstance();
        initView();
        initVideoWork();


    }

    private void initVideoWork() {
        mRootEglBase = EglBase.create();

        mLocalSurfaceView.init(mRootEglBase.getEglBaseContext(), null);
        mLocalSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        mLocalSurfaceView.setMirror(true);
        mLocalSurfaceView.setEnableHardwareScaler(false /* enabled */);

        mRemoteSurfaceView.init(mRootEglBase.getEglBaseContext(), null);
        mRemoteSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        mRemoteSurfaceView.setMirror(true);
        mRemoteSurfaceView.setEnableHardwareScaler(true /* enabled */);
        mRemoteSurfaceView.setZOrderMediaOverlay(true);

        //创建 factory， pc是从factory里获得的
        mPeerConnectionFactory = CreateOptionFactory.createPeerConnectionFactory(this,mRootEglBase);
        // NOTE: this _must_ happen while PeerConnectionFactory is alive!
        Logging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE);

        mVideoCapturer = CreateOptionFactory.createVideoCapturer(this);

        mSurfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", mRootEglBase.getEglBaseContext());
        VideoSource videoSource = mPeerConnectionFactory.createVideoSource(false);
        mVideoCapturer.initialize(mSurfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());

        mVideoTrack = mPeerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        mVideoTrack.setEnabled(true);
        mVideoTrack.addSink(mLocalSurfaceView);

        AudioSource audioSource = mPeerConnectionFactory.createAudioSource(new MediaConstraints());
        mAudioTrack = mPeerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        mAudioTrack.setEnabled(true);

        roomName = getIntent().getStringExtra("RoomName");
        signalClient.joinRoom(roomName,SignalType.SINGLE_COMMAND);
    }

    private void initView() {
        mLogcatView = (TextView) findViewById(R.id.LogcatView);
        mLocalSurfaceView = (SurfaceViewRenderer) findViewById(R.id.LocalSurfaceView);
        mRemoteSurfaceView = (SurfaceViewRenderer) findViewById(R.id.RemoteSurfaceView);
    }
    private void logcatOnUI(String msg) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String output = mLogcatView.getText() + "\n" + msg;
                mLogcatView.setText(output);
            }
        });
    }

    private void updateCallState(boolean idle) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (idle) {
                    mRemoteSurfaceView.setVisibility(View.GONE);
                } else {
                    mRemoteSurfaceView.setVisibility(View.VISIBLE);
                }
            }
        });
    }
    public void doLeave() {
        logcatOnUI("Leave room, Wait ...");
        hangup();

        signalClient.leaveRoom(Integer.parseInt(roomName));

    }
    private void hangup() {
        logcatOnUI("Hangup Call, Wait ...");
        if (mPeerConnection == null) {
            return;
        }
        mPeerConnection.close();
        mPeerConnection = null;
        logcatOnUI("Hangup Done.");
        updateCallState(true);
    }
    /*****************************************生命周期处理*********************************************************/
    @Override
    protected void onResume ( ) {
        super.onResume ( );
        mVideoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, VIDEO_FPS);
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            mVideoCapturer.stopCapture();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onStart ( ) {
        super.onStart ( );
    }

    @Override
    protected void onStop ( ) {
        super.onStop ( );
    }

    @Override
    protected void onDestroy ( ) {
        super.onDestroy ( );
        EventBus.getDefault ().unregister ( this );
        doLeave();
        mLocalSurfaceView.release();
        mRemoteSurfaceView.release();
        mVideoCapturer.dispose();
        mSurfaceTextureHelper.dispose();
        PeerConnectionFactory.stopInternalTracingCapture();
        PeerConnectionFactory.shutdownInternalTracer();
        mPeerConnectionFactory.dispose();
    }
    /*****************************offer -  Answer   candidate     CallBack Event*********************************/
    private void onRemoteOfferReceived(JSONObject message) {
        logcatOnUI("Receive Remote Call ...");

        if (mPeerConnection == null) {
            mPeerConnection = createPeerConnection();
        }

        try {
            String description = message.getString("sdp");
            mPeerConnection.setRemoteDescription(
                    new SimpleSdpObserver(),
                    new SessionDescription(
                            SessionDescription.Type.OFFER,
                            description));
            doAnswerCall();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onRemoteAnswerReceived(JSONObject message) {
        logcatOnUI("Receive Remote Answer ...");
        try {
            String description = message.getString("sdp");
            mPeerConnection.setRemoteDescription(
                    new SimpleSdpObserver(),
                    new SessionDescription(
                            SessionDescription.Type.ANSWER,
                            description));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        updateCallState(false);
    }

    private void onRemoteCandidateReceived(JSONObject message) {
        logcatOnUI("Receive Remote Candidate ...");
        try {
            IceCandidate remoteIceCandidate =
                    new IceCandidate(message.getString("id"),
                            message.getInt("label"),
                            message.getString("candidate"));

            mPeerConnection.addIceCandidate(remoteIceCandidate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    

    /**********************************************EventBus回调******************************************************/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoMesageEvent messageEvent) {

        JSONObject message  =  messageEvent.msg;
        try {
            String type =message.getString("type");
            if (type.equals("offer")) {
                onRemoteOfferReceived(message);
            }else if(type.equals("answer")) {
                onRemoteAnswerReceived(message);
            }else if(type.equals("candidate")) {
                onRemoteCandidateReceived(message);
            }else{
                LogUtils.d( "the type is invalid: " + type);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
    public void Event(IoJoinRoomEvent messageEvent) {

         logcatOnUI("local user joined!");
         mState = "joined";
        //这里应该创建PeerConnection
        if (mPeerConnection == null) {
            mPeerConnection = createPeerConnection();
        }

    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoLeaveRoomEvent messageEvent) {

        logcatOnUI("local user leaved!");
        mState = "leaved";

    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoOtherJoinEvent messageEvent) {

        logcatOnUI("Remote User Joined, room: " + messageEvent.roomid);
        if(mState.equals("joined_unbind")){
            if (mPeerConnection == null) {
                mPeerConnection = createPeerConnection();
            }
        }

        mState = "joined_conn";
        //调用call， 进行媒体协商
        doStartCall();


    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoOtherLeaveEvent messageEvent) {

        logcatOnUI("Remote User Leaved, room: " + messageEvent.roomid + "uid:"  + messageEvent.userid);
        mState = "joined_unbind";

        if(mPeerConnection !=null ){
            mPeerConnection.close();
            mPeerConnection = null;
        }

    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoRoomFullEvent messageEvent) {
        logcatOnUI("The Room is Full, room: "+ messageEvent.roomid + "uid:"  + messageEvent.userid);
        mState = "leaved";

        if(mLocalSurfaceView != null) {
            mLocalSurfaceView.release();
            mLocalSurfaceView = null;
        }

        if(mRemoteSurfaceView != null) {
            mRemoteSurfaceView.release();
            mRemoteSurfaceView = null;
        }

        if(mVideoCapturer != null) {
            mVideoCapturer.dispose();
            mVideoCapturer = null;
        }

        if(mSurfaceTextureHelper != null) {
            mSurfaceTextureHelper.dispose();
            mSurfaceTextureHelper = null;

        }

        PeerConnectionFactory.stopInternalTracingCapture();
        PeerConnectionFactory.shutdownInternalTracer();

        if(mPeerConnectionFactory !=null) {
            mPeerConnectionFactory.dispose();
            mPeerConnectionFactory = null;
        }

        finish();

    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoErrorEvent messageEvent) {

        LogUtils.d("ERROR=" );
    }
    /*************************************通话Core************************************************************/
    /**
     *  创建节点连接
     * ***/
    public PeerConnection createPeerConnection() {

        LogUtils.d( "Create PeerConnection ...");
        LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<PeerConnection.IceServer>();

        //create ICE Server
        PeerConnection.IceServer ice_server =
                PeerConnection.IceServer.builder("turn:47.99.207.177:3478")
                        .setPassword("123456")
                        .setUsername("guo")
                        .createIceServer();

        iceServers.add(ice_server);

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        //rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        //rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        //rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        // Enable DTLS for normal calls and disable for loopback calls.
        rtcConfig.enableDtlsSrtp = true;
        //rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        PeerConnection connection =
                mPeerConnectionFactory.createPeerConnection(rtcConfig,
                        mPeerConnectionObserver);
        if (connection == null) {
            LogUtils.d(  "Failed to createPeerConnection !");
            return null;
        }

        List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");
        connection.addTrack(mVideoTrack, mediaStreamLabels);
        connection.addTrack(mAudioTrack, mediaStreamLabels);

        return connection;
    }
    public void doAnswerCall() {
        logcatOnUI("Answer Call, Wait ...");

        if (mPeerConnection == null) {
            mPeerConnection = createPeerConnection();
        }

        MediaConstraints sdpMediaConstraints = new MediaConstraints();
        LogUtils.d( "Create answer ...");
        mPeerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                LogUtils.d( "Create answer success !");
                mPeerConnection.setLocalDescription(new SimpleSdpObserver(),
                        sessionDescription);

                JSONObject message = new JSONObject();
                try {
                    message.put("type", "answer");
                    message.put("sdp", sessionDescription.description);
                    signalClient.sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, sdpMediaConstraints);
        updateCallState(false);
    }
    public void doStartCall() {
        logcatOnUI("Start Call, Wait ...");
        if (mPeerConnection == null) {
            mPeerConnection = createPeerConnection();
        }
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        mPeerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                LogUtils.d(  "Create local offer success: \n" + sessionDescription.description);
                mPeerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                JSONObject message = new JSONObject();
                try {
                    message.put("type", "offer");
                    message.put("sdp", sessionDescription.description);
                    signalClient.sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, mediaConstraints);
    }
    private PeerConnection.Observer mPeerConnectionObserver = new PeerConnection.Observer() {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            LogUtils.d( "onSignalingChange: " + signalingState);
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            LogUtils.d( "onIceConnectionChange: " + iceConnectionState);
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            LogUtils.d( "onIceConnectionChange: " + b);
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            LogUtils.d( "onIceGatheringChange: " + iceGatheringState);
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            LogUtils.d( "onIceCandidate: " + iceCandidate);

            try {
                JSONObject message = new JSONObject();
                //message.put("userId", RTCSignalClient.getInstance().getUserId());
                message.put("type", "candidate");
                message.put("label", iceCandidate.sdpMLineIndex);
                message.put("id", iceCandidate.sdpMid);
                message.put("candidate", iceCandidate.sdp);
                signalClient.sendMessage(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            for (int i = 0; i < iceCandidates.length; i++) {
                LogUtils.d( "onIceCandidatesRemoved: " + iceCandidates[i]);
            }
            mPeerConnection.removeIceCandidates(iceCandidates);
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            LogUtils.d( "onAddStream: " + mediaStream.videoTracks.size());
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            LogUtils.d( "onRemoveStream");
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            LogUtils.d( "onDataChannel");
        }

        @Override
        public void onRenegotiationNeeded() {
            LogUtils.d( "onRenegotiationNeeded");
        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            MediaStreamTrack track = rtpReceiver.track();
            if (track instanceof VideoTrack) {
                LogUtils.d( "onAddVideoTrack");
                VideoTrack remoteVideoTrack = (VideoTrack) track;
                remoteVideoTrack.setEnabled(true);
                remoteVideoTrack.addSink(mRemoteSurfaceView);
            }else if (track instanceof AudioTrack)
            {
               // AudioTrack remoteAideoTrack = (AudioTrack) track;
            }

        }
    };
    public static class SimpleSdpObserver implements SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
           LogUtils.d( "SdpObserver: onCreateSuccess !");
        }

        @Override
        public void onSetSuccess() {
           LogUtils.d( "SdpObserver: onSetSuccess");
        }

        @Override
        public void onCreateFailure(String msg) {
           LogUtils.d( "SdpObserver onCreateFailure: " + msg);
        }

        @Override
        public void onSetFailure(String msg) {

           LogUtils.d( "SdpObserver onSetFailure: " + msg);
        }
    }
}
