package com.nini.streamplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "StreamingPlayer";

//    private PlayThread playThread;

    private Intent mStreamPlayerServiceIntent;

    private StreamPlayerService mStreamPlayerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (mStreamPlayerServiceIntent == null) {
            mStreamPlayerServiceIntent = new Intent(this, StreamPlayerService.class);
        }

        Log.i(TAG, "songURL: ****** " + "https://streams.calmradio.com/api/31/128/stream");
        mStreamPlayerServiceIntent.putExtra("url", "https://streams.calmradio.com/api/31/128/stream");
        bindService(mStreamPlayerServiceIntent, channelPlayingServiceConnection, Context.BIND_AUTO_CREATE);

        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        StreamPlayerService.actionPlayPause(MainActivity.this);

//                        if (playThread != null) {w
//                            playThread.stopPlaying();
//                        }
//
//                        playThread = new PlayThread("https://streams.calmradio.com/api/31/128/stream");
//                        playThread.start();
                    }
                }).start();

            }
        });

        findViewById(R.id.pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        StreamPlayerService.actionPlayPause(MainActivity.this);
//                        playThread.pausePlaying();
                    }
                }).start();

            }
        });

        findViewById(R.id.resume).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        StreamPlayerService.actionPlayPause(MainActivity.this);
//                        playThread.resumePlaying();
                    }
                }).start();

            }
        });

        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        StreamPlayerService.actionStop(MainActivity.this);
//                        playThread.stopPlaying();
                    }
                }).start();

            }
        });
    }

    ServiceConnection channelPlayingServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected: channelPlayingServiceConnection");
            mStreamPlayerService = ((StreamPlayerService.LocalBinder) iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
}
