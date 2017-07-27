package com.carbonplayer.model.network.entity;

import com.carbonplayer.model.entity.SongID;
import com.carbonplayer.model.entity.enums.StorageType;
import com.carbonplayer.model.entity.enums.StreamQuality;


public class DownloadRequest {

    public static int PRIORITY_AUTOCACHE = 200;
    public static int PRIORITY_KEEPON = 100;
    public static int PRIORITY_PREFETCH1 = 1;
    public static int PRIORITY_PREFETCH2 = 2;
    public static int PRIORITY_PREFETCH3 = 3;
    public static int PRIORITY_PREFETCH4 = 4;
    public static int PRIORITY_STREAM = 0;

    private SongID id;

    private final StreamQuality existingQuality;
    private final StreamQuality requestedQuality;
    private final long seekMillis;
    private final String trackTitle;
    private FileLocation fileLocation;

    private State state;

    enum State {
        NOT_STARTED,
        DOWNLOADING,
        COMPLETED,
        FAILED,
        CANCELED
    }

    public DownloadRequest(SongID id, String trackTitle, int priority, long seekMillis, FileLocation fileLocation, boolean explicit, StreamQuality requestedQuality, StreamQuality existingQuality) {
        if (trackTitle == null) {
            throw new IllegalArgumentException("The track title is required");
        } else if (seekMillis < 0) {
            throw new IllegalArgumentException("Negative seek time: " + seekMillis);
        } else if (requestedQuality == StreamQuality.UNDEFINED) {
            throw new IllegalArgumentException("Requested quality cannot be unknown");
        } else if (existingQuality == StreamQuality.UNDEFINED || requestedQuality.compareTo(existingQuality) > 0) {
            this.id = id;
            this.trackTitle = trackTitle;
            this.seekMillis = seekMillis;
            this.requestedQuality = requestedQuality;
            this.existingQuality = existingQuality;
            this.fileLocation = fileLocation;
        } else {
            throw new IllegalArgumentException("If existing quality is known, requested quality must exceed it: existing=" + existingQuality + " requested=" + requestedQuality);
        }
    }

    public SongID getId() {
        return this.id;
    }

    public String getTrackTitle() {
        return this.trackTitle;
    }

    public long getSeekMillis() {
        return this.seekMillis;
    }

    public FileLocation getFileLocation(){
        return fileLocation;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DownloadRequest)) {
            return false;
        }
        DownloadRequest downloadRequest = (DownloadRequest) obj;
        if (this.id == downloadRequest.id && this.seekMillis == downloadRequest.getSeekMillis()) {
            return true;
        }
        return false;
    }

    public String toString() {
        return "DownloadRequest{id='" + this.id + '\'' + ", mSourceAccount=" + '\'' + ", trackTitle='" + this.trackTitle + '\'' + ", seekMillis=" + this.seekMillis + "} ";
    }

    public State getState(){
        return state;
    }

    public void setState(State state) {this.state = state;}

    protected int getMinPriority() {
        return PRIORITY_AUTOCACHE;
    }

    protected int getMaxPriority() {
        return PRIORITY_STREAM;
    }

    public StreamQuality getRequestedQuality() {
        return this.requestedQuality;
    }

    public StreamQuality getExistingQuality() {
        return this.existingQuality;
    }
}
