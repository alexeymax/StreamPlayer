///*
//       Licensed to the Apache Software Foundation (ASF) under one
//       or more contributor license agreements.  See the NOTICE file
//       distributed with this work for additional information
//       regarding copyright ownership.  The ASF licenses this file
//       to you under the Apache License, Version 2.0 (the
//       "License"); you may not use this file except in compliance
//       with the License.  You may obtain a copy of the License at
//
//         http://www.apache.org/licenses/LICENSE-2.0
//
//       Unless required by applicable law or agreed to in writing,
//       software distributed under the License is distributed on an
//       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//       KIND, either express or implied.  See the License for the
//       specific language governing permissions and limitations
//       under the License.
//*/
//package com.nini.streamplayer;
//
//import android.media.AudioManager;
//import android.media.MediaPlayer;
//import android.media.MediaPlayer.OnCompletionListener;
//import android.media.MediaPlayer.OnErrorListener;
//import android.media.MediaPlayer.OnPreparedListener;
//import android.media.MediaRecorder;
//import android.os.Environment;
//
//import org.apache.cordova.LOG;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.io.IOException;
//import java.util.LinkedList;
//
///**
// * This class implements the audio playback and recording capabilities used by Cordova.
// * It is called by the AudioHandler Cordova class.
// * Only one file can be played or recorded per class instance.
// *
// * Local audio files must reside in one of two places:
// *      android_asset:      file name must start with /android_asset/sound.mp3
// *      sdcard:             file name is just sound.mp3
// */
//public class AudioPlayer implements OnCompletionListener, OnPreparedListener, OnErrorListener, PlayThread.PlayThreadListener {
//
//    @Override
//    public void onEndOfStrream() {
//        sendStatusChange(MEDIA_ERROR, MEDIA_STREAM_END, null);
//    }
//
//    @Override
//    public void onStartReadSample() {
//        sendStatusChange(MEDIA_ERROR, MEDIA_SAMPLE_START, null);
//    }
//
//    @Override
//    public void onEndReadSample() {
//        sendStatusChange(MEDIA_ERROR, MEDIA_SAMPLE_END, null);
//    }
//
//    @Override
//    public void onError() {
//        sendStatusChange(MEDIA_ERROR, MEDIA_ERR_ABORTED, null);
//    }
//
//    // AudioPlayer modes
//    public enum MODE { NONE, PLAY, RECORD };
//
//    // AudioPlayer states
//    public enum STATE { MEDIA_NONE,
//                        MEDIA_STARTING,
//                        MEDIA_RUNNING,
//                        MEDIA_PAUSED,
//                        MEDIA_STOPPED,
//                        MEDIA_LOADING
//                      };
//
//    private static final String LOG_TAG = "AudioPlayer";
//
//    // AudioPlayer message ids
//    private static int MEDIA_STATE = 1;
//    private static int MEDIA_DURATION = 2;
//    private static int MEDIA_POSITION = 3;
//    private static int MEDIA_ERROR = 9;
//
//    // Media error codes
//    private static int MEDIA_ERR_NONE_ACTIVE    = 0;
//    private static int MEDIA_ERR_ABORTED        = 1;
////    private static int MEDIA_ERR_NETWORK        = 2;
////    private static int MEDIA_ERR_DECODE         = 3;
////    private static int MEDIA_ERR_NONE_SUPPORTED = 4;
//    private static int MEDIA_STREAM_END = 10001;
//
//    private static int MEDIA_SAMPLE_START = 10002;
//    private static int MEDIA_SAMPLE_END = 10002;
//
//    private AudioHandler handler;           // The AudioHandler object
//    private String id;                      // The id of this player (used to identify Media object in JavaScript)
//    private MODE mode = MODE.NONE;          // Playback or Recording mode
//    private STATE state = STATE.MEDIA_NONE; // State of recording or playback
//
//    private String audioFile = null;        // File name to play or record to
//    private float duration = -1;            // Duration of audio
//
//    private MediaRecorder recorder = null;  // Audio recording object
//    private LinkedList<String> tempFiles = null; // Temporary recording file name
//    private String tempFile = null;
//
//    private boolean prepareOnly = true;     // playback after file prepare flag
//    private int seekOnPrepared = 0;     // seek to this location once media is prepared
//
//    private PlayThread player;
//
//    /**
//     * Constructor.
//     *
//     * @param handler           The audio handler object
//     * @param id                The id of this audio player
//     */
//    public AudioPlayer(AudioHandler handler, String id, String file) {
//        this.handler = handler;
//        this.id = id;
//        this.audioFile = file;
//        this.tempFiles = new LinkedList<String>();
//    }
//
//    private String generateTempFile() {
//      String tempFileName = null;
//      if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//          tempFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tmprecording-" + System.currentTimeMillis() + ".3gp";
//      } else {
//          tempFileName = "/data/data/" + handler.cordova.getActivity().getPackageName() + "/cache/tmprecording-" + System.currentTimeMillis() + ".3gp";
//      }
//      return tempFileName;
//    }
//
//    /**
//     * Destroy player and stop audio playing or recording.
//     */
//    public void destroy() {
//        // Stop any play or record
//        if (this.player != null) {
//            if ((this.state == STATE.MEDIA_RUNNING) || (this.state == STATE.MEDIA_PAUSED)) {
//                this.player.stop();
//                this.setState(STATE.MEDIA_STOPPED);
//            }
//            this.player.destroyPlayer();
//            this.player = null;
//        }
//        if (this.recorder != null) {
//            this.stopRecording(true);
//            this.recorder.release();
//            this.recorder = null;
//        }
//    }
//
//    /**
//     * Start recording the specified file.
//     *
//     * @param file              The name of the file
//     */
//    public void startRecording(String file) {
//        switch (this.mode) {
//        case PLAY:
//            LOG.d(LOG_TAG, "AudioPlayer Error: Can't record in play mode.");
//            sendErrorStatus(MEDIA_ERR_ABORTED);
//            break;
//        case NONE:
//            this.audioFile = file;
//            this.recorder = new MediaRecorder();
//            this.recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//            this.recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS); // RAW_AMR);
//            this.recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); //AMR_NB);
//            this.tempFile = generateTempFile();
//            this.recorder.setOutputFile(this.tempFile);
//            try {
//                this.recorder.prepare();
//                this.recorder.start();
//                this.setState(STATE.MEDIA_RUNNING);
//                return;
//            } catch (IllegalStateException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            sendErrorStatus(MEDIA_ERR_ABORTED);
//            break;
//        case RECORD:
//            LOG.d(LOG_TAG, "AudioPlayer Error: Already recording.");
//            sendErrorStatus(MEDIA_ERR_ABORTED);
//        }
//    }
//
//    /**
//     * Save temporary recorded file to specified name
//     *
//     * @param file
//     */
//    public void moveFile(String file) {
//        /* this is a hack to save the file as the specified name */
//
//        if (!file.startsWith("/")) {
//            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//                file = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + file;
//            } else {
//                file = "/data/data/" + handler.cordova.getActivity().getPackageName() + "/cache/" + file;
//            }
//        }
//
//        int size = this.tempFiles.size();
//        LOG.d(LOG_TAG, "size = " + size);
//
//        // only one file so just copy it
//        if (size == 1) {
//            String logMsg = "renaming " + this.tempFile + " to " + file;
//            LOG.d(LOG_TAG, logMsg);
//            File f = new File(this.tempFile);
//            if (!f.renameTo(new File(file))) LOG.e(LOG_TAG, "FAILED " + logMsg);
//        }
//        // more than one file so the user must have pause recording. We'll need to concat files.
//        else {
//          FileOutputStream outputStream = null;
//          try {
//              outputStream = new FileOutputStream(new File(file));
//              FileInputStream inputStream = null;
//              File inputFile = null;
//              for (int i = 0; i < size; i++) {
//                  try {
//                      inputFile = new File(this.tempFiles.get(i));
//                      inputStream = new FileInputStream(inputFile);
//                      copy(inputStream, outputStream, (i>0));
//                  } catch(Exception e) {
//                      LOG.e(LOG_TAG, e.getLocalizedMessage(), e);
//                  } finally {
//                      if (inputStream != null) try {
//                          inputStream.close();
//                          inputFile.delete();
//                          inputFile = null;
//                      } catch (Exception e) {
//                          LOG.e(LOG_TAG, e.getLocalizedMessage(), e);
//                      }
//                  }
//              }
//          } catch(Exception e) {
//              e.printStackTrace();
//          } finally {
//              if (outputStream != null) try {
//                  outputStream.close();
//              } catch (Exception e) {
//                  LOG.e(LOG_TAG, e.getLocalizedMessage(), e);
//              }
//          }
//        }
//    }
//
//    private static long copy(InputStream from, OutputStream to, boolean skipHeader)
//                throws IOException {
//        byte[] buf = new byte[8096];
//        long total = 0;
//        if (skipHeader) {
//            from.skip(6);
//        }
//        while (true) {
//            int r = from.read(buf);
//            if (r == -1) {
//                break;
//            }
//            to.write(buf, 0, r);
//            total += r;
//        }
//        return total;
//    }
//
//    /**
//     * Stop/Pause recording and save to the file specified when recording started.
//     */
//    public void stopRecording(boolean stop) {
//        if (this.recorder != null) {
//            try{
//                if (this.state == STATE.MEDIA_RUNNING) {
//                    this.recorder.stop();
//                }
//                this.recorder.reset();
//                if (!this.tempFiles.contains(this.tempFile)) {
//                    this.tempFiles.add(this.tempFile);
//                }
//                if (stop) {
//                    LOG.d(LOG_TAG, "stopping recording");
//                    this.setState(STATE.MEDIA_STOPPED);
//                    this.moveFile(this.audioFile);
//                } else {
//                    LOG.d(LOG_TAG, "pause recording");
//                    this.setState(STATE.MEDIA_PAUSED);
//                }
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    /**
//     * Resume recording and save to the file specified when recording started.
//     */
//    public void resumeRecording() {
//        startRecording(this.audioFile);
//    }
//
//    //==========================================================================
//    // Playback
//    //==========================================================================
//
//    /**
//     * Start or resume playing audio file.
//     *
//     * @param file              The name of the audio file.
//     */
//    public void startPlaying(String file) {
//        this.readyPlayer(file);
//    }
//
//    /**
//     * Seek or jump to a new time in the track.
//     */
//    public void seekToPlaying(int milliseconds) {}
//
//    /**
//     * Pause playing.
//     */
//    public void pausePlaying() {
//        player.pausePlaying();
//    }
//
//    /**
//     * Stop playing the audio file.
//     */
//    public void stopPlaying() {
//        if (player != null) {
//            player.stopPlaying();
//            player = null;
//        }
//    }
//
//    /**
//     * Resume playing.
//     */
//    public void resumePlaying() {
//        player.resumePlaying();
//    }
//
//    /**
//     * Callback to be invoked when playback of a media source has completed.
//     *
//     * @param player           The MediaPlayer that reached the end of the file
//     */
//    public void onCompletion(MediaPlayer player) {}
//
//    /**
//     * Get current position of playback.
//     *
//     * @return                  position in msec or -1 if not playing
//     */
//    public long getCurrentPosition() {
//        return -1;
//    }
//
//    /**
//     * Determine if playback file is streaming or local.
//     * It is streaming if file name starts with "http://"
//     *
//     * @param file              The file name
//     * @return                  T=streaming, F=local
//     */
//    public boolean isStreaming(String file) {
//        if (file.contains("http://") || file.contains("https://") || file.contains("rtsp://")) {
//            return true;
//        }
//        else {
//            return false;
//        }
//    }
//
//    /**
//      * Get the duration of the audio file.
//      *
//      * @param file             The name of the audio file.
//      * @return                 The duration in msec.
//      *                             -1=can't be determined
//      *                             -2=not allowed
//      */
//    public float getDuration(String file) {
//
//        // Can't get duration of recording
//        if (this.recorder != null) {
//            return (-2); // not allowed
//        }
//
//        // If audio file already loaded and started, then return duration
//        if (this.player != null) {
//            return this.duration;
//        }
//
//        // If no player yet, then create one
//        else {
//            this.prepareOnly = true;
//            this.startPlaying(file);
//
//            // This will only return value for local, since streaming
//            // file hasn't been read yet.
//            return this.duration;
//        }
//    }
//
//    /**
//     * Callback to be invoked when the media source is ready for playback.
//     *
//     * @param player           The MediaPlayer that is ready for playback
//     */
//    public void onPrepared(MediaPlayer player) {}
//
//    /**
//     * By default Android returns the length of audio in mills but we want seconds
//     *
//     * @return length of clip in seconds
//     */
//    private float getDurationInSeconds() {
//        return 0;
//    }
//
//    /**
//     * Callback to be invoked when there has been an error during an asynchronous operation
//     *  (other errors will throw exceptions at method call time).
//     *
//     * @param player           the MediaPlayer the error pertains to
//     * @param arg1              the type of error that has occurred: (MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_SERVER_DIED)
//     * @param arg2              an extra code, specific to the error.
//     */
//    public boolean onError(MediaPlayer player, int arg1, int arg2) {
//        return true;
//    }
//
//    /**
//     * Set the state and send it to JavaScript.
//     *
//     * @param state
//     */
//    private void setState(STATE state) {
//        if (this.state != state) {
//            sendStatusChange(MEDIA_STATE, null, (float)state.ordinal());
//        }
//        this.state = state;
//    }
//
//    /**
//     * Set the mode and send it to JavaScript.
//     *
//     * @param mode
//     */
//    private void setMode(MODE mode) {
//        if (this.mode != mode) {
//            //mode is not part of the expected behavior, so no notification
//            //this.handler.webView.sendJavascript("cordova.require('cordova-plugin-media.Media').onStatus('" + this.id + "', " + MEDIA_STATE + ", " + mode + ");");
//        }
//        this.mode = mode;
//    }
//
//    /**
//     * Get the audio state.
//     *
//     * @return int
//     */
//    public int getState() {
//        return this.state.ordinal();
//    }
//
//    /**
//     * Set the volume for audio player
//     *
//     * @param volume
//     */
//    public void setVolume(float volume) {
//        player.setVolume(volume);
//    }
//
//    /**
//     * attempts to put the player in play mode
//     * @return true if in playmode, false otherwise
//     */
//    private boolean playMode() {
//        return true;
//    }
//
//    /**
//     * attempts to initialize the media player for playback
//     * @param file the file to play
//     * @return false if player not ready, reports if in wrong mode or state
//     */
//    private boolean readyPlayer(final String file) {
//        if (playMode()) {
//            stopPlaying();
//
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    player = new PlayThread(file);
//                    player.start();
//                    player.setListener(AudioPlayer.this);
//                }
//            }).start();
//
//
//
//        }
//        return true;
//    }
//
//
//    private void sendErrorStatus(int errorCode) {
//        sendStatusChange(MEDIA_ERROR, errorCode, null);
//    }
//
//    private void sendStatusChange(int messageType, Integer additionalCode, Float value) {
//
//        if (additionalCode != null && value != null) {
//            throw new IllegalArgumentException("Only one of additionalCode or value can be specified, not both");
//        }
//
//        JSONObject statusDetails = new JSONObject();
//        try {
//            statusDetails.put("id", this.id);
//            statusDetails.put("msgType", messageType);
//            if (additionalCode != null) {
//                JSONObject code = new JSONObject();
//                code.put("code", additionalCode.intValue());
//                statusDetails.put("value", code);
//            }
//            else if (value != null) {
//                statusDetails.put("value", value.floatValue());
//            }
//        } catch (JSONException e) {
//            LOG.e(LOG_TAG, "Failed to create status details", e);
//        }
//
//        this.handler.sendEventMessage("status", statusDetails);
//    }
//
//    /**
//     * Get current amplitude of recording.
//     *
//     * @return amplitude or 0 if not recording
//     */
//    public float getCurrentAmplitude() {
//        if (this.recorder != null) {
//            try{
//                if (this.state == STATE.MEDIA_RUNNING) {
//                    return (float) this.recorder.getMaxAmplitude() / 32762;
//                }
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        return 0;
//    }
//}
