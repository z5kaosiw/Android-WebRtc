package com.pttiot.rtcim.core.parm;

import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.pttiot.rtcim.activity.VideoCallActivity;
import com.pttiot.rtcim.core.CreateOptionFactory;
import com.pttiot.rtcim.core.RtcSignalClient;
import com.pttiot.rtcim.core.SignalClient;
import com.pttiot.rtcim.util.LogUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BasePeerConnectionHelper implements PeerConfiger {

    public final static String TAG = "PeerConnectionHelper";
    protected   ExecutorService executor;
    protected    AudioManager mAudioManager;
    protected PeerConnectionFactory _factory;
    protected Context _context;
    protected  String _myId;
    protected EglBase _rootEglBase;
    protected  boolean useHardwareAcousticEchoCanceler = true;
    protected  boolean useHardwareNoiseSuppressor = true;
    protected ArrayList<String> _connectionIdArray;
    protected Map<String, Peer> _connectionPeerDic;
    //media
    protected MediaStream _localStream;
    protected VideoTrack _localVideoTrack;
    protected AudioTrack _localAudioTrack;
    protected VideoCapturer captureAndroid;
    protected VideoSource videoSource;
    protected AudioSource audioSource;


    @Nullable
    protected SurfaceTextureHelper surfaceTextureHelper;

    protected LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<PeerConnection.IceServer>();
    protected  AudioDeviceModule adm;

    protected abstract  void onAddStream(MediaStream mediaStream, String socketId);
    protected abstract  void onRemoveStream(MediaStream mediaStream, String socketId);
    protected abstract  void onCreatePeerConnectionError(String error);
    public  BasePeerConnectionHelper(Context _context)
    {
        this._context =_context;
        this._connectionPeerDic = new HashMap<>();
        this._connectionIdArray = new ArrayList<>();
        _rootEglBase = EglBase.create();
        //只添加一个ICE Server
        PeerConnection.IceServer iceServer = PeerConnection.IceServer
                .builder(ICE_SERVICES_URI)
                .setUsername(ICE_SERVICES_USER)
                .setPassword(ICE_SERVICES_PASSWD)
                .createIceServer();
        iceServers.add(iceServer);
        executor = Executors.newSingleThreadExecutor();
        mAudioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);


    }

    protected void initPeerConnection()
    {
        adm  = createJavaAudioDevice();
        if (_factory == null) {
            _factory = CreateOptionFactory.createAudioPeerConnectionFactory(_context,_rootEglBase,adm);
        }
        // NOTE: this _must_ happen while PeerConnectionFactory is alive!
        Logging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE);
    }

    protected AudioDeviceModule createJavaAudioDevice() {
        // Enable/disable OpenSL ES playback.

        // Set audio record error callbacks.
        JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback = new JavaAudioDeviceModule.AudioRecordErrorCallback() {
            @Override
            public void onWebRtcAudioRecordInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
                BasePeerConnectionHelper.this.onCreatePeerConnectionError(errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordStartError(
                    JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
                BasePeerConnectionHelper.this.onCreatePeerConnectionError(errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
                BasePeerConnectionHelper.this.onCreatePeerConnectionError(errorMessage);
            }
        };

        JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback = new JavaAudioDeviceModule.AudioTrackErrorCallback() {
            @Override
            public void onWebRtcAudioTrackInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackInitError: " + errorMessage);
                BasePeerConnectionHelper.this.onCreatePeerConnectionError(errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackStartError(
                    JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
                BasePeerConnectionHelper.this.onCreatePeerConnectionError(errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackError: " + errorMessage);
                BasePeerConnectionHelper.this.onCreatePeerConnectionError(errorMessage);
            }
        };

        return JavaAudioDeviceModule.builder(_context)
                .setSamplesReadyCallback(new JavaAudioDeviceModule.SamplesReadyCallback() {
                    @Override
                    public void onWebRtcAudioRecordSamplesReady(JavaAudioDeviceModule.AudioSamples audioSamples) {

//                        LogUtils.e("长度大小-->"+audioSamples.getData().length +"  实际大小--->"+getValidLength(audioSamples.getData()));

                    }
                })
                .setUseHardwareAcousticEchoCanceler(useHardwareAcousticEchoCanceler)
                .setUseHardwareNoiseSuppressor(useHardwareNoiseSuppressor)
                .setAudioRecordErrorCallback(audioRecordErrorCallback)
                .setAudioTrackErrorCallback(audioTrackErrorCallback)
                .createAudioDeviceModule();
    }

    //**************************************内部类******************************************/
    protected class Peer implements PeerConnection.Observer {

        protected PeerConnection pc;
        private String socketId;
        public Peer(String socketId) {
            createPeerConnection();
            this.socketId = socketId;

        }
        //初始化 RTCPeerConnection 连接管道
        private void  createPeerConnection() {

            // 管道连接抽象类实现方法
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

            // to track for   remote peer
            this.pc = _factory.createPeerConnection(rtcConfig, this);
//            List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");
//            this.pc.addTrack(_localVideoTrack, mediaStreamLabels);
//            this.pc.addTrack(_localAudioTrack, mediaStreamLabels);
            this.pc.addStream(_localStream);

        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState newState) {
            Log.i(TAG, "onSignalingChange: " + newState);
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
            Log.i(TAG, "onIceConnectionChange: " + newState);
        }

        @Override
        public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
            Log.i(TAG, "onConnectionChange: " + newState.toString());
        }

        @Override
        public void onIceConnectionReceivingChange(boolean receiving) {
            Log.i(TAG, "onIceConnectionReceivingChange:" + receiving);
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {

            LogUtils.e( "onIceCandidate suid=: "+_myId+"   socketId="+socketId );

            try {
                JSONObject message = new JSONObject();
                message.put("type", "candidate");
                message.put("label", iceCandidate.sdpMLineIndex);
                message.put("id", iceCandidate.sdpMid);
                message.put("candidate", iceCandidate.sdp);
                message.put("uid", socketId);
                message.put("suid", _myId);
                RtcSignalClient.getInstance().sendRtcSdp(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] candidates) {

            for (int i = 0; i < candidates.length; i++) {
                LogUtils.e( "onIceCandidatesRemoved: " + candidates[i]);
            }
            pc.removeIceCandidates(candidates);

        }

        @Override
        public void onAddStream(MediaStream stream) {
            LogUtils.e( "onAddStream: ");
            BasePeerConnectionHelper.this.onAddStream(stream,socketId);

        }

        @Override
        public void onRemoveStream(MediaStream stream) {
            LogUtils.e( "onRemoveStream: ");
            BasePeerConnectionHelper.this.onRemoveStream(stream,socketId);

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }

        @Override
        public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
            LogUtils.e( "onAddTrack: ");
        }

        @Override
        public void onTrack(RtpTransceiver transceiver) {
            LogUtils.e( "onTrack: ");
        }

        public String getSocketId() {
            return socketId;
        }

        public PeerConnection getPc() {
            return pc;
        }
    }
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
