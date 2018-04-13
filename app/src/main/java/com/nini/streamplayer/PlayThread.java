package com.nini.streamplayer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by alexey on 2018-03-30.
 */

public class PlayThread extends Thread {
    private static final String TAG = "StreamingPlayer";

    public interface PlayThreadListener {
        void onEndOfStrream();
        void onStartReadSample();
        void onEndReadSample();
        void onError();
    }

    MediaExtractor extractor;
    MediaCodec codec;
    ByteBuffer[] codecInputBuffers;
    ByteBuffer[] codecOutputBuffers;
    MediaFormat format = null;

    boolean sawInputEOS = false;

    public volatile boolean stopPlayback = false;
    public volatile boolean pausePlayback = false;

    private AudioTrack mAudioTrack;
    // boolean sawOutputEOS = false;

    public PlayThreadListener getListener() {
        return listener;
    }

    public void setListener(PlayThreadListener listener) {
        this.listener = listener;
    }

    private PlayThreadListener listener;



    public PlayThread(String url) {
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
            Log.e(TAG, e.getMessage());
        }

        if (format == null || !mime.startsWith("audio/")) {
            Log.e(TAG, " format == null || !mime.startsWith(\"audio/\")");
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

        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                8192 * 2,
                AudioTrack.MODE_STREAM);
    }

    @Override
    public void run() {
        try {
            while (!stopPlayback) {

                if (pausePlayback) {
                    try {
                        Thread.sleep(200);
                        continue;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

//                Log.d(TAG, "step_1");

                int inputBufIndex = codec.dequeueInputBuffer(2000);

//                Log.d(TAG, "step_2");

//                Log.d(TAG, "inputBufIndex " + inputBufIndex);

                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                    if (listener != null) {
                        Log.d(TAG, "onStartReadSample ");
                        listener.onStartReadSample();
                    }

                    int sampleSize = extractor.readSampleData(dstBuf, 0);

                    if (listener != null) {
                        Log.d(TAG, "onStartReadSample ");
                        listener.onEndReadSample();
                    }

//                    Log.d(TAG, "step_4");
                    long presentationTimeUs = 0;
                    if (sampleSize < 0) {
                        sawInputEOS = true;
                        sampleSize = 0;
                        if(listener != null) {
                            Log.d(TAG, "onEndOfStrream ");
                            listener.onEndOfStrream();
                        }
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

//                    Log.d(TAG, "res " + res);

                    if (res >= 0) {
                        int outputBufIndex = res;
                        ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                        final byte[] chunk = new byte[info.size];
                        buf.get(chunk);
                        buf.clear();

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
        catch (Exception e) {
            if (listener != null) {
                listener.onError();
            }

            e.printStackTrace();
        }
        finally {
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
                        Log.e(TAG, " codec is null");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Pause playing.
     */
    public void pausePlaying() {
        try {
            pausePlayback = true;
            mAudioTrack.pause();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop playing the audio file.
     */
    public void stopPlaying() {
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
                    Log.e(TAG, " codec is null");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        stopPlayback = true;
    }

    /**
     * Resume playing.
     */
    public void resumePlaying() {
        try {
            pausePlayback = false;
            mAudioTrack.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setVolume(float volume) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                mAudioTrack.setVolume(volume);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void destroyPlayer() {
        stopPlaying();
    }
}