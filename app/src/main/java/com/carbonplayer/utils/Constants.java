package com.carbonplayer.utils;

/**
 * Holds constants shared between activities
 */
public final class Constants {

    public interface ACTION {
        //Actions
        String START_SERVICE = "START_SERVICE";
        String STOP_SERVICE = "STOP_SERVICE";

        String PREVIOUS = "PREVIOUS";
        String PLAYPAUSE = "PLAYPAUSE";
        String NEXT = "NEXT";

        String SEND_QUEUE = "SEND_QUEUE";
    }

    public interface UIACTION {
        int CLICK_PREVIOUS = 0;
        int CLICK_PLAY = 1;
        int CLICK_NEXT = 2;
    }

    public interface KEY {
        //Constant keys for key-value pairs
        String INITITAL_TRACKS = "INITIAL_TRACKS";
    }

    public interface ID {
        int MUSIC_PLAYER_SERVICE = 117;
    }

    public interface MESSAGE {
        int REGISTER_CLIENT = 3;
        int UNREGISTER_CLIENT = 4;
    }

}
