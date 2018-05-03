package com.nini.streamplayer;

import android.app.Application;
import android.util.Log;

import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.player.VideoCastController;

/**
 * Created by alexey on 2018-04-16.
 */

public class CalmRadioApplication  extends Application {

    public static final double VOLUME_INCREMENT = 0.05;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("", "<calm> onCreate");


        // initialize VideoCastManager
        VideoCastManager.
                initialize(this, "E262551C", null, "urn:x-cast:com.calmradio").
                setVolumeStep(VOLUME_INCREMENT).
                enableFeatures(VideoCastManager.FEATURE_NOTIFICATION |
                        VideoCastManager.FEATURE_LOCKSCREEN |
                        VideoCastManager.FEATURE_WIFI_RECONNECT |
                        VideoCastManager.FEATURE_CAPTIONS_PREFERENCE |
                        VideoCastManager.FEATURE_DEBUGGING);

        // this is the default behavior but is mentioned to make it clear that it is configurable.
        VideoCastManager.getInstance().setNextPreviousVisibilityPolicy(
                VideoCastController.NEXT_PREV_VISIBILITY_POLICY_DISABLED);

        // this is the default behavior but is mentioned to make it clear that it is configurable.
        VideoCastManager.getInstance().setCastControllerImmersive(true);

    }
}
