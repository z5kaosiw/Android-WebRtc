package com.pttiot.rtcim.core;

import android.media.AudioFormat;
import android.support.annotation.Nullable;
import android.util.Log;

import com.pttiot.rtcim.util.LogUtils;

import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.voiceengine.WebRtcAudioRecord;

public class RecordedAudioController implements JavaAudioDeviceModule.SamplesReadyCallback {

    private static final String TAG = "RecordedAudioController";
    private static final long MAX_FILE_SIZE_IN_BYTES = 58348800L;
    //Lock
    private final Object lock = new Object();
    //work flag
    private boolean isRunning;

    @Nullable
    private RecordedAudioController saveRecordedAudio;

    public   RecordedAudioController()
    {

    }
    /**
     * Should be called on the same executor thread as the one provided at
     * construction.
     */
    public void  start() {
        Log.d(TAG, "start");
        synchronized (lock) {
            isRunning = true;
        }
    }
    /**
     * Should be called on the same executor thread as the one provided at
     * construction.
     */
    public void stop() {
        Log.d(TAG, "stop");
        synchronized (lock) {
            isRunning = false;
        }
    }
    @Override
    public void onWebRtcAudioRecordSamplesReady(JavaAudioDeviceModule.AudioSamples samples) {

        if (samples.getAudioFormat() != AudioFormat.ENCODING_PCM_16BIT) {
            Log.e(TAG, "Invalid audio format");
            return;
        }

        synchronized (lock) {

            if (!isRunning) {
                return;
            }
            //
            int sampleRate = samples.getSampleRate();
            int channelCount = samples.getChannelCount();
            byte[] data = samples.getData();
            LogUtils.e("音频数据大小:"+data.length);
//            if (rawAudioFileOutputStream == null) {
//                openRawAudioOutputFile(,);
//                fileSizeInBytes = 0;
//            }

        }

    }
}
