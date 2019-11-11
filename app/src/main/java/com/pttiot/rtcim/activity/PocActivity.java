package com.pttiot.rtcim.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.pttiot.rtcim.App;
import com.pttiot.rtcim.R;
import com.pttiot.rtcim.core.PeerConnectionHelper;
import com.pttiot.rtcim.core.interfaces.IViewCallback;
import com.pttiot.rtcim.core.parm.PeerConfiger;
import com.pttiot.rtcim.core.parm.SessionType;
import com.pttiot.rtcim.core.parm.SignalType;
import com.pttiot.rtcim.eventbus.IoPttEvent;
import com.pttiot.rtcim.util.LogUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;

import java.util.ArrayList;
import java.util.List;

public class PocActivity extends AppCompatActivity implements IViewCallback {

    private int talkstatu = SessionType.IDLE_PTT;
    private ListView mList;
    private Button talkBtn;
    private PeerConnectionHelper _peerHelper;
    private String rid;
    private AudioTrack _localAudioTrack;
    private ListAdapter adapter;
    protected ArrayList<String> _connectionIdArray;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poc);
        EventBus.getDefault().register(this);
        rid = getIntent().getStringExtra(PeerConfiger.ROOMID);
        initViews();
        initSeesion();
    }

    private void initSeesion() {

        _peerHelper =new PeerConnectionHelper(this,this,false);
        _peerHelper.joinRoom(rid,SignalType.PTT_COMMAND);
        toggleMic(false);
        toggleSpeaker(true);
        _connectionIdArray =new ArrayList<>();
        adapter = new ListAdapter();
        mList.setAdapter(adapter);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initViews() {

        mList = (ListView) findViewById(R.id.list);
        talkBtn = (Button) findViewById(R.id.talkBtn);
        talkBtn.setOnTouchListener(new  View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN ){

                    //1:判断当前的状态
                    if(talkstatu == SessionType.TALKING_PTT || talkstatu == SessionType.REMOTE_TALKING_PTT)
                    {
                        LogUtils.e("status   is not idel");
                        return false;
                    }
                    if (talkstatu == SessionType.IDLE_PTT)
                         //sengd single   to  server
                         _peerHelper.sendPttSignal(SessionType.REQUEST_PTT);

                     talkBtn.setText(getString(R.string.ptt_3));
                }else  if (motionEvent.getAction() == MotionEvent.ACTION_UP){


                    if(talkstatu == SessionType.TALKING_PTT || talkstatu == SessionType.REMOTE_TALKING_PTT)
                    {
                        _peerHelper.sendPttSignal(SessionType.IDLE_PTT);
                        LogUtils.d("touch up event   if ");
                        return false;
                    }
                        LogUtils.d("touch up event   else ");
                }

                return false;
            }
        });
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
    /*EvebtBus call back**/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void Event(IoPttEvent ioPttEvent) {
        String user =   ioPttEvent.rid;
        switch (  ioPttEvent.status )
        {

            case  SessionType.IDLE_PTT:
                LogUtils.d("Event ->  SessionType.IDLE_PTT  + user="+user);
                talkstatu = SessionType.IDLE_PTT;
                if(user == _peerHelper.getMyid()){
                    //myself  to  relese mic
                    App.getApplication().getGlobHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            toggleMic(false);
                        }
                    },300);
                }else
                {
                    //close   sppeeker
                    _localAudioTrack.setEnabled(false);
                }
                talkBtn.setText(getString(R.string.ptt_1));
                break;
            case  SessionType.REMOTE_TALKING_PTT:
                LogUtils.d("Event ->  SessionType.REMOTE_TALKING_PTT  + user="+user);
                talkstatu =  SessionType.REMOTE_TALKING_PTT;
                //open  speeker
                _localAudioTrack.setEnabled(true);
                talkBtn.setText(getString(R.string.ptt_4));
                break;
            case  SessionType.REQUEST_PTT:

                break;
            case  SessionType.REQUEST_SUCCESS:

                LogUtils.d("Event ->  SessionType.REQUEST_SUCCESS  + user="+user);
                //talking
               talkstatu =  SessionType.TALKING_PTT;
               //open mic
                toggleMic(true);
                talkBtn.setText(getString(R.string.ptt_2));
            break;

        }


    }

    //----------------ICE CALL BACK---------------------------------
    @Override
    public void onSetLocalStream(MediaStream stream, String socketId) {
        List<AudioTrack> audioTracks = stream.audioTracks;
        if (audioTracks.size() > 0) {
            _localAudioTrack = audioTracks.get(0);
        }
        _localAudioTrack.setEnabled(false);

        _connectionIdArray.add(socketId+"(本机)");
        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    @Override
    public void onAddRemoteStream(MediaStream stream, String socketId) {

        _connectionIdArray.add(socketId);

        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    @Override
    public void onCloseWithId(String socketId) {

        _connectionIdArray.remove(socketId);
        runOnUiThread(() -> adapter.notifyDataSetChanged());

    }

    @Override
    public void onCreatePeerConnectionError(String error) {

    }
    private void exit() {
        //退出房间操作
        if (_peerHelper != null)
        {
            _peerHelper.exit(rid);
            _peerHelper.peerRelese();
            _peerHelper= null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        exit();
    }

    //------------------------------------------------------------------------------------------------------------------
    public  class ListAdapter extends BaseAdapter {


        @Override
        public int getCount ( ) {
            return  _connectionIdArray.size();
        }

        @Override
        public Object getItem ( int position ) {
            return null;
        }

        @Override
        public long getItemId ( int position ) {
            return 0;
        }

        @Override
        public View getView ( int position, View convertView, ViewGroup parent ) {

            ViewHolder holder = null;
            if ( convertView == null )
            {
                convertView =  LayoutInflater.from ( PocActivity.this ).inflate ( R.layout.item ,null);

                holder =new ViewHolder ();
                holder.name  =  convertView.findViewById ( R.id.userTv );
                convertView.setTag ( holder );

            }else
            {
                holder = (ViewHolder) convertView.getTag ();
            }

            holder.name.setText ( _connectionIdArray.get(position) );

            return convertView;
        }
    }

    class  ViewHolder
    {
        TextView     name;

    }

}
