package com.nini.streamplayer;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by alexey on 2018-04-13.
 */

public class StreamPlayerService  extends Service implements Handler.Callback {
    private static final String TAG = "StreamPlayerService";

    private static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    private static final String ACTION_STOP = "ACTION_STOP";


    private LocalBinder mLocalBinder = new LocalBinder();

    private AudioTrack mAudioTrack;

    private PlayThread mPlayThread;
    public boolean stopPlayback = true;

    public boolean flag;

    String url = "https://streams.calmradio.com/api/31/128/stream";
    String stopService = null;

    public boolean isAnyActivityBound;

    private AudioManager mAudioManager;

    private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener;

    private ConnectivityManager connectivityManager;

    public static void actionPlayPause(Context cxt) {
        Intent i = new Intent(cxt, StreamPlayerService.class);
        i.setAction(ACTION_PLAY_PAUSE);
        cxt.startService(i);
    }

    public static void actionStop(Context cxt) {
        Intent i = new Intent(cxt, StreamPlayerService.class);
        i.setAction(ACTION_STOP);
        cxt.startService(i);
    }

    @Override
    public boolean handleMessage(Message message) {
        return false;
    }


    public class LocalBinder extends Binder {
        public StreamPlayerService getService() {
            return StreamPlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind called");
        isAnyActivityBound = true;
        return mLocalBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // onUnbind is only called when all clients have disconnected
        Log.i(TAG, "onUnbind called");
        isAnyActivityBound = false;

        return true; // ensures onRebind is called
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, "onRebind is called");
        isAnyActivityBound = true;


    }

    @Override
    public void onCreate() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
    }

    private boolean isConnected() {
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean isWifiConn = networkInfo.isConnected();
        networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean isMobileConn = networkInfo.isConnected();

        return isWifiConn || isMobileConn;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Log.i(TAG, "onStartCommand called. intent.getType: " + intent.getType());
            Log.i(TAG, "onStartCommand called. action: " + intent.getAction());
        }

        connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        //mUserData = UserData.getInstance(getApplicationContext());

        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                8192 * 2,
                AudioTrack.MODE_STREAM);
        mAudioTrack.setStereoVolume(1, 1);

//        stopPlayback = false;
        flag = true;

        if (intent == null) {
            this.stopSelf();
        } else {

            String action = intent.getAction();

            if (action != null) {
                if (action.equalsIgnoreCase(ACTION_PLAY_PAUSE)) {
                    if (intent.hasExtra("url")) {
                        url = intent.getExtras().getString("url", null);
                    }

                    if (!stopPlayback) {
                        stop();
                    } else {
                        stopPlayback = false;
                        flag = true;
                        addAudioFocusListener();

                        play(url);
                    }

                } else if (action.equalsIgnoreCase(ACTION_STOP)) {
                    stopForeground(true);
                    this.stopSelf();
                    stop();
                }
//
            } else {

                url = intent.getExtras().getString("url", null);
                Log.i(TAG, "url in onStartCommand:" + url);
                stopService = intent.getExtras().getString("stopService", null);

                if (stopService != null) {
                    if (stopService.equalsIgnoreCase("true")) {
                        Log.i(TAG, "stopService TRUE, SPS stopping itself");
                        this.stopSelf();
                        Log.i(TAG, "11111111111111111111111");
                    }
                } else {


                }
            }
        }

        return START_NOT_STICKY;
    }


    private void addAudioFocusListener() {
//        if (onAudioFocusChangeListener != null) {
//            return;
//        }
        onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int i) {
                switch (i) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        // Set Volume level to desired levels
                        if (mAudioTrack != null)
                            mAudioTrack.setStereoVolume(1, 1);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                        //You have audio focus for shot time
                        if (mAudioTrack != null)
                            mAudioTrack.setStereoVolume(1, 1);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                        //Play over existing audio
                        if (mAudioTrack != null)
                            mAudioTrack.setStereoVolume(1, 1);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        //lost the focus stop
                        if (mAudioTrack != null)
                            mAudioTrack.setStereoVolume(0.0f, 0.0f);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        // Temporary loss of audio focus -expect to get it back- you can keep your resources around
                        if (mAudioTrack != null)
                            mAudioTrack.setStereoVolume(0.0f, 0.0f);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        // Lower the volume
                        if (mAudioTrack != null)
                            mAudioTrack.setStereoVolume(0.0f, 0.0f);
                        break;
                }
            }
        };
    }



    public void play(String url) {
        Log.i(TAG, "burak: url passed to play in SPS: " + url);
        new PlayMusic().execute(url);

    }

    public boolean handleMessage(Notification.MessagingStyle.Message msg) {
        Log.i(TAG, "handleMessage called");

        return true;
    }

    public void pause() {
        mAudioTrack.pause();
    }

    @Override
    public void onDestroy() {
		Log.i(TAG, "onDestroy called");
        stop();

        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        }

        super.onDestroy();
    }


    public void stop() {
        if (mPlayThread != null) {
            mPlayThread.interrupt();
        }
        mPlayThread = null;
        stopPlayback = true;
        flag = true;
    }

    public void setVolume(float vol) {
        if (mAudioTrack != null) {
            mAudioTrack.setStereoVolume(vol, vol);
        } else {
//            Crashlytics.log(1, TAG, "mAudioTrack NULL");
        }
    }


    private class PlayThread extends Thread {

        MediaExtractor extractor;
        MediaCodec codec;
        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;
        MediaFormat format = null;

        boolean sawInputEOS = false;
        // boolean sawOutputEOS = false;

        public PlayThread(String url) {
            Log.i(TAG, "PlayThread url: " + url);

            extractor = new MediaExtractor();
            try {
                extractor.setDataSource(url);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            String mime = null;
            try {
                format = extractor.getTrackFormat(0);
                mime = format.getString(MediaFormat.KEY_MIME);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (format == null || !mime.startsWith("audio/")) {
                return;
            }

            try {
                codec = MediaCodec.createDecoderByType(mime);
            } catch (Exception e) {
                e.printStackTrace();
            }
            codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
            codec.start();
            codecInputBuffers = codec.getInputBuffers();
            codecOutputBuffers = codec.getOutputBuffers();

            extractor.selectTrack(0); // <= You must select a track. You will read samples from the media from this track!
        }

        @Override
        public void run() {
            try {
                while (!stopPlayback) {
                    if (!isConnected()) {
                        stopPlayback = true;
                        StreamPlayerService.this.stopSelf();
                    } else {
                        int inputBufIndex = codec.dequeueInputBuffer(2000);
                        if (inputBufIndex >= 0) {
                            ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                            int sampleSize = extractor.readSampleData(dstBuf, 0);
                            long presentationTimeUs = 0;
                            if (sampleSize < 0) {
                                sawInputEOS = true;
                                sampleSize = 0;
                            } else {
                                presentationTimeUs = extractor.getSampleTime();
                            }

                            codec.queueInputBuffer(inputBufIndex,
                                    0, //offset
                                    sampleSize,
                                    presentationTimeUs,
                                    sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                            if (!sawInputEOS) {
                                extractor.advance();
                            }

                            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                            final int res = codec.dequeueOutputBuffer(info, 2000);
                            if (res >= 0) {
                                int outputBufIndex = res;
                                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                                final byte[] chunk = new byte[info.size];
                                buf.get(chunk);
                                buf.clear();

                                if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {

                                }

                                mAudioTrack.play();
                                if (chunk.length > 0) {
                                    mAudioTrack.write(chunk, 0, chunk.length);
                                }
                                codec.releaseOutputBuffer(outputBufIndex, false /* render */);

//		                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//		                            sawOutputEOS = true;
//		                        }
                            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                                codecOutputBuffers = codec.getOutputBuffers();
                            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                final MediaFormat oformat = codec.getOutputFormat();
                                Log.d(TAG, "Output format has changed to " + oformat);
                                mAudioTrack.setPlaybackRate(oformat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                if (ex != null) {
                    Log.e(TAG, ex.getClass().toString());
                }
            } finally {
                try {
                    if (mAudioTrack != null) {
                        mAudioTrack.pause();
                        mAudioTrack.flush();
                        mAudioTrack.release();
                        mAudioTrack = null;

                        extractor.release();
                        extractor = null;

                        if (codec != null) {
                            codec.release();
                            codec = null;
                        } else {
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    boolean isSongLoading = false;

    class PlayMusic extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isSongLoading = true;
            Log.d(TAG, "onPreExecute: loading PlayBack");

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            isSongLoading = false;
            Log.d(TAG, "onPreExecute: Playing PlayBack");
        }

        @Override
        protected Void doInBackground(String... strings) {
            mPlayThread = new PlayThread(url);
            mPlayThread.start();
            return null;
        }
    }

}

