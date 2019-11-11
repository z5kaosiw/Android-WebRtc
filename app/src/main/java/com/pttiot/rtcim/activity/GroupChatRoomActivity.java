package com.pttiot.rtcim.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.pttiot.rtcim.R;
import com.pttiot.rtcim.core.PeerConnectionHelper;
import com.pttiot.rtcim.core.ProxyVideoSink;
import com.pttiot.rtcim.core.interfaces.IViewCallback;
import com.pttiot.rtcim.core.parm.MediaType;
import com.pttiot.rtcim.core.parm.PeerConfiger;
import com.pttiot.rtcim.core.parm.SignalType;
import com.pttiot.rtcim.fragments.ChatRoomFragment;
import com.pttiot.rtcim.util.AppUtil;
import com.pttiot.rtcim.util.LogUtils;

import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *   add by guojianyong  on 2019-5-14
 *
 *   support 9 road  video or  audio
 *
 * **/
public class GroupChatRoomActivity extends AppCompatActivity implements IViewCallback {

    private FrameLayout wr_video_view;
    private int mScreenWidth;
    private String rid;
    private boolean  isVideoEnable;
    private PeerConnectionHelper _peerHelper;
    private VideoTrack _localVideoTrack;

    private Map<String, SurfaceViewRenderer> _videoViews = new HashMap<>();
    private Map<String, ProxyVideoSink> _sinks = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        initView();
        ChatRoomFragment chatRoomFragment = new ChatRoomFragment();
        replaceFragment(chatRoomFragment);
        rid = getIntent().getStringExtra(PeerConfiger.ROOMID);
        int mediatype = getIntent().getIntExtra(PeerConfiger.MEDIA_TYPE, 0);
        isVideoEnable  =  (mediatype !=  MediaType.TYPE_AUDIO );
        startCall();
    }

    /*-----------------------------Peer connection  call back----------  stat----------*/
    @Override
    public void onSetLocalStream(MediaStream stream, String socketId) {
        List<VideoTrack> videoTracks = stream.videoTracks;
        if (videoTracks.size() > 0) {
            _localVideoTrack = videoTracks.get(0);
        }
        runOnUiThread(() -> {
            LogUtils.e("onSetLocalStream socketId->"+socketId);
            addView(socketId, stream);
        });
    }

    @Override
    public void onAddRemoteStream(MediaStream stream, String socketId) {
        runOnUiThread(() -> {
            LogUtils.e("onAddRemoteStream socketId->"+socketId);
            addView(socketId, stream);
        });

    }

    @Override
    public void onCloseWithId(String socketId) {
        runOnUiThread(() -> {
            removeView(socketId);
        });

    }

    @Override
    public void onCreatePeerConnectionError(String error) {

    }
    /*-----------------------------Peer connection  call back----------  end----------*/
    private void addView(String id, MediaStream stream) {

        SurfaceViewRenderer renderer = new SurfaceViewRenderer(this);
        renderer.init(_peerHelper.getEglBase().getEglBaseContext(), null);
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        renderer.setMirror(true);

        // set render
        ProxyVideoSink sink = new ProxyVideoSink();
        sink.setTarget(renderer);
        if (stream.videoTracks.size() > 0) {
            stream.videoTracks.get(0).addSink(sink);
        }

        _videoViews.put(id, renderer);
        _sinks.put(id, sink);
        wr_video_view.addView(renderer);

        List<String> connectionIdArray = _peerHelper.getConnectionIdArray();
        connectionIdArray.add(_peerHelper.getMyid());
        int size =  connectionIdArray.size();//connectionIdArray.size();
        LogUtils.e("size->"+size);

        for (int i = 0; i < size; i++) {
            SurfaceViewRenderer renderer1 = _videoViews.get(connectionIdArray.get(i));
            if (renderer1 != null) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.height = AppUtil.getWidth(size,mScreenWidth);
                layoutParams.width = AppUtil.getWidth(size,mScreenWidth);
                layoutParams.leftMargin = AppUtil.getX(size, i,mScreenWidth);
                layoutParams.topMargin = AppUtil.getY(size, i,mScreenWidth);
                renderer1.setLayoutParams(layoutParams);
            }

        }
        connectionIdArray.remove(_peerHelper.getMyid());
    }
    private void removeView(String userId) {
        ProxyVideoSink sink = _sinks.get(userId);
        SurfaceViewRenderer renderer = _videoViews.get(userId);
        if (sink != null) {
            sink.setTarget(null);
        }
        if (renderer != null) {
            renderer.release();
        }
        _sinks.remove(userId);
        _videoViews.remove(userId);
        wr_video_view.removeView(renderer);

        List<String> connectionIdArray = _peerHelper.getConnectionIdArray();
        connectionIdArray.add(_peerHelper.getMyid());
        int size =  connectionIdArray.size();//connectionIdArray.size();
        LogUtils.e("size->"+size);

        for (int i = 0; i <size ; i++) {
            SurfaceViewRenderer renderer1 = _videoViews.get(connectionIdArray.get(i));
            if (renderer1 != null) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.height = AppUtil.getWidth(size,mScreenWidth);
                layoutParams.width = AppUtil.getWidth(size,mScreenWidth);
                layoutParams.leftMargin = AppUtil.getX(size, i,mScreenWidth);
                layoutParams.topMargin = AppUtil.getY(size, i,mScreenWidth);
                renderer1.setLayoutParams(layoutParams);
            }

        }
        connectionIdArray.remove(_peerHelper.getMyid());
    }
    // 静音
    public void toggleMic(boolean enable) {
        if (_peerHelper!=null) _peerHelper.toggleMute(enable);
    }

    // 免提
    public void toggleSpeaker(boolean enable) {
        if (_peerHelper!=null)
            _peerHelper.toggleSpeaker(enable);
    }

    // 打开关闭摄像头
    public void toggleCamera(boolean enableCamera) {
        if (_localVideoTrack != null) {
            _localVideoTrack.setEnabled(enableCamera);
        }
    }
    // 切换摄像头
    public void switchCamera() {
        if (_peerHelper!=null)
            _peerHelper.switchCamera();
    }

    private void startCall() {

        _peerHelper =new PeerConnectionHelper(this,this,isVideoEnable);
        _peerHelper.joinRoom(rid,SignalType.METTING_COMMAND);

    }
    // 挂断
    public void hangUp() {
        exit();
        this.finish();
    }
    private void exit() {
        //退出房间操作
        if (_peerHelper != null)
        {
            _peerHelper.exit(rid);
            _peerHelper.peerRelese();
            _peerHelper= null;
        }
        //manager.exitRoom();
        for (SurfaceViewRenderer renderer : _videoViews.values()) {
            renderer.release();
        }
        for (ProxyVideoSink sink : _sinks.values()) {
            sink.setTarget(null);
        }
        _videoViews.clear();
        _sinks.clear();



    }

    private void initView() {
        setContentView(R.layout.activity_chatroom);
        wr_video_view = (FrameLayout) findViewById(R.id.wr_video_view);

        mScreenWidth = AppUtil.getDisplayWidth(this);
        wr_video_view.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mScreenWidth ));
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.wr_container, fragment)
                .commit();

    }

    @Override  // 屏蔽返回键
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exit();

    }
}
