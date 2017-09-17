package com.carbonplayer.utils;

/**
 * Holds constants shared between activities and service
 */
public final class Constants {

    public interface ACTION {
        // Actions are sent from the UI to the service
        String START_SERVICE = "START_SERVICE";
        String STOP_SERVICE = "STOP_SERVICE";

        String NEW_QUEUE = "NEW_QUEUE";
        String INSERT_AT_END = "INSERT_AT_END";
        String INSERT_NEXT = "INSERT_NEXT";
        String REORDER = "REORDER";

        String PREVIOUS = "PREVIOUS";
        String PLAYPAUSE = "PLAYPAUSE";
        String NEXT = "NEXT";

        String SEND_QUEUE = "SEND_QUEUE";
    }

    public interface EVENT {
        // Events are sent from the service to the UI
        int NextSong = 0;
        int PrevSong = 1;
        int Play = 2;
        int Pause = 3;
        int SendQueue = 4;
        int BufferProgress = 5;
        int Error = 6;
    }

    public interface KEY {
        // Constant keys for key-value pairs
        String TRACKS = "TRACKS";
        String REORDER_FROM = "REORDER_FROM";
        String REORDER_TO = "REORDER_TO";
    }

    public interface ID {
        int MUSIC_PLAYER_SERVICE = 227266;
    }

    public interface MESSAGE {
        int REGISTER_CLIENT = 3;
        int UNREGISTER_CLIENT = 4;
    }

}
